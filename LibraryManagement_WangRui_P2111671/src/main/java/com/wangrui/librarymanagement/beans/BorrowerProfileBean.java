package com.wangrui.librarymanagement.beans;

import com.wangrui.librarymanagement.entity.Borrower;
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
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Named
@SessionScoped
public class BorrowerProfileBean implements Serializable {

    @PersistenceContext(unitName = "LibraryPU")
    private EntityManager em;

    private Borrower currentBorrower;

    // 修改密码相关字段
    private String newPassword;
    private String confirmPassword;

    @PostConstruct
    public void init() {        
        currentBorrower = null;
        newPassword = null;
        confirmPassword = null;
    }

    /**
     * ✅ 强制登录 + 加载当前 Borrower
     *
     * 在 borrower-profile.xhtml 里使用：
     *
     * <f:metadata>
     *     <f:event type="preRenderView"
     *              listener="#{borrowerProfileBean.ensureLoggedIn}" />
     * </f:metadata>
     */
    public void ensureLoggedIn() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) return;

        var ec = ctx.getExternalContext();
        Map<String, Object> sessionMap = ec.getSessionMap();
        Object obj = sessionMap.get("loggedInBorrower");

        // ① 未登录：跳转到 borrower-login.xhtml
        if (!(obj instanceof Borrower)) {
            currentBorrower = null;
            try {
                ec.redirect(ec.getRequestContextPath() + "/borrower-login.xhtml");
                ctx.responseComplete();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // ② 已登录：从数据库重新拿一遍最新的 Borrower
        Borrower sessionBorrower = (Borrower) obj;
        this.currentBorrower = em.find(Borrower.class, sessionBorrower.getId());
    }

    // ========== getter / setter ==========

    public Borrower getCurrentBorrower() {
        return currentBorrower;
    }

    public String getNewPassword() {
        return newPassword;
    }
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }
    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    // DOB 显示用
    public String getFormattedDob() {
        if (currentBorrower == null || currentBorrower.getDateOfBirth() == null) {
            return "";
        }
        return currentBorrower.getDateOfBirth().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    // ========== 修改密码逻辑 ==========

    @Transactional
    public String changePassword() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        if (currentBorrower == null) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, "You are not logged in.", null));
            return null;
        }

        // 1. 空检查
        if (newPassword == null || newPassword.isBlank()) {
            ctx.addMessage("profileForm:newPwd",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "New password is required.", null));
            return null;
        }

        // 2. 强度检查
        if (!isStrongPassword(newPassword)) {
            ctx.addMessage("profileForm:newPwd",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Password must be at least 8 characters and contain upper/lowercase letters, numbers and special characters.",
                            null));
            return null;
        }

        // 3. 两次一致
        if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
            ctx.addMessage("profileForm:confirmPwd",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Passwords do not match.", null));
            return null;
        }

        // 4. 更新密码
        Borrower managed = em.find(Borrower.class, currentBorrower.getId());
        if (managed == null) {
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Borrower not found.", null));
            return null;
        }

        String hashed = PasswordUtil.hashPassword(newPassword);
        managed.setPasswordHash(hashed);
        em.merge(managed);

        // 同步到当前 session 中的 borrower
        currentBorrower = managed;

        // 清空表单字段
        newPassword = null;
        confirmPassword = null;

        ctx.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Password changed successfully.", null));

        return null;
    }

    // 与注册时一致的密码强度规则
    private boolean isStrongPassword(String pwd) {
        if (pwd == null) return false;

        if (pwd.length() < 8) return false;
        if (!pwd.matches(".*[A-Z].*")) return false;              // 大写
        if (!pwd.matches(".*[a-z].*")) return false;              // 小写
        if (!pwd.matches(".*[0-9].*")) return false;              // 数字
        if (!pwd.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) return false; // 特殊符号

        return true;
    }
}