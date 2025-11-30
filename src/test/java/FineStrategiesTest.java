import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class FineStrategiesTest {

    @Test
    void testSilverFineStrategy_normalCase() {
        FineCalculationStrategy strategy = new SilverFineStrategy();

        double fine = strategy.calculateFine(5, 2.0); // 5 أيام * 2$ = 10$
        assertEquals(10.0, fine);
    }

    @Test
    void testSilverFineStrategy_zeroDaysOrZeroRate() {
        FineCalculationStrategy strategy = new SilverFineStrategy();

        assertEquals(0.0, strategy.calculateFine(0, 2.0)); // 0 يوم
        assertEquals(0.0, strategy.calculateFine(5, 0.0)); // 0 سعر
    }

    @Test
    void testGoldFineStrategy_halfPrice() {
        FineCalculationStrategy strategy = new GoldFineStrategy();

        double fine = strategy.calculateFine(4, 3.0); // 4 أيام * (3 * 0.5) = 6
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

        double fine = calculator.calculate(3, 2.0); // 3 * 2 = 6
        assertEquals(6.0, fine);
    }

    @Test
    void testFineCalculator_withGoldStrategy() {
        FineCalculator calculator = new FineCalculator();
        calculator.setStrategy(new GoldFineStrategy());

        double fine = calculator.calculate(3, 2.0); // 3 * (2 * 0.5) = 3
        assertEquals(3.0, fine);
    }

    @Test
    void testFineCalculator_switchStrategyAtRuntime() {
        FineCalculator calculator = new FineCalculator();

        // أول اشي Silver
        calculator.setStrategy(new SilverFineStrategy());
        double silverFine = calculator.calculate(2, 5.0); // 2 * 5 = 10
        assertEquals(10.0, silverFine);

        // بعدين نبدّل لـ Gold
        calculator.setStrategy(new GoldFineStrategy());
        double goldFine = calculator.calculate(2, 5.0); // 2 * (5 * 0.5) = 5
        assertEquals(5.0, goldFine);
    }
}
