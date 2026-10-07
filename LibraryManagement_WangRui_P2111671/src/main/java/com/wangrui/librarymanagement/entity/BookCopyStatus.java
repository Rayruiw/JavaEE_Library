package com.wangrui.librarymanagement.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "book_copy_status")
public class BookCopyStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;          // 例如 1,2,3...

    @Column(nullable = false, length = 30, unique = true)
    private String code;      // 例如: AVAILABLE, BORROWED

    @Column(nullable = false, length = 100)
    private String name;      // 显示名称: 可借阅 / 已借出 等

    @Column(length = 255)
    private String description;

    public BookCopyStatus() {
    }

    public BookCopyStatus(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }

    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }

    public void setDescription(String description) {
        this.description = description;
    }
}