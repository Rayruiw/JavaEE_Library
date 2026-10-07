package com.wangrui.librarymanagement.beans;

import com.wangrui.librarymanagement.entity.Book;
import com.wangrui.librarymanagement.entity.BookCategory;
import com.wangrui.librarymanagement.entity.BookCopy;
import com.wangrui.librarymanagement.entity.BookCopyStatus;
import com.wangrui.librarymanagement.entity.Borrower;
import com.wangrui.librarymanagement.entity.BorrowingRecord;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Named
@RequestScoped
public class BorrowerHomeBean implements Serializable {

    @PersistenceContext(unitName = "LibraryPU")
    private EntityManager em;

    // 当前登录的借阅者
    private Borrower currentBorrower;

    // 可借书列表（会根据搜索条件刷新）
    private List<Book> availableBooks;

    // 当前用户已借未还记录（在 borrower-borrowed-books 页面里用）
    private List<BorrowingRecord> borrowedBooks;

    // 搜索关键字（title 或 author）
    private String keyword;

    // 选中的分类（BookCategory.id）
    private Long selectedCategoryId;

    // 所有分类下拉数据
    private List<BookCategory> allCategories;

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        Map<String, Object> sessionMap = ctx.getExternalContext().getSessionMap();
        Object obj = sessionMap.get("loggedInBorrower");

        // 1. 先看当前是否有登录的 borrower（可能为 null）
        if (obj instanceof Borrower) {
            Borrower sessionBorrower = (Borrower) obj;
            this.currentBorrower = em.find(Borrower.class, sessionBorrower.getId());
        } else {
            this.currentBorrower = null;
        }

        // 2. 不管有没有登录，都加载“所有分类”
        allCategories = em.createQuery(
                "SELECT c FROM BookCategory c ORDER BY c.code",
                BookCategory.class
        ).getResultList();

        // 3. 不管有没有登录，都加载“可用书目列表”
        availableBooks = em.createQuery(
                "SELECT b FROM Book b WHERE b.active = true ORDER BY b.title",
                Book.class
        ).getResultList();

