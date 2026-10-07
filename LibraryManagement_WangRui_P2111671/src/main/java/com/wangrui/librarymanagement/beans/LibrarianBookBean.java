package com.wangrui.librarymanagement.beans;

import com.wangrui.librarymanagement.entity.Book;
import com.wangrui.librarymanagement.entity.BookCategory;
import com.wangrui.librarymanagement.entity.Librarian;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Part;
import jakarta.transaction.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Named
@SessionScoped
public class LibrarianBookBean implements Serializable {

    @PersistenceContext(unitName = "LibraryPU")
    private EntityManager em;

    private Librarian currentLibrarian;

    private List<Book> books;
    private List<BookCategory> categories;

    private Book editingBook = new Book();
    private Long selectedCategoryId;
    private Part uploadedFile;
    
    // 搜索关键字（title / author / isbn）
    private String searchKeyword;


    /** 控制右侧表单是否显示 */
    private boolean showForm = false;

    // ================= 初始化 =================
    @PostConstruct
    public void init() {
        // 这里只做轻量级初始化，真正的登录检查放到 ensureLoggedIn()
        showForm = false;
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

        // ② 已登录：如果本地缓存为空或与 session 中不一致，则重新加载
        Librarian sessionLib = (Librarian) obj;
        if (currentLibrarian == null
                || currentLibrarian.getId() == null
                || !currentLibrarian.getId().equals(sessionLib.getId())) {

            currentLibrarian = em.find(Librarian.class, sessionLib.getId());
            refreshBooks();
            loadCategories();
            if (editingBook == null) {
                editingBook = new Book();
            }
            showForm = false;
        }
    }

    // ================== 加载数据 ==================
    private void refreshBooks() {
        String kw = (searchKeyword == null) ? "" : searchKeyword.trim().toLowerCase();

        if (!kw.isEmpty()) {
            // 有搜索关键字：按 title / author / isbn 模糊查询
            books = em.createQuery(
                    "SELECT b FROM Book b " +
                    "WHERE (LOWER(b.title)  LIKE :kw " +
                    "   OR LOWER(b.author) LIKE :kw " +
                    "   OR LOWER(b.isbn)   LIKE :kw) " +
                    "ORDER BY b.active DESC, b.title",
                    Book.class
            ).setParameter("kw", "%" + kw + "%")
             .getResultList();
        } else {
            // 没有搜索关键字：查全部
            books = em.createQuery(
                    "SELECT b FROM Book b ORDER BY b.active DESC, b.title",
                    Book.class
            ).getResultList();
        }
    }

    private void loadCategories() {
        categories = em.createQuery(
                "SELECT c FROM BookCategory c ORDER BY c.code",
                BookCategory.class
        ).getResultList();
    }
    
    // 点击 Search 按钮
    public String search() {
        refreshBooks();
        return null; // 留在当前页
    }

    // 点击 Reset 按钮
    public String clearSearch() {
        searchKeyword = null;
        refreshBooks();
        return null;
    }

    // =============== getter / setter ===============
    public List<Book> getBooks() { return books; }

    public List<BookCategory> getCategories() { return categories; }

    public Book getEditingBook() { return editingBook; }

    public void setEditingBook(Book editingBook) { this.editingBook = editingBook; }

    public Long getSelectedCategoryId() { return selectedCategoryId; }

    public void setSelectedCategoryId(Long selectedCategoryId) { this.selectedCategoryId = selectedCategoryId; }

    public Part getUploadedFile() { return uploadedFile; }

    public void setUploadedFile(Part uploadedFile) { this.uploadedFile = uploadedFile; }

    public boolean isShowForm() { return showForm; }

    public void setShowForm(boolean showForm) { this.showForm = showForm; }

    public Librarian getCurrentLibrarian() { return currentLibrarian; }
    
    public String getSearchKeyword() {return searchKeyword;}

    public void setSearchKeyword(String searchKeyword) {this.searchKeyword = searchKeyword;}

    // =============== New / Edit / Cancel ===============

    /** 点击 New Book */
    public String startNew() {
        editingBook = new Book();
        editingBook.setActive(true);         // 新书默认有效
        selectedCategoryId = null;
        uploadedFile = null;
        showForm = true;                     // 展开右侧表单
        return "librarian-manage-books?faces-redirect=true";
    }

