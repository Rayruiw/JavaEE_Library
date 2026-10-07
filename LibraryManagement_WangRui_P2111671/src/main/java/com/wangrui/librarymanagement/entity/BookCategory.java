package com.wangrui.librarymanagement.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "book_category")
public class BookCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;           // 数字型主键，比如 1, 2, 3...

    @Column(nullable = false, length = 2, unique = true)
    private String code;       // A, B, C, ... Z

    @Column(nullable = false, length = 200)
    private String name;       // 比如：GENERAL WORKS

    @Column(length = 255)
    private String description; // 更详细说明（可选）

    public BookCategory() {
    }

    public BookCategory(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    // ===== Getter / Setter =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) { this.id = id; }

    public String getCode() {
        return code;
    }

    public void setCode(String code) { this.code = code; }

    public String getName() {
        return name;
    }

    public void setName(String name) { this.name = name; }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) { this.description = description; }
}