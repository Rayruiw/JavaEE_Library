package com.wangrui.librarymanagement.resources;

import com.wangrui.librarymanagement.entity.BookCopy;
import com.wangrui.librarymanagement.entity.BookCopyStatus;
import com.wangrui.librarymanagement.entity.Borrower;
import com.wangrui.librarymanagement.entity.BorrowingRecord;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.util.List;

@Path("borrowing")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BorrowingResource {

    @PersistenceContext(unitName = "LibraryPU")
    private EntityManager em;

    // ========== 1. 借书 ==========
    @POST
    @Path("borrow")
    @Transactional
    public Response borrowBook(BorrowRequest req) {

        // 1) 找到 BookCopy
        BookCopy copy = em.find(BookCopy.class, req.bookCopyId);
        if (copy == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Book copy not found")
                    .build();
        }

        // 2) 检查当前状态是否可借
        if (copy.getStatus() == null ||
                !"AVAILABLE".equals(copy.getStatus().getCode())) {

            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Book copy is not available")
                    .build();
        }

        // 3) 找到 Borrower
        Borrower borrower = em.find(Borrower.class, req.borrowerId);
        if (borrower == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Borrower not found")
                    .build();
        }

        // 4) 计算日期：借出当天 + 借阅天数（默认 14 天）
        LocalDate borrowDate = LocalDate.now();
        int days = (req.days == null || req.days <= 0) ? 14 : req.days;
        LocalDate dueDate = borrowDate.plusDays(days);

        // 5) 创建 BorrowingRecord
        BorrowingRecord record = new BorrowingRecord();
        record.setBorrowDate(borrowDate);
        record.setDueDate(dueDate);
        record.setBookCopy(copy);
        record.setBorrower(borrower);

        em.persist(record);

        // 6) 更新 BookCopy 状态为 BORROWED
        BookCopyStatus borrowedStatus = findStatusByCode("BORROWED");
        if (borrowedStatus == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Status BORROWED not configured")
                    .build();
        }
        copy.setStatus(borrowedStatus);
        em.merge(copy);

        return Response.status(Response.Status.CREATED)
                .entity(new BorrowingResponse(record))
                .build();
    }

    // ========== 2. 还书 ==========
    @POST
    @Path("return")
    @Transactional
    public Response returnBook(ReturnRequest req) {

        // 这里用 recordId 来定位借阅记录
        BorrowingRecord record = em.find(BorrowingRecord.class, req.recordId);
        if (record == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Borrowing record not found")
                    .build();
        }

        if (record.getReturnDate() != null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("This record is already returned")
                    .build();
        }

        // 1) 设置归还日期为今天
        LocalDate returnDate = LocalDate.now();
        record.setReturnDate(returnDate);

        // 2) 更新 BookCopy 状态回 AVAILABLE
        BookCopy copy = record.getBookCopy();
        if (copy == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Book copy missing in record")
                    .build();
        }

        BookCopyStatus availableStatus = findStatusByCode("AVAILABLE");
        if (availableStatus == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Status AVAILABLE not configured")
                    .build();
        }

        copy.setStatus(availableStatus);
        em.merge(copy);

        // 3) 记录本身的修改 JPA 会自动 flush
        return Response.ok(new BorrowingResponse(record)).build();
    }

    // ========== 3. 查询所有借阅记录（管理员用） ==========
    @GET
    public List<BorrowingResponse> getAll() {
        List<BorrowingRecord> list = em.createQuery(
                "SELECT r FROM BorrowingRecord r ORDER BY r.borrowDate DESC",
                BorrowingRecord.class
        ).getResultList();

        return list.stream()
                .map(BorrowingResponse::new)
                .toList();
    }
    
    // ========== 4. 查看某个借阅者当前未归还的记录 ==========
    @GET
    @Path("current")
    public List<BorrowingResponse> getCurrentByBorrower(@QueryParam("borrowerId") Long borrowerId) {
        if (borrowerId == null) {
            throw new BadRequestException("borrowerId is required");
        }

        List<BorrowingRecord> list = em.createQuery(
                "SELECT r FROM BorrowingRecord r " +
                "WHERE r.borrower.id = :bid AND r.returnDate IS NULL " +
                "ORDER BY r.borrowDate DESC",
                BorrowingRecord.class
        )
        .setParameter("bid", borrowerId)
        .getResultList();

        return list.stream()
                .map(BorrowingResponse::new)
                .toList();
    }

    // ========== 5. 查看某个借阅者的全部历史记录 ==========
    @GET
    @Path("history")
    public List<BorrowingResponse> getHistoryByBorrower(@QueryParam("borrowerId") Long borrowerId) {
        if (borrowerId == null) {
            throw new BadRequestException("borrowerId is required");
        }

        List<BorrowingRecord> list = em.createQuery(
                "SELECT r FROM BorrowingRecord r " +
                "WHERE r.borrower.id = :bid " +
                "ORDER BY r.borrowDate DESC",
                BorrowingRecord.class
        )
        .setParameter("bid", borrowerId)
        .getResultList();

        return list.stream()
                .map(BorrowingResponse::new)
                .toList();
    }

 
    // ========== 工具方法：按 code 查状态 ==========
    private BookCopyStatus findStatusByCode(String code) {
        TypedQuery<BookCopyStatus> q = em.createQuery(
                "SELECT s FROM BookCopyStatus s WHERE s.code = :code",
                BookCopyStatus.class
        );
        q.setParameter("code", code);
        List<BookCopyStatus> list = q.getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    // ========== DTO 类，用于请求 / 响应 ==========
    public static class BorrowRequest {
        public Long borrowerId;
        public Long bookCopyId;
        public Integer days;   // 借多少天，可选，默认 14
    }

    public static class ReturnRequest {
        public Long recordId;  // 要归还的那条借阅记录的 id
    }

    public static class BorrowingResponse {
        public Long id;
        public Long borrowerId;
        public String borrowerEmail;
        public Long bookCopyId;
        public Long bookId;
        public String bookTitle;
        public String statusCode;
        public String statusName;
        public String borrowDate;
        public String dueDate;
        public String returnDate;

        public BorrowingResponse(BorrowingRecord r) {
            this.id = r.getId();

            if (r.getBorrower() != null) {
                this.borrowerId = r.getBorrower().getId();
                this.borrowerEmail = r.getBorrower().getEmail();
            }

            if (r.getBookCopy() != null) {
                this.bookCopyId = r.getBookCopy().getId();
                if (r.getBookCopy().getBook() != null) {
                    this.bookId = r.getBookCopy().getBook().getId();
                    this.bookTitle = r.getBookCopy().getBook().getTitle();
                }
                if (r.getBookCopy().getStatus() != null) {
                    this.statusCode = r.getBookCopy().getStatus().getCode();
                    this.statusName = r.getBookCopy().getStatus().getName();
                }
            }

            this.borrowDate = r.getBorrowDate() == null ? null : r.getBorrowDate().toString();
            this.dueDate = r.getDueDate() == null ? null : r.getDueDate().toString();
            this.returnDate = r.getReturnDate() == null ? null : r.getReturnDate().toString();
        }
    }
}