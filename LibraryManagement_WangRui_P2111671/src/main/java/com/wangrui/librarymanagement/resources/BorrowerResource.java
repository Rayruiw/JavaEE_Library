package com.wangrui.librarymanagement.resources;

import com.wangrui.librarymanagement.entity.Borrower;
import com.wangrui.librarymanagement.util.PasswordUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.util.List;

@Path("borrowers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BorrowerResource {

    @PersistenceContext(unitName = "LibraryPU")
    private EntityManager em;

    // ========== 1. 注册 ==========
    @POST
    @Path("register")
    @Transactional
    public Response register(RegisterRequest req) {

        // 1) 检查 email 是否已存在
        TypedQuery<Borrower> q = em.createQuery(
                "SELECT b FROM Borrower b WHERE b.email = :email",
                Borrower.class
        );
        q.setParameter("email", req.email);
        List<Borrower> existing = q.getResultList();
        if (!existing.isEmpty()) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("Email already registered")
                    .build();
        }

        // 2) 哈希密码
        String hashed = PasswordUtil.hashPassword(req.password);

        // 3) 创建实体
        Borrower b = new Borrower();
        b.setEmail(req.email);
        b.setPasswordHash(hashed);
        b.setFirstName(req.firstName);
        b.setLastName(req.lastName);

        // 出生日期是可选
        if (req.dateOfBirth != null && !req.dateOfBirth.isBlank()) {
            b.setDateOfBirth(LocalDate.parse(req.dateOfBirth)); // 格式：YYYY-MM-DD
        }

        // 4) 保存
        em.persist(b);

        // 5) 返回时不要暴露 passwordHash
        return Response.status(Response.Status.CREATED)
                .entity(new BorrowerResponse(b))
                .build();
    }

    // ========== 2. 登录 ==========
    @POST
    @Path("login")
    public Response login(LoginRequest req) {

        // 1) 根据 email 找用户
        TypedQuery<Borrower> q = em.createQuery(
                "SELECT b FROM Borrower b WHERE b.email = :email",
                Borrower.class
        );
        q.setParameter("email", req.email);
        List<Borrower> found = q.getResultList();

        if (found.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid email or password")
                    .build();
        }

        Borrower b = found.get(0);

        // 2) 校验密码
        boolean ok = PasswordUtil.verifyPassword(req.password, b.getPasswordHash());
        if (!ok) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid email or password")
                    .build();
        }

        // 3) 登录成功，返回基本信息（不带密码）
        return Response.ok(new BorrowerResponse(b)).build();
    }

    // （可选）3. 管理员查看所有借阅者
    @GET
    public List<BorrowerResponse> getAll() {
        List<Borrower> list = em.createQuery(
                "SELECT b FROM Borrower b",
                Borrower.class
        ).getResultList();

        return list.stream()
                .map(BorrowerResponse::new)
                .toList();
    }

    // ================= DTO 类 =================

    public static class RegisterRequest {
        public String email;
        public String password;
        public String firstName;
        public String lastName;
        public String dateOfBirth; // "1990-01-01" 这种格式，可选
    }

    public static class LoginRequest {
        public String email;
        public String password;
    }

    public static class BorrowerResponse {
        public Long id;
        public String email;
        public String firstName;
        public String lastName;
        public String dateOfBirth;

        public BorrowerResponse(Borrower b) {
            this.id = b.getId();
            this.email = b.getEmail();
            this.firstName = b.getFirstName();
            this.lastName = b.getLastName();
            this.dateOfBirth = (b.getDateOfBirth() == null)
                    ? null
                    : b.getDateOfBirth().toString();
        }
    }
}