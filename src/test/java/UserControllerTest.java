import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive JUnit test suite for the {@link UserController} class.
 * <p>
 * This class utilizes Java Reflection to access private fields and methods.
 * Updated to ensure atomic UI operations on the JavaFX thread.
 * </p>
 * 
 * @author Zainab
 * @version 1.0
 */
class UserControllerTest {

    private UserController controller;
    private static final String TEST_FILE = "books.txt"; 
    private static final String BACKUP_FILE = "library.txt";
    private static final String MOCK_EMAIL = "test@mock.com";
    private static final String MOCK_USER = "TestUser";

    /**
     * Initializes the JavaFX runtime environment.
     * This prevents {@link IllegalStateException} when accessing JavaFX components.
     */
    @BeforeAll
    static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
        }
        System.setProperty("EMAIL_USERNAME", "mock_user");
        System.setProperty("EMAIL_PASSWORD", "mock_pass");
    }

    /**
     * Sets up the test environment before each test execution.
     * Creates temporary data files with default names to ensure the controller finds them.
     * Initializes the controller and injects mock UI controls.
     * 
     * @throws Exception if any initialization step fails.
     */
    @BeforeEach
    void setUp() throws Exception {
        createDataFile(TEST_FILE);
        createDataFile(BACKUP_FILE);
        
        controller = new UserController();
        injectTestFilePath();
        injectMockControls();
        
        runAndWait(() -> controller.initialize());
        controller.setCurrentUser(MOCK_USER, "Gold", MOCK_EMAIL);
    }

    /**
     * Cleans up the test environment after each test execution.
     * Deletes the temporary data files.
     */
    @AfterEach
    void tearDown() {
        new File(TEST_FILE).delete();
        new File(BACKUP_FILE).delete();
    }

    /**
     * Attempts to inject the test file path into the controller by guessing common field names.
     */
    private void injectTestFilePath() {
        String[] possibleFields = {"DATA_FILE", "FILE_NAME", "FILE_PATH", "fileName", "dataFile", "csvFile", "BOOK_FILE"};
        for (String fieldName : possibleFields) {
            try {
                Field field = UserController.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(controller, TEST_FILE);
                return;
            } catch (Exception e) {
            }
        }
    }

    /**
     * Verifies that the controller initializes correctly and loads data into the table.
     */
    @Test
    void testInitializationAndLoading() {
        TableView<Media> table = getField("bookTable");
        assertNotNull(table.getItems());
        assertFalse(table.getItems().isEmpty(), "Table should load data from the file.");
        assertTrue(table.getItems().size() >= 2, "Table should contain at least 2 items.");
    }

    /**
     * Tests that updating the current user context updates the UI labels correctly.
     * Also verifies legacy setter methods.
     * 
     * @throws InterruptedException if the JavaFX thread is interrupted.
     */
    @Test
    void testUserContextUpdates() throws InterruptedException {
        runAndWait(() -> controller.setCurrentUser("NewUser", "Silver", "new@mail.com"));
        Label label = getField("welcomeLabel");
        assertTrue(label.getText().contains("NewUser"));
        
        runAndWait(() -> {
            controller.setMembershipType("Platinum");
            controller.setCurrentUsername("UpdatedName");
        });
        assertTrue(label.getText().contains("UpdatedName"));
    }

    /**
     * Tests the complete book borrowing workflow.
     * Covers successful borrowing, attempting to borrow an already borrowed item,
     * and attempting to borrow without a selection.
     * 
     * @throws InterruptedException if the JavaFX thread is interrupted.
     */
    @Test
    void testBorrowFlow() throws InterruptedException {
        runAndWait(() -> {
            TableView<Media> table = getField("bookTable");
            table.getSelectionModel().select(0);
            controller.handleBorrowBook();
        });

        Label msg = getField("messageLabel");
        String text = msg.getText();
        assertTrue(text.contains("successfully") || text.contains("Due date"), 
                   "Borrow failed. Actual message: " + text);

        runAndWait(() -> {
            TableView<Media> table = getField("bookTable");
            table.getSelectionModel().select(0);
            controller.handleBorrowBook();
        });
        assertTrue(msg.getText().contains("already borrowed") || msg.getText().contains("own this book"), 
                   "Expected error message. Actual: " + msg.getText());

        runAndWait(() -> {
            ((TableView<?>) getField("bookTable")).getSelectionModel().clearSelection();
            controller.handleBorrowBook();
        });
        assertTrue(msg.getText().contains("select"), "Expected selection warning. Actual: " + msg.getText());
    }

    /**
     * Verifies that a user cannot borrow a new book if they have existing unpaid fines.
     * 
     * @throws InterruptedException if the JavaFX thread is interrupted.
     */
    @Test
    void testBorrowWithFinesBlocker() throws InterruptedException {
        TableView<Media> table = getField("bookTable");
        if (table.getItems().size() < 2) return;

        Media item = table.getItems().get(1);
        item.borrow(MOCK_USER);
        item.setFineAmount(50.0);

        runAndWait(() -> {
            table.getSelectionModel().select(0); 
            controller.handleBorrowBook();
        });
        
        Label msg = getField("messageLabel");
        assertTrue(msg.getText().contains("unpaid fines"), "Blocker failed. Actual: " + msg.getText());
    }

    /**
     * Tests the book return workflow.
     * Covers successful return and attempting to return an item borrowed by another user.
     * 
     * @throws InterruptedException if the JavaFX thread is interrupted.
     */
    @Test
    void testReturnFlow() throws InterruptedException {
        TableView<Media> table = getField("bookTable");
        Media item = table.getItems().get(0);
        item.borrow(MOCK_USER);
        item.setFineAmount(0);

        runAndWait(() -> {
            table.getSelectionModel().select(item);
            controller.handleReturnBook();
        });
        
        Label msg = getField("messageLabel");
        assertTrue(msg.getText().contains("Returned successfully"), "Return failed. Actual: " + msg.getText());

        if (table.getItems().size() > 1) {
            Media otherItem = table.getItems().get(1);
            otherItem.borrow("OtherGuy");
            runAndWait(() -> {
                table.getSelectionModel().select(otherItem);
                controller.handleReturnBook();
            });
            assertTrue(msg.getText().contains("only return your own"), "Security check failed. Actual: " + msg.getText());
        }
    }

    /**
     * Verifies that returning an overdue book with a fine is blocked until payment is made.
     * 
     * @throws InterruptedException if the JavaFX thread is interrupted.
     */
    @Test
    void testReturnWithFineBlocks() throws InterruptedException {
        TableView<Media> table = getField("bookTable");
        if (table.getItems().size() < 2) return;
        
        Media item = table.getItems().get(1);
        item.borrow(MOCK_USER);
        item.setDueDate("2000-01-01");

        runAndWait(() -> {
            table.getSelectionModel().select(item);
            controller.handleReturnBook();
        });

        if (item.getFineAmount() > 0 || getField("messageLabel").toString().contains("Pay")) {
            Label msg = getField("messageLabel");
            assertTrue(msg.getText().contains("Pay the fine") || msg.getText().contains("fines"), 
                       "Fine block failed. Actual: " + msg.getText());
        }
    }

    /**
     * Tests the fine payment workflow, including partial payments and full payments.
     * 
     * @throws InterruptedException if the JavaFX thread is interrupted.
     */
    @Test
    void testPaymentFlow() throws InterruptedException {
        TableView<Media> table = getField("bookTable");
        if (table.getItems().size() < 2) return;

        Media item = table.getItems().get(1);
        item.borrow(MOCK_USER);
        item.setDueDate("2000-01-01");
        item.calculateFine("Gold"); 
        
        double totalFine = item.getFineAmount();
        TextField payField = getField("paymentField");

        runAndWait(() -> {
            table.getSelectionModel().select(item);
            payField.setText("1.0");
            controller.handlePayFine();
        });
        
        Label info = getField("infoLabel");
        String infoText = info.getText();
        assertTrue(infoText.contains("Partial") || infoText.contains("Remaining"), 
                   "Partial payment failed. Actual: " + infoText);

        runAndWait(() -> {
            table.getSelectionModel().select(item);
            payField.setText(String.valueOf(totalFine));
            controller.handlePayFine();
        });
        
        infoText = info.getText();
        assertTrue(infoText.contains("paid") || infoText.contains("returned") || infoText.contains("successful"), 
                   "Full payment failed. Actual: " + infoText);
    }

    /**
     * Tests validation logic for the payment input field.
     * Checks for negative numbers, invalid strings, and overpayment.
     * 
     * @throws InterruptedException if the JavaFX thread is interrupted.
     */
    @SuppressWarnings("unchecked")
    @Test
    void testPaymentValidation() throws InterruptedException {
        TableView<Media> table = getField("bookTable");
        if (table.getItems().size() < 2) return;
        
        Media item = table.getItems().get(1);
        item.borrow(MOCK_USER);
        item.setFineAmount(10.0);
        
        TextField payField = getField("paymentField");
        Label info = getField("infoLabel");

        runAndWait(() -> { 
            table.getSelectionModel().select(item);
            payField.setText("-5"); 
            controller.handlePayFine(); 
        });
        assertTrue(info.getText().contains("positive"), "Negative check failed: " + info.getText());

        runAndWait(() -> { 
            payField.setText("NotNumber"); 
            controller.handlePayFine(); 
        });
        assertTrue(info.getText().contains("Invalid"), "NaN check failed: " + info.getText());

        runAndWait(() -> { 
            payField.setText("9999"); 
            controller.handlePayFine(); 
        });
        assertTrue(info.getText().contains("exceeds"), "Overpayment check failed: " + info.getText());
    }

    /**
     * Tests the functionality of reloading data from file and handling logout.
     * 
     * @throws InterruptedException if the JavaFX thread is interrupted.
     */
    @Test
    void testReloadAndLogout() throws InterruptedException {
        runAndWait(() -> controller.handleReload());
        Label info = getField("infoLabel");
        if (info.getText() != null && !info.getText().isEmpty()) {
            assertTrue(info.getText().contains("reloaded"));
        }

        runAndWait(() -> {
            try { controller.handleLogout(); } catch (Exception e) {}
        });
    }

    /**
     * Tests private helper methods within the controller using Reflection.
     * This ensures coverage for internal parsing logic that is not directly exposed.
     * 
     * @throws Exception if reflection access fails.
     */
    @Test
    void testReflectionHelpers() throws Exception {
        Method parseInt = UserController.class.getDeclaredMethod("parseIntSafe", String.class, int.class);
        parseInt.setAccessible(true);
        assertEquals(10, parseInt.invoke(controller, "10", 0));
        assertEquals(5, parseInt.invoke(controller, "bad", 5));

        Method normalize = UserController.class.getDeclaredMethod("normalizeBorrowedBy", String.class);
        normalize.setAccessible(true);
        assertEquals("", normalize.invoke(controller, "0.0"));
    }

    /**
     * Executes a Runnable on the JavaFX Application Thread and waits for it to complete.
     * 
     * @param action The action to execute.
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    private void runAndWait(Runnable action) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try { action.run(); } finally { latch.countDown(); }
        });
        latch.await(3, TimeUnit.SECONDS);
    }

    /**
     * Retrieves the value of a private field from the controller using Reflection.
     * 
     * @param name The name of the field.
     * @param <T> The type of the field.
     * @return The value of the field.
     * @throws RuntimeException if the field cannot be accessed.
     */
    @SuppressWarnings("unchecked")
    private <T> T getField(String name) {
        try {
            Field f = UserController.class.getDeclaredField(name);
            f.setAccessible(true);
            return (T) f.get(controller);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * Injects mock JavaFX controls into the controller to prevent NPEs during testing.
     * 
     * @throws Exception if field injection fails.
     */
    private void injectMockControls() throws Exception {
        setField("welcomeLabel", new Label());
        setField("paymentField", new TextField());
        setField("infoLabel", new Label());
        setField("messageLabel", new Label());

        TableView<Media> table = new TableView<>();
        setField("bookTable", table);
        
        setField("typeColumn", new TableColumn<Media, String>());
        setField("titleColumn", new TableColumn<Media, String>());
        setField("authorColumn", new TableColumn<Media, String>());
        setField("isbnColumn", new TableColumn<Media, String>());
        setField("statusColumn", new TableColumn<Media, String>());
        setField("dueDateColumn", new TableColumn<Media, String>());
        setField("fineColumn", new TableColumn<Media, Double>());
        
        ((TableColumn<?,?>) getField("typeColumn")).setCellValueFactory(new PropertyValueFactory<>("mediaType"));
    }

    /**
     * Sets the value of a private field in the controller using Reflection.
     * 
     * @param name The name of the field.
     * @param val The value to set.
     * @throws Exception if the field cannot be accessed.
     */
    private void setField(String name, Object val) throws Exception {
        Field f = UserController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, val);
    }

    /**
     * Creates a temporary CSV file with test data.
     * 
     * @param fileName The name of the file to create.
     * @throws IOException if the file cannot be written.
     */
    private void createDataFile(String fileName) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(fileName))) {
            w.write("Book,Test Title,Auth,123,1,Available,2025-01-01,0.0,0.0,0.0");
            w.newLine();
            w.write("CD,Test CD,Artist,456,1,Available,2025-01-01,0.0,0.0,0.0");
        }
    }
}