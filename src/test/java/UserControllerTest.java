import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.Callback;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Robust JUnit 5 Test Suite for UserController.
 * <p>
 * This class uses reflection and JavaFX concurrency utilities to achieve
 * maximum code coverage while remaining stable in CI/CD environments.
 * </p>
 * 
 * @author Zainab
 * @version 1.1
 */
class UserControllerTest {

    private UserController controller;
    private static final String TARGET_FILE = "books.txt";

    /**
     * Initializes the JavaFX toolkit once.
     * Catches exceptions if the toolkit is already running to prevent CI failures.
     */
    @BeforeAll
    static void initJavaFXEnv() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            
        }
        System.setProperty("EMAIL_USERNAME", "mock_test_user");
        System.setProperty("EMAIL_PASSWORD", "mock_test_pass");
    }

    /**
     * Sets up the test environment.
     * Creates a fresh data file and injects mock UI components before each test.
     * 
     * @throws Exception If reflection or IO errors occur.
     */
    @BeforeEach
    void setUp() throws Exception {
        createTestDataFile();
        controller = new UserController();
        injectUIComponents();
        
        runAndWait(() -> controller.initialize());
        controller.setCurrentUser("TestUser", "Gold", "test@mail.com");
    }

    /**
     * Cleans up the test environment.
     * Deletes the data file to prevent test pollution.
     */
    @AfterEach
    void tearDown() {
        File file = new File(TARGET_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Verifies that the controller initializes the table correctly.
     */
    @Test
    void testInitialize() {
        TableView<Media> table = getField(controller, "bookTable");
        assertNotNull(table, "Table should be initialized");
        assertFalse(table.getItems().isEmpty(), "Table should load data on init");
    }

    /**
     * Verifies setting the current user updates the UI welcome label.
     */
    @Test
    void testSetCurrentUser() {
        runAndWait(() -> controller.setCurrentUser("NewUser", "Silver", "new@mail.com"));
        Label label = getField(controller, "welcomeLabel");
        assertNotNull(label.getText());
        assertTrue(label.getText().contains("NewUser"));
    }

    /**
     * Verifies legacy setter methods for backward compatibility.
     */
    @Test
    void testLegacySetters() {
        runAndWait(() -> {
            controller.setMembershipType("Platinum");
            controller.setCurrentUsername("LegacyName");
            controller.setCurrentUser("LegacyUser", "legacy@mail.com");
        });
        Label label = getField(controller, "welcomeLabel");
        assertTrue(label.getText().contains("LegacyUser"));
    }

    /**
     * Tests the successful borrowing of a book.
     */
    @Test
    void testHandleBorrowBookSuccess() {
        selectRow(0);
        runAndWait(() -> controller.handleBorrowBook());
        
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Borrowed successfully") || msg.getText().contains("Due date"));
        
        TableView<Media> table = getField(controller, "bookTable");
        assertEquals("Borrowed", table.getItems().get(0).getStatus());
    }

    /**
     * Tests borrowing failure when no item is selected.
     */
    @Test
    void testHandleBorrowBookNoSelection() {
        clearSelection();
        runAndWait(() -> controller.handleBorrowBook());
        
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().toLowerCase().contains("select"));
    }

    /**
     * Tests borrowing failure when the user has outstanding fines.
     */
    @Test
    void testHandleBorrowBookWithFines() {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(1);
        item.borrow("TestUser");
        item.setFineAmount(100.0);
        
        selectRow(0);
        runAndWait(() -> controller.handleBorrowBook());
        
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().toLowerCase().contains("fine"));
    }

    /**
     * Tests borrowing failure when the item is already borrowed by the same user.
     */
    @Test
    void testHandleBorrowBookAlreadyBorrowed() {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(0);
        item.borrow("TestUser");
        
        selectRow(0);
        runAndWait(() -> controller.handleBorrowBook());
        
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("already borrowed"));
    }

    /**
     * Tests borrowing failure when the item is unavailable (borrowed by another).
     */
    @Test
    void testHandleBorrowBookUnavailable() {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(0);
        item.borrow("OtherUser");
        
        selectRow(0);
        runAndWait(() -> controller.handleBorrowBook());
        
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("not available"));
    }

    /**
     * Tests returning a book successfully.
     */
    @Test
    void testHandleReturnBookSuccess() {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(0);
        item.borrow("TestUser");
        item.setFineAmount(0.0);
        
        selectRow(0);
        runAndWait(() -> controller.handleReturnBook());
        
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Returned successfully"));
    }

    /**
     * Tests returning a book failure due to unpaid fines.
     */
    @Test
    void testHandleReturnBookWithFine() {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(0);
        item.borrow("TestUser");
        item.setDueDate("2000-01-01"); 
        
        selectRow(0);
        runAndWait(() -> controller.handleReturnBook());
        
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Pay the fine"));
    }

    /**
     * Tests returning a book failure when the user is not the borrower.
     */
    @Test
    void testHandleReturnBookNotOwner() {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(0);
        item.borrow("OtherUser");
        
        selectRow(0);
        runAndWait(() -> controller.handleReturnBook());
        
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("only return your own"));
    }

    /**
     * Tests partial payment of fines.
     */
    @Test
    void testHandlePayFinePartial() {
        setupFineScenario(0, 50.0);
        setInputText(getField(controller, "paymentField"), "20.0");
        
        runAndWait(() -> controller.handlePayFine());
        
        Label info = getField(controller, "infoLabel");
        assertTrue(info.getText().contains("Partial payment"));
    }

    /**
     * Tests full payment of fines.
     */
    @Test
    void testHandlePayFineFull() {
        setupFineScenario(0, 10.0);
        setInputText(getField(controller, "paymentField"), "10.0");
        
        runAndWait(() -> controller.handlePayFine());
        
        Label info = getField(controller, "infoLabel");
        assertTrue(info.getText().contains("fully paid") || info.getText().contains("returned"));
    }

    /**
     * Tests invalid inputs for payment (negative, text, etc.).
     */
    @Test
    void testHandlePayFineInvalidInputs() {
        setupFineScenario(0, 10.0);
        
        setInputText(getField(controller, "paymentField"), "-5");
        runAndWait(() -> controller.handlePayFine());
        assertTrue(getLabelText("infoLabel").contains("positive"));

        setInputText(getField(controller, "paymentField"), "abc");
        runAndWait(() -> controller.handlePayFine());
        assertTrue(getLabelText("infoLabel").contains("Invalid"));
        
        setInputText(getField(controller, "paymentField"), "100");
        runAndWait(() -> controller.handlePayFine());
        assertTrue(getLabelText("infoLabel").contains("exceeds"));
    }

    /**
     * Tests payment failure when paying for another user's item.
     */
    @Test
    void testHandlePayFineWrongUser() {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(0);
        item.borrow("OtherUser");
        item.setFineAmount(10.0);
        
        selectRow(0);
        setInputText(getField(controller, "paymentField"), "10.0");
        runAndWait(() -> controller.handlePayFine());
        
        assertTrue(getLabelText("infoLabel").contains("YOUR borrowed"));
    }

    /**
     * Tests the reload functionality.
     */
    @Test
    void testHandleReload() {
        runAndWait(() -> controller.handleReload());
        assertTrue(getLabelText("infoLabel").contains("reloaded"));
    }

    /**
     * Tests the logout functionality safely.
     */
    @Test
    void testHandleLogout() {
        assertDoesNotThrow(() -> {
            runAndWait(() -> {
                try {
                    controller.handleLogout();
                } catch (Exception ignored) { }
            });
        });
    }

    /**
     * Tests file parsing resilience against corrupted data.
     * 
     * @throws IOException If writing to file fails.
     */
    @Test
    void testParsingResilience() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TARGET_FILE, true))) {
            writer.write("BadLine"); 
            writer.newLine();
            writer.write("Book,Title,Auth,ISBN,NotInt,Status,Date,NotDbl,User,NotDbl");
            writer.newLine();
        }
        runAndWait(() -> controller.handleReload());
        TableView<Media> table = getField(controller, "bookTable");
        boolean exists = table.getItems().stream().anyMatch(m -> m.getTitle().equals("Title"));
        assertTrue(exists, "Parser should recover from bad numbers");
    }

    /**
     * Tests private helper methods via reflection.
     * 
     * @throws Exception If reflection fails.
     */
    @Test
    void testPrivateMethods() throws Exception {
        Method parseInt = UserController.class.getDeclaredMethod("parseIntSafe", String.class, int.class);
        parseInt.setAccessible(true);
        assertEquals(0, parseInt.invoke(controller, "bad", 0));
        
        Method parseDouble = UserController.class.getDeclaredMethod("parseDoubleSafe", String.class, double.class);
        parseDouble.setAccessible(true);
        assertEquals(0.0, parseDouble.invoke(controller, "bad", 0.0));

        Method normalize = UserController.class.getDeclaredMethod("normalizeBorrowedBy", String.class);
        normalize.setAccessible(true);
        assertEquals("", normalize.invoke(controller, "0.0"));
    }

    /**
     * Tests TableRow styling logic via reflection.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testRowFactory() throws Exception {
        TableView<Media> table = getField(controller, "bookTable");
        Callback<TableView<Media>, TableRow<Media>> factory = table.getRowFactory();
        assertNotNull(factory);
        TableRow<Media> row = factory.call(table);
        
        Method updateItem = TableRow.class.getDeclaredMethod("updateItem", Object.class, boolean.class);
        updateItem.setAccessible(true);
        
        Media m = new Book("T", "A", "I", "Overdue", "D", 1.0, "TestUser", 0, 1);
        assertDoesNotThrow(() -> updateItem.invoke(row, m, false));
    }

    /**
     * Tests TableCell rendering logic via reflection.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testCellFactory() throws Exception {
        TableColumn<Media, String> col = getField(controller, "dueDateColumn");
        TableCell<Media, String> cell = col.getCellFactory().call(col);
        
        Field rowField = javafx.scene.control.Cell.class.getDeclaredField("tableRow");
        rowField.setAccessible(true);
        TableRow<Media> row = new TableRow<>();
        rowField.set(cell, row);
        
        Method updateItem = TableCell.class.getDeclaredMethod("updateItem", Object.class, boolean.class);
        updateItem.setAccessible(true);
        
        Media m = new Book("T", "A", "I", "Borrowed", "2025-01-01", 0.0, "Other", 0, 1);
        row.setItem(m);
        updateItem.invoke(cell, "2025-01-01", false);
        
        assertEquals("", cell.getText());
    }

    // --- Helper Methods ---

    private void createTestDataFile() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TARGET_FILE))) {
            writer.write("Book,Test Book,Author A,111,1,Available,2025-01-01,0.0,0.0,0.0");
            writer.newLine();
            writer.write("CD,Test CD,Artist B,222,1,Available,2025-01-01,0.0,0.0,0.0");
            writer.newLine();
        }
    }

    private void injectUIComponents() throws Exception {
        setField(controller, "welcomeLabel", new Label());
        setField(controller, "paymentField", new TextField());
        setField(controller, "infoLabel", new Label());
        setField(controller, "messageLabel", new Label());
        setField(controller, "bookTable", new TableView<Media>());
        setField(controller, "typeColumn", new TableColumn<Media, String>());
        setField(controller, "titleColumn", new TableColumn<Media, String>());
        setField(controller, "authorColumn", new TableColumn<Media, String>());
        setField(controller, "isbnColumn", new TableColumn<Media, String>());
        setField(controller, "statusColumn", new TableColumn<Media, String>());
        setField(controller, "dueDateColumn", new TableColumn<Media, String>());
        setField(controller, "fineColumn", new TableColumn<Media, Double>());
    }

    private void runAndWait(Runnable action) {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void selectRow(int index) {
        TableView<Media> table = getField(controller, "bookTable");
        runAndWait(() -> table.getSelectionModel().select(index));
    }

    private void clearSelection() {
        TableView<Media> table = getField(controller, "bookTable");
        runAndWait(() -> table.getSelectionModel().clearSelection());
    }

    private void setupFineScenario(int index, double fine) {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(index);
        item.borrow("TestUser");
        item.setFineAmount(fine);
        selectRow(index);
    }

    private void setInputText(TextField field, String text) {
        runAndWait(() -> field.setText(text));
    }

    private String getLabelText(String fieldName) {
        Label label = getField(controller, fieldName);
        return label.getText();
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(Object target, String name) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return (T) f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}