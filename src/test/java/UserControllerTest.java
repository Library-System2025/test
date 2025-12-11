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
 * Comprehensive JUnit Test suite for the {@link UserController} class.
 * <p>
 * This suite employs Reflection to inject mock UI components and invokes
 * methods on the JavaFX thread. It targets 100% code coverage by testing
 * public API, private helper methods, UI cell factories, and error handling.
 * </p>
 * 
 * @author Zainab
 * @version 1.0
 */
class UserControllerTest {

    private UserController controller;
    private static final String TEST_FILE = "books.txt";

    /**
     * Initializes the JavaFX Toolkit once before all tests.
     * Prevents "Toolkit not initialized" errors.
     */
    @BeforeAll
    static void initJfxAndEnv() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            
        }
        
        System.setProperty("EMAIL_USERNAME", "mock_user");
        System.setProperty("EMAIL_PASSWORD", "mock_pass");
    }

    /**
     * Sets up the test environment before each test execution.
     * Creates a dummy data file, initializes the controller, and injects mocks.
     * 
     * @throws Exception If reflection or file I/O fails.
     */
    @BeforeEach
    void setUp() throws Exception {
        createDummyFile();
        controller = new UserController();
        injectMockUI();
        
        runOnFxThreadAndWait(() -> controller.initialize());
        controller.setCurrentUser("TestUser", "Gold", "test@mail.com");
    }

    /**
     * Cleans up resources after each test.
     * Deletes the temporary test file.
     */
    @AfterEach
    void tearDown() {
        File file = new File(TEST_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Helper method to create a temporary CSV file with test data.
     * Includes valid books, CDs, and edge case entries.
     * 
     * @throws IOException If writing to the file fails.
     */
    private void createDummyFile() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEST_FILE))) {
            
            writer.write("Book,Clean Code,Robert Martin,111,1,Available,2025-01-01,0.0,,0.0");
            writer.newLine();
            
            writer.write("CD,Greatest Hits,Queen,222,2,Overdue,2020-01-01,10.0,TestUser,0.0");
            writer.newLine();
            
            writer.write("Book,Refactoring,Fowler,333,3,Borrowed,2025-05-05,0.0,OtherGuy,0.0");
            writer.newLine();
        }
    }

    /**
     * Uses Reflection to inject JavaFX controls into the controller.
     * Bypasses the need for an FXML loader.
     * 
     * @throws Exception If field access fails.
     */
    private void injectMockUI() throws Exception {
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
    }

    /**
     * Executes a Runnable on the JavaFX Application Thread and waits for it to finish.
     * Essential for testing UI-related logic safely.
     * 
     * @param action The code to execute.
     */
    private void runOnFxThreadAndWait(Runnable action) {
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
     * Tests that the controller initializes correctly and loads data into the table.
     */
    @Test
    void testInitializeLoadsData() {
        TableView<Media> table = getField(controller, "bookTable");
        assertNotNull(table.getItems());
        assertEquals(3, table.getItems().size(), "Table should contain 3 items from file");
    }

    /**
     * Tests that the welcome label updates correctly when user details change.
     */
    @Test
    void testWelcomeLabelUpdates() {
        runOnFxThreadAndWait(() -> {
            controller.setMembershipType("Silver");
            controller.setCurrentUsername("NewUser");
        });
        Label label = getField(controller, "welcomeLabel");
        assertTrue(label.getText().contains("NewUser"));
        assertTrue(label.getText().contains("Silver"));
    }

    /**
     * Tests the successful borrowing of an available book.
     */
    @Test
    void testBorrowBookSuccess() {
        TableView<Media> table = getField(controller, "bookTable");
        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(0); // Select Available Book
            controller.handleBorrowBook();
        });
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Borrowed successfully"));
    }

    /**
     * Tests that borrowing fails if the user has unpaid fines on other items.
     */
    @Test
    void testBorrowFailWithFines() {
        TableView<Media> table = getField(controller, "bookTable");
        
        Media overdueItem = table.getItems().get(1);
        overdueItem.setFineAmount(50.0); 

        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(0); 
            controller.handleBorrowBook();
        });
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("unpaid fines"));
    }

    /**
     * Tests that borrowing fails if the item is already borrowed by the same user.
     */
    @Test
    void testBorrowFailAlreadyBorrowed() {
        TableView<Media> table = getField(controller, "bookTable");
        
        table.getItems().get(0).borrow("TestUser");

        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(0);
            controller.handleBorrowBook();
        });
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("already borrowed"));
    }

    /**
     * Tests that borrowing fails if the item is unavailable (borrowed by someone else).
     */
    @Test
    void testBorrowFailUnavailable() {
        TableView<Media> table = getField(controller, "bookTable");
        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(2); // Borrowed by 'OtherGuy'
            controller.handleBorrowBook();
        });
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("not available"));
    }

    /**
     * Tests that borrowing fails gracefully when no item is selected.
     */
    @Test
    void testBorrowNoSelection() {
        TableView<Media> table = getField(controller, "bookTable");
        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().clearSelection();
            controller.handleBorrowBook();
        });
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("select an item"));
    }

    /**
     * Tests paying a fine successfully (full payment).
     */
    @Test
    void testPayFineFullSuccess() {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(1); // Overdue item
        item.calculateFine("Gold");
        double fine = item.getFineAmount();
        
        TextField payField = getField(controller, "paymentField");
        payField.setText(String.valueOf(fine));

        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(1);
            controller.handlePayFine();
        });
        
        Label info = getField(controller, "infoLabel");
        assertTrue(info.getText().contains("fully paid") || info.getText().contains("returned"));
        assertEquals(0, item.getFineAmount(), 0.01);
    }

    /**
     * Tests paying a fine partially.
     */
    @Test
    void testPayFinePartial() {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(1);
        item.calculateFine("Gold");
        
        TextField payField = getField(controller, "paymentField");
        payField.setText("1.0"); // Pay only 1.0

        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(1);
            controller.handlePayFine();
        });
        
        Label info = getField(controller, "infoLabel");
        assertTrue(info.getText().contains("Partial payment"));
        assertTrue(item.getFineAmount() > 0);
    }

    /**
     * Tests validation logic for fine payments (negative numbers, invalid text).
     */
    @Test
    void testPayFineValidation() {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(1);
        TextField payField = getField(controller, "paymentField");

        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(item);
            
            payField.setText("-5");
            controller.handlePayFine();
            assertTrue(getField(controller, "infoLabel").toString().contains("positive"));

            payField.setText("NotANumber");
            controller.handlePayFine();
            assertTrue(getField(controller, "infoLabel").toString().contains("Invalid"));
            
            payField.setText("1000000"); 
            controller.handlePayFine();
            assertTrue(getField(controller, "infoLabel").toString().contains("exceeds"));
        });
    }

    /**
     * Tests trying to pay a fine for a book borrowed by another user.
     */
    @Test
    void testPayFineWrongUser() {
        TableView<Media> table = getField(controller, "bookTable");
        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(2); 
            controller.handlePayFine();
        });
        Label info = getField(controller, "infoLabel");
        assertTrue(info.getText().contains("YOUR borrowed items"));
    }

    /**
     * Tests returning a book successfully.
     */
    @Test
    void testReturnBookSuccess() {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(0);
        item.borrow("TestUser"); // Borrow it first
        
        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(0);
            controller.handleReturnBook();
        });
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Returned successfully"));
    }

    /**
     * Tests that returning a book fails if there are unpaid fines.
     */
    @Test
    void testReturnBookFailWithFine() {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(1); // Already overdue
        
        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(1);
            controller.handleReturnBook();
        });
        
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Pay the fine"));
    }

    /**
     * Tests reload functionality.
     */
    @Test
    void testReload() {
        runOnFxThreadAndWait(() -> controller.handleReload());
        Label info = getField(controller, "infoLabel");
        assertTrue(info.getText().contains("reloaded"));
    }

    /**
     * Tests file parsing robustness with corrupted lines.
     * Ensures the application doesn't crash.
     * 
     * @throws IOException If write fails.
     */
    @Test
    void testFileParsingEdgeCases() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEST_FILE))) {
            writer.write("Book,BadLine,NoIsbn"); 
            writer.newLine();
            writer.write("CD,BadNumbers,Auth,123,Nan,Avail,Date,Nan,User,Nan"); 
            writer.newLine();
        }
        runOnFxThreadAndWait(() -> controller.handleReload());
        // Should not throw exception
        TableView<Media> table = getField(controller, "bookTable");
        assertFalse(table.getItems().isEmpty()); 
    }

    /**
     * Tests the private helper method `parseIntSafe` via Reflection.
     * Needed for 100% coverage of utility methods.
     * 
     * @throws Exception If reflection fails.
     */
    @Test
    void testPrivateParseIntSafe() throws Exception {
        Method method = UserController.class.getDeclaredMethod("parseIntSafe", String.class, int.class);
        method.setAccessible(true);
        
        int val1 = (int) method.invoke(controller, " 10 ", 0);
        assertEquals(10, val1);
        
        int val2 = (int) method.invoke(controller, "invalid", 5);
        assertEquals(5, val2);
    }

    /**
     * Tests the private helper method `normalizeBorrowedBy` via Reflection.
     * 
     * @throws Exception If reflection fails.
     */
    @Test
    void testPrivateNormalizeBorrowedBy() throws Exception {
        Method method = UserController.class.getDeclaredMethod("normalizeBorrowedBy", String.class);
        method.setAccessible(true);
        
        assertEquals("", method.invoke(controller, "0.0"));
        assertEquals("", method.invoke(controller, (Object)null));
        assertEquals("User", method.invoke(controller, " User "));
    }

    /**
     * Tests the CellFactory logic for hiding Due Date and Fine Amount for other users.
     * Simulates the TableCell updateItem method.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testCellFactoryPrivacy() {
        TableColumn<Media, String> dateCol = getField(controller, "dueDateColumn");
        Callback<?, TableCell<Media, String>> factory = dateCol.getCellFactory();
        TableCell<Media, String> cell = factory.call(null);

        Media myItem = new Book("T", "A", "1", "Borrowed", "2025-01-01", 10.0, "TestUser", 0, 1);
        Media otherItem = new Book("T", "A", "2", "Borrowed", "2025-01-01", 10.0, "Stranger", 0, 2);

        
        TableRow<Media> row = new TableRow<>();
        try {
            Field rowField = javafx.scene.control.Cell.class.getDeclaredField("tableRow");
            rowField.setAccessible(true);
            rowField.set(cell, row);
            
            
            row.setItem(myItem);
            Method update = cell.getClass().getDeclaredMethod("updateItem", Object.class, boolean.class);
            update.setAccessible(true);
            update.invoke(cell, myItem.getDueDate(), false);
            assertEquals("2025-01-01", cell.getText());

            
            row.setItem(otherItem);
            update.invoke(cell, otherItem.getDueDate(), false);
            assertEquals("", cell.getText());

        } catch (Exception e) {
            fail("Reflection failed for Cell testing: " + e.getMessage());
        }
    }

    /**
     * Tests the RowFactory logic for coloring rows based on status.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testRowFactoryStyling() {
        TableView<Media> table = getField(controller, "bookTable");
        Callback<TableView<Media>, TableRow<Media>> factory = table.getRowFactory();
        TableRow<Media> row = factory.call(table);

        Media overdueMine = new Book("T", "A", "1", "Overdue", "D", 10, "TestUser", 0, 1);
        Media borrowedMine = new Book("T", "A", "2", "Borrowed", "D", 0, "TestUser", 0, 1);
        Media overdueOther = new Book("T", "A", "3", "Overdue", "D", 10, "Other", 0, 1);

        try {
            Method update = TableRow.class.getDeclaredMethod("updateItem", Object.class, boolean.class);
            update.setAccessible(true);

            
            update.invoke(row, overdueMine, false);
            assertTrue(row.getStyle().contains("ffcccc")); 

            
            update.invoke(row, borrowedMine, false);
            assertTrue(row.getStyle().contains("c8f7c5")); 

            
            update.invoke(row, overdueOther, false);
            assertTrue(row.getStyle().contains("fff3cd")); 
            
        } catch (Exception e) {
            fail("Reflection failed for Row testing: " + e.getMessage());
        }
    }

    

    /**
     * Injects a value into a private field of an object.
     */
    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * Retrieves the value of a private field from an object.
     */
    @SuppressWarnings("unchecked")
    private <T> T getField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (Exception e) {
            throw new RuntimeException("Could not get field " + fieldName, e);
        }
    }
}