import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MediaTest {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ---------- borrow() WITH TIME MOCK ----------

    @Test
    void testBorrow_setsBorrowedFieldsAndDueDate_withMockedTime() {

        // نثبت التاريخ إلى 2025-01-10
        LocalDate fixedDate = LocalDate.of(2025, 1, 10);

        try (MockedStatic<Media> mocked = mockStatic(Media.class)) {
            mocked.when(Media::now).thenReturn(fixedDate);

            Book b = new Book("Clean Code", "Robert Martin", "111");

            b.borrow("u1");

            assertEquals("Borrowed", b.getStatus());
            assertEquals("u1", b.getBorrowedBy());

            // كتاب → 28 يوم
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

        // نثبت اليوم ليكون 2025-01-20
        LocalDate fixedNow = LocalDate.of(2025, 1, 20);

        try (MockedStatic<Media> mocked = mockStatic(Media.class)) {
            mocked.when(Media::now).thenReturn(fixedNow);

            Book b = new Book("Clean Code", "Robert Martin", "111");

            // تاريخ بالمستقبل → مش متأخر
            b.setDueDate("2025-01-25");
            assertFalse(b.isOverdue());

            // تاريخ بالماضي → متأخر
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

            // نفترض انه كان Overdue بالغلط وتاريخه بالمستقبل
            b.setStatus("Overdue");
            b.setFineAmount(15.0);
            b.setDueDate("2025-01-20"); // future

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
            b.setDueDate("2025-01-10"); // past

            b.calculateFine("Silver");

            assertEquals("Overdue", b.getStatus());
            assertTrue(b.getFineAmount() >= 0.0);
        }
    }

    // ---------- calculateFine() GOLD + amountPaid ----------

    @Test
    void testCalculateFine_goldMembership_usesDiscountAndAmountPaid() {

        // اليوم 2025-01-20 و due 2025-01-16 → 4 أيام تأخير
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
                    1.0   // دفع سابقاً 1$
            );

            b.calculateFine("Gold");

            // Book: baseDailyFine = 1.0
            // Gold: خصم 50% → 0.5 لليوم
            // 4 أيام * 0.5 = 2.0
            // totalDebt = 2.0 - amountPaid(1.0) = 1.0
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