        // 4. 只有登录时才加载“当前用户已借未还记录”
        if (currentBorrower != null) {
            borrowedBooks = em.createQuery(
                            "SELECT r FROM BorrowingRecord r " +
                                    "JOIN FETCH r.bookCopy bc " +
                                    "JOIN FETCH bc.book b " +
                                    "WHERE r.borrower.id = :bid AND r.returnDate IS NULL " +
                                    "ORDER BY r.borrowDate DESC",
                            BorrowingRecord.class
                    )
                    .setParameter("bid", currentBorrower.getId())
                    .getResultList();
        } else {
            borrowedBooks = Collections.emptyList();
        }
    }

    // ====================== 搜索 / 过滤 ======================

    /** 点击 Search 按钮调用 */
    public String searchAvailableBooks() {
        // 去掉首尾空格
        String kw = (keyword != null) ? keyword.trim() : "";
        boolean hasKw = !kw.isEmpty();
        boolean hasCat = (selectedCategoryId != null);

        StringBuilder jpql = new StringBuilder("SELECT b FROM Book b WHERE 1=1 AND b.active = true ");

        if (hasKw) {
            jpql.append(" AND (LOWER(b.title) LIKE :kw OR LOWER(b.author) LIKE :kw)");
        }
        if (hasCat) {
            jpql.append(" AND b.category.id = :catId");
        }
        jpql.append(" ORDER BY b.title");

        TypedQuery<Book> query = em.createQuery(jpql.toString(), Book.class);

        if (hasKw) {
            query.setParameter("kw", "%" + kw.toLowerCase() + "%");
        }
        if (hasCat) {
            query.setParameter("catId", selectedCategoryId);
        }

        availableBooks = query.getResultList();

        // 返回 null 表示留在当前页面
        return null;
    }

    // ====================== 借书申请逻辑 ======================

   // ====================== 借书申请逻辑 ======================
    @Transactional
    public String requestBorrow(Long bookId) {
        if (currentBorrower == null) {
            addMessage("You are not logged in.");
            return null;
        }

        // 1. 找一个该书的 AVAILABLE 副本
        var copyQuery = em.createQuery(
                "SELECT c FROM BookCopy c " +
                        "WHERE c.book.id = :bid AND UPPER(c.status.code) = 'AVAILABLE'",
                BookCopy.class
        );
        copyQuery.setParameter("bid", bookId);
        copyQuery.setMaxResults(1);

        var optionalCopy = copyQuery.getResultStream().findFirst();
        if (optionalCopy.isEmpty()) {
            addMessage("No available copy for this book.");
            return null;
        }

        BookCopy copy = optionalCopy.get();

        // 2. 把副本状态改成 PENDING
        var pendingStatus = findStatusByCode("PENDING");
        if (pendingStatus == null) {
            addMessage("Status PENDING not found in database.");
            return null;
        }
        copy.setStatus(pendingStatus);

        // 3. 创建 BorrowingRecord：申请阶段
        BorrowingRecord record = new BorrowingRecord();
        record.setBorrower(currentBorrower);
        record.setBookCopy(copy);

        LocalDate now = LocalDate.now();
        record.setRequestDate(now);
        record.setBorrowDate(null);
        record.setDueDate(null);
        record.setReturnRequestDate(null);
        record.setReturnDate(null);
        record.setStatus("REQUESTED_BORROW");

        em.persist(record);

        addMessage("Borrow request submitted. Waiting for librarian approval.");

        return "borrower-available-books?faces-redirect=true";
    }
    
    

    // ====================== 还书逻辑（在 borrowed-books 页面也会用到） ======================

    @Transactional
    public String returnBook(Long recordId) {
        if (currentBorrower == null) {
            addMessage("You are not logged in.");
            return null;
        }

        BorrowingRecord record = em.find(BorrowingRecord.class, recordId);
        if (record == null) {
            addMessage("Borrowing record not found.");
            return null;
        }

        if (record.getReturnDate() != null) {
            addMessage("This book has already been returned.");
            return null;
        }

        if (!record.getBorrower().getId().equals(currentBorrower.getId())) {
            addMessage("You are not the borrower of this record.");
            return null;
        }

        BookCopyStatus availableStatus = findStatusByCode("AVAILABLE");
        if (availableStatus == null) {
            addMessage("Status AVAILABLE not found in database.");
            return null;
        }

        record.setReturnDate(LocalDate.now());
        BookCopy copy = record.getBookCopy();
        copy.setStatus(availableStatus);

        addMessage("Returned successfully.");

        return "borrower-borrowed-books?faces-redirect=true";
    }

    // ====================== Logout ======================

    public String logout() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        ctx.getExternalContext().invalidateSession();
        return "/index.html?faces-redirect=true";
    }

    // ====================== 工具方法 ======================

    private BookCopyStatus findStatusByCode(String code) {
        List<BookCopyStatus> list = em.createQuery(
                        "SELECT s FROM BookCopyStatus s WHERE s.code = :code",
                        BookCopyStatus.class)
                .setParameter("code", code)
                .getResultList();

        if (list.isEmpty()) return null;
        return list.get(0);
    }

    private void addMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(
                null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null)
        );
    }

    // ====================== Getter / Setter，供 xhtml 绑定 ======================

    public Borrower getCurrentBorrower() {
        return currentBorrower;
    }

    public List<Book> getAvailableBooks() {
        return availableBooks;
    }

    public List<BorrowingRecord> getBorrowedBooks() {
        return borrowedBooks;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public void setSelectedCategoryId(Long selectedCategoryId) {
        this.selectedCategoryId = selectedCategoryId;
    }

    public List<BookCategory> getAllCategories() {
        return allCategories;
    }
}