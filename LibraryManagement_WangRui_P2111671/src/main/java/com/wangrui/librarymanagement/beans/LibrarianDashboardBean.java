package com.wangrui.librarymanagement.beans;

import com.wangrui.librarymanagement.entity.Librarian;
import com.wangrui.librarymanagement.entity.Book;
import com.wangrui.librarymanagement.entity.BookCopy;
import com.wangrui.librarymanagement.entity.Borrower;
// 这里可以删掉 BorrowingRecord 的 import，因为不再使用
// import com.wangrui.librarymanagement.entity.BorrowingRecord;
import java.io.IOException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.Serializable;
import java.util.Map;

@Named
@RequestScoped
public class LibrarianDashboardBean implements Serializable {

    @PersistenceContext(unitName = "LibraryPU")
    private EntityManager em;

    private Librarian currentLibrarian;

    private long bookCount;
    private long copyCount;
    private long borrowerCount;
    private long borrowedCount;   // 当前“状态为 BORROWED 的副本”数量

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        var ec = ctx.getExternalContext();
        Map<String, Object> sessionMap = ec.getSessionMap();
        Object obj = sessionMap.get("loggedInLibrarian");

        // ① 如果没有登录管理员，直接重定向到管理员登录页
        if (!(obj instanceof Librarian)) {
            try {
                // librarian-login.xhtml 就是你管理员登录页面的名字
                ec.redirect(ec.getRequestContextPath() + "/librarian-login.xhtml");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return; // 后面统计逻辑就不执行了
        }

        // ② 已登录，正常初始化
        Librarian sessionLib = (Librarian) obj;
        currentLibrarian = em.find(Librarian.class, sessionLib.getId());

        // 统计数据（你原来的代码照抄即可）
        bookCount = getCount(Book.class);
        copyCount = getCount(BookCopy.class);
        borrowerCount = getCount(Borrower.class);
        borrowedCount = em.createQuery(
                        "SELECT COUNT(r) FROM BorrowingRecord r " +
                        "WHERE r.status = 'BORROWED' OR r.status = 'RETURN_REQUESTED'",
                        Long.class
                )
                .getSingleResult();
    }

    // 通用统计方法
    private long getCount(Class<?> entityClass) {
        String jpql = "SELECT COUNT(e) FROM " + entityClass.getSimpleName() + " e";
        return em.createQuery(jpql, Long.class).getSingleResult();
    }

    // ===== getters，给 xhtml 用 =====
    public Librarian getCurrentLibrarian() {
        return currentLibrarian;
    }

    public long getBookCount() {
        return bookCount;
    }

    public long getCopyCount() {
        return copyCount;
    }

    public long getBorrowerCount() {
        return borrowerCount;
    }

    public long getBorrowedCount() {
        return borrowedCount;
    }
}