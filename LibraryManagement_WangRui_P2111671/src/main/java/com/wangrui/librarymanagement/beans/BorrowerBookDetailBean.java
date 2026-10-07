package com.wangrui.librarymanagement.beans;

import com.wangrui.librarymanagement.entity.Book;
import com.wangrui.librarymanagement.entity.BookCopy;
import com.wangrui.librarymanagement.entity.BookCopyStatus; // ⭐ 一定要加这个
import com.wangrui.librarymanagement.entity.Borrower;
import com.wangrui.librarymanagement.entity.BorrowingRecord;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Named
@ViewScoped
public class BorrowerBookDetailBean implements Serializable {

    @PersistenceContext(unitName = "LibraryPU")
    private EntityManager em;

    private Long bookId;
    private Book book;
    private long availableCount;

    // 当前登录的借阅者（可为 null）
    private Borrower currentBorrower;

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) return;

        // POST 回发时不要重新查参数/书本
        if (ctx.isPostback()) {
            return;
        }

        // 1. 从 URL 取 bookId
        Map<String, String> params = ctx.getExternalContext().getRequestParameterMap();
        String idStr = params.get("bookId");
        if (idStr != null) {
            try {
                bookId = Long.parseLong(idStr);
            } catch (NumberFormatException e) {
                bookId = null;
            }
        }

        // 2. 查书
        if (bookId != null) {
            book = em.find(Book.class, bookId);
        }

        // 2.1 如果书不存在，或者 active = false，就不允许访问
        if (book == null || Boolean.FALSE.equals(book.isActive())) {
            var ec = ctx.getExternalContext();
            try {
                ec.getFlash().setKeepMessages(true);
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_WARN,
                        "This book is currently not available.", null));

                ec.redirect(ec.getRequestContextPath() + "/borrower-available-books.xhtml");
                ctx.responseComplete();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // 3. 统计可借副本数量
        reloadAvailableCount();

        // 4. 从 session 中取当前借阅者（可为空）
        Map<String, Object> sessionMap = ctx.getExternalContext().getSessionMap();
        Object obj = sessionMap.get("loggedInBorrower");
        if (obj instanceof Borrower) {
            Borrower sessionBorrower = (Borrower) obj;
            currentBorrower = em.find(Borrower.class, sessionBorrower.getId());
        }
    }

    /** 重新统计当前书的可借副本数量（只算 AVAILABLE） */
    private void reloadAvailableCount() {
        if (book == null) {
            availableCount = 0;
            return;
        }

        availableCount = em.createQuery(
                        "SELECT COUNT(c) FROM BookCopy c " +
                                "WHERE c.book.id = :bid " +
                                "  AND UPPER(c.status.code) = 'AVAILABLE'",
                        Long.class
                )
                .setParameter("bid", book.getId())
                .getSingleResult();
    }

    // ========== 借阅请求（点击 Request Borrow） ==========
    @Transactional
    public String requestBorrow() {

        // 1. 必须登录
        if (currentBorrower == null) {
            addMessage("Please login before requesting to borrow this book.");
            return null;
        }

        // 2. 必须有这本书
        if (book == null) {
            addMessage("Book not found.");
            return null;
        }

        // 3. 查所有 AVAILABLE 的副本
        List<BookCopy> availableCopies = em.createQuery(
                        "SELECT c FROM BookCopy c " +
                                "WHERE c.book.id = :bid " +
                                "  AND UPPER(c.status.code) = 'AVAILABLE'",
                        BookCopy.class
                )
                .setParameter("bid", book.getId())
                .getResultList();

        if (availableCopies.isEmpty()) {
            addMessage("No available copy for this book.");
            reloadAvailableCount();
            return null;
        }

        // 4. 随机选一册
        BookCopy copy;
        if (availableCopies.size() == 1) {
            copy = availableCopies.get(0);
        } else {
            int idx = ThreadLocalRandom.current().nextInt(availableCopies.size());
            copy = availableCopies.get(idx);
        }

        // 4.1 查出 PENDING 状态，并把副本设为 PENDING
        List<BookCopyStatus> list = em.createQuery(
                        "SELECT s FROM BookCopyStatus s WHERE UPPER(s.code) = 'PENDING'",
                        BookCopyStatus.class
                )
                .setMaxResults(1)
                .getResultList();

        if (list.isEmpty()) {
            addMessage("Status PENDING not found in database.");
            return null;
        }

        BookCopyStatus pendingStatus = list.get(0);
        copy.setStatus(pendingStatus);

        // 5. 创建 BorrowingRecord（借书申请阶段）
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

        // 6. 更新数量 + 提示
        reloadAvailableCount();
        addMessage("Borrow request submitted. Please wait for librarian approval.");

        return null;
    }

    private void addMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(
                null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null)
        );
    }

    // ===== Getter，给 xhtml 用 =====

    public Book getBook() {
        return book;
    }

    public long getAvailableCount() {
        return availableCount;
    }

    public Borrower getCurrentBorrower() {
        return currentBorrower;
    }
}