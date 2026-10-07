package com.wangrui.librarymanagement.beans;

import com.wangrui.librarymanagement.entity.Librarian;
import com.wangrui.librarymanagement.util.PasswordUtil;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

@Named
@RequestScoped
public class LibrarianLoginBean implements Serializable {

    private String email;
    private String password;

    @PersistenceContext(unitName = "LibraryPU")
    private EntityManager em;

    // ===== getters / setters =====
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // ===== 登录逻辑 =====
    public String login() {
        try {
            // 1. 按邮箱查找管理员
            TypedQuery<Librarian> q = em.createQuery(
                    "SELECT l FROM Librarian l WHERE l.email = :email",
                    Librarian.class
            );
            q.setParameter("email", email);
            List<Librarian> result = q.getResultList();

            if (result.isEmpty()) {
                addMessage("Invalid email or password.");
                return null; // 回到当前登录页
            }

            Librarian lib = result.get(0);

            // 2. 验证密码（PasswordUtil 使用 BCrypt）
            if (!PasswordUtil.verifyPassword(password, lib.getPasswordHash())) {
                addMessage("Invalid email or password.");
                return null;
            }

            // 3. 登录成功：放到 Session 里
            FacesContext.getCurrentInstance()
                    .getExternalContext()
                    .getSessionMap()
                    .put("loggedInLibrarian", lib);

            // 4. 跳转到管理员首页
            return "librarian-dashboard?faces-redirect=true";

        } catch (Exception e) {
            // 防止 500 看不到原因，这里先打日志 + 显示友好信息
            e.printStackTrace();
            addMessage("Login failed: " + e.getMessage());
            return null;
        }
    }

    // ===== 退出登录 =====
    @Transactional
    public String logout() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        var ec= ctx.getExternalContext();
        ec.invalidateSession();     // ⭐ 彻底销毁 session，所有 @SessionScoped bean 一起清掉
        return "index?faces-redirect=true";              // 回首页或你想去的页面
    }

    // ===== 工具：添加 FacesMessage =====
    private void addMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(
                null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null)
        );
    }
}