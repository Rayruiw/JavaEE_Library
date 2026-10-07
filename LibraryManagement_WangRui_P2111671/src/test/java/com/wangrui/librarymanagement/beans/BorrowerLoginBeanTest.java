package com.wangrui.librarymanagement.beans;

import com.wangrui.librarymanagement.entity.Borrower;
import com.wangrui.librarymanagement.util.PasswordUtil;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 单元测试 BorrowerLoginBean.login()
 * 说明：使用 Mockito 模拟 FacesContext / EntityManager，不需要真正的数据库和 JSF 环境。
 */
public class BorrowerLoginBeanTest {

    private BorrowerLoginBean bean;

    // mocks
    private FacesContext facesContext;
    private ExternalContext externalContext;
    private EntityManager em;
    private TypedQuery<Borrower> typedQuery;

    // “伪造”的返回数据
    private final List<Borrower> queryResult = new ArrayList<>();
    private final Map<String, Object> sessionMap = new HashMap<>();

    @BeforeEach
    void setUp() throws Exception {

        // 1. mock FacesContext & ExternalContext
        facesContext = mock(FacesContext.class);
        externalContext = mock(ExternalContext.class);
        when(facesContext.getExternalContext()).thenReturn(externalContext);
        when(externalContext.getSessionMap()).thenReturn(sessionMap);

        // addMessage 只是验证被调用，不需要真实逻辑
        doNothing().when(facesContext)
                .addMessage(any(), any(FacesMessage.class));

        // 2. mock EntityManager & TypedQuery
        em = mock(EntityManager.class);
        typedQuery = mock(TypedQuery.class);

        when(em.createQuery(anyString(), eq(Borrower.class)))
                .thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), any()))
                .thenReturn(typedQuery);
        // 每次 getResultList() 都返回我们控制的 queryResult
        when(typedQuery.getResultList()).thenReturn(queryResult);

        // 3. 创建一个可以 override getFacesContext() 的 bean
        bean = new BorrowerLoginBean() {
            @Override
            protected FacesContext getFacesContext() {
                return facesContext;
            }
        };

        // 用反射把 mock 的 EntityManager 塞进去
        Field f = BorrowerLoginBean.class.getDeclaredField("em");
        f.setAccessible(true);
        f.set(bean, em);

        // 每个测试前清空
        queryResult.clear();
        sessionMap.clear();
    }

    // ========== 正常情况 ==========

    @Test
    void testLogin_Success() {
        String email = "test@example.com";
        String rawPwd = "Abc12345!";
        String hash = PasswordUtil.hashPassword(rawPwd);

        Borrower b = new Borrower();
        b.setEmail(email);
        b.setPasswordHash(hash);

        queryResult.add(b);

        bean.setEmail(email);
        bean.setPassword(rawPwd);

        String outcome = bean.login();

        assertEquals("borrower-available-books?faces-redirect=true", outcome);
        assertSame(b, sessionMap.get("loggedInBorrower"),
                   "Borrower should be stored in session when login succeeds");
    }

    @Test
    void testLogin_UnknownEmail() {
        // queryResult 为空 -> 模拟数据库中没有这个邮箱
        bean.setEmail("notfound@example.com");
        bean.setPassword("Whatever123!");

        String outcome = bean.login();

        assertNull(outcome, "Unknown email should stay on login page");
        verify(facesContext, atLeastOnce())
                .addMessage(isNull(), any(FacesMessage.class));
        assertTrue(sessionMap.isEmpty(), "No user should be stored in session");
    }

    @Test
    void testLogin_WrongPassword() {
        String email = "user@example.com";
        String realPwd = "Abc12345!";
        String hash = PasswordUtil.hashPassword(realPwd);

        Borrower b = new Borrower();
        b.setEmail(email);
        b.setPasswordHash(hash);
        queryResult.add(b);

        bean.setEmail(email);
        bean.setPassword("WrongPassword!!!");

        String outcome = bean.login();

        assertNull(outcome, "Wrong password should not redirect");
        verify(facesContext, atLeastOnce())
                .addMessage(isNull(), any(FacesMessage.class));
        assertTrue(sessionMap.isEmpty(), "Session should still be empty");
    }

    // ========== 极端 / 边界情况 ==========

    /**
     * 邮箱为空字符串或全是空格
     * 行为：当作查不到用户 -> 显示错误消息，留在登录页
     */
    @Test
    void testLogin_BlankEmail() {
        bean.setEmail("   ");   // 全是空格
        bean.setPassword("Abc12345!");

        // 没有向 queryResult 添加任何 Borrower -> 模拟“查不到”
        String outcome = bean.login();

        assertNull(outcome);
        verify(facesContext, atLeastOnce())
                .addMessage(isNull(), any(FacesMessage.class));
    }

    /**
     * 密码为 null 或空字符串
     * 说明：此测试假定 PasswordUtil.verifyPassword 对 null 安全地返回 false
     */
    @Test
    void testLogin_NullPassword() {
        String email = "nullpwd@example.com";
        String realPwd = "Abc12345!";
        String hash = PasswordUtil.hashPassword(realPwd);

        Borrower b = new Borrower();
        b.setEmail(email);
        b.setPasswordHash(hash);
        queryResult.add(b);

        bean.setEmail(email);
        bean.setPassword(null);    // 极端情况：null

        String outcome = bean.login();

        assertNull(outcome, "Null password should fail login");
        verify(facesContext, atLeastOnce())
                .addMessage(isNull(), any(FacesMessage.class));
        assertTrue(sessionMap.isEmpty());
    }

    /**
     * same email 返回多条记录（防御性测试）
     */
    @Test
    void testLogin_MultipleBorrowersSameEmail_UseFirst() {
        String email = "multi@example.com";
        String pwd = "Abc12345!";
        String hash1 = PasswordUtil.hashPassword(pwd);
        String hash2 = PasswordUtil.hashPassword("AnotherPwd999!");

        Borrower first = new Borrower();
        first.setEmail(email);
        first.setPasswordHash(hash1);

        Borrower second = new Borrower();
        second.setEmail(email);
        second.setPasswordHash(hash2);

        // 模拟 DB 返回两条记录，顺序为 first, second
        queryResult.add(first);
        queryResult.add(second);

        bean.setEmail(email);
        bean.setPassword(pwd);

        String outcome = bean.login();

        assertEquals("borrower-available-books?faces-redirect=true", outcome);
        assertSame(first, sessionMap.get("loggedInBorrower"),
                   "Bean should use the first record from result list");
    }

    /**
     * Borrower.passwordHash = null
     * 行为：verifyPassword 应返回 false -> 登录失败
     */
    @Test
    void testLogin_BorrowerWithNullPasswordHash() {
        String email = "nullhash@example.com";

        Borrower b = new Borrower();
        b.setEmail(email);
        b.setPasswordHash(null);   // 极端情况

        queryResult.add(b);

        bean.setEmail(email);
        bean.setPassword("Abc12345!");

        String outcome = bean.login();

        assertNull(outcome);
        verify(facesContext, atLeastOnce())
                .addMessage(isNull(), any(FacesMessage.class));
        assertTrue(sessionMap.isEmpty());
    }

    /**
     * 非常长的密码字符串（压力测试，不关注业务逻辑，只关注不抛异常）
     */
    @Test
    void testLogin_VeryLongPassword() {
        String email = "longpwd@example.com";

        // 生成一个比较长的原始密码
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {   // 200 字符
            sb.append("Abc123!");
        }
        String longPwd = sb.toString();
        String hash = PasswordUtil.hashPassword(longPwd);

        Borrower b = new Borrower();
        b.setEmail(email);
        b.setPasswordHash(hash);
        queryResult.add(b);

        bean.setEmail(email);
        bean.setPassword(longPwd);

        String outcome = bean.login();

        // 只要不抛异常，且能正常成功登录就算通过
        assertEquals("borrower-available-books?faces-redirect=true", outcome);
        assertSame(b, sessionMap.get("loggedInBorrower"));
    }
}