package com.wangrui.librarymanagement.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                 // 书目主键 ID

    @Column(nullable = false, length = 200)
    private String title;            // 书名

    @Column(length = 100)
    private String author;           // 作者

    @Column(length = 13)
    private String isbn;             // ISBN

    @Column(length = 100)
    private String publisher;        // 出版社

    private Integer publishYear;     // 出版年份
    
    @Column(length = 2000)
    private String description;
    
    /**
     * 图书分类：指向上面的 BookCategory（LCC）
     * 多本书可以属于同一个分类
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id")   // 外键字段名
    private BookCategory category;

    /**
     * 图书封面：
     * 这里推荐存图片的 URL / 相对路径，而不是二进制，避免 REST JSON 太大。
     * 比如："/images/books/java101.jpg"
     */
    @Column(length = 255)
    private String coverImageUrl;
    
    /** 逻辑删除标记：true = 正常，false = 已删除（不再显示） */
    @Column(nullable = false)
    private boolean active = true;

    public Book() {}

    public Book(String title,
                String author,
                String isbn,
                String publisher,
                Integer publishYear,
                BookCategory category,
                String coverImageUrl) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publisher = publisher;
        this.publishYear = publishYear;
        this.category = category;
        this.coverImageUrl = coverImageUrl;
    }

    // ===== Getter / Setter =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) { this.id = id; }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) { this.title = title; }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) { this.author = author; }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) { this.publisher = publisher; }

    public Integer getPublishYear() {
        return publishYear;
    }

    public void setPublishYear(Integer publishYear) { this.publishYear = publishYear; }

    public BookCategory getCategory() {
        return category;
    }

    public void setCategory(BookCategory category) { this.category = category; }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    
    public String getDescription() {
    return description;
}

    public void setDescription(String description) {
        this.description = description;
    }

}

    