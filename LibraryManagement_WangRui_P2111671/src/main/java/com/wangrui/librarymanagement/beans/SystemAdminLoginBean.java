package com.wangrui.librarymanagement.beans;

import com.wangrui.librarymanagement.entity.SystemAdmin;
import com.wangrui.librarymanagement.util.PasswordUtil;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Named
@SessionScoped
public class SystemAdminLoginBean implements Serializable {

    @PersistenceContext(unitName = "LibraryPU")
    private EntityManager em;

    private String email;
    private String password;
    private SystemAdmin currentAdmin;

    // ===== getters/setters =====
    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }

    public SystemAdmin getCurrentAdmin() { return currentAdmin; }

    // ===== 登录 =====
    public String login() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        List<SystemAdmin> list = em.createQuery(
                        "SELECT a FROM SystemAdmin a WHERE a.email = :email",
                        SystemAdmin.class)
                .setParameter("email", email)
                .getResultList();

        if (list.isEmpty()) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Admin not found.", null));
            return null;
        }

        SystemAdmin admin = list.get(0);

        // 调试时可以打印一下确认拿到了 hash
        System.out.println("[SystemAdminLogin] db email = " + admin.getEmail());
        System.out.println("[SystemAdminLogin] db hash  = " + admin.getPasswordHash());

        if (!PasswordUtil.verifyPassword(password, admin.getPasswordHash())) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Invalid password.", null));
            return null;
        }

        currentAdmin = admin;

        // 把 admin 放进 session（可选）
        ctx.getExternalContext().getSessionMap()
                .put("loggedInSystemAdmin", currentAdmin);

        return "system-admin-librarians?faces-redirect=true";
    }

    // ===== 登出 =====
    public String logout() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        Map<String, Object> sessionMap = ctx.getExternalContext().getSessionMap();
        sessionMap.remove("loggedInSystemAdmin");
        currentAdmin = null;
        email = null;
        password = null;
        return "index?faces-redirect=true";
    }
    
    public void ensureLoggedIn() {
        if (currentAdmin == null) {
            try {
                FacesContext ctx = FacesContext.getCurrentInstance();
                var ec = ctx.getExternalContext();
                String ctxPath = ec.getRequestContextPath();
                ec.redirect(ctxPath + "/system-admin-login.xhtml");
                ctx.responseComplete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}