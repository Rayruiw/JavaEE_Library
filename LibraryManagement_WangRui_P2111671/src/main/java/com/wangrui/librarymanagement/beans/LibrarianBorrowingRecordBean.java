package com.wangrui.librarymanagement.beans;

import com.wangrui.librarymanagement.entity.BookCopy;
import com.wangrui.librarymanagement.entity.BookCopyStatus;
import com.wangrui.librarymanagement.entity.BorrowingRecord;
import com.wangrui.librarymanagement.entity.Librarian;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Named
@SessionScoped
public class LibrarianBorrowingRecordBean implements Serializable {

    @PersistenceContext(unitName = "LibraryPU")
    private EntityManager em;
    private Librarian currentLibrarian;

    // ================== 视图模式：BORROW / RETURN ==================
    private String viewMode = "BORROW";   // 默认显示借书申请

    // ================== 分页字段（只用于借书申请） ==================
    private int pageSize = 10;
    private int currentPage = 0;

    // ================== 还书申请 ==================
    private List<BorrowingRecord> returnRequests;
    private Long actionRecordId;

    // 每条记录单独保存“新副本状态”
    // key: BorrowingRecord.id, value: "AVAILABLE"/"DAMAGED"/"LOST"
    private Map<Long, String> returnStatusMap = new HashMap<>();

    // ================== 登录保护 ==================
    @PostConstruct
    public void init() {
        viewMode = "BORROW";
    }
    
