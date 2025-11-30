import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class BookTest {

    @Test
    void testShortConstructor_initialValuesCorrect() {
        Book b = new Book("Clean Code", "Robert Martin", "111");

        assertEquals("Clean Code", b.getTitle());
        assertEquals("Robert Martin", b.getAuthor());
        assertEquals("111", b.getIsbn());

        assertEquals("Available", b.getStatus());
        assertEquals("", b.getDueDate());
        assertEquals(0.0, b.getFineAmount());
        assertEquals("", b.getBorrowedBy());
        assertEquals(0.0, b.getAmountPaid());
    }

    @Test
    void testFullConstructor_setsAllValuesCorrectly() {
        Book b = new Book(
                "Effective Java",
                "Joshua Bloch",
                "222",
                "Borrowed",
                "2025-12-20",
                3.5,
                "u1",
                0.0
        );

        assertEquals("Effective Java", b.getTitle());
        assertEquals("Joshua Bloch", b.getAuthor());
        assertEquals("222", b.getIsbn());
        assertEquals("Borrowed", b.getStatus());
        assertEquals("2025-12-20", b.getDueDate());
        assertEquals(3.5, b.getFineAmount());
        assertEquals("u1", b.getBorrowedBy());
        assertEquals(0.0, b.getAmountPaid());
    }

    @Test
    void testGetLoanPeriod_returns28Days() {
        Book b = new Book("Test", "A", "1");
        assertEquals(28, b.getLoanPeriod());
    }

    @Test
    void testGetBaseDailyFine_returns1Dollar() {
        Book b = new Book("Test", "A", "1");
        assertEquals(1.0, b.getBaseDailyFine());
    }

    @Test
    void testGetMediaType_returnsBook() {
        Book b = new Book("Test", "A", "1");
        assertEquals("Book", b.getMediaType());
    }
}
