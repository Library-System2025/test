import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link Book} class.
 * <p>
 * Ensures that all constructors correctly initialize the state and that overridden methods 
 * from the {@link Media} superclass return the expected values specific to a Book.
 * </p>
 * 
 * @author Zainab
 * @version 1.2
 */
public class BookTest {

	/**
     * Verifies that the simplified constructor initializes fields with default values.
     * <p>
     * Specifically checks that:
     * <ul>
     *   <li>Title, Author, and ISBN are set correctly.</li>
     *   <li>Status defaults to "Available".</li>
     *   <li>Copy ID defaults to 1.</li>
     *   <li>Financial and due date fields are empty or zero.</li>
     * </ul>
     * </p>
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

        assertEquals(1, b.getCopyId());
    }

    /**
     * Verifies that the constructor accepting a copy ID correctly sets it while
     * maintaining default values for other fields.
     */
    @Test
    void testConstructorWithCopyId_initializesValuesCorrectly() {
        Book b = new Book("Refactoring", "Martin Fowler", "333", 5);

        assertEquals("Refactoring", b.getTitle());
        assertEquals("Martin Fowler", b.getAuthor());
        assertEquals("333", b.getIsbn());

        assertEquals("Available", b.getStatus());
        assertEquals("", b.getDueDate());
        assertEquals(0.0, b.getFineAmount());
        assertEquals("", b.getBorrowedBy());
        assertEquals(0.0, b.getAmountPaid());

        assertEquals(5, b.getCopyId());
    }

    /**
     * Verifies that the full constructor correctly populates all fields.
     * This ensures data loaded from persistence layers (files/databases) is mapped correctly.
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
                0.0,
                2
        );

        assertEquals("Effective Java", b.getTitle());
        assertEquals("Joshua Bloch", b.getAuthor());
        assertEquals("222", b.getIsbn());
        assertEquals("Borrowed", b.getStatus());
        assertEquals("2025-12-20", b.getDueDate());
        assertEquals(3.5, b.getFineAmount());
        assertEquals("u1", b.getBorrowedBy());
        assertEquals(0.0, b.getAmountPaid());

        assertEquals(2, b.getCopyId());
    }

    /**
     * Verifies that the loan period for a Book is strictly 28 days.
     */
    @Test
    void testGetLoanPeriod_returns28Days() {
        Book b = new Book("Test", "A", "1");
        assertEquals(28, b.getLoanPeriod());
    }

    /**
     * Verifies that the base daily fine rate for a Book is $1.0.
     */
    @Test
    void testGetBaseDailyFine_returns1Dollar() {
        Book b = new Book("Test", "A", "1");
        assertEquals(1.0, b.getBaseDailyFine());
    }

    /**
     * Verifies that the media type identifier returns "Book".
     */
    @Test
    void testGetMediaType_returnsBook() {
        Book b = new Book("Test", "A", "1");
        assertEquals("Book", b.getMediaType());
    }
}
