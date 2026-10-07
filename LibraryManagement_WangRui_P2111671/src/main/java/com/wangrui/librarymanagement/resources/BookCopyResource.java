package com.wangrui.librarymanagement.resources;

import com.wangrui.librarymanagement.entity.Book;
import com.wangrui.librarymanagement.entity.BookCopy;
import com.wangrui.librarymanagement.entity.BookCopyStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.List;

@Path("copies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookCopyResource {

    @PersistenceContext(unitName = "LibraryPU")
    private EntityManager em;

    // 用于接收创建请求
    public static class CreateCopyRequest {
        public Long bookId;       // 必须字段
        public String statusCode; // 必须字段
    }

    public static class UpdateStatusRequest {
        public String statusCode;
    }

    // 1. 查询所有副本
    @GET
    public List<BookCopy> getAll() {
        return em.createQuery("SELECT c FROM BookCopy c", BookCopy.class)
                 .getResultList();
    }

    // 2. 按 ID 查询副本
    @GET
    @Path("{id}")
    public Response findById(@PathParam("id") Long id) {
        BookCopy copy = em.find(BookCopy.class, id);
        if (copy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(copy).build();
    }

    // 3. 查询某本书的所有副本
    @GET
    @Path("by-book/{bookId}")
    public List<BookCopy> findByBook(@PathParam("bookId") Long bookId) {
        return em.createQuery(
                "SELECT c FROM BookCopy c WHERE c.book.id = :bookId",
                BookCopy.class)
                 .setParameter("bookId", bookId)
                 .getResultList();
    }

    // 4. 创建副本
    @POST
    @Transactional
    public Response createCopy(CreateCopyRequest req) {
        if (req.bookId == null || req.statusCode == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("bookId and statusCode are required")
                    .build();
        }

        Book book = em.find(Book.class, req.bookId);
        if (book == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Book not found")
                    .build();
        }

        BookCopyStatus status = em.createQuery(
                        "SELECT s FROM BookCopyStatus s WHERE s.code = :code",
                        BookCopyStatus.class)
                .setParameter("code", req.statusCode)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (status == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Status not found")
                    .build();
        }

        BookCopy copy = new BookCopy(book, status);
        em.persist(copy);

        return Response.created(URI.create("/LibraryManagement/api/copies/" + copy.getId()))
                       .entity(copy)
                       .build();
    }

    // 5. 修改副本状态（借书、还书、丢失、损坏，都走这里）
    @PUT
    @Path("{id}/status")
    @Transactional
    public Response updateStatus(@PathParam("id") Long id, UpdateStatusRequest req) {
        BookCopy copy = em.find(BookCopy.class, id);
        if (copy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        BookCopyStatus status = em.createQuery(
                        "SELECT s FROM BookCopyStatus s WHERE s.code = :code",
                        BookCopyStatus.class)
                .setParameter("code", req.statusCode)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (status == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Status not found")
                    .build();
        }

        copy.setStatus(status);
        return Response.ok(copy).build();
    }
}