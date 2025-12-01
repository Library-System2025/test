import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Unit tests for the abstract Media class logic.
 * Covers borrow, return, overdue checks, and fine calculations using MockedStatic for time.
 * 
 * @author Zainab
 * @version 1.0
 */

public class MediaTest {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ---------- borrow() WITH TIME MOCK ----------

    @Test
    void testBorrow_setsBorrowedFieldsAndDueDate_withMockedTime() {

        
        LocalDate fixedDate = LocalDate.of(2025, 1, 10);

        try (MockedStatic<Media> mocked = mockStatic(Media.class)) {
            mocked.when(Media::now).thenReturn(fixedDate);

            Book b = new Book("Clean Code", "Robert Martin", "111");

            b.borrow("u1");

            assertEquals("Borrowed", b.getStatus());
            assertEquals("u1", b.getBorrowedBy());

            
            String expectedDue = fixedDate.plusDays(b.getLoanPeriod()).format(FMT);
            assertEquals(expectedDue, b.getDueDate());
            assertEquals(0.0, b.getFineAmount());
            assertEquals(0.0, b.getAmountPaid());
        }
    }

    // ---------- returnMedia() ----------

    @Test
    void testReturnMedia_resetsAllState() {
        Book b = new Book("Clean Code", "Robert Martin", "111");
        b.borrow("u1");
        b.setFineAmount(10.0);
        b.addPayment(3.0);

        b.returnMedia();

        assertEquals("Available", b.getStatus());
        assertEquals("", b.getBorrowedBy());
        assertEquals("", b.getDueDate());
        assertEquals(0.0, b.getFineAmount());
        assertEquals(0.0, b.getAmountPaid());
    }

    // ---------- isOverdue() WITH TIME MOCK ----------

    @Test
    void testIsOverdue_withMockedTime() {

        
        LocalDate fixedNow = LocalDate.of(2025, 1, 20);

        try (MockedStatic<Media> mocked = mockStatic(Media.class)) {
            mocked.when(Media::now).thenReturn(fixedNow);

            Book b = new Book("Clean Code", "Robert Martin", "111");

            
            b.setDueDate("2025-01-25");
            assertFalse(b.isOverdue());

            
            b.setDueDate("2025-01-10");
            assertTrue(b.isOverdue());
        }
    }

    // ---------- calculateFine() NOT OVERDUE ----------

    @Test
    void testCalculateFine_notOverdue_setsZeroAndFixesStatus() {

        LocalDate fixedNow = LocalDate.of(2025, 1, 10);

        try (MockedStatic<Media> mocked = mockStatic(Media.class)) {
            mocked.when(Media::now).thenReturn(fixedNow);

            Book b = new Book("Clean Code", "Robert Martin", "111");

            
            b.setStatus("Overdue");
            b.setFineAmount(15.0);
            b.setDueDate("2025-01-20"); 

            b.calculateFine("Silver");

            assertEquals(0.0, b.getFineAmount());
            assertEquals("Borrowed", b.getStatus());
        }
    }

    // ---------- calculateFine() OVERDUE (Silver) ----------

    @Test
    void testCalculateFine_overdue_setsStatusOverdueAndNonNegativeFine() {

        LocalDate fixedNow = LocalDate.of(2025, 1, 20);

        try (MockedStatic<Media> mocked = mockStatic(Media.class)) {
            mocked.when(Media::now).thenReturn(fixedNow);

            Book b = new Book("Clean Code", "Robert Martin", "111");
            b.setStatus("Borrowed");
            b.setDueDate("2025-01-10"); 

            b.calculateFine("Silver");

            assertEquals("Overdue", b.getStatus());
            assertTrue(b.getFineAmount() >= 0.0);
        }
    }

    // ---------- calculateFine() GOLD + amountPaid ----------

    @Test
    void testCalculateFine_goldMembership_usesDiscountAndAmountPaid() {

        
        LocalDate fixedNow = LocalDate.of(2025, 1, 20);

        try (MockedStatic<Media> mocked = mockStatic(Media.class)) {
            mocked.when(Media::now).thenReturn(fixedNow);

            Book b = new Book(
                    "Clean Code",
                    "Robert Martin",
                    "111",
                    "Borrowed",
                    "2025-01-16",
                    0.0,
                    "u1",
                    1.0   
            );

            b.calculateFine("Gold");

            
            assertEquals("Overdue", b.getStatus());
            assertEquals(1.0, b.getFineAmount(), 0.0001);
        }
    }

    // ---------- toFileFormat() ----------

    @Test
    void testToFileFormat_usesAllFieldsInCorrectOrder() {
        Book b = new Book(
                "Clean Code",
                "Robert Martin",
                "111",
                "Borrowed",
                "2025-12-20",
                3.5,
                "u1",
                1.0
        );

        String expected = String.join(",",
                "Book",
                "Clean Code",
                "Robert Martin",
                "111",
                "Borrowed",
                "2025-12-20",
                "3.5",
                "u1",
                "1.0"
        );

        assertEquals(expected, b.toFileFormat());
    }
}
