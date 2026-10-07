package com.wangrui.librarymanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "borrowing_records")
public class BorrowingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 谁借的 */
    @ManyToOne(optional = false)
    @JoinColumn(name = "borrower_id")
    private Borrower borrower;

    /** 借的是哪一册副本 */
    @ManyToOne(optional = false)
    @JoinColumn(name = "book_copy_id")
    private BookCopy bookCopy;

    /** 借书申请日期（借阅者点“Request Borrow”那一刻） */
    private LocalDate requestDate;

    /** 实际借出日期（管理员批准时填写） */
    private LocalDate borrowDate;

    /** 应还日期（通常在管理员批准时设定） */
    private LocalDate dueDate;

    /** 借阅者申请还书的日期（点击 “Request Return”） */
    private LocalDate returnRequestDate;

    /** 管理员确认归还的日期 */
    private LocalDate returnDate;

    /**
     * 借阅流程状态：
     * - REQUESTED_BORROW   借书申请中（用户发起）
     * - BORROWED           已借出（管理员批准）
     * - RETURN_REQUESTED   还书申请中（用户发起）
     * - COMPLETED          已归还（管理员确认）
     */
    @Column(nullable = false, length = 30)
    private String status;

    public BorrowingRecord() {}

    public BorrowingRecord(Borrower borrower, BookCopy bookCopy) {
        this.borrower = borrower;
        this.bookCopy = bookCopy;
        this.requestDate = LocalDate.now();
        this.status = "REQUESTED_BORROW";
    }

    // ===== Getter / Setter =====

    public Long getId() {
        return id;
    }

    public Borrower getBorrower() {
        return borrower;
    }

    public void setBorrower(Borrower borrower) {
        this.borrower = borrower;
    }

    public BookCopy getBookCopy() {
        return bookCopy;
    }

    public void setBookCopy(BookCopy bookCopy) {
        this.bookCopy = bookCopy;
    }

    public LocalDate getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDate requestDate) {
        this.requestDate = requestDate;
    }

    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    public void setBorrowDate(LocalDate borrowDate) {
        this.borrowDate = borrowDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnRequestDate() {
        return returnRequestDate;
    }

    public void setReturnRequestDate(LocalDate returnRequestDate) {
        this.returnRequestDate = returnRequestDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /** 是否已归还 */
    @Transient
    public boolean isReturned() {
        return returnDate != null;
    }

    /** 是否逾期：未归还 && 今天晚于应还日期 */
    @Transient
    public boolean isOverdue() {
        return dueDate != null
                && returnDate == null
                && LocalDate.now().isAfter(dueDate);
    }
}