package com.wangrui.librarymanagement.beans;

import com.wangrui.librarymanagement.entity.Librarian;
import com.wangrui.librarymanagement.util.PasswordUtil;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

@Named
@SessionScoped
public class LibrarianProfileBean implements Serializable {

    @PersistenceContext(unitName = "LibraryPU")
    private EntityManager em;

    /** 当前登录的图书管理员 */
    private Librarian currentLibrarian;

    /** 修改密码表单字段 */
    private String newPassword;
    private String confirmPassword;

    /** 是否已经做过初始化（用于避免重复加载） */
    private boolean initialized = false;

    @PostConstruct
    public void init() {
        // 不在这里做重定向，只是标记为“尚未初始化”
        initialized = false;
    }
    
    public void ensureLoggedIn() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) return;

        var ec = ctx.getExternalContext();
        Map<String, Object> sessionMap = ec.getSessionMap();
        Object obj = sessionMap.get("loggedInLibrarian");

        // ① 未登录：重定向到登录页
        if (!(obj instanceof Librarian)) {
            currentLibrarian = null;
            try {
                ec.redirect(ec.getRequestContextPath() + "/librarian-login.xhtml");
                ctx.responseComplete();    // 告诉 JSF：这次请求结束了
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // ② 已登录：如果第一次进入，或者 session 里的图书管理员变了，就重新从 DB 载入
        Librarian sessionLib = (Librarian) obj;

        if (currentLibrarian == null ||
            !sessionLib.getId().equals(currentLibrarian.getId())) {

            currentLibrarian = em.find(Librarian.class, sessionLib.getId());
            // 切换账号时，把表单内容清空
            newPassword = "";
            confirmPassword = "";
            initialized = false;
        }

        if (!initialized) {
            // 目前 profile 页面没什么额外数据要查，这里只是标记一下
            initialized = true;
        }
    }

    // ================== getter / setter ==================

    public Librarian getCurrentLibrarian() {
        return currentLibrarian;
    }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String np) { this.newPassword = np; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String cp) { this.confirmPassword = cp; }

    // ================== 修改密码逻辑 ==================
    @Transactional
    public String changePassword() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        // 防御：如果 somehow 没登录，直接让他重新登录
        if (currentLibrarian == null) {
            try {
                ctx.getExternalContext().redirect(
                        ctx.getExternalContext().getRequestContextPath() + "/librarian-login.xhtml"
                );
                ctx.responseComplete();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        // 1. 是否填写新密码
        if (newPassword == null || newPassword.isBlank()) {
            ctx.addMessage("profileForm:newPwd",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "New password is required.", null));
            return null;
        }

        // 2. 两次密码是否一致
        if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
            ctx.addMessage("profileForm:confirmPwd",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Passwords do not match.", null));
            return null;
        }

        // 3. 强密码规则（和 Borrower 一致）
        if (!isStrongPassword(newPassword)) {
            ctx.addMessage("profileForm:newPwd",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Password must be at least 8 chars with upper/lowercase, number, special symbol.",
                            null));
            return null;
        }

        // 4. 更新数据库中的密码
        Librarian managed = em.find(Librarian.class, currentLibrarian.getId());
        managed.setPasswordHash(PasswordUtil.hashPassword(newPassword));
        em.merge(managed);

        // 5. 清空表单字段
        newPassword = "";
        confirmPassword = "";

        // 6. 提示成功
        ctx.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Password updated successfully.", null));

        return null;
    }

    // 与 Borrower 的强密码规则保持一致
    private boolean isStrongPassword(String pwd) {
        if (pwd == null) return false;
        if (pwd.length() < 8) return false;
        if (!pwd.matches(".*[A-Z].*")) return false;
        if (!pwd.matches(".*[a-z].*")) return false;
        if (!pwd.matches(".*[0-9].*")) return false;
        if (!pwd.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) return false;
        return true;
    }
}