public class Book extends Media {

    // الكونستركتور الكامل (لما نقرأ من الملف)
    public Book(String title, String author, String isbn, String status, String dueDate, double fineAmount, String borrowedBy, double amountPaid) {
        super(title, author, isbn, status, dueDate, fineAmount, borrowedBy, amountPaid);
    }
    
    // كونستركتور مختصر (لما نضيف كتاب جديد)
    public Book(String title, String author, String isbn) {
         super(title, author, isbn, "Available", "", 0.0, "", 0.0);
    }

    // 1. الكتاب مدته 28 يوم
    @Override
    public int getLoanPeriod() {
        return 28;
    }

    // 2. الكتاب غرامته 1.0$
    @Override
    public double getBaseDailyFine() {
        return 1.0;
    }

    // 3. نوعه "Book"
    @Override
    public String getMediaType() {
        return "Book";
    }
}