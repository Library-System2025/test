
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Interface defining the strategy for fine calculation.
 * 
 * @author Zainab
 * @version 1.0
 */

interface FineCalculationStrategy {
	
	/**
     * Calculates the fine based on overdue days and daily rate.
     * 
     * @param overdueDays The number of days the item is overdue.
     * @param dailyRate The standard daily fine rate.
     * @return The calculated fine amount.
     */
	
    double calculateFine(long overdueDays, double dailyRate);
}

/**
 * Strategy for Silver members.
 * Silver members pay the full fine amount without discount.
 * 
 * @author Zainab
 * @version 1.0
 */

class SilverFineStrategy implements FineCalculationStrategy {
    
	/**
     * Calculates the full fine amount.
     * 
     * @param overdueDays The number of days the item is overdue.
     * @param dailyRate The standard daily fine rate.
     * @return The total fine (days * rate).
     */
	
	@Override
    public double calculateFine(long overdueDays, double dailyRate) {
        return overdueDays * dailyRate;
    }
}

/**
 * Strategy for Gold members.
 * Gold members receive a 50% discount on fines.
 * 
 * @author Zainab
 * @version 1.0
 */

class GoldFineStrategy implements FineCalculationStrategy {
    
	/**
     * Calculates the fine with a 50% discount.
     * 
     * @param overdueDays The number of days the item is overdue.
     * @param dailyRate The standard daily fine rate.
     * @return The discounted fine amount.
     */
	
	@Override
    public double calculateFine(long overdueDays, double dailyRate) {
        return overdueDays * (dailyRate * 0.5);
    }
}

/**
 * Context class that uses a strategy to calculate fines.
 * 
 * @author Zainab
 * @version 1.0
 */

class FineCalculator {
    private FineCalculationStrategy strategy;

    /**
     * Sets the strategy to be used for calculation.
     * 
     * @param strategy The strategy (Gold or Silver).
     */
    
    public void setStrategy(FineCalculationStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Executes the strategy to calculate the fine.
     * 
     * @param overdueDays The number of days overdue.
     * @param dailyRate The daily rate.
     * @return The final calculated fine.
     */
    
    public double calculate(long overdueDays, double dailyRate) {
        if (strategy == null) return 0.0;
        return strategy.calculateFine(overdueDays, dailyRate);
    }
}