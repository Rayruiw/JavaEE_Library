package com.wangrui.librarymanagement.service;

import com.wangrui.librarymanagement.entity.Book;
import com.wangrui.librarymanagement.entity.BookCopy;
import com.wangrui.librarymanagement.entity.BookCopyStatus;
import com.wangrui.librarymanagement.entity.Borrower;
import com.wangrui.librarymanagement.entity.BorrowingRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Domain service that contains core borrowing workflow logic.
 * This service is purposely separated from JSF controllers and JPA EntityManager
 * so it can be unit-tested using JUnit.
 */
public class BorrowingWorkflowService {

    /**
     * Borrower requests to borrow a book.
     *
     * Rules:
     *  - Borrower must not be null.
     *  - Book must not be null.
     *  - There must be at least one AVAILABLE copy.
     *  - The selected copy becomes PENDING.
     *  - A BorrowingRecord is created with status REQUESTED_BORROW.
     *
     * @return the new BorrowingRecord, or null if no copies are available.
     */
    public BorrowingRecord requestBorrow(
            Borrower borrower,
            Book book,
            List<BookCopy> availableCopies,
            BookCopyStatus pendingStatus,
            LocalDate today
    ) {
        if (borrower == null) {
            throw new IllegalArgumentException("Borrower must not be null.");
        }
        if (book == null) {
            throw new IllegalArgumentException("Book must not be null.");
        }
        if (availableCopies == null || availableCopies.isEmpty()) {
            return null; // No available copies → return null.
        }
        if (pendingStatus == null) {
            throw new IllegalArgumentException("Pending status must not be null.");
        }
        if (today == null) {
            today = LocalDate.now();
        }

        // In this simplified version, always pick the first available copy
        BookCopy chosenCopy = availableCopies.get(0);

        // Change copy status to PENDING
        chosenCopy.setStatus(pendingStatus);

        // Create borrowing record
        BorrowingRecord record = new BorrowingRecord();
        record.setBorrower(borrower);
        record.setBookCopy(chosenCopy);
        record.setRequestDate(today);
        record.setBorrowDate(null);
        record.setDueDate(null);
        record.setReturnDate(null);
        record.setReturnRequestDate(null);
        record.setStatus("REQUESTED_BORROW");

        return record;
    }

    /**
     * Librarian approves the borrow request.
     *
     * Rules:
     *  - BorrowingRecord.status → BORROWED
     *  - BookCopy.status → BORROWED
     *  - Set borrowDate = today
     *  - Set dueDate = today + 14 days
     */
    public void approveBorrow(
            BorrowingRecord record,
            BookCopyStatus borrowedStatus,
            LocalDate today
    ) {
        Objects.requireNonNull(record, "record must not be null");
        Objects.requireNonNull(record.getBookCopy(), "bookCopy must not be null");
        Objects.requireNonNull(borrowedStatus, "borrowedStatus must not be null");

        if (today == null) today = LocalDate.now();

        record.setStatus("BORROWED");
        record.setBorrowDate(today);
        record.setDueDate(today.plusDays(14));

        record.getBookCopy().setStatus(borrowedStatus);
    }

    /**
     * Librarian rejects the request.
     *
     * Rules:
     *  - BorrowingRecord.status → INCOMPLETE
     *  - BookCopy.status → AVAILABLE
     */
    public void rejectBorrow(BorrowingRecord record, BookCopyStatus availableStatus) {
        Objects.requireNonNull(record, "record must not be null");
        Objects.requireNonNull(record.getBookCopy(), "bookCopy must not be null");
        Objects.requireNonNull(availableStatus, "availableStatus must not be null");

        record.setStatus("INCOMPLETE");
        record.getBookCopy().setStatus(availableStatus);
    }

    /**
     * Borrower initiates a return request.
     *
     * Rules:
     *  - BorrowingRecord.status → RETURN_REQUESTED
     *  - BookCopy stays BORROWED
     */
    public void borrowerRequestReturn(BorrowingRecord record, LocalDate today) {
        Objects.requireNonNull(record, "record must not be null");
        if (today == null) today = LocalDate.now();

        record.setStatus("RETURN_REQUESTED");
        record.setReturnRequestDate(today);
    }

    /**
     * Librarian finalizes the return.
     *
     * Rules:
     *  - Set returnDate = today
     *  - Update copy status to finalCopyStatus
     *  - If copyStatus = AVAILABLE → record.status = COMPLETED
     *  - Otherwise (LOST/DAMAGED) → record.status = INCOMPLETE
     */
    public void librarianFinalizeReturn(
            BorrowingRecord record,
            BookCopyStatus finalCopyStatus,
            LocalDate today
    ) {
        Objects.requireNonNull(record, "record must not be null");
        Objects.requireNonNull(record.getBookCopy(), "bookCopy must not be null");
        Objects.requireNonNull(finalCopyStatus, "finalCopyStatus must not be null");

        if (today == null) today = LocalDate.now();

        record.setReturnDate(today);
        record.getBookCopy().setStatus(finalCopyStatus);

        if ("AVAILABLE".equalsIgnoreCase(finalCopyStatus.getCode())) {
            record.setStatus("COMPLETED");
        } else {
            record.setStatus("INCOMPLETE");
        }
    }
}