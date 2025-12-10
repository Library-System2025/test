import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link CD} class.
 * <p>
 * Ensures that all constructors correctly initialize the state and that overridden methods 
 * from the {@link Media} superclass return the expected values specific to a CD.
 */
public class CDTest {

    /**
     * Verifies that the simplified constructor initializes fields with default values.
     * <p>
     * Specifically checks that:
     * <ul>
     *   <li>Title, Artist, and ISBN are set correctly.</li>
     *   <li>Status defaults to "Available".</li>
     *   <li>Copy ID defaults to 1.</li>
     * </ul>
     */
    @Test
    void testShortConstructor_initialValuesCorrect() {
        CD cd = new CD("Greatest Hits", "Artist X", "CD001");

        assertEquals("Greatest Hits", cd.getTitle());
        assertEquals("Artist X", cd.getAuthor());
        assertEquals("CD001", cd.getIsbn());

        assertEquals("Available", cd.getStatus());
        assertEquals("", cd.getDueDate());
        assertEquals(0.0, cd.getFineAmount());
        assertEquals("", cd.getBorrowedBy());
        assertEquals(0.0, cd.getAmountPaid());

        assertEquals(1, cd.getCopyId());
    }

    /**
     * Verifies that the constructor accepting a copy ID correctly sets it while
     * maintaining default values for other fields.
     */
    @Test
    void testConstructorWithCopyId_initializesValuesCorrectly() {
        CD cd = new CD("Summer Mix", "DJ Alex", "CD500", 7);

        assertEquals("Summer Mix", cd.getTitle());
        assertEquals("DJ Alex", cd.getAuthor());
        assertEquals("CD500", cd.getIsbn());

        assertEquals("Available", cd.getStatus());
        assertEquals("", cd.getDueDate());
        assertEquals(0.0, cd.getFineAmount());
        assertEquals("", cd.getBorrowedBy());
        assertEquals(0.0, cd.getAmountPaid());

        assertEquals(7, cd.getCopyId());
    }

    /**
     * Verifies that the full constructor correctly populates all fields.
     * This ensures data loaded from persistence layers is mapped correctly.
     */
    @Test
    void testFullConstructor_setsAllValuesCorrectly() {
        CD cd = new CD(
                "Top Mix",
                "DJ Max",
                "CD002",
                "Borrowed",
                "2025-12-25",
                4.5,
                "u1",
                0.0,
                3
        );

        assertEquals("Top Mix", cd.getTitle());
        assertEquals("DJ Max", cd.getAuthor());
        assertEquals("CD002", cd.getIsbn());
        assertEquals("Borrowed", cd.getStatus());
        assertEquals("2025-12-25", cd.getDueDate());
        assertEquals(4.5, cd.getFineAmount());
        assertEquals("u1", cd.getBorrowedBy());
        assertEquals(0.0, cd.getAmountPaid());

        assertEquals(3, cd.getCopyId());
    }

    /**
     * Verifies that the loan period for a CD is strictly 7 days.
     */
    @Test
    void testGetLoanPeriod_returns7Days() {
        CD cd = new CD("Test", "A", "1");
        assertEquals(7, cd.getLoanPeriod());
    }

    /**
     * Verifies that the base daily fine rate for a CD is $2.0.
     */
    @Test
    void testGetBaseDailyFine_returns2Dollars() {
        CD cd = new CD("Test", "A", "1");
        assertEquals(2.0, cd.getBaseDailyFine());
    }

    /**
     * Verifies that the media type identifier returns "CD".
     */
    @Test
    void testGetMediaType_returnsCD() {
        CD cd = new CD("Test", "A", "1");
        assertEquals("CD", cd.getMediaType());
    }
}
