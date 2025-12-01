import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Fine Calculation Strategies.
 * Verifies Silver and Gold strategies logic.
 * 
 * @author Zainab
 * @version 1.0
 */

public class FineStrategiesTest {

	/**
     * Tests standard fine calculation for Silver members.
     */
	
    @Test
    void testSilverFineStrategy_normalCase() {
        FineCalculationStrategy strategy = new SilverFineStrategy();

        double fine = strategy.calculateFine(5, 2.0); 
        assertEquals(10.0, fine);
    }

    @Test
    void testSilverFineStrategy_zeroDaysOrZeroRate() {
        FineCalculationStrategy strategy = new SilverFineStrategy();

        assertEquals(0.0, strategy.calculateFine(0, 2.0)); 
        assertEquals(0.0, strategy.calculateFine(5, 0.0)); 
    }

    /**
     * Tests 50% discount calculation for Gold members.
     */
    
    @Test
    void testGoldFineStrategy_halfPrice() {
        FineCalculationStrategy strategy = new GoldFineStrategy();

        double fine = strategy.calculateFine(4, 3.0); 
        assertEquals(6.0, fine);
    }

    @Test
    void testGoldFineStrategy_zeroDaysOrZeroRate() {
        FineCalculationStrategy strategy = new GoldFineStrategy();

        assertEquals(0.0, strategy.calculateFine(0, 3.0));
        assertEquals(0.0, strategy.calculateFine(4, 0.0));
    }

    @Test
    void testFineCalculator_noStrategy_returnsZero() {
        FineCalculator calculator = new FineCalculator();

        double fine = calculator.calculate(10, 2.0);
        assertEquals(0.0, fine);
    }

    @Test
    void testFineCalculator_withSilverStrategy() {
        FineCalculator calculator = new FineCalculator();
        calculator.setStrategy(new SilverFineStrategy());

        double fine = calculator.calculate(3, 2.0); 
        assertEquals(6.0, fine);
    }

    @Test
    void testFineCalculator_withGoldStrategy() {
        FineCalculator calculator = new FineCalculator();
        calculator.setStrategy(new GoldFineStrategy());

        double fine = calculator.calculate(3, 2.0); 
        assertEquals(3.0, fine);
    }

    /**
     * Verifies that strategy can be switched at runtime.
     */
    
    @Test
    void testFineCalculator_switchStrategyAtRuntime() {
        FineCalculator calculator = new FineCalculator();

        
        calculator.setStrategy(new SilverFineStrategy());
        double silverFine = calculator.calculate(2, 5.0); 
        assertEquals(10.0, silverFine);

       
        calculator.setStrategy(new GoldFineStrategy());
        double goldFine = calculator.calculate(2, 5.0); 
        assertEquals(5.0, goldFine);
    }
}
