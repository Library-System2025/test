import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// الواجهة: بتقبل عدد الأيام وسعر اليوم
interface FineCalculationStrategy {
    double calculateFine(long overdueDays, double dailyRate);
}

// 🥈 استراتيجية Silver: بتدفع السعر كامل (الأيام * السعر)
class SilverFineStrategy implements FineCalculationStrategy {
    @Override
    public double calculateFine(long overdueDays, double dailyRate) {
        return overdueDays * dailyRate;
    }
}

// 🥇 استراتيجية Gold: بتدفع نص السعر (خصم 50%)
class GoldFineStrategy implements FineCalculationStrategy {
    @Override
    public double calculateFine(long overdueDays, double dailyRate) {
        return overdueDays * (dailyRate * 0.5);
    }
}

// ⚙️ الآلة الحاسبة اللي بتختار الاستراتيجية
class FineCalculator {
    private FineCalculationStrategy strategy;

    public void setStrategy(FineCalculationStrategy strategy) {
        this.strategy = strategy;
    }

    public double calculate(long overdueDays, double dailyRate) {
        if (strategy == null) return 0.0;
        return strategy.calculateFine(overdueDays, dailyRate);
    }
}