    // ================== 登录保护（给 preRenderView 调用） ==================
    public void ensureLoggedIn() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) {
            return; // 理论上不会发生，在 JSF 请求里一定有
        }

        var ec = ctx.getExternalContext();
        Map<String, Object> sessionMap = ec.getSessionMap();
        Object obj = sessionMap.get("loggedInLibrarian");

        // 未登录：重定向到馆员登录页
        if (!(obj instanceof Librarian)) {
            try {
                ec.redirect(ec.getRequestContextPath() + "/librarian-login.xhtml");
                ctx.responseComplete();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // 已登录：保证 currentLibrarian 与 session 中一致，并从数据库刷新一次
        Librarian sessionLib = (Librarian) obj;
        if (currentLibrarian == null
                || currentLibrarian.getId() == null
                || !currentLibrarian.getId().equals(sessionLib.getId())) {

            currentLibrarian = em.find(Librarian.class, sessionLib.getId());
        }
    }


    // ============================================================
    //                  getter / setter
    // ============================================================

    public Librarian getCurrentLibrarian() {
        return currentLibrarian;
    }

    public String getViewMode() {
        return viewMode;
    }

    public void setViewMode(String viewMode) {
        this.viewMode = viewMode;
    }

    public Long getActionRecordId() {
        return actionRecordId;
    }

    public void setActionRecordId(Long actionRecordId) {
        this.actionRecordId = actionRecordId;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public Map<Long, String> getReturnStatusMap() {
        return returnStatusMap;
    }

    public void setReturnStatusMap(Map<Long, String> returnStatusMap) {
        this.returnStatusMap = returnStatusMap;
    }

    // ============================================================
    //                  借书申请分页查询
    // ============================================================

    public long getTotalPendingCount() {
        Long cnt = em.createQuery(
                "SELECT COUNT(r) FROM BorrowingRecord r WHERE r.status = 'REQUESTED_BORROW'",
                Long.class
        ).getSingleResult();
        return (cnt == null) ? 0 : cnt;
    }

    public int getTotalPages() {
        long total = getTotalPendingCount();
        return total == 0 ? 1 : (int) ((total + pageSize - 1) / pageSize);
    }

    public List<BorrowingRecord> getPendingRecords() {

        int totalPages = getTotalPages();
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;

        return em.createQuery(
                "SELECT r FROM BorrowingRecord r " +
                        "JOIN FETCH r.borrower br " +
                        "JOIN FETCH r.bookCopy c " +
                        "JOIN FETCH c.book b " +
                        "WHERE r.status = 'REQUESTED_BORROW' " +
                        "ORDER BY r.requestDate DESC",
                BorrowingRecord.class
        )
                .setFirstResult(currentPage * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
    }

    public String nextPage() {
        if (currentPage + 1 < getTotalPages()) currentPage++;
        return null;
    }

    public String prevPage() {
        if (currentPage > 0) currentPage--;
        return null;
    }

    // ============================================================
    //                  借书审批
    // ============================================================

    @Transactional
    public String approveBorrow(Long recordId) {
        BorrowingRecord record = em.find(BorrowingRecord.class, recordId);
        if (record == null) {
            addMessage("Borrowing record not found.");
            return null;
        }

        // 只允许对 REQUESTED_BORROW 状态审批
        if (!"REQUESTED_BORROW".equals(record.getStatus())) {
            addMessage("This record is not in REQUESTED_BORROW status.");
            return null;
        }

        BookCopy copy = record.getBookCopy();

        // 要求副本当前是 PENDING
        if (!"PENDING".equalsIgnoreCase(copy.getStatus().getCode())) {
            addMessage("Book copy is not in PENDING status.");
            return null;
        }

        // 更新借书记录：设置借书日期、到期日期、状态
        LocalDate now = LocalDate.now();
        record.setBorrowDate(now);
        record.setDueDate(now.plusDays(14));
        record.setStatus("BORROWED");

        // 更新副本状态为 BORROWED
        BookCopyStatus borrowedStatus = findStatusByCode("BORROWED");
        copy.setStatus(borrowedStatus);

        addMessage("Borrow request approved.");
        return null;
    }
    
    
    
    // ============================================================
//                  借书审批 - Reject
// ============================================================
    @Transactional
    public String rejectBorrow(Long recordId) {
        BorrowingRecord record = em.find(BorrowingRecord.class, recordId);
        if (record == null) {
            addMessage("Borrowing record not found.");
            return null;
        }

        if (!"REQUESTED_BORROW".equals(record.getStatus())) {
            addMessage("This record is not in REQUESTED_BORROW status.");
            return null;
        }

        BookCopy copy = record.getBookCopy();

        // 把副本状态改回 AVAILABLE
        BookCopyStatus availableStatus = findStatusByCode("AVAILABLE");
        if (availableStatus == null) {
            addMessage("Status AVAILABLE not found.");
            return null;
        }
        copy.setStatus(availableStatus);

        // 申请失败：标记为 INCOMPLETE
        record.setStatus("INCOMPLETE");

        addMessage("Borrow request rejected.");
        return null;
    }

    // ============================================================
    //                  还书审批（AVAILABLE / DAMAGED / LOST）
    // ============================================================

    public List<BorrowingRecord> getReturnRequests() {
        returnRequests = em.createQuery(
                "SELECT r FROM BorrowingRecord r " +
                        "JOIN FETCH r.bookCopy c " +
                        "JOIN FETCH c.book b " +
                        "JOIN FETCH r.borrower u " +
                        "WHERE r.status = 'RETURN_REQUESTED' " +
                        "ORDER BY r.returnRequestDate ASC",
                BorrowingRecord.class
        ).getResultList();
        return returnRequests;
    }

    @Transactional
    public String approveReturn() {

        if (actionRecordId == null) {
            addMessage("No record selected.");
            return null;
        }

        // 从 map 中取出当前这条记录选择的新状态
        String selectedCode = returnStatusMap.get(actionRecordId);

        if (selectedCode == null || selectedCode.isBlank()) {
            addMessage("Please select the new copy status.");
            return null;
        }

        BorrowingRecord record = em.find(BorrowingRecord.class, actionRecordId);
        if (record == null) {
            addMessage("Record not found.");
            return null;
        }

        BookCopy copy = record.getBookCopy();

        BookCopyStatus newStatus = findStatusByCode(selectedCode);
        if (newStatus == null) {
            addMessage("BookCopyStatus not found: " + selectedCode);
            return null;
        }

        LocalDate today = LocalDate.now();
        record.setReturnDate(today);

        // 状态机逻辑：
        // 可用  -> COMPLETED（正常还书，押金可退）
        // 损坏/丢失 -> INCOMPLETE（押金不退）
        if ("AVAILABLE".equalsIgnoreCase(selectedCode)) {
            record.setStatus("COMPLETED");
        } else {
            record.setStatus("INCOMPLETE");
        }

        copy.setStatus(newStatus);

        // 用完这条记录的选择后，从 map 中清理掉
        returnStatusMap.remove(actionRecordId);
        actionRecordId = null;

        addMessage("Return processed. New status: " + selectedCode);

        return null;
    }

    // ============================================================
    //                自动处理：逾期转 LOST + INCOMPLETE
    // ============================================================

    @Transactional
    public String markOverdueAsLost() {
        LocalDate today = LocalDate.now();

        List<BorrowingRecord> list = em.createQuery(
                "SELECT r FROM BorrowingRecord r " +
                        "JOIN FETCH r.bookCopy c " +
                        "WHERE r.status = 'BORROWED' " +
                        "AND r.returnDate IS NULL " +
                        "AND r.dueDate < :today",
                BorrowingRecord.class
        ).setParameter("today", today).getResultList();

        if (list.isEmpty()) {
            addMessage("No overdue records found.");
            return null;
        }

        BookCopyStatus lostStatus = findStatusByCode("LOST");

        for (BorrowingRecord r : list) {
            r.setReturnDate(today);
            r.setStatus("INCOMPLETE");
            r.getBookCopy().setStatus(lostStatus);
        }

        addMessage("Marked " + list.size() + " overdue record(s) as LOST.");
        return null;
    }

    // ============================================================
    //                      工具方法
    // ============================================================

    private BookCopyStatus findStatusByCode(String code) {
        List<BookCopyStatus> list = em.createQuery(
                "SELECT s FROM BookCopyStatus s WHERE UPPER(s.code) = :code",
                BookCopyStatus.class
        ).setParameter("code", code.toUpperCase()).getResultList();

        return list.isEmpty() ? null : list.get(0);
    }

    private void addMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(
                null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null)
        );
    }
}