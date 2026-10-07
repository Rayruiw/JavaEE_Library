package com.wangrui.librarymanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "borrowers")
public class Borrower {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // First name
    @Column(nullable = false, length = 100)
    private String firstName;

    // Last name
    @Column(nullable = false, length = 100)
    private String lastName;

    // Date of birth
    private LocalDate dateOfBirth;

    // Gender: M, F, Other
    @Column(length = 20)
    private String gender;

    // Email: must be unique (login uses only email)
    @Column(nullable = false, length = 150, unique = true)
    private String email;

    // Password hash (SHA-256)
    @Column(nullable = false, length = 64)
    private String passwordHash;

    public Borrower() {}

    public Borrower(String firstName,
                    String lastName,
                    LocalDate dateOfBirth,
                    String gender,
                    String email,
                    String passwordHash) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    // ===== Getters and Setters =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) { this.id = id; }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) { this.lastName = lastName; }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) { this.gender = gender; }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}