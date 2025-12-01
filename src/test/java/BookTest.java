import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Book class.
 * Ensures constructors and overridden methods behave correctly.
 * 
 * @author Zainab
 * @version 1.0
 */

public class BookTest {

	/**
     * Verifies that the short constructor initializes fields with default values.
     */
	
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

    /**
     * Verifies that the full constructor correctly sets all fields.
     */
    
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
    
    /**
     * Verifies that the loan period for a Book is 28 days.
     */

    @Test
    void testGetLoanPeriod_returns28Days() {
        Book b = new Book("Test", "A", "1");
        assertEquals(28, b.getLoanPeriod());
    }

    /**
     * Verifies that the daily fine rate for a Book is $1.0.
     */
    
    @Test
    void testGetBaseDailyFine_returns1Dollar() {
        Book b = new Book("Test", "A", "1");
        assertEquals(1.0, b.getBaseDailyFine());
    }

    /**
     * Verifies that the media type is correctly identified as "Book".
     */
    
    @Test
    void testGetMediaType_returnsBook() {
        Book b = new Book("Test", "A", "1");
        assertEquals("Book", b.getMediaType());
    }
}
