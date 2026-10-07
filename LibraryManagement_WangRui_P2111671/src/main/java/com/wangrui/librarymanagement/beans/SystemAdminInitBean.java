package com.wangrui.librarymanagement.beans;

import com.wangrui.librarymanagement.entity.SystemAdmin;
import com.wangrui.librarymanagement.util.PasswordUtil;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.Serializable;

@Named
@ApplicationScoped
public class SystemAdminInitBean implements Serializable {

    @PersistenceContext(unitName = "LibraryPU")
    private EntityManager em;

    @PostConstruct
    public void init() {
        // 检查是否已经有 system admin，如果没有就自动创建一个
        Long count = em.createQuery(
                        "SELECT COUNT(a) FROM SystemAdmin a",
                        Long.class)
                .getSingleResult();

        if (count == null || count == 0) {
            SystemAdmin admin = new SystemAdmin();
            admin.setEmail("admin@library.com");

            // 用你项目里的 PasswordUtil 统一加密
            String hash = PasswordUtil.hashPassword("Admin123!");
            admin.setPasswordHash(hash);

            em.persist(admin);
            System.out.println("[SystemAdminInitBean] Created default system admin: admin@library.com");
        }
    }
}