package com.wangrui.librarymanagement.beans;

import com.wangrui.librarymanagement.entity.Borrower;
import com.wangrui.librarymanagement.entity.BorrowingRecord;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Named
@SessionScoped
public class BorrowerBorrowedBean implements Serializable {

    @PersistenceContext(unitName = "LibraryPU")
    private EntityManager em;

    private Borrower currentBorrower;
    private List<BorrowingRecord> borrowedBooks;

    @PostConstruct
    public void init() {
        borrowedBooks = Collections.emptyList();
    }

    /**
     * ⭐ 强制登录 + 加载借阅者数据
     * 在 borrower-borrowed-books.xhtml 里使用：
     *
     * <f:metadata>
     *     <f:event type="preRenderView"
     *              listener="#{borrowerBorrowedBean.ensureLoggedIn}" />
     * </f:metadata>
     */
    public void ensureLoggedIn() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) return;

        var ec = ctx.getExternalContext();
        Map<String, Object> sessionMap = ec.getSessionMap();
        Object obj = sessionMap.get("loggedInBorrower");

        // ① 未登录 → 重定向到 borrower-login.xhtml
        if (!(obj instanceof Borrower)) {
            currentBorrower = null;
            borrowedBooks = Collections.emptyList();

            try {
                ec.redirect(ec.getRequestContextPath() + "/borrower-login.xhtml");
                ctx.responseComplete();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // ② 已登录 → 重新同步数据库里的 Borrower
        Borrower sessionBorrower = (Borrower) obj;
        currentBorrower = em.find(Borrower.class, sessionBorrower.getId());

        // ③ 加载该用户的所有借阅记录（按申请日期、借出日期排序）
        borrowedBooks = em.createQuery(
                        "SELECT r FROM BorrowingRecord r " +
                                "JOIN FETCH r.bookCopy bc " +
                                "JOIN FETCH bc.book b " +
                                "WHERE r.borrower.id = :bid " +
                                "ORDER BY r.requestDate DESC, r.borrowDate DESC",
                        BorrowingRecord.class
                )
                .setParameter("bid", currentBorrower.getId())
                .getResultList();
    }

    public Borrower getCurrentBorrower() {
        return currentBorrower;
    }

    public List<BorrowingRecord> getBorrowedBooks() {
        return borrowedBooks;
    }

    /**
     * 借阅者点击“申请还书”
     */
    @Transactional
    public String returnBook(Long recordId) {
        if (currentBorrower == null) {
            addMessage("You are not logged in.");
            return null;
        }

        BorrowingRecord record = em.find(BorrowingRecord.class, recordId);
        if (record == null) {
            addMessage("Record not found.");
            return null;
        }

        if (!record.getBorrower().getId().equals(currentBorrower.getId())) {
            addMessage("You did not borrow this book.");
            return null;
        }

        if (!"BORROWED".equals(record.getStatus())) {
            addMessage("This record cannot be returned now.");
            return null;
        }

        record.setStatus("RETURN_REQUESTED");
        record.setReturnRequestDate(LocalDate.now());

        addMessage("Return request submitted.");

        return "borrower-borrowed-books?faces-redirect=true";
    }

    // ===== 工具方法 =====

    private void addMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(
                null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null)
        );
    }
}