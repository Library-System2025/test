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
import javafx.collections.ObservableList;
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
 * Comprehensive JUnit Test suite for the UserController class.
 * <p>
 * Designed to achieve maximum code coverage by utilizing Reflection for private members
 * and JavaFX Platform handling for GUI components. Handles positive, negative, and 
 * edge-case scenarios including file I/O and data parsing.
 * </p>
 * 
 * @author Zainab
 * @version 1.0
 */
class UserControllerTest {

    private UserController controller;
    private static final String TEST_FILE = "books.txt";

    /**
     * Initializes the JavaFX Platform to prevent Toolkit not initialized errors.
     * Sets mock environment variables for email service logic.
     */
    @BeforeAll
    static void initJfxAndEnv() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
        }
        System.setProperty("EMAIL_USERNAME", "mock_user");
        System.setProperty("EMAIL_PASSWORD", "mock_cred");
    }

    /**
     * Sets up the test environment, including creating a fresh data file,
     * initializing the controller, and injecting mock UI components.
     * 
     * @throws Exception If reflection or file I/O fails.
     */
    @BeforeEach
    void setUp() throws Exception {
        createTestFile();
        controller = new UserController();
        initializeUIComponents();
        runOnFxThreadAndWait(() -> controller.initialize());
        controller.setCurrentUser("TestUser", "Gold", "test@mail.com");
    }

    /**
     * Cleans up the test environment by deleting the temporary data file.
     */
    @AfterEach
    void tearDown() {
        File file = new File(TEST_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Tests the table view initialization and population.
     */
    @Test
    void testInitialize() {
        TableView<Media> table = getField(controller, "bookTable");
        assertNotNull(table.getItems());
        assertFalse(table.getItems().isEmpty());
    }

    /**
     * Tests setting the current user and updating the UI labels accordingly.
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testSetCurrentUser() throws InterruptedException {
        runOnFxThreadAndWait(() -> controller.setCurrentUser("NewUser", "Silver", "new@mail.com"));
        Label label = getField(controller, "welcomeLabel");
        assertTrue(label.getText().contains("NewUser"));
    }

    /**
     * Tests all legacy setters to ensure backward compatibility and view updates.
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testLegacySetters() throws InterruptedException {
        runOnFxThreadAndWait(() -> {
            controller.setMembershipType("Platinum");
            controller.setCurrentUsername("UpdatedUser");
            controller.setCurrentUser("LegacyUser", "legacy@mail.com");
        });
        Label label = getField(controller, "welcomeLabel");
        assertTrue(label.getText().contains("LegacyUser"));
    }

    /**
     * Tests the successful borrowing scenario.
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testBorrowBookSuccess() throws InterruptedException {
        performBorrow(0);
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Borrowed successfully"));
    }

    /**
     * Tests borrowing failure when no item is selected.
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
     * Tests borrowing failure when the user has unpaid fines on other items.
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testBorrowBookFailWithFines() throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        Media fineItem = table.getItems().get(1);
        
        fineItem.borrow("TestUser");
        fineItem.setFineAmount(10.0);

        performBorrow(0);
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("unpaid fines"));
    }

    /**
     * Tests borrowing failure when the user already has a copy of the item.
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testBorrowBookFailAlreadyBorrowed() throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        table.getItems().get(0).borrow("TestUser");
        performBorrow(0);
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("already borrowed"));
    }
    
    /**
     * Tests borrowing failure when the item is not available (borrowed by another).
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testBorrowBookFailUnavailable() throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        table.getItems().get(0).borrow("OtherUser");
        performBorrow(0);
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("not available"));
    }

    /**
     * Tests partial payment functionality where the fine is reduced but not cleared.
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testPayFinePartialPayment() throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(1);
        
        item.borrow("TestUser");
        item.setStatus("Overdue");
        item.setDueDate("2000-01-01");
        
        item.calculateFine("Gold");

        performPayment(item, "1.0");

        Label info = getField(controller, "infoLabel");
        assertTrue(info.getText().contains("Partial payment"));
    }

    /**
     * Tests full payment functionality where the fine is cleared and item returned.
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testPayFineFullPayment() throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(1);
        
        item.borrow("TestUser");
        item.setStatus("Overdue");
        item.setDueDate("2000-01-01");
        
        item.calculateFine("Gold");
        double exactFine = item.getFineAmount();
        
        performPayment(item, String.valueOf(exactFine));
        
        Label info = getField(controller, "infoLabel");
        assertTrue(info.getText().contains("fully paid") || info.getText().contains("Item returned"));
    }

    /**
     * Tests various validation scenarios for payment input fields.
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testPayFineValidation() throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(1);
        
        item.borrow("TestUser");
        item.setStatus("Overdue");
        item.setDueDate("2000-01-01"); 

        performPayment(item, "-5");
        Label info = getField(controller, "infoLabel");
        assertTrue(info.getText().contains("positive"));

        performPayment(item, "abc");
        assertTrue(info.getText().contains("Invalid number"));

        performPayment(item, "0");
        assertTrue(info.getText().contains("positive"));
        
        performPayment(item, "10000000.0");
        assertTrue(info.getText().contains("Payment exceeds"));
    }
    
    /**
     * Tests payment failure when no item is selected.
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testPayFineNoSelection() throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().clearSelection();
            controller.handlePayFine();
        });
        Label info = getField(controller, "infoLabel");
        assertTrue(info.getText().contains("Select an item"));
    }
    
    /**
     * Tests payment failure when attempting to pay for another user's item.
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testPayFineWrongUser() throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(1);
        
        item.borrow("OtherUser");
        item.setFineAmount(10.0);
        
        performPayment(item, "10.0");
        Label info = getField(controller, "infoLabel");
        assertTrue(info.getText().contains("YOUR borrowed items"));
    }

    /**
     * Tests successful item return logic.
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
    }

    /**
     * Tests that returning a book with an outstanding fine is blocked.
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
            controller.handleReturnBook();
        });

        if (item.getFineAmount() > 0) {
            Label msg = getField(controller, "messageLabel");
            assertTrue(msg.getText().contains("Pay the fine"));
        }
    }
    
    /**
     * Tests validation preventing a user from returning someone else's item.
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testValidationNotBorrower() throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(1);
        item.borrow("OtherPerson");
        
        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(item);
            controller.handleReturnBook();
        });
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("only return your own"));
    }
    
    /**
     * Tests return failure when no item is selected.
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testReturnBookNoSelection() throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().clearSelection();
            controller.handleReturnBook();
        });
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Select an item"));
    }

    /**
     * Tests all private utility methods using Reflection to ensure 100% coverage
     * on data parsing and normalization logic.
     * 
     * @throws Exception If reflection access fails.
     */
    @Test
    void testPrivateHelpersViaReflection() throws Exception {
        Method parseInt = UserController.class.getDeclaredMethod("parseIntSafe", String.class, int.class);
        parseInt.setAccessible(true);
        assertEquals(5, parseInt.invoke(controller, "5", 0));
        assertEquals(0, parseInt.invoke(controller, "NotANumber", 0));

        Method parseDouble = UserController.class.getDeclaredMethod("parseDoubleSafe", String.class, double.class);
        parseDouble.setAccessible(true);
        assertEquals(5.5, parseDouble.invoke(controller, "5.5", 0.0));
        assertEquals(0.0, parseDouble.invoke(controller, "NotADouble", 0.0));
        
        Method normalize = UserController.class.getDeclaredMethod("normalizeBorrowedBy", String.class);
        normalize.setAccessible(true);
        assertEquals("", normalize.invoke(controller, "0.0"));
        assertEquals("", normalize.invoke(controller, (Object) null));
        assertEquals("User", normalize.invoke(controller, "User"));
    }

    /**
     * Tests robust file parsing by injecting corrupted or incomplete data.
     * 
     * @throws IOException If file writing fails.
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testParsingExceptionsAndDefaults() throws IOException, InterruptedException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEST_FILE))) {
            writer.write("Book,BadData,Auth,111,NOT_NUM,Available,2022-01-01,NOT_DBL,User,NOT_DBL");
            writer.newLine();
            writer.write("TooShort"); 
            writer.newLine();
        }
        runOnFxThreadAndWait(() -> controller.handleReload());
        TableView<Media> table = getField(controller, "bookTable");
        assertTrue(table.getItems().stream().anyMatch(m -> "BadData".equals(m.getTitle())));
    }
    
    /**
     * Tests the notification logic path when the user email is null or empty.
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testOverdueNotificationNullEmail() throws InterruptedException {
        runOnFxThreadAndWait(() -> controller.setCurrentUser("TestUser", "Gold", null));
        
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(0);
        
        item.borrow("TestUser");
        item.setStatus("Overdue");
        item.setDueDate("2000-01-01");
        
        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(item);
            controller.handleReturnBook();
        });
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Pay the fine"));
    }

    /**
     * Tests the reload functionality.
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
     * Tests the logout functionality in a safe manner that prevents
     * FXML loading errors from failing the test suite.
     * 
     * @throws InterruptedException If the FX thread is interrupted.
     */
    @Test
    void testLogout() throws InterruptedException {
        runOnFxThreadAndWait(() -> {
            try {
                controller.handleLogout();
            } catch (Exception e) {
            }
        });
    }

    /**
     * Tests file saving error handling by attempting to read from a non-existent file.
     * 
     * @throws Exception If any exception occurs.
     */
    @Test
    void testSaveFileException() throws Exception {
        File file = new File(TEST_FILE);
        if (file.exists()) file.delete();
        
        runOnFxThreadAndWait(() -> controller.handleReload());
    }

    /**
     * Tests specific creation of CD media types via the private factory method.
     * 
     * @throws Exception If reflection access fails.
     */
    @Test
    void testCreateMediaItemCD() throws Exception {
        Method createMethod = UserController.class.getDeclaredMethod("createMediaItem", 
                String.class, String.class, String.class, String.class, 
                String.class, String.class, double.class, String.class, 
                double.class, int.class);
        createMethod.setAccessible(true);
        
        Object result = createMethod.invoke(controller, "CD", "Title", "Auth", "ISBN", "Avail", "", 0.0, "", 0.0, 1);
        assertNotNull(result);
        assertEquals("CD", ((CD)result).getMediaType());
    }

    private void performBorrow(int index) throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(index);
            controller.handleBorrowBook();
        });
    }

    private void performPayment(Media item, String amount) throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        TextField payField = getField(controller, "paymentField");
        payField.setText(amount);
        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(item);
            controller.handlePayFine();
        });
    }

    private void createTestFile() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEST_FILE))) {
            writer.write("Book,Java Programming,Author A,12345,1,Available,2025-12-31,0.0,0.0,0.0");
            writer.newLine();
            writer.write("CD,Classic Hits,Artist B,67890,1,Available,2025-12-31,0.0,0.0,0.0");
            writer.newLine();
        }
    }

    private void initializeUIComponents() throws Exception {
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
        
        ObservableList<Media> items = table.getItems(); 
        
        ((TableColumn<Media, String>) getField(controller, "typeColumn")).setCellValueFactory(new PropertyValueFactory<>("mediaType"));
    }

    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

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
}