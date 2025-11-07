import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

public class Book {
    private String title;
    private String author;
    private String isbn;
    private String status;
    private String dueDate;
    private double fineAmount;
    private String borrowedBy; // 👈 اسم المستخدم اللي استعار الكتاب

    // ✅ Constructors
    public Book(String title, String author, String isbn) {
        this(title, author, isbn, "Available", "", 0.0, "");
    }

    public Book(String title, String author, String isbn, String status) {
        this(title, author, isbn, status, "", 0.0, "");
    }

    public Book(String title, String author, String isbn, String status, String dueDate) {
        this(title, author, isbn, status, dueDate, 0.0, "");
    }

    public Book(String title, String author, String isbn, String status, String dueDate, double fineAmount) {
        this(title, author, isbn, status, dueDate, fineAmount, "");
    }

    public Book(String title, String author, String isbn, String status, String dueDate, double fineAmount, String borrowedBy) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.status = status;
        this.dueDate = dueDate;
        this.fineAmount = fineAmount;
        this.borrowedBy = borrowedBy;
    }

    // ✅ Getters
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getIsbn() { return isbn; }
    public String getStatus() { return status; }
    public String getDueDate() { return dueDate; }
    public double getFineAmount() { return fineAmount; }
    public String getBorrowedBy() { return borrowedBy; }

    // ✅ Setters
    public void setStatus(String status) { this.status = status; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public void setFineAmount(double fineAmount) { this.fineAmount = fineAmount; }
    public void setBorrowedBy(String borrowedBy) { this.borrowedBy = borrowedBy; }

    // 📚 استعارة كتاب
    public void borrow(String username) {
        this.status = "Borrowed";
        this.borrowedBy = username;
        LocalDate due = LocalDate.now().plusDays(28);
        this.dueDate = due.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        this.fineAmount = 0.0;
    }

    public void returnBook() {
        this.status = "Available";
        this.borrowedBy = "";
        this.dueDate = "";
        this.fineAmount = 0.0;
    }


    // ⏰ فحص التأخير
    public boolean isOverdue() {
        if (dueDate == null || dueDate.isEmpty()) return false;

        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR) 
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH)  
            .toFormatter();

        LocalDate due = LocalDate.parse(dueDate, formatter);
        return LocalDate.now().isAfter(due);
    }

 // 💰 حساب الغرامة حسب عدد الأيام المتأخرة
    public void calculateFine() {
        if (!isOverdue()) {
            fineAmount = 0.0;
            if (status.equals("Overdue")) {
                status = "Borrowed";
            }
            return;
        }

        // نفس الـ formatter مثل isOverdue
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH)
            .toFormatter();

        LocalDate due = LocalDate.parse(dueDate, formatter);
        long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(due, LocalDate.now());

        // زيادة دولار لكل يوم
        fineAmount = daysOverdue * 1.0;

        status = "Overdue";
    }


    // 🧾 تنسيق السطر للملف
    public String toFileFormat() {
        return String.join(",",
            title,
            author,
            isbn,
            status,
            dueDate,
            String.valueOf(fineAmount),
            borrowedBy
        );
    }
}
