

/**
 * Represents a Book item in the library.
 * Extends the Media class with specific rules for books.
 * 
 * @author Zainab
 * @version 1.0
 */
public class Book extends Media {

    /**
     * Constructor to create a Book object with full details.
     * Usually used when loading data from a file.
     * 
     * @param title The title of the book.
     * @param author The author of the book.
     * @param isbn The ISBN of the book.
     * @param status The current status (e.g., Available, Borrowed).
     * @param dueDate The due date if borrowed.
     * @param fineAmount The calculated fine amount.
     * @param borrowedBy The user who borrowed the book.
     * @param amountPaid The amount paid for fines.
     * @param copyId The unique copy identifier.
     */
    public Book(String title, String author, String isbn, String status, String dueDate,
            double fineAmount, String borrowedBy, double amountPaid, int copyId) {
        super(title, author, isbn, status, dueDate, fineAmount, borrowedBy, amountPaid, copyId);
    }

    /**
     * Constructor to create a new Book with basic details and specific copy ID.
     * Used when adding a new book copy to the library.
     * 
     * @param title The title of the book.
     * @param author The author of the book.
     * @param isbn The ISBN of the book.
     * @param copyId The copy ID of the book.
     */
    public Book(String title, String author, String isbn, int copyId) {
        super(title, author, isbn, "Available", "", 0.0, "", 0.0, copyId);
    }

    /**
     * Constructor to create a new Book with basic details (Default copyId = 1).
     * Used when adding a new book to the library.
     * 
     * @param title The title of the book.
     * @param author The author of the book.
     * @param isbn The ISBN of the book.
     */
    public Book(String title, String author, String isbn) {
        this(title, author, isbn, 1);
    }

    /** 
     * Gets the loan period for a book.
     * 
     * @return The number of days a book can be borrowed (28 days).
     */
    @Override
    public int getLoanPeriod() {
        return 28;
    }

    /**
     * Gets the daily fine for overdue books.
     * 
     * @return The fine amount per day (1.0).
     */
    @Override
    public double getBaseDailyFine() {
        return 1.0;
    }

    /**
     * Identifies the type of this media.
     * 
     * @return The string "Book".
     */
    @Override
    public String getMediaType() {
        return "Book";
    }
}