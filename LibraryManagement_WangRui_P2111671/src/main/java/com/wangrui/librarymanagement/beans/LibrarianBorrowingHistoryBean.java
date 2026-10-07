package com.wangrui.librarymanagement.beans;

import com.wangrui.librarymanagement.entity.BookCopy;
import com.wangrui.librarymanagement.entity.BookCopyStatus;
import com.wangrui.librarymanagement.entity.BorrowingRecord;
import com.wangrui.librarymanagement.entity.Librarian;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Named
@RequestScoped
public class LibrarianBorrowingHistoryBean implements Serializable {

    @PersistenceContext(unitName = "LibraryPU")
    private EntityManager em;

    // 当前登录的管理员（只做身份验证用）
    private Librarian currentLibrarian;

    // 状态过滤：ALL / REQUESTED_BORROW / BORROWED / RETURN_REQUESTED / COMPLETED / INCOMPLETE
    private String statusFilter = "ALL";

    // 按借阅者姓名 / 邮箱搜索
    private String keyword;

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) return;

        var ec = ctx.getExternalContext();
        Map<String, Object> sessionMap = ec.getSessionMap();
        Object obj = sessionMap.get("loggedInLibrarian");

        // 没登录 → 强制跳登录页
        if (!(obj instanceof Librarian)) {
            try {
                ec.redirect(ec.getRequestContextPath() + "/librarian-login.xhtml");
                ctx.responseComplete();   // 本次响应结束
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // 已登录管理员
        currentLibrarian = (Librarian) obj;
    }

    // ====== 自动把逾期 14 天以上的副本标记为 LOST / INCOMPLETE ======
    /**
     * 在页面 preRenderView 阶段调用：
     *
     * <f:metadata>
     *   <f:event type="preRenderView"
     *            listener="#{librarianBorrowingHistoryBean.autoMarkOverdueLost}"/>
     * </f:metadata>
     *
     * 逻辑：
     *   - 找出 status = BORROWED 或 RETURN_REQUESTED
     *   - 且 dueDate < 今天，且 returnDate IS NULL
     *   - 将 BorrowingRecord.status 置为 INCOMPLETE
     *   - 对应 BookCopy.status 置为 LOST
     */
    @Transactional
    public void autoMarkOverdueLost() {
        LocalDate today = LocalDate.now();

        // 1. 找出所有“逾期未还”的记录
        List<BorrowingRecord> overdueList = em.createQuery(
                "SELECT r FROM BorrowingRecord r " +
                        "JOIN FETCH r.bookCopy c " +
                        "WHERE r.status IN ('BORROWED', 'RETURN_REQUESTED') " +
                        "  AND r.dueDate IS NOT NULL " +
                        "  AND r.dueDate < :today " +
                        "  AND r.returnDate IS NULL",
                BorrowingRecord.class
        ).setParameter("today", today)
         .getResultList();

        if (overdueList.isEmpty()) {
            return; // 没有需要处理的
        }

        // 2. 找到 LOST 状态（假定 BookCopyStatus.code = 'LOST'）
        List<BookCopyStatus> lostList = em.createQuery(
                        "SELECT s FROM BookCopyStatus s WHERE UPPER(s.code) = 'LOST'",
                        BookCopyStatus.class)
                .setMaxResults(1)
                .getResultList();

        if (lostList.isEmpty()) {
            // 数据库没配置 LOST 状态，就先不处理
            return;
        }

        BookCopyStatus lostStatus = lostList.get(0);

        // 3. 逐条更新
        for (BorrowingRecord r : overdueList) {
            // 借阅记录标记为 INCOMPLETE（逾期未归还）
            r.setStatus("INCOMPLETE");

            // 对应的 BookCopy 标记为 LOST
            BookCopy copy = r.getBookCopy();
            copy.setStatus(lostStatus);
        }
    }

    // ===== getter / setter =====

    public Librarian getCurrentLibrarian() {
        return currentLibrarian;
    }

    public String getStatusFilter() {
        return statusFilter;
    }

    public void setStatusFilter(String statusFilter) {
        this.statusFilter = statusFilter;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    // 点击 Search 按钮调用：只是提交表单，实际过滤逻辑在 getRecords()
    public String applyFilters() {
        return null;  // 留在当前页
    }

    // 点击 Clear 按钮调用：清空搜索条件
    public String clearFilters() {
        this.keyword = null;
        // 看需求是否重置状态，这里保留当前 statusFilter
        // this.statusFilter = "ALL";
        return null;
    }

    /**
     * 根据当前 statusFilter + keyword 查询借阅记录
     */
    public List<BorrowingRecord> getRecords() {

        StringBuilder jpql = new StringBuilder(
                "SELECT r FROM BorrowingRecord r " +
                "JOIN FETCH r.borrower br " +
                "JOIN FETCH r.bookCopy c " +
                "JOIN FETCH c.book b "
        );

        boolean filterByStatus = (statusFilter != null && !"ALL".equalsIgnoreCase(statusFilter));
        boolean hasKeyword = (keyword != null && !keyword.trim().isEmpty());

        // 拼 WHERE 条件
        StringBuilder where = new StringBuilder();
        if (filterByStatus) {
            where.append(" r.status = :status ");
        }
        if (hasKeyword) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append(" (LOWER(br.firstName) LIKE :kw " +
                         "OR LOWER(br.lastName)  LIKE :kw " +
                         "OR LOWER(br.email)     LIKE :kw) ");
        }

        if (where.length() > 0) {
            jpql.append(" WHERE ").append(where);
        }

        jpql.append(" ORDER BY r.requestDate DESC, r.borrowDate DESC");

        var query = em.createQuery(jpql.toString(), BorrowingRecord.class);

        if (filterByStatus) {
            query.setParameter("status", statusFilter);
        }
        if (hasKeyword) {
            String kw = "%" + keyword.trim().toLowerCase() + "%";
            query.setParameter("kw", kw);
        }

        return query.getResultList();
    }
}