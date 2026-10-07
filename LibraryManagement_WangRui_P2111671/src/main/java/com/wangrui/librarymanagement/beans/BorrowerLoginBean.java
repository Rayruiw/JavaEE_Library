package com.wangrui.librarymanagement.beans;

import com.wangrui.librarymanagement.entity.Borrower;
import com.wangrui.librarymanagement.util.PasswordUtil;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.io.Serializable;
import java.util.List;

@Named
@RequestScoped
public class BorrowerLoginBean implements Serializable {

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

    // ======================
    // ⭐ 修改过的：使用 getFacesContext()
    // ======================
    public String login() {
        FacesContext ctx = getFacesContext();

        // 查询邮箱
        TypedQuery<Borrower> q = em.createQuery(
                "SELECT b FROM Borrower b WHERE b.email = :email",
                Borrower.class
        );
        q.setParameter("email", email);
        List<Borrower> result = q.getResultList();

        // 1) 邮箱不存在
        if (result.isEmpty()) {
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Invalid email or password.", null));
            return null;
        }

        Borrower b = result.get(0);

        // 2) 密码错误
        if (!PasswordUtil.verifyPassword(password, b.getPasswordHash())) {
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Invalid email or password.", null));
            return null;
        }

        // 3) 登录成功 → 写入 Session
        ctx.getExternalContext()
                .getSessionMap()
                .put("loggedInBorrower", b);

        return "borrower-available-books?faces-redirect=true";
    }

    // mock FacesContext   
    protected FacesContext getFacesContext() {
        return FacesContext.getCurrentInstance();
    }

    // 可选：退出
    public String logout() {
        FacesContext ctx = getFacesContext();
        ctx.getExternalContext().invalidateSession();
        return "index?faces-redirect=true";
    }
}