package com.wangrui.librarymanagement.util;

import com.wangrui.librarymanagement.entity.BookCopyStatus;
import com.wangrui.librarymanagement.entity.BorrowingRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Utility class for determining overdue borrowing records
 * and marking overdue copies as LOST.
 */
public final class OverdueUtil {

    private OverdueUtil() {}

    /** Returns true if dueDate is earlier than today. */
    public static boolean isOverdue(LocalDate dueDate, LocalDate today) {
        if (dueDate == null || today == null) {
            return false;
        }
        return dueDate.isBefore(today);
    }

    /**
     * Mark overdue borrowed/return-requested items as LOST.
     *
     * Rules:
     *   - status ∈ { BORROWED, RETURN_REQUESTED }
     *   - dueDate < today
     *   - returnDate == null
     *
     * Changes:
     *   - record.status = INCOMPLETE
     *   - bookCopy.status = LOST
     */
    public static void markOverdueAsLost(
            List<BorrowingRecord> records,
            BookCopyStatus lostStatus,
            LocalDate today
    ) {
        Objects.requireNonNull(records);
        Objects.requireNonNull(lostStatus);

        for (BorrowingRecord r : records) {
            if (r == null || r.getBookCopy() == null) continue;

            boolean activeBorrow =
                    "BORROWED".equals(r.getStatus()) ||
                    "RETURN_REQUESTED".equals(r.getStatus());

            if (activeBorrow &&
                r.getDueDate() != null &&
                r.getReturnDate() == null &&
                isOverdue(r.getDueDate(), today)) {

                r.setStatus("INCOMPLETE");
                r.getBookCopy().setStatus(lostStatus);
            }
        }
    }
}