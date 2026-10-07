package com.wangrui.librarymanagement.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "book_copy")
public class BookCopy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 每一个实体书属于哪本书
    @ManyToOne(optional = false)
    @JoinColumn(name = "book_id")
    private Book book;

    // 副本的当前状态：AVAILABLE / BORROWED / LOST / DAMAGED
    @ManyToOne(optional = false)
    @JoinColumn(name = "status_id")
    private BookCopyStatus status;

    public BookCopy() {}

    public BookCopy(Book book, BookCopyStatus status) {
        this.book = book;
        this.status = status;
    }

    // ===== Getter / Setter =====
    public Long getId() {
        return id;
    }

    public void setId(Long id) { this.id = id; }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) { this.book = book; }

    public BookCopyStatus getStatus() {
        return status;
    }

    public void setStatus(BookCopyStatus status) { this.status = status; }
}