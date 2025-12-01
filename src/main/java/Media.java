import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

/**
 * Abstract class representing a generic media item in the library.
 * Serves as the base class for Book and CD.
 * 
 * @author Zainab
 * @version 1.0
 */

public abstract class Media {
    
    protected String title;
    protected String author;
    protected String isbn;
    protected String status;
    protected String dueDate;
    protected double fineAmount;
    protected String borrowedBy;
    protected double amountPaid;

    /**
     * Gets the current date. Used for mocking and consistency.
     * @return The current LocalDate.
     */
    
    protected static LocalDate now() {
        return LocalDate.now();
    }

    /**
     * Constructor to initialize a Media object.
     * 
     * @param title The title of the item.
     * @param author The author of the item.
     * @param isbn The ISBN or ID.
     * @param status The current status (Available, Borrowed, etc.).
     * @param dueDate The due date string.
     * @param fineAmount The current fine amount.
     * @param borrowedBy The username of the borrower.
     * @param amountPaid The amount paid towards fines.
     */
    
    public Media(String title, String author, String isbn, String status,
                 String dueDate, double fineAmount, String borrowedBy, double amountPaid) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.status = status;
        this.dueDate = dueDate;
        this.fineAmount = fineAmount;
        this.borrowedBy = borrowedBy;
        this.amountPaid = amountPaid;
    }

    /**
     * @return The loan period in days.
     */
    
    public abstract int getLoanPeriod();  
    
    /**
     * @return The daily fine rate.
     */
    
    public abstract double getBaseDailyFine(); 
    
    /**
     * @return The type of media (e.g., "Book", "CD").
     */
    
    public abstract String getMediaType();     

    
    public String getTitle()      { return title; }
    public String getAuthor()     { return author; }
    public String getIsbn()       { return isbn; }
    public String getStatus()     { return status; }
    public String getDueDate()    { return dueDate; }
    public double getFineAmount() { return fineAmount; }
    public String getBorrowedBy() { return borrowedBy; }
    public double getAmountPaid() { return amountPaid; }

    public void setStatus(String status)       { this.status = status; }
    public void setDueDate(String dueDate)     { this.dueDate = dueDate; }
    public void setFineAmount(double fineAmount) { this.fineAmount = fineAmount; }
    public void setBorrowedBy(String borrowedBy) { this.borrowedBy = borrowedBy; }
    public void addPayment(double amount)      { this.amountPaid += amount; }

   
    /**
     * Borrows the item for a specific user.
     * Calculates the due date based on the specific media type loan period.
     * 
     * @param username The user borrowing the item.
     */
    
    public void borrow(String username) {
        this.status = "Borrowed";
        this.borrowedBy = username;

        
        LocalDate due = Media.now().plusDays(getLoanPeriod());
        this.dueDate = due.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        this.fineAmount = 0.0;
        this.amountPaid = 0.0;
    }

    /**
     * Returns the item to the library.
     * Resets status, borrower, and fine details.
     */
    
    public void returnMedia() {
        this.status = "Available";
        this.borrowedBy = "";
        this.dueDate = "";
        this.fineAmount = 0.0;
        this.amountPaid = 0.0;
    }

    /**
     * Checks if the item is overdue.
     * 
     * @return true if the current date is after the due date, false otherwise.
     */
    
    public boolean isOverdue() {
        if (dueDate == null || dueDate.isEmpty()) return false;

        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 4).appendLiteral('-')
                .appendValue(ChronoField.MONTH_OF_YEAR).appendLiteral('-')
                .appendValue(ChronoField.DAY_OF_MONTH).toFormatter();

        LocalDate due = LocalDate.parse(dueDate, formatter);
        return Media.now().isAfter(due);
    }

    /**
     * Calculates the fine based on the user's membership type.
     * Uses the Strategy Pattern via FineCalculator.
     * 
     * @param membershipType The membership type (Gold/Silver).
     */
    
    public void calculateFine(String membershipType) {
        if (!isOverdue()) {
            fineAmount = 0.0;
            if (status.equals("Overdue")) status = "Borrowed";
            return;
        }

        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 4).appendLiteral('-')
                .appendValue(ChronoField.MONTH_OF_YEAR).appendLiteral('-')
                .appendValue(ChronoField.DAY_OF_MONTH).toFormatter();

        LocalDate due = LocalDate.parse(dueDate, formatter);
        long daysOverdue = ChronoUnit.DAYS.between(due, Media.now());

        FineCalculator calculator = new FineCalculator();
        if (membershipType != null && membershipType.equalsIgnoreCase("Gold")) {
            calculator.setStrategy(new GoldFineStrategy());
        } else {
            calculator.setStrategy(new SilverFineStrategy());
        }

        
        double totalDebt = calculator.calculate(daysOverdue, getBaseDailyFine());

        this.fineAmount = totalDebt - this.amountPaid;
        if (this.fineAmount < 0) this.fineAmount = 0.0;
        this.status = "Overdue";
    }

    /**
     * Formats the media object as a CSV string for file storage.
     * 
     * @return Comma-separated string representing the object.
     */
    
    public String toFileFormat() {
        return String.join(",",
                getMediaType(), 
                title,
                author,
                isbn,
                status,
                dueDate,
                String.valueOf(fineAmount),
                borrowedBy,
                String.valueOf(amountPaid)
        );
    }
}
