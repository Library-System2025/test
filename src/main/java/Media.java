import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

/**
 * Abstract class representing a generic media item in the library.
 * <p>
 * This class serves as the base for specific media types like {@link Book} and {@link CD}.
 * It encapsulates common properties such as title, author, ISBN, and borrowing status,
 * as well as business logic for borrowing, returning, and fine calculation.
 * </p>
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
     * Gets the current date. 
     * This method is used to allow mocking of the current date during testing.
     * 
     * @return The current {@link LocalDate}.
     */
    
    protected static LocalDate now() {
        return LocalDate.now();
    }

    /**
     * Constructor to initialize a Media object with full details.
     *
     * @param title The title of the item.
     * @param author The author of the item.
     * @param isbn The ISBN or unique ID of the item.
     * @param status The current status (e.g., Available, Borrowed).
     * @param dueDate The due date if borrowed.
     * @param fineAmount The current fine amount.
     * @param borrowedBy The username of the borrower.
     * @param amountPaid The amount already paid towards the fine.
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
     * Returns the loan period for this specific media type.
     * @return The number of days the item can be borrowed.
     */
    public abstract int getLoanPeriod();

    /**
     * Returns the daily fine rate for this specific media type.
     * @return The fine amount per day.
     */
    public abstract double getBaseDailyFine();

    /**
     * Returns the type of media as a string (e.g., "Book", "CD").
     * @return The media type identifier.
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

    public void setStatus(String status)           { this.status = status; }
    public void setDueDate(String dueDate)         { this.dueDate = dueDate; }
    public void setFineAmount(double fineAmount)   { this.fineAmount = fineAmount; }
    public void setBorrowedBy(String borrowedBy)   { this.borrowedBy = borrowedBy; }
    public void addPayment(double amount)          { this.amountPaid += amount; }

    /**
     * Gets the current fine amount.
     * @return The fine amount.
     */
    
    public double getFine() {
        return fineAmount;
    }

    /**
     * Sets the fine amount.
     * @param fine The new fine amount.
     */
    
    public void setFine(double fine) {
        this.fineAmount = fine;
    }

    /**
     * Gets the amount paid towards overdue fines.
     * @return The paid amount.
     */
    
    public double getOverdueFine() {
        return amountPaid;
    }
    
    /**
     * Sets the amount paid towards overdue fines.
     * @param overdueFine The paid amount.
     */

    public void setOverdueFine(double overdueFine) {
        this.amountPaid = overdueFine;
    }

    /**
     * Borrows the item for a specific user.
     * Sets the status to "Borrowed", records the borrower, and calculates the due date.
     *
     * @param username The username of the borrower.
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
     * Resets status to "Available" and clears borrower and fine information.
     */
    
    public void returnMedia() {
        this.status = "Available";
        this.borrowedBy = "";
        this.dueDate = "";
        this.fineAmount = 0.0;
        this.amountPaid = 0.0;
    }

    /**
     * Checks if the item is currently overdue.
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
     * Calculates the fine based on overdue days and user membership.
     * Uses the {@link FineCalculator} strategy pattern.
     *
     * @param membershipType The user's membership type (e.g., "Gold", "Silver").
     */
    
    public void calculateFine(String membershipType) {
        if (!isOverdue()) {
            fineAmount = 0.0;
            if ("Overdue".equals(status)) status = "Borrowed";
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
     * Converts the media object to a CSV-formatted string for file storage.
     *
     * @return A comma-separated string containing media details.
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
