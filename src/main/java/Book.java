import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

// 🧩 واجهة الاستراتيجية
interface FineCalculationStrategy {
    double calculateFine(long overdueDays);
}

// 🥈 استراتيجية Silver
class SilverFineStrategy implements FineCalculationStrategy {
    @Override
    public double calculateFine(long overdueDays) {
        return overdueDays * 1.0; // 1$ per day
    }
}

// 🥇 استراتيجية Gold
class GoldFineStrategy implements FineCalculationStrategy {
    @Override
    public double calculateFine(long overdueDays) {
        return overdueDays * 0.5; // 50% discount
    }
}

// ⚙️ كلاس مسؤول عن اختيار الاستراتيجية
class FineCalculator {
    private FineCalculationStrategy strategy;

    public void setStrategy(FineCalculationStrategy strategy) {
        this.strategy = strategy;
    }

    public double calculate(long overdueDays) {
        if (strategy == null) {
            throw new IllegalStateException("Fine strategy not set!");
        }
        return strategy.calculateFine(overdueDays);
    }
}

// 📚 الكلاس الرئيسي Book
public class Book {
    private String title;
    private String author;
    private String isbn;
    private String status;
    private String dueDate;
    private double fineAmount;
    private String borrowedBy;

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

    public void calculateFine(String membershipType) {
        if (!isOverdue()) {
            fineAmount = 0.0;
            if (status.equals("Overdue")) {
                status = "Borrowed";
            }
            return;
        }

        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 4)
                .appendLiteral('-')
                .appendValue(ChronoField.MONTH_OF_YEAR)
                .appendLiteral('-')
                .appendValue(ChronoField.DAY_OF_MONTH)
                .toFormatter();

        LocalDate due = LocalDate.parse(dueDate, formatter);
        long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(due, LocalDate.now());

        FineCalculator calculator = new FineCalculator();

        if (membershipType != null && membershipType.equalsIgnoreCase("Gold")) {
            calculator.setStrategy(new GoldFineStrategy());
        } else {
            calculator.setStrategy(new SilverFineStrategy());
        }

        this.fineAmount = calculator.calculate(daysOverdue);
        this.status = "Overdue";

        // Debug
        System.out.println("🔍 Book: " + title + " | BorrowedBy: " + borrowedBy +
                " | Membership: " + membershipType +
                " | DaysOverdue: " + daysOverdue +
                " | FineCalculated: " + fineAmount);
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
