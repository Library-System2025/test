import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

public abstract class Media {
    // المتغيرات المشتركة (موجودة عند الكتاب وعند الـ CD)
    protected String title;
    protected String author;
    protected String isbn;
    protected String status;
    protected String dueDate;
    protected double fineAmount;
    protected String borrowedBy;
    protected double amountPaid;

    // الكونستركتور
    public Media(String title, String author, String isbn, String status, String dueDate, double fineAmount, String borrowedBy, double amountPaid) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.status = status;
        this.dueDate = dueDate;
        this.fineAmount = fineAmount;
        this.borrowedBy = borrowedBy;
        this.amountPaid = amountPaid;
    }

    // 🔥 دوال مجردة: كل ابن لازم يجاوب عليها بطريقته
    public abstract int getLoanPeriod();      // كم يوم مسموح؟
    public abstract double getBaseDailyFine(); // كم سعر الغرامة؟
    public abstract String getMediaType();    // شو نوعك؟ (Book ولا CD)

    // ✅ دوال جاهزة مشتركة (Getters)
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getIsbn() { return isbn; }
    public String getStatus() { return status; }
    public String getDueDate() { return dueDate; }
    public double getFineAmount() { return fineAmount; }
    public String getBorrowedBy() { return borrowedBy; }

    public void setStatus(String status) { this.status = status; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public void setFineAmount(double fineAmount) { this.fineAmount = fineAmount; }
    public void setBorrowedBy(String borrowedBy) { this.borrowedBy = borrowedBy; }
    public void addPayment(double amount) { this.amountPaid += amount; }

    // 📚 دالة الاستعارة (ذكية: بتسأل الابن عن المدة المسموحة)
    public void borrow(String username) {
        this.status = "Borrowed";
        this.borrowedBy = username;
        
        // هون السر: بنجيب عدد الأيام من الابن (getLoanPeriod)
        LocalDate due = LocalDate.now().plusDays(getLoanPeriod());
        this.dueDate = due.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        this.fineAmount = 0.0;
        this.amountPaid = 0.0;
    }

    // 🔁 دالة الإرجاع
    public void returnMedia() {
        this.status = "Available";
        this.borrowedBy = "";
        this.dueDate = "";
        this.fineAmount = 0.0;
        this.amountPaid = 0.0;
    }

    // ⏰ دالة فحص التأخير
    public boolean isOverdue() {
        if (dueDate == null || dueDate.isEmpty()) return false;
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 4).appendLiteral('-')
                .appendValue(ChronoField.MONTH_OF_YEAR).appendLiteral('-')
                .appendValue(ChronoField.DAY_OF_MONTH).toFormatter();
        LocalDate due = LocalDate.parse(dueDate, formatter);
        return LocalDate.now().isAfter(due);
    }

    // 💰 دالة حساب الغرامة (ذكية: بتسأل الابن عن سعره)
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
        long daysOverdue = ChronoUnit.DAYS.between(due, LocalDate.now());

        FineCalculator calculator = new FineCalculator();
        if (membershipType != null && membershipType.equalsIgnoreCase("Gold")) {
            calculator.setStrategy(new GoldFineStrategy());
        } else {
            calculator.setStrategy(new SilverFineStrategy());
        }

        // هون السر الثاني: بنبعث سعر اليوم الخاص بالابن (getBaseDailyFine)
        double totalDebt = calculator.calculate(daysOverdue, getBaseDailyFine());

        this.fineAmount = totalDebt - this.amountPaid;
        if (this.fineAmount < 0) this.fineAmount = 0.0;
        this.status = "Overdue";
    }

    // 🧾 تنسيق الملف: لازم نحط النوع أول اشي
    public String toFileFormat() {
        return String.join(",",
                getMediaType(), // Book أو CD
                title, author, isbn, status, dueDate,
                String.valueOf(fineAmount), borrowedBy, String.valueOf(amountPaid)
        );
    }
}