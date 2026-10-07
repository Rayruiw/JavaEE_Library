package com.wangrui.librarymanagement.resources;

import com.wangrui.librarymanagement.entity.Librarian;
import com.wangrui.librarymanagement.util.PasswordUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("librarians")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LibrarianResource {

    @PersistenceContext(unitName = "LibraryPU")
    private EntityManager em;

    public static class LoginRequest {
        public String email;
        public String password;
    }

    public static class LibrarianResponse {
        public Long id;
        public String email;
        public String firstName;
        public String lastName;

        public LibrarianResponse(Librarian l) {
            this.id = l.getId();
            this.email = l.getEmail();
            this.firstName = l.getFirstName();
            this.lastName = l.getLastName();
        }
    }

    // ===== 只提供登录接口（不对外开放注册） =====
    @POST
    @Path("login")
    public Response login(LoginRequest req) {

        if (req.email == null || req.email.isBlank()
                || req.password == null || req.password.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("email and password are required")
                    .build();
        }

        List<Librarian> list = em.createQuery(
                "SELECT l FROM Librarian l WHERE l.email = :email",
                Librarian.class
        )
        .setParameter("email", req.email)
        .setMaxResults(1)
        .getResultList();

        if (list.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Librarian not found")
                    .build();
        }

        Librarian l = list.get(0);

        boolean ok = PasswordUtil.verifyPassword(req.password, l.getPasswordHash());
        if (!ok) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid password")
                    .build();
        }

        return Response.ok(new LibrarianResponse(l)).build();
    }

    // ===== 调试用：不一定要在正式系统里开放 =====
    @GET
    public List<LibrarianResponse> getAll() {
        return em.createQuery("SELECT l FROM Librarian l", Librarian.class)
                .getResultList()
                .stream()
                .map(LibrarianResponse::new)
                .toList();
    }
}