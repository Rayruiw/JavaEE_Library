package com.wangrui.librarymanagement.resources;

import com.wangrui.librarymanagement.entity.BookCategory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookCategoryResource {

    @PersistenceContext(unitName = "LibraryPU")
    private EntityManager em;

    /**
     * GET /api/categories
     * 列出所有分类
     */
    @GET
    public List<BookCategory> getAll() {
        return em.createQuery("SELECT c FROM BookCategory c", BookCategory.class)
                 .getResultList();
    }

    /**
     * GET /api/categories/{id}
     * 获取单个分类
     */
    @GET
    @Path("{id}")
    public BookCategory findById(@PathParam("id") Long id) {
        return em.find(BookCategory.class, id);
    }

    /**
     * POST /api/categories
     * 创建新的分类（建议只有管理员能使用）
     */
    @POST
    @Transactional
    public BookCategory create(BookCategory category) {
        em.persist(category);
        return category;
    }

    /**
     * PUT /api/categories/{id}
     * 修改分类（管理员操作）
     */
    @PUT
    @Path("{id}")
    @Transactional
    public BookCategory update(@PathParam("id") Long id, BookCategory updated) {
        BookCategory existing = em.find(BookCategory.class, id);
        if (existing == null) {
            throw new NotFoundException("Category not found");
        }

        existing.setCode(updated.getCode());
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());

        return existing;
    }

    /**
     * DELETE /api/categories/{id}
     * 删除分类（管理员操作）
     */
    @DELETE
    @Path("{id}")
    @Transactional
    public void delete(@PathParam("id") Long id) {
        BookCategory cat = em.find(BookCategory.class, id);
        if (cat != null) {
            em.remove(cat);
        }
    }
}