    /** 点击 Edit */
    public String startEdit(Book b) {
        editingBook = em.find(Book.class, b.getId());
        if (editingBook != null && editingBook.getCategory() != null) {
            selectedCategoryId = editingBook.getCategory().getId();
        } else {
            selectedCategoryId = null;
        }
        uploadedFile = null;
        showForm = true;                     // 展开右侧表单
        return "librarian-manage-books?faces-redirect=true";
    }

    /** 点击 Cancel */
    public String resetForm() {
        editingBook = new Book();
        selectedCategoryId = null;
        uploadedFile = null;
        showForm = false;                    // 折叠右侧表单
        return null;
    }

    // =============== 保存 / 逻辑删除 ===============

    @Transactional
    public String saveBook() {

        if (editingBook == null) {
            editingBook = new Book();
        }

        boolean isNew = (editingBook.getId() == null);

        // 处理分类
        if (selectedCategoryId != null) {
            BookCategory cat = em.find(BookCategory.class, selectedCategoryId);
            editingBook.setCategory(cat);
        } else {
            editingBook.setCategory(null);
        }

        if (uploadedFile != null) {
            System.out.println("[DEBUG] uploadedFile name = " + uploadedFile.getSubmittedFileName()
                    + ", size = " + uploadedFile.getSize()
                    + ", contentType = " + uploadedFile.getContentType());
        } else {
            System.out.println("[DEBUG] uploadedFile is NULL");
        }

        // 1) 有选文件：保存到 /images/books，并更新 coverImageUrl
        if (uploadedFile != null && uploadedFile.getSize() > 0) {
            String dbPath = saveUploadedFile(uploadedFile);
            if (dbPath != null) {
                editingBook.setCoverImageUrl(dbPath);
            }
        }
        // 2) 新书 + 目前还没封面 → 给默认占位图
        else if (isNew &&
                (editingBook.getCoverImageUrl() == null
                        || editingBook.getCoverImageUrl().isBlank())) {
            editingBook.setCoverImageUrl("/images/Image-Placeholder.png");
        }

        if (isNew) {
            editingBook.setActive(true);
            em.persist(editingBook);
            addMessage("Book created.");
        } else {
            editingBook = em.merge(editingBook);
            addMessage("Book updated.");
        }

        refreshBooks();
        showForm = false;
        uploadedFile = null;

        return null;
    }

    /**  active = false， DB soft delete */
    @Transactional
    public String softDelete(Book b) {
        Book managed = em.find(Book.class, b.getId());
        if (managed != null) {
            managed.setActive(false);
            em.merge(managed);
            addMessage("Book hidden (soft delete).");
            refreshBooks();
        }
        return null;
    }

    /** active ↔ inactive */
    @Transactional
    public String toggleActive(Book b) {
        Book managed = em.find(Book.class, b.getId());
        if (managed != null) {
            boolean current = Boolean.TRUE.equals(managed.isActive());
            managed.setActive(!current);
            em.merge(managed);
        }
        refreshBooks();
        return "librarian-manage-books?faces-redirect=true";
    }

    // =============== 上传文件工具 ===============

    private String saveUploadedFile(Part filePart) {

        try {
            String originalName = filePart.getSubmittedFileName();
            if (originalName == null || originalName.isBlank()) {
                return null;
            }
            originalName = Path.of(originalName).getFileName().toString();

            String fileName = System.currentTimeMillis() + "_" + originalName;

            FacesContext fc = FacesContext.getCurrentInstance();
            ServletContext sc =
                    (ServletContext) fc.getExternalContext().getContext();

            // 强制：无论如何都写入 webapp/images/books/
            String folderPath = sc.getRealPath("/images/books");

            if (folderPath == null) {
                folderPath = sc.getRealPath("/") + "images/books";
            }

            File folder = new File(folderPath);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            File dest = new File(folder, fileName);
            try (InputStream in = filePart.getInputStream()) {
                Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            return "/images/books/" + fileName;

        } catch (Exception e) {
            e.printStackTrace();
            addMessage("Upload failed: " + e.getMessage());
            return null;
        }
    }

    // =============== 消息工具 ===============

    private void addMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(
                null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null)
        );
    }
}