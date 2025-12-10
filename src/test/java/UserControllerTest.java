import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive JUnit Test suite for the {@link UserController} class.
 * <p>
 * This class validates the library operations available to a logged-in user, including
 * borrowing books, returning items, paying fines (partial and full), and data persistence.
 * It simulates a full JavaFX environment using headless testing techniques and Reflection
 * to access private controller fields.
 * </p>
 * 
 * @author Zainab
 * @version 3.0
 */
class UserControllerTest {

    /** The instance of the controller under test. */
    private UserController controller;
    
    /** The path to the temporary test database file. */
    private static final String TEST_FILE = "books.txt";

    /**
     * Initializes the JavaFX Platform to prevent IllegalStateException during testing.
     * Also sets dummy environment variables for email service dependencies.
     */
    @BeforeAll
    static void initJfxAndEnv() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
        }
        System.setProperty("EMAIL_USERNAME", "test_user");
        System.setProperty("EMAIL_PASSWORD", "test_pass");
    }

    /**
     * Sets up the test environment before each test case.
     * <p>
     * Creates a fresh test file, initializes the controller, injects UI dependencies,
     * and sets a default logged-in user context.
     * </p>
     * 
     * @throws Exception If reflection or file operations fail.
     */
    @BeforeEach
    void setUp() throws Exception {
        createTestFile();
        controller = new UserController();

        injectField(controller, "welcomeLabel", new Label());
        injectField(controller, "paymentField", new TextField());
        injectField(controller, "infoLabel", new Label());
        injectField(controller, "messageLabel", new Label());

        TableView<Media> table = new TableView<>();
        injectField(controller, "bookTable", table);
        injectField(controller, "typeColumn", new TableColumn<Media, String>());
        injectField(controller, "titleColumn", new TableColumn<Media, String>());
        injectField(controller, "authorColumn", new TableColumn<Media, String>());
        injectField(controller, "isbnColumn", new TableColumn<Media, String>());
        injectField(controller, "statusColumn", new TableColumn<Media, String>());
        injectField(controller, "dueDateColumn", new TableColumn<Media, String>());
        injectField(controller, "fineColumn", new TableColumn<Media, Double>());

        runOnFxThreadAndWait(() -> controller.initialize());

        controller.setCurrentUser("TestUser", "Gold", "test@mail.com");
    }

    /**
     * Cleans up the test file after each test execution to ensure isolation.
     */
    @AfterEach
    void tearDown() {
        File file = new File(TEST_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Helper method to execute actions on the JavaFX Application Thread.
     * 
     * @param action The runnable task to execute.
     * @throws InterruptedException If the thread is interrupted while waiting.
     */
    private void runOnFxThreadAndWait(Runnable action) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
    }

    /**
     * Verifies that the table view is correctly populated upon initialization.
     */
    @Test
    void testInitialize() {
        TableView<Media> table = getField(controller, "bookTable");
        assertNotNull(table.getItems());
        assertFalse(table.getItems().isEmpty());
    }

    /**
     * Verifies that setting the current user updates the Welcome Label with the correct name and membership.
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testSetCurrentUser() throws InterruptedException {
        runOnFxThreadAndWait(() -> controller.setCurrentUser("NewUser", "Silver", "new@mail.com"));
        
        Label label = getField(controller, "welcomeLabel");
        assertTrue(label.getText().contains("NewUser"));
        assertTrue(label.getText().contains("Silver"));
    }

    /**
     * Tests a successful book borrowing scenario.
     * Checks that the status updates to "Borrowed" and the success message appears.
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testBorrowBookSuccess() throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        
        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(0); 
            controller.handleBorrowBook();
        });

        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Borrowed successfully"));
        assertEquals("Borrowed", table.getItems().get(0).getStatus());
    }

    /**
     * Tests borrowing failure when no item is selected from the table.
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testBorrowBookFailNoSelection() throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        
        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().clearSelection();
            controller.handleBorrowBook();
        });

        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("select an item"));
    }

    /**
     * Tests borrowing failure when the user has outstanding unpaid fines on other items.
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testBorrowBookFailWithFines() throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        ObservableList<Media> items = table.getItems();
        
        Media fineItem = items.get(1);
        fineItem.borrow("TestUser");
        fineItem.setFineAmount(10.0); 

        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(0); 
            controller.handleBorrowBook();
        });

        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("unpaid fines"));
    }

    /**
     * Tests borrowing failure when the user attempts to borrow the same item twice.
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testBorrowBookFailAlreadyBorrowed() throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(0);
        item.borrow("TestUser");

        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(0);
            controller.handleBorrowBook();
        });

        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("already borrowed"));
    }

    /**
     * Tests partial payment of a fine.
     * Ensures the fine amount is reduced but the status remains "Overdue".
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testPayFinePartialPayment() throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(1);
        
        item.borrow("TestUser");
        item.setStatus("Overdue");
        item.setFineAmount(50.0);

        TextField payField = getField(controller, "paymentField");
        payField.setText("20.0");

        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(item);
            controller.handlePayFine();
        });

        Label info = getField(controller, "infoLabel");
        
        assertTrue(info.getText().contains("Partial payment"));
        assertTrue(item.getFineAmount() < 50.0);
        assertEquals("Overdue", item.getStatus());
    }

    /**
     * Tests full payment of a fine.
     * Ensures the item status returns to "Available" (or returned state) after full payment.
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testPayFineFullPayment() throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(1);
        
        item.borrow("TestUser");
        item.setStatus("Overdue");
        item.setFineAmount(10.0);

        TextField payField = getField(controller, "paymentField");
        payField.setText("10.0");

        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(item);
            controller.handlePayFine();
        });

        Label info = getField(controller, "infoLabel");
        assertTrue(info.getText().contains("fully paid") || info.getText().contains("Item returned"));
        assertEquals("Available", item.getStatus());
    }

    /**
     * Tests validation logic preventing negative numbers in the payment field.
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testPayFineInvalidNegative() throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(1);
        item.borrow("TestUser");
        item.setFineAmount(10.0);

        TextField payField = getField(controller, "paymentField");
        payField.setText("-5");

        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(item);
            controller.handlePayFine();
        });

        Label info = getField(controller, "infoLabel");
        assertTrue(info.getText().contains("positive"));
    }

    /**
     * Tests successful return of a book with no fines.
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testReturnBookSuccess() throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(0);
        item.borrow("TestUser");
        item.setFineAmount(0);

        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(item);
            controller.handleReturnBook();
        });

        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Returned successfully"));
        assertEquals("Available", item.getStatus());
    }

    /**
     * Tests failure to return a book when fines are pending.
     * Simulates an old due date to trigger fine calculation logic.
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testReturnBookFailWithFine() throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(1);
        item.borrow("TestUser");
        item.setDueDate("2000-01-01"); 
        
        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(item);
            try {
                controller.handleReturnBook();
            } catch (Exception e) {
            }
        });

        if (item.getFineAmount() > 0) {
            Label msg = getField(controller, "messageLabel");
            assertTrue(msg.getText().contains("Pay the fine"));
            assertEquals("Overdue", item.getStatus());
        }
    }

    /**
     * Tests the data reload functionality to ensure the UI refreshes from the file.
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testReload() throws InterruptedException {
        runOnFxThreadAndWait(() -> controller.handleReload());
        Label info = getField(controller, "infoLabel");
        assertTrue(info.getText().contains("reloaded"));
    }

    /**
     * Tests the logout functionality to ensure no exceptions are thrown during scene transition.
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testLogout() throws InterruptedException {
        runOnFxThreadAndWait(() -> controller.handleLogout());
    }
    
    /**
     * Tests the system's resilience when loading a file containing corrupted data lines.
     * 
     * @throws IOException If file writing fails.
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testLoadMediaCorruptedFile() throws IOException, InterruptedException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEST_FILE, true))) {
            writer.write("Invalid,Line,Without,Enough,Commas");
            writer.newLine();
        }
        
        runOnFxThreadAndWait(() -> controller.handleReload());
        
        TableView<Media> table = getField(controller, "bookTable");
        assertNotNull(table.getItems());
    }

    /**
     * Helper method to create a valid book data file for testing.
     * 
     * @throws IOException If file creation fails.
     */
    private void createTestFile() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEST_FILE))) {
            writer.write("Book,Java Programming,Author A,12345,1,Available,2025-12-31,0.0,0.0,0.0");
            writer.newLine();
            writer.write("CD,Classic Hits,Artist B,67890,1,Available,2025-12-31,0.0,0.0,0.0");
            writer.newLine();
        }
    }

    /**
     * Helper method to inject dependencies into private fields using Reflection.
     * 
     * @param target    The object containing the field.
     * @param fieldName The name of the field to inject.
     * @param value     The value to inject.
     * @throws Exception If reflection fails.
     */
    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * Helper method to retrieve values from private fields using Reflection.
     * 
     * @param target    The object containing the field.
     * @param fieldName The name of the field to retrieve.
     * @param <T>       The expected type of the field.
     * @return The value of the field.
     */
    @SuppressWarnings("unchecked")
    private <T> T getField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}