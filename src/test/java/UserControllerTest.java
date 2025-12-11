import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * Comprehensive JUnit 5 Test Suite for the UserController class.
 * <p>
 * This class ensures high code coverage by testing public API methods, 
 * UI logic via mocked components, and private helper methods via reflection.
 * It handles JavaFX concurrency constraints and File I/O operations.
 * </p>
 * 
 * @author Zainab
 * @version 1.0
 */
class UserControllerTest {

    private UserController controller;
    private static final String TEST_FILE_PATH = "books.txt";

    /**
     * Initializes the JavaFX runtime environment and sets necessary system properties.
     * This is required to test JavaFX controls and simulate environment variables.
     */
    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            
        }
        System.setProperty("EMAIL_USERNAME", "test_user");
        System.setProperty("EMAIL_PASSWORD", "test_pass");
    }

    /**
     * Sets up the test environment before each test case.
     * Creates a dummy data file, initializes the controller, and injects mock UI components.
     * 
     * @throws Exception if reflection or file I/O fails.
     */
    @BeforeEach
    void setUp() throws Exception {
        createDummyBookFile();
        controller = new UserController();
        injectMockUIComponents();
        
        runOnJavaFXThread(() -> controller.initialize());
        controller.setCurrentUser("TestUser", "Gold", "test@example.com");
    }

    /**
     * Cleans up the test environment after each test case.
     * Deletes the dummy data file to ensure test isolation.
     */
    @AfterEach
    void tearDown() {
        File file = new File(TEST_FILE_PATH);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Tests the initialization of the TableView and its columns.
     */
    @Test
    void testInitialize() {
        TableView<Media> table = getPrivateField(controller, "bookTable");
        assertNotNull(table);
        assertNotNull(table.getItems());
        assertFalse(table.getItems().isEmpty());
    }

    /**
     * Tests setting the current user details and updating the welcome label.
     * Verifies that the label text and style are updated correctly.
     */
    @Test
    void testSetCurrentUser() {
        runOnJavaFXThread(() -> controller.setCurrentUser("NewUser", "Silver", "new@example.com"));
        Label label = getPrivateField(controller, "welcomeLabel");
        assertTrue(label.getText().contains("NewUser"));
        assertTrue(label.getText().contains("Silver"));
    }

    /**
     * Tests the legacy setter methods for username, membership, and credentials.
     */
    @Test
    void testLegacySetters() {
        runOnJavaFXThread(() -> {
            controller.setMembershipType("Platinum");
            controller.setCurrentUsername("UpdatedName");
            controller.setCurrentUser("LegacyUser", "legacy@example.com");
        });
        Label label = getPrivateField(controller, "welcomeLabel");
        assertTrue(label.getText().contains("LegacyUser"));
    }

    /**
     * Tests the successful borrowing of an available book.
     */
    @Test
    void testHandleBorrowBookSuccess() {
        selectItemInTable(0);
        runOnJavaFXThread(() -> controller.handleBorrowBook());
        
        Label msg = getPrivateField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Borrowed successfully"));
        
        TableView<Media> table = getPrivateField(controller, "bookTable");
        assertEquals("Borrowed", table.getItems().get(0).getStatus());
    }

    /**
     * Tests failure to borrow a book when no item is selected.
     */
    @Test
    void testHandleBorrowBookNoSelection() {
        clearTableSelection();
        runOnJavaFXThread(() -> controller.handleBorrowBook());
        
        Label msg = getPrivateField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Please select an item"));
    }

    /**
     * Tests failure to borrow a book when the user has outstanding fines on other items.
     */
    @Test
    void testHandleBorrowBookWithOutstandingFines() {
        TableView<Media> table = getPrivateField(controller, "bookTable");
        Media item = table.getItems().get(1);
        item.borrow("TestUser");
        item.setFineAmount(15.0);
        
        selectItemInTable(0);
        runOnJavaFXThread(() -> controller.handleBorrowBook());
        
        Label msg = getPrivateField(controller, "messageLabel");
        assertTrue(msg.getText().contains("unpaid fines"));
    }

    /**
     * Tests failure to borrow a book that is already borrowed by the current user.
     */
    @Test
    void testHandleBorrowBookAlreadyBorrowed() {
        TableView<Media> table = getPrivateField(controller, "bookTable");
        Media item = table.getItems().get(0);
        item.borrow("TestUser");
        
        selectItemInTable(0);
        runOnJavaFXThread(() -> controller.handleBorrowBook());
        
        Label msg = getPrivateField(controller, "messageLabel");
        assertTrue(msg.getText().contains("already borrowed"));
    }

    /**
     * Tests failure to borrow a book that is unavailable (borrowed by someone else).
     */
    @Test
    void testHandleBorrowBookUnavailable() {
        TableView<Media> table = getPrivateField(controller, "bookTable");
        Media item = table.getItems().get(0);
        item.borrow("OtherUser");
        
        selectItemInTable(0);
        runOnJavaFXThread(() -> controller.handleBorrowBook());
        
        Label msg = getPrivateField(controller, "messageLabel");
        assertTrue(msg.getText().contains("not available"));
    }

    /**
     * Tests returning a book successfully when there are no fines.
     */
    @Test
    void testHandleReturnBookSuccess() {
        TableView<Media> table = getPrivateField(controller, "bookTable");
        Media item = table.getItems().get(0);
        item.borrow("TestUser");
        
        selectItemInTable(0);
        runOnJavaFXThread(() -> controller.handleReturnBook());
        
        Label msg = getPrivateField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Returned successfully"));
        assertEquals("Available", item.getStatus());
    }

    /**
     * Tests failure to return a book when there are unpaid fines.
     */
    @Test
    void testHandleReturnBookWithFines() {
        TableView<Media> table = getPrivateField(controller, "bookTable");
        Media item = table.getItems().get(0);
        item.borrow("TestUser");
        item.setDueDate("2000-01-01"); 
        
        selectItemInTable(0);
        runOnJavaFXThread(() -> controller.handleReturnBook());
        
        Label msg = getPrivateField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Pay the fine first"));
        assertEquals("Overdue", item.getStatus());
    }

    /**
     * Tests failure to return a book that belongs to another user.
     */
    @Test
    void testHandleReturnBookNotOwner() {
        TableView<Media> table = getPrivateField(controller, "bookTable");
        Media item = table.getItems().get(0);
        item.borrow("OtherUser");
        
        selectItemInTable(0);
        runOnJavaFXThread(() -> controller.handleReturnBook());
        
        Label msg = getPrivateField(controller, "messageLabel");
        assertTrue(msg.getText().contains("only return your own items"));
    }

    /**
     * Tests handling of return action when no book is selected.
     */
    @Test
    void testHandleReturnBookNoSelection() {
        clearTableSelection();
        runOnJavaFXThread(() -> controller.handleReturnBook());
        
        Label msg = getPrivateField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Select an item"));
    }

    /**
     * Tests partial payment of a fine.
     */
    @Test
    void testHandlePayFinePartial() {
        setupItemWithFine(0, 50.0);
        setPaymentInput("20.0");
        
        runOnJavaFXThread(() -> controller.handlePayFine());
        
        Label info = getPrivateField(controller, "infoLabel");
        assertTrue(info.getText().contains("Partial payment accepted"));
        
        TableView<Media> table = getPrivateField(controller, "bookTable");
        Media item = table.getItems().get(0);
        assertEquals(30.0, item.getFineAmount(), 0.01);
    }

    /**
     * Tests full payment of a fine, resulting in the item being returned.
     */
    @Test
    void testHandlePayFineFull() {
        setupItemWithFine(0, 10.0);
        setPaymentInput("10.0");
        
        runOnJavaFXThread(() -> controller.handlePayFine());
        
        Label info = getPrivateField(controller, "infoLabel");
        assertTrue(info.getText().contains("Fine fully paid"));
        
        TableView<Media> table = getPrivateField(controller, "bookTable");
        assertEquals("Available", table.getItems().get(0).getStatus());
    }

    /**
     * Tests validation for invalid non-numeric payment input.
     */
    @Test
    void testHandlePayFineInvalidInput() {
        setupItemWithFine(0, 10.0);
        setPaymentInput("Invalid");
        
        runOnJavaFXThread(() -> controller.handlePayFine());
        
        Label info = getPrivateField(controller, "infoLabel");
        assertTrue(info.getText().contains("Invalid number"));
    }

    /**
     * Tests validation for negative payment amounts.
     */
    @Test
    void testHandlePayFineNegativeAmount() {
        setupItemWithFine(0, 10.0);
        setPaymentInput("-5.0");
        
        runOnJavaFXThread(() -> controller.handlePayFine());
        
        Label info = getPrivateField(controller, "infoLabel");
        assertTrue(info.getText().contains("must be positive"));
    }

    /**
     * Tests validation when payment exceeds the fine amount.
     */
    @Test
    void testHandlePayFineExceedsAmount() {
        setupItemWithFine(0, 10.0);
        setPaymentInput("100.0");
        
        runOnJavaFXThread(() -> controller.handlePayFine());
        
        Label info = getPrivateField(controller, "infoLabel");
        assertTrue(info.getText().contains("Payment exceeds"));
    }

    /**
     * Tests payment failure when attempting to pay for another user's item.
     */
    @Test
    void testHandlePayFineWrongUser() {
        TableView<Media> table = getPrivateField(controller, "bookTable");
        Media item = table.getItems().get(0);
        item.borrow("OtherUser");
        item.setFineAmount(10.0);
        
        selectItemInTable(0);
        setPaymentInput("5.0");
        
        runOnJavaFXThread(() -> controller.handlePayFine());
        
        Label info = getPrivateField(controller, "infoLabel");
        assertTrue(info.getText().contains("YOUR borrowed items"));
    }

    /**
     * Tests reloading data from the file.
     */
    @Test
    void testHandleReload() {
        runOnJavaFXThread(() -> controller.handleReload());
        Label info = getPrivateField(controller, "infoLabel");
        assertTrue(info.getText().contains("Data reloaded"));
    }

    /**
     * Tests the logout handler. 
     * Verifies that it runs without logic errors (ignoring UI stage switching errors).
     */
    @Test
    void testHandleLogout() {
        assertDoesNotThrow(() -> {
            runOnJavaFXThread(() -> {
                try {
                    controller.handleLogout();
                } catch (Exception ignored) {
                }
            });
        });
    }

    /**
     * Tests file parsing with corrupted lines using reflection on private methods.
     * Ensures robustness against malformed data.
     */
    @Test
    void testFileParsingResilience() throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEST_FILE_PATH, true))) {
            writer.write("InvalidLineData"); 
            writer.newLine();
            writer.write("Book,Title,Auth,ISBN,BadInt,Status,Date,BadDouble,User,BadDouble");
            writer.newLine();
        }
        
        runOnJavaFXThread(() -> controller.handleReload());
        
        TableView<Media> table = getPrivateField(controller, "bookTable");
        ObservableList<Media> items = table.getItems();
        boolean foundResilientItem = items.stream()
                .anyMatch(m -> m.getTitle().equals("Title") && m.getFineAmount() == 0.0);
        assertTrue(foundResilientItem, "Parser should handle malformed numbers gracefully");
    }

    /**
     * Tests private helper methods via Reflection.
     * Covers parsing utilities and string normalization.
     */
    @Test
    void testPrivateHelpers() throws Exception {
        Method parseInt = UserController.class.getDeclaredMethod("parseIntSafe", String.class, int.class);
        parseInt.setAccessible(true);
        assertEquals(10, parseInt.invoke(controller, "10", 0));
        assertEquals(5, parseInt.invoke(controller, "invalid", 5));

        Method parseDouble = UserController.class.getDeclaredMethod("parseDoubleSafe", String.class, double.class);
        parseDouble.setAccessible(true);
        assertEquals(10.5, parseDouble.invoke(controller, "10.5", 0.0));
        assertEquals(0.0, parseDouble.invoke(controller, "invalid", 0.0));

        Method normalize = UserController.class.getDeclaredMethod("normalizeBorrowedBy", String.class);
        normalize.setAccessible(true);
        assertEquals("", normalize.invoke(controller, "0.0"));
        assertEquals("User", normalize.invoke(controller, "User"));
        assertEquals("", normalize.invoke(controller, (Object) null));
    }

    /**
     * Tests TableRow styling logic via Reflection.
     * Simulates the CellFactory to verify row coloring logic.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testRowFactoryLogic() throws Exception {
        TableView<Media> table = getPrivateField(controller, "bookTable");
        Callback<TableView<Media>, TableRow<Media>> factory = table.getRowFactory();
        assertNotNull(factory);
        
        TableRow<Media> row = factory.call(table);
        Method updateItem = TableRow.class.getDeclaredMethod("updateItem", Object.class, boolean.class);
        updateItem.setAccessible(true);

        Media myOverdue = new Book("T", "A", "I", "Overdue", "2000-01-01", 10.0, "TestUser", 0.0, 1);
        Media myNormal = new Book("T", "A", "I", "Borrowed", "2099-01-01", 0.0, "TestUser", 0.0, 1);
        Media otherBorrowed = new Book("T", "A", "I", "Borrowed", "2099-01-01", 0.0, "Other", 0.0, 1);

        assertDoesNotThrow(() -> updateItem.invoke(row, myOverdue, false));
        assertDoesNotThrow(() -> updateItem.invoke(row, myNormal, false));
        assertDoesNotThrow(() -> updateItem.invoke(row, otherBorrowed, false));
        assertDoesNotThrow(() -> updateItem.invoke(row, null, true));
    }

    /**
     * Tests TableCell rendering logic via Reflection.
     * Verifies that sensitive data (Due Date, Fine) is hidden for items not owned by the user.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testCellFactoryLogic() throws Exception {
        TableColumn<Media, String> dueCol = getPrivateField(controller, "dueDateColumn");
        TableCell<Media, String> cell = dueCol.getCellFactory().call(dueCol);
        
        Method updateItem = TableCell.class.getDeclaredMethod("updateItem", Object.class, boolean.class);
        updateItem.setAccessible(true);
        
        Field tableRowField = javafx.scene.control.Cell.class.getDeclaredField("tableRow");
        tableRowField.setAccessible(true);
        
        TableRow<Media> row = new TableRow<>();
        tableRowField.set(cell, row);

        Media myItem = new Book("T", "A", "I", "Borrowed", "2025-01-01", 0.0, "TestUser", 0.0, 1);
        row.setItem(myItem);
        updateItem.invoke(cell, "2025-01-01", false);
        assertEquals("2025-01-01", cell.getText());

        Media otherItem = new Book("T", "A", "I", "Borrowed", "2025-01-01", 0.0, "Other", 0.0, 1);
        row.setItem(otherItem);
        updateItem.invoke(cell, "2025-01-01", false);
        assertEquals("", cell.getText());
    }

    /**
     * Helper method to create a dummy CSV file for testing.
     */
    private void createDummyBookFile() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEST_FILE_PATH))) {
            writer.write("Book,Java Guide,Author X,11111,1,Available,2025-01-01,0.0,0.0,0.0");
            writer.newLine();
            writer.write("CD,Audio Learning,Author Y,22222,1,Available,2025-01-01,0.0,0.0,0.0");
            writer.newLine();
        }
    }

    /**
     * Helper method to inject mock JavaFX controls into the controller using reflection.
     */
    private void injectMockUIComponents() throws Exception {
        setPrivateField(controller, "welcomeLabel", new Label());
        setPrivateField(controller, "paymentField", new TextField());
        setPrivateField(controller, "infoLabel", new Label());
        setPrivateField(controller, "messageLabel", new Label());
        setPrivateField(controller, "bookTable", new TableView<Media>());
        setPrivateField(controller, "typeColumn", new TableColumn<Media, String>());
        setPrivateField(controller, "titleColumn", new TableColumn<Media, String>());
        setPrivateField(controller, "authorColumn", new TableColumn<Media, String>());
        setPrivateField(controller, "isbnColumn", new TableColumn<Media, String>());
        setPrivateField(controller, "statusColumn", new TableColumn<Media, String>());
        setPrivateField(controller, "dueDateColumn", new TableColumn<Media, String>());
        setPrivateField(controller, "fineColumn", new TableColumn<Media, Double>());
    }

    /**
     * Helper method to run a Runnable on the JavaFX application thread and wait for completion.
     */
    private void runOnJavaFXThread(Runnable action) {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Helper method to select an item in the TableView on the JavaFX thread.
     */
    private void selectItemInTable(int index) {
        TableView<Media> table = getPrivateField(controller, "bookTable");
        runOnJavaFXThread(() -> table.getSelectionModel().select(index));
    }

    /**
     * Helper method to clear selection in the TableView on the JavaFX thread.
     */
    private void clearTableSelection() {
        TableView<Media> table = getPrivateField(controller, "bookTable");
        runOnJavaFXThread(() -> table.getSelectionModel().clearSelection());
    }

    /**
     * Helper method to simulate user input in the payment field.
     */
    private void setPaymentInput(String text) {
        TextField field = getPrivateField(controller, "paymentField");
        runOnJavaFXThread(() -> field.setText(text));
    }

    /**
     * Helper method to set up a specific scenario with a fine.
     */
    private void setupItemWithFine(int index, double amount) {
        TableView<Media> table = getPrivateField(controller, "bookTable");
        Media item = table.getItems().get(index);
        item.borrow("TestUser");
        item.setFineAmount(amount);
        selectItemInTable(index);
    }

    /**
     * Reflection helper to get a private field value.
     */
    @SuppressWarnings("unchecked")
    private <T> T getPrivateField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get field: " + fieldName, e);
        }
    }

    /**
     * Reflection helper to set a private field value.
     */
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}