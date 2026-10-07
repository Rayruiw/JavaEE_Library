package com.wangrui.librarymanagement.service;

import com.wangrui.librarymanagement.entity.Book;
import com.wangrui.librarymanagement.entity.BookCopy;
import com.wangrui.librarymanagement.entity.BookCopyStatus;
import com.wangrui.librarymanagement.entity.Borrower;
import com.wangrui.librarymanagement.entity.BorrowingRecord;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BorrowingWorkflowServiceTest {

    private final BorrowingWorkflowService service = new BorrowingWorkflowService();

    @Test
    public void testRequestBorrow_Success() {
        Borrower borrower = new Borrower();
        borrower.setFirstName("Alice");

        Book book = new Book();
        book.setTitle("Java Learning");

        BookCopy copy = new BookCopy();
        copy.setBook(book);

        BookCopyStatus pending = new BookCopyStatus();
        pending.setCode("PENDING");

        List<BookCopy> list = new ArrayList<>();
        list.add(copy);

        LocalDate today = LocalDate.of(2025, 1, 1);

        BorrowingRecord r = service.requestBorrow(borrower, book, list, pending, today);

        assertNotNull(r);
        assertEquals("REQUESTED_BORROW", r.getStatus());
        assertEquals("PENDING", r.getBookCopy().getStatus().getCode());
        assertEquals(today, r.getRequestDate());
    }

    @Test
    public void testRequestBorrow_NoAvailableCopy() {
        Borrower borrower = new Borrower();
        Book book = new Book();
        BookCopyStatus pending = new BookCopyStatus();

        BorrowingRecord r = service.requestBorrow(borrower, book, new ArrayList<>(), pending, LocalDate.now());

        assertNull(r, "Should return null when no available copies.");
    }

    @Test
    public void testRequestBorrow_BorrowerNull_ThrowsException() {
        Book book = new Book();
        List<BookCopy> list = new ArrayList<>();
        list.add(new BookCopy());

        BookCopyStatus pending = new BookCopyStatus();

        assertThrows(IllegalArgumentException.class, () ->
                service.requestBorrow(null, book, list, pending, LocalDate.now())
        );
    }

    @Test
    public void testApproveBorrow() {
        Borrower borrower = new Borrower();
        Book book = new Book();
        BookCopy copy = new BookCopy();
        copy.setBook(book);

        BookCopyStatus pending = new BookCopyStatus();
        pending.setCode("PENDING");

        BookCopyStatus borrowed = new BookCopyStatus();
        borrowed.setCode("BORROWED");

        List<BookCopy> list = new ArrayList<>();
        list.add(copy);

        LocalDate today = LocalDate.of(2025, 2, 1);

        BorrowingRecord r = service.requestBorrow(borrower, book, list, pending, today);
        service.approveBorrow(r, borrowed, today);

        assertEquals("BORROWED", r.getStatus());
        assertEquals("BORROWED", copy.getStatus().getCode());
        assertEquals(today.plusDays(14), r.getDueDate());
    }

    @Test
    public void testRejectBorrow() {
        Borrower borrower = new Borrower();
        Book book = new Book();
        BookCopy copy = new BookCopy();
        copy.setBook(book);

        BookCopyStatus pending = new BookCopyStatus();
        pending.setCode("PENDING");
        BookCopyStatus available = new BookCopyStatus();
        available.setCode("AVAILABLE");

        List<BookCopy> list = new ArrayList<>();
        list.add(copy);

        BorrowingRecord r = service.requestBorrow(borrower, book, list, pending, LocalDate.now());
        service.rejectBorrow(r, available);

        assertEquals("INCOMPLETE", r.getStatus());
        assertEquals("AVAILABLE", copy.getStatus().getCode());
    }

    @Test
    public void testBorrowerRequestReturn() {
        Borrower borrower = new Borrower();
        Book book = new Book();
        BookCopy copy = new BookCopy();
        copy.setBook(book);

        BookCopyStatus pending = new BookCopyStatus();
        pending.setCode("PENDING");
        BookCopyStatus borrowed = new BookCopyStatus();
        borrowed.setCode("BORROWED");

        LocalDate today = LocalDate.of(2025, 3, 1);

        List<BookCopy> list = new ArrayList<>();
        list.add(copy);

        BorrowingRecord r = service.requestBorrow(borrower, book, list, pending, today.minusDays(1));
        service.approveBorrow(r, borrowed, today.minusDays(1));

        service.borrowerRequestReturn(r, today);

        assertEquals("RETURN_REQUESTED", r.getStatus());
        assertEquals(today, r.getReturnRequestDate());
        assertEquals("BORROWED", copy.getStatus().getCode());
    }

    @Test
    public void testLibrarianFinalizeReturn_Available() {
        Borrower borrower = new Borrower();
        Book book = new Book();
        BookCopy copy = new BookCopy();
        copy.setBook(book);

        BookCopyStatus available = new BookCopyStatus();
        available.setCode("AVAILABLE");

        BookCopyStatus pending = new BookCopyStatus();
        pending.setCode("PENDING");

        BookCopyStatus borrowed = new BookCopyStatus();
        borrowed.setCode("BORROWED");

        LocalDate today = LocalDate.of(2025, 4, 1);

        List<BookCopy> list = new ArrayList<>();
        list.add(copy);

        BorrowingRecord r = service.requestBorrow(borrower, book, list, pending, today.minusDays(5));
        service.approveBorrow(r, borrowed, today.minusDays(5));
        service.borrowerRequestReturn(r, today.minusDays(1));

        service.librarianFinalizeReturn(r, available, today);

        assertEquals("COMPLETED", r.getStatus());
        assertEquals("AVAILABLE", copy.getStatus().getCode());
    }
}