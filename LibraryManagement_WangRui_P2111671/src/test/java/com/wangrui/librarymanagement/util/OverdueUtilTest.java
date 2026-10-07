package com.wangrui.librarymanagement.util;

import com.wangrui.librarymanagement.entity.BookCopy;
import com.wangrui.librarymanagement.entity.BookCopyStatus;
import com.wangrui.librarymanagement.entity.BorrowingRecord;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OverdueUtilTest {

    @Test
    public void testIsOverdue() {
        LocalDate today = LocalDate.of(2025, 1, 10);

        assertTrue(OverdueUtil.isOverdue(today.minusDays(1), today));
        assertFalse(OverdueUtil.isOverdue(today, today));
        assertFalse(OverdueUtil.isOverdue(today.plusDays(1), today));
    }

    @Test
    public void testMarkOverdueAsLost() {
        LocalDate today = LocalDate.of(2025, 2, 1);

        BookCopyStatus lost = new BookCopyStatus();
        lost.setCode("LOST");

        // Overdue
        BorrowingRecord r1 = new BorrowingRecord();
        r1.setStatus("BORROWED");
        r1.setDueDate(today.minusDays(5));
        r1.setReturnDate(null);
        r1.setBookCopy(new BookCopy());

        // Not overdue
        BorrowingRecord r2 = new BorrowingRecord();
        r2.setStatus("BORROWED");
        r2.setDueDate(today.plusDays(3));
        r2.setReturnDate(null);
        r2.setBookCopy(new BookCopy());

        // Already returned
        BorrowingRecord r3 = new BorrowingRecord();
        r3.setStatus("BORROWED");
        r3.setDueDate(today.minusDays(5));
        r3.setReturnDate(today.minusDays(1));
        r3.setBookCopy(new BookCopy());

        List<BorrowingRecord> list = new ArrayList<>();
        list.add(r1);
        list.add(r2);
        list.add(r3);

        OverdueUtil.markOverdueAsLost(list, lost, today);

        assertEquals("INCOMPLETE", r1.getStatus());
        assertEquals("LOST", r1.getBookCopy().getStatus().getCode());

        assertEquals("BORROWED", r2.getStatus());
        assertNull(r2.getBookCopy().getStatus());

        assertEquals("BORROWED", r3.getStatus());
        assertNull(r3.getBookCopy().getStatus());
    }
}