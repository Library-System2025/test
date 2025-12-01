
/**
 * Represents a CD item in the library.
 * Extends the Media class with specific rules for CDs.
 * 
 * @author Zainab
 * @version 1.0
 */

public class CD extends Media {

	/**
	 * Constructor to create a CD object with full details.
     * Usually used when loading data from a file.
     * 
     * @param title The title of the CD.
     * @param author The author/artist of the CD.
     * @param isbn The ISBN/ID of the CD.
     * @param status The current status (e.g., Available, Borrowed).
     * @param dueDate The due date if borrowed.
     * @param fineAmount The calculated fine amount.
     * @param borrowedBy The user who borrowed the CD.
     * @param amountPaid The amount paid for fines.
     */
	
    public CD(String title, String author, String isbn, String status, String dueDate, double fineAmount, String borrowedBy, double amountPaid) {
        super(title, author, isbn, status, dueDate, fineAmount, borrowedBy, amountPaid);
    }
    
    /**
     * Constructor to create a new CD with basic details.
     * Used when adding a new CD to the library.
     * 
     * @param title The title of the CD.
     * @param author The author/artist of the CD.
     * @param isbn The ISBN/ID of the CD.
     */

    public CD(String title, String author, String isbn) {
         super(title, author, isbn, "Available", "", 0.0, "", 0.0);
    }

    /**
     * Gets the loan period for a CD.
     *
     * @return The number of days a CD can be borrowed (7 days).
     */
    
    @Override
    public int getLoanPeriod() {
        return 7;
    }

    /**
     * Gets the daily fine for overdue CDs.
     * 
     * @return The fine amount per day (2.0).
     */
    
    @Override
    public double getBaseDailyFine() {
        return 2.0;
    }

    /**
     * Identifies the type of this media.
     * 
     * @return The string "CD".
     */
    
    @Override
    public String getMediaType() {
        return "CD";
    }
}