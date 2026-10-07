package com.wangrui.librarymanagement.beans;

import com.wangrui.librarymanagement.entity.Borrower;
import com.wangrui.librarymanagement.util.PasswordUtil;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Named
@RequestScoped
public class BorrowerRegisterBean implements Serializable {

    @PersistenceContext(unitName = "LibraryPU")
    private EntityManager em;

    // 表单字段
    private String firstName;
    private String lastName;
    private String dateOfBirth;   // yyyy-MM-dd
    private String gender;
    private String email;
    private String password;
    private String confirmPassword;

    // ========== getter / setter ==========

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    // ========== 注册逻辑 ==========

    @Transactional
    public String register() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        // 去掉首尾空格，避免 " a@b.com "
        if (email != null) {
            email = email.trim();
        }
        if (gender != null) {
            gender = gender.trim();
        }

        // 0. 防御：空密码（理论上 required="true" 已经拦了）
        if (password == null || password.isBlank()) {
            ctx.addMessage("registerForm:pwd",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Password is required.", null));
            return null;
        }

        // 1. 密码强度校验：长度≥8 + 大小写 + 数字 + 特殊符号
        if (!isStrongPassword(password)) {
            ctx.addMessage("registerForm:pwd",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Password must be at least 8 characters and contain upper/lowercase letters, numbers and special characters.",
                            null));
            return null;
        }

        // 2. 两次密码是否一致？
        if (confirmPassword == null || !password.equals(confirmPassword)) {
            ctx.addMessage("registerForm:confirmPwd",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Passwords do not match.", null));
            return null;
        }

        // 3. Email 唯一性校验
        Long count = em.createQuery(
                        "SELECT COUNT(b) FROM Borrower b WHERE b.email = :email",
                        Long.class)
                .setParameter("email", email)
                .getSingleResult();
        if (count != null && count > 0) {
            ctx.addMessage("registerForm:email",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "This email has been used.", null));
            return null;
        }

        // 4. DOB 解析
        LocalDate dob;
        try {
            if (dateOfBirth != null && !dateOfBirth.isBlank()) {
                dob = LocalDate.parse(dateOfBirth); // yyyy-MM-dd
            } else {
                ctx.addMessage("registerForm:dob",
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Date of birth is required.", null));
                return null;
            }
        } catch (DateTimeParseException e) {
            ctx.addMessage("registerForm:dob",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Invalid date format.", null));
            return null;
        }

        // 5. 创建 Borrower 实体并持久化
        Borrower b = new Borrower();
        b.setFirstName(firstName);
        b.setLastName(lastName);
        b.setDateOfBirth(dob);
        b.setGender(gender);
        b.setEmail(email);

        String hashed = PasswordUtil.hashPassword(password);
        b.setPasswordHash(hashed);

        em.persist(b);

        // 6. 成功提示 + 重定向到登录页
        ctx.getExternalContext().getFlash().setKeepMessages(true);
        ctx.addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_INFO,
                "Register success! Please login.", null));

        return "borrower-login?faces-redirect=true";
    }

    // ========== 密码强度校验 ==========

    private boolean isStrongPassword(String pwd) {
        if (pwd == null) return false;

        // 长度至少 8
        if (pwd.length() < 8) return false;

        // 至少一个大写字母
        if (!pwd.matches(".*[A-Z].*")) return false;

        // 至少一个小写字母
        if (!pwd.matches(".*[a-z].*")) return false;

        // 至少一个数字
        if (!pwd.matches(".*[0-9].*")) return false;

        // 至少一个特殊符号
        if (!pwd.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) return false;

        return true;
    }
}