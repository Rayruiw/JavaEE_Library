package com.wangrui.librarymanagement.resources;

import com.wangrui.librarymanagement.entity.Book;
import com.wangrui.librarymanagement.entity.BookCategory;
import com.wangrui.librarymanagement.entity.BookCopy;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("books")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookResource {

    @PersistenceContext(unitName = "LibraryPU")
    private EntityManager em;

    /**
     * GET /api/books
     * 获取所有书目
     */
    @GET
    public List<Book> getAll() {
        return em.createQuery("SELECT b FROM Book b", Book.class)
                 .getResultList();
    }

    /**
     * GET /api/books/{id}
     * 获取单本书
     */
    @GET
    @Path("{id}")
    public Book findById(@PathParam("id") Long id) {
        return em.find(Book.class, id);
    }

    /**
     * POST /api/books
     * 创建一本新书（包含分类）
     * JSON 结构示例：
     * {
     *   "title":"Java",
     *   "author":"Gosling",
     *   "isbn":"123",
     *   "publisher":"Oracle",
     *   "publishYear":2000,
     *   "category":{"id":3},
     *   "coverImageUrl":"/images/xxx.jpg"
     * }
     */
    @POST
    @Transactional
    public Book create(Book book) {

        // 分类必须是数据库中已有 ID
        if (book.getCategory() != null) {
            Long catId = book.getCategory().getId();
            BookCategory category = em.find(BookCategory.class, catId);
            book.setCategory(category);
        }

        em.persist(book);
        return book;
    }

    /**
     * PUT /api/books/{id}
     * 修改书信息
     */
    @PUT
    @Path("{id}")
    @Transactional
    public Book update(@PathParam("id") Long id, Book updated) {
        Book existing = em.find(Book.class, id);
        if (existing == null) {
            throw new NotFoundException("Book not found");
        }

        existing.setTitle(updated.getTitle());
        existing.setAuthor(updated.getAuthor());
        existing.setIsbn(updated.getIsbn());
        existing.setPublisher(updated.getPublisher());
        existing.setPublishYear(updated.getPublishYear());
        existing.setCoverImageUrl(updated.getCoverImageUrl());

        // 修改分类
        if (updated.getCategory() != null) {
            BookCategory cat = em.find(BookCategory.class, updated.getCategory().getId());
            existing.setCategory(cat);
        }

        return existing;
    }

    /**
     * DELETE /api/books/{id}
     */
    @DELETE
    @Path("{id}")
    @Transactional
    public void delete(@PathParam("id") Long id) {
        Book book = em.find(Book.class, id);
        if (book != null) {
            em.remove(book);
        }
    }
    
    // ===== 1) 搜索图书（按标题/作者模糊查询） =====
    @GET
    @Path("search")
    public List<Book> search(@QueryParam("keyword") String keyword) {
        if (keyword == null || keyword.isBlank()) {
            // 如果没给关键字，就返回所有
            return getAll();
        }

        String pattern = "%" + keyword.toLowerCase() + "%";

        return em.createQuery(
                "SELECT b FROM Book b " +
                "WHERE LOWER(b.title)  LIKE :kw " +
                "   OR LOWER(b.author) LIKE :kw",
                Book.class
        )
        .setParameter("kw", pattern)
        .getResultList();
    }

    // ===== 2) 查看某本书的所有副本 =====
    @GET
    @Path("{id}/copies")
    public List<BookCopyResponse> getCopiesByBook(@PathParam("id") Long bookId) {
        Book book = em.find(Book.class, bookId);
        if (book == null) {
            // 直接返回空列表也行，你也可以改成 Response 404
            return List.of();
        }

        List<BookCopy> copies = em.createQuery(
                "SELECT c FROM BookCopy c WHERE c.book.id = :bookId",
                BookCopy.class
        )
        .setParameter("bookId", bookId)
        .getResultList();

        return copies.stream()
                .map(BookCopyResponse::new)
                .collect(Collectors.toList());
    }

    // ===== 内部 DTO：只返回副本需要的信息 =====
    public static class BookCopyResponse {
        public Long id;
        public String statusCode;
        public String statusName;

        public BookCopyResponse(BookCopy c) {
            this.id = c.getId();
            if (c.getStatus() != null) {
                this.statusCode = c.getStatus().getCode();
                this.statusName = c.getStatus().getName();
            }
        }
    }

}