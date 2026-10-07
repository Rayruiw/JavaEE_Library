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

import java.io.Serializable;
import java.util.List;

@Named
@SessionScoped
public class SystemAdminManageLibrariansBean implements Serializable {

    @PersistenceContext(unitName = "LibraryPU")
    private EntityManager em;

    // 列出所有 Librarian
    private List<Librarian> librarians;

    // 新建 Librarian 使用的表单字段
    private String firstName;
    private String lastName;
    private String email;
    private String password;   // 

    // =============== 初始化：加载所有 librarian ===============
    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        var ec = ctx.getExternalContext();

        // 从 session 里拿系统管理员（登录成功时放进去的）
        Object admin = ec.getSessionMap().get("loggedInSystemAdmin");

        // 未登录系统管理员：立刻重定向到 system-admin-login.xhtml
        if (admin == null) {
            try {
                String ctxPath = ec.getRequestContextPath(); 
                ec.redirect(ctxPath + "/system-admin-login.xhtml");
                ctx.responseComplete();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        // 已登录：正常加载数据
        refreshLibrarians();
    }

    private void refreshLibrarians() {
        librarians = em.createQuery(
                "SELECT l FROM Librarian l ORDER BY l.id",
                Librarian.class
        ).getResultList();
    }

    // =============== getter / setter ===============

    public List<Librarian> getLibrarians() {
        return librarians;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

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

    // =============== 新增图书管理员 ===============

    @Transactional
    public String createLibrarian() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        // 1. 基本非空校验
        if (email == null || email.isBlank() ||
            password == null || password.isBlank()) {
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Email and password are required.", null));
            return null;
        }

        // 2. 密码强度校验：长度≥8，含大小写、数字、特殊符号
        if (!isStrongPassword(password)) {
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Password must be at least 8 characters and contain upper/lowercase letters, numbers and special characters.",
                            null));
            return null;
        }

        // 3. 邮箱唯一性检查
        Long count = em.createQuery(
                        "SELECT COUNT(l) FROM Librarian l WHERE l.email = :email",
                        Long.class
                )
                .setParameter("email", email)
                .getSingleResult();
        if (count != null && count > 0) {
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "This email is already used by another librarian.", null));
            return null;
        }

        // 4. 创建实体
        Librarian lib = new Librarian();
        lib.setFirstName(firstName);
        lib.setLastName(lastName);
        lib.setEmail(email);

        String hash = PasswordUtil.hashPassword(password);
        lib.setPasswordHash(hash);

        em.persist(lib);

        // 5. 刷新列表 & 清空表单
        refreshLibrarians();
        firstName = null;
        lastName = null;
        email = null;
        password = null;

        ctx.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "New librarian created successfully.", null));

        return null;  // 留在当前页面
    }

    // =============== 删除图书管理员 ===============

    @Transactional
    public String deleteLibrarian(Long librarianId) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        Librarian lib = em.find(Librarian.class, librarianId);
        if (lib == null) {
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Librarian not found.", null));
            return null;
        }

        em.remove(lib);
        refreshLibrarians();

        ctx.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Librarian removed.", null));

        return null;
    }

    // =============== security passcode===============

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