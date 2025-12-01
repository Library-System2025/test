import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the CD class.
 * Ensures constructors and overridden methods behave correctly.
 * 
 * @author Zainab
 * @version 1.0
 */

public class CDTest {

	/**
     * Verifies that the short constructor initializes fields with default values.
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
    }

    /**
     * Verifies that the full constructor correctly sets all fields.
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
                0.0
        );

        assertEquals("Top Mix", cd.getTitle());
        assertEquals("DJ Max", cd.getAuthor());
        assertEquals("CD002", cd.getIsbn());
        assertEquals("Borrowed", cd.getStatus());
        assertEquals("2025-12-25", cd.getDueDate());
        assertEquals(4.5, cd.getFineAmount());
        assertEquals("u1", cd.getBorrowedBy());
        assertEquals(0.0, cd.getAmountPaid());
    }

    /**
     * Verifies that the loan period for a CD is 7 days.
     */
    
    @Test
    void testGetLoanPeriod_returns7Days() {
        CD cd = new CD("Test", "A", "1");
        assertEquals(7, cd.getLoanPeriod());
    }

    /**
     * Verifies that the daily fine rate for a CD is $2.0.
     */
    
    @Test
    void testGetBaseDailyFine_returns2Dollars() {
        CD cd = new CD("Test", "A", "1");
        assertEquals(2.0, cd.getBaseDailyFine());
    }

    /**
     * Verifies that the media type is correctly identified as "CD".
     */
    
    @Test
    void testGetMediaType_returnsCD() {
        CD cd = new CD("Test", "A", "1");
        assertEquals("CD", cd.getMediaType());
    }
}
