package com.wangrui.librarymanagement.resources;

import com.wangrui.librarymanagement.entity.BookCopyStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("copy-status")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookCopyStatusResource {

    @PersistenceContext(unitName = "LibraryPU")
    private EntityManager em;

    // GET /api/copy-status  —— 查询所有状态
    @GET
    public List<BookCopyStatus> getAll() {
        return em.createQuery("SELECT s FROM BookCopyStatus s",
                        BookCopyStatus.class)
                 .getResultList();
    }

    // GET /api/copy-status/{id}
    @GET
    @Path("{id}")
    public BookCopyStatus findById(@PathParam("id") Long id) {
        return em.find(BookCopyStatus.class, id);
    }

    // POST /api/copy-status  —— 新增状态（一般初始化用）
    @POST
    @Transactional
    public BookCopyStatus create(BookCopyStatus status) {
        em.persist(status);
        return status;
    }
}