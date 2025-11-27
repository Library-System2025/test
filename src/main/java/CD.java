public class CD extends Media {

    public CD(String title, String author, String isbn, String status, String dueDate, double fineAmount, String borrowedBy, double amountPaid) {
        super(title, author, isbn, status, dueDate, fineAmount, borrowedBy, amountPaid);
    }

    public CD(String title, String author, String isbn) {
         super(title, author, isbn, "Available", "", 0.0, "", 0.0);
    }

    // 1. الـ CD مدته 7 أيام بس (حسب المطلوب US5.1)
    @Override
    public int getLoanPeriod() {
        return 7;
    }

    // 2. الـ CD غرامته غالية 2.0$ (حسب المطلوب US5.2)
    @Override
    public double getBaseDailyFine() {
        return 2.0;
    }

    // 3. نوعه "CD"
    @Override
    public String getMediaType() {
        return "CD";
    }
}