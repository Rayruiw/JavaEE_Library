package com.wangrui.librarymanagement.beans;

import com.wangrui.librarymanagement.entity.Book;
import com.wangrui.librarymanagement.entity.BookCopy;
import com.wangrui.librarymanagement.entity.BookCopyStatus;
import com.wangrui.librarymanagement.entity.Librarian;
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
public class LibrarianBookCopyBean implements Serializable {

    @PersistenceContext(unitName = "LibraryPU")
    private EntityManager em;

    // 当前登录的图书管理员（做权限）
    private Librarian currentLibrarian;

    // 列表数据
    private List<BookCopy> copies;
    private List<Book> books;
    private List<BookCopyStatus> statuses;

    // 右侧表单正在编辑的副本
    private BookCopy editingCopy;

    // 下拉框选中的 book / status ID
    private Long selectedBookId;
    private Long selectedStatusId;

    // 是否显示右侧表单
    private boolean showForm = false;

    // 搜索用：按书名模糊查询
    private String searchTitle;

    // 是否已经做过「首次初始化」
    private boolean initialized = false;

    // ============ PostConstruct：只做轻量初始化 ============
    @PostConstruct
    public void init() {
        // 这里只做很轻的初始化，真正的登录检查和数据加载放在 ensureLoggedIn()
        showForm = false;
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

        // ② 已登录：如果本地缓存为空，或者还没初始化过，就加载一次数据
        Librarian sessionLib = (Librarian) obj;
        if (currentLibrarian == null || !sessionLib.getId().equals(currentLibrarian.getId())) {
            currentLibrarian = em.find(Librarian.class, sessionLib.getId());
            initialized = false;  // 换人了，就重新初始化
        }

        if (!initialized) {
            loadBooks();
            loadStatuses();
            refreshCopies();
            editingCopy = null;
            showForm = false;
            initialized = true;
        }
    }

    // ============ 加载辅助数据 ============
    private void loadBooks() {
        books = em.createQuery(
                "SELECT b FROM Book b ORDER BY b.title",
                Book.class
        ).getResultList();
    }

    private void loadStatuses() {
        statuses = em.createQuery(
                "SELECT s FROM BookCopyStatus s ORDER BY s.id",
                BookCopyStatus.class
        ).getResultList();
    }

    private void refreshCopies() {
        if (searchTitle != null && !searchTitle.isBlank()) {
            String kw = "%" + searchTitle.trim().toLowerCase() + "%";
            copies = em.createQuery(
                    "SELECT c FROM BookCopy c " +
                            "JOIN FETCH c.book b " +
                            "JOIN FETCH c.status s " +
                            "WHERE LOWER(b.title) LIKE :kw " +
                            "ORDER BY b.title, c.id",
                    BookCopy.class
            ).setParameter("kw", kw)
             .getResultList();
        } else {
            copies = em.createQuery(
                    "SELECT c FROM BookCopy c " +
                            "JOIN FETCH c.book b " +
                            "JOIN FETCH c.status s " +
                            "ORDER BY b.title, c.id",
                    BookCopy.class
            ).getResultList();
        }
    }

    // ============ getter / setter ============
    public Librarian getCurrentLibrarian() {
        return currentLibrarian;
    }

    public List<BookCopy> getCopies() {
        return copies;
    }

    public List<Book> getBooks() {
        return books;
    }

    public List<BookCopyStatus> getStatuses() {
        return statuses;
    }

    public BookCopy getEditingCopy() {
        return editingCopy;
    }

    public void setEditingCopy(BookCopy editingCopy) {
        this.editingCopy = editingCopy;
    }

    public Long getSelectedBookId() {
        return selectedBookId;
    }

    public void setSelectedBookId(Long selectedBookId) {
        this.selectedBookId = selectedBookId;
    }

    public Long getSelectedStatusId() {
        return selectedStatusId;
    }

    public void setSelectedStatusId(Long selectedStatusId) {
        this.selectedStatusId = selectedStatusId;
    }

    public boolean isShowForm() {
        return showForm;
    }

    public void setShowForm(boolean showForm) {
        this.showForm = showForm;
    }

    public String getSearchTitle() {
        return searchTitle;
    }

    public void setSearchTitle(String searchTitle) {
        this.searchTitle = searchTitle;
    }

    // ============ 列表搜索 ============
    public String search() {
        refreshCopies();
        return null;
    }

    public String clearSearch() {
        searchTitle = null;
        refreshCopies();
        return null;
    }

    // ============ New / Edit / Cancel ============
    /** 点击 “New Copy” */
    public String startNew() {
        editingCopy = new BookCopy();
        selectedBookId = null;

        // 默认状态设为 AVAILABLE（如果存在）
        selectedStatusId = null;
        if (statuses != null) {
            for (BookCopyStatus s : statuses) {
                if ("AVAILABLE".equalsIgnoreCase(s.getCode())) {
                    selectedStatusId = s.getId();
                    break;
                }
            }
        }

        showForm = true;
        return null;
    }

    /** 点击 “Edit” */
    public String startEdit(BookCopy copy) {
        editingCopy = em.find(BookCopy.class, copy.getId());
        if (editingCopy != null) {
            if (editingCopy.getBook() != null) {
                selectedBookId = editingCopy.getBook().getId();
            } else {
                selectedBookId = null;
            }
            if (editingCopy.getStatus() != null) {
                selectedStatusId = editingCopy.getStatus().getId();
            } else {
                selectedStatusId = null;
            }
        }
        showForm = true;
        return null;
    }

    /** 点击 Cancel */
    public String resetForm() {
        editingCopy = null;
        selectedBookId = null;
        selectedStatusId = null;
        showForm = false;
        return null;
    }

    // ============ Save ============
    @Transactional
    public String saveCopy() {
        if (editingCopy == null) {
            addMessage("No copy to save.");
            return null;
        }

        // 选中的图书
        if (selectedBookId == null) {
            addMessage("Please select a book.");
            return null;
        }
        Book book = em.find(Book.class, selectedBookId);
        if (book == null) {
            addMessage("Selected book does not exist.");
            return null;
        }
        editingCopy.setBook(book);

        // 选中的状态
        if (selectedStatusId == null) {
            addMessage("Please select a status.");
            return null;
        }
        BookCopyStatus status = em.find(BookCopyStatus.class, selectedStatusId);
        if (status == null) {
            addMessage("Selected status does not exist.");
            return null;
        }
        editingCopy.setStatus(status);

        if (editingCopy.getId() == null) {
            em.persist(editingCopy);
            addMessage("Book copy created.");
        } else {
            editingCopy = em.merge(editingCopy);
            addMessage("Book copy updated.");
        }

        refreshCopies();
        showForm = false;
        return null;
    }

    // ============ 工具 ============
    private void addMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(
                null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null)
        );
    }
}