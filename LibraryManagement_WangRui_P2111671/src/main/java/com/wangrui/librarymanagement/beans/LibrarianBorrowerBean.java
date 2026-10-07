package com.wangrui.librarymanagement.beans;

import com.wangrui.librarymanagement.entity.Borrower;
import com.wangrui.librarymanagement.entity.Librarian;
import com.wangrui.librarymanagement.util.PasswordUtil; // 其实这里没用到，可以删掉也行
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
import java.util.List;
import java.util.Map;

@Named
@SessionScoped
public class LibrarianBorrowerBean implements Serializable {

    @PersistenceContext(unitName = "LibraryPU")
    private EntityManager em;

    // 当前登录的图书管理员（做权限）
    private Librarian currentLibrarian;

    // 借阅者列表
    private List<Borrower> borrowers;

    // 搜索关键字（姓名 / 邮箱的模糊查询）
    private String keyword;

    // 是否已经初始化过（避免每次请求都重新查 DB）
    private boolean initialized = false;

    // ================== 轻量级初始化 ==================
    @PostConstruct
    public void init() {
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
                ctx.responseComplete();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // ② 已登录：如果是第一次，或登录人发生变化，重新加载数据
        Librarian sessionLib = (Librarian) obj;
        if (currentLibrarian == null || !sessionLib.getId().equals(currentLibrarian.getId())) {
            currentLibrarian = em.find(Librarian.class, sessionLib.getId());
            initialized = false;
        }

        if (!initialized) {
            refreshBorrowers();
            initialized = true;
        }
    }

    // ================== 数据加载 ==================
    private void refreshBorrowers() {
        borrowers = em.createQuery(
                "SELECT b FROM Borrower b ORDER BY b.lastName, b.firstName",
                Borrower.class
        ).getResultList();
    }

    // ================== getter / setter ==================
    public Librarian getCurrentLibrarian() {
        return currentLibrarian;
    }

    public List<Borrower> getBorrowers() {
        return borrowers;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    // ========== 搜索功能 ==========
    public String search() {
        String kw = (keyword == null) ? "" : keyword.trim().toLowerCase();
        if (kw.isEmpty()) {
            // 没填关键字，就回到全部列表
            refreshBorrowers();
            return null;
        }

        borrowers = em.createQuery(
                        "SELECT b FROM Borrower b " +
                        "WHERE LOWER(b.firstName) LIKE :kw " +
                        "   OR LOWER(b.lastName)  LIKE :kw " +
                        "   OR LOWER(b.email)     LIKE :kw " +
                        "ORDER BY b.lastName, b.firstName",
                        Borrower.class)
                .setParameter("kw", "%" + kw + "%")
                .getResultList();
        return null;
    }

    public String clearSearch() {
        keyword = null;
        refreshBorrowers();
        return null;
    }
}