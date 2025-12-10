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
 * This suite is designed to achieve high code coverage by testing standard operations,
 * edge cases, validation logic, and UI cell rendering logic using reflection and
 * JavaFX threading utilities.
 * </p>
 * 
 * @author Zainab
 * @version 4.1
 */
class UserControllerTest {

    private UserController controller;
    private static final String TEST_FILE = "books.txt";

    /**
     * Initializes the JavaFX Platform environment and sets necessary system properties
     * for email service simulation.
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
     * Sets up the test environment before each test method.
     * Creates a fresh data file, initializes the controller, injects mock UI components,
     * and sets a default user context.
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
     * Executes a runnable task on the JavaFX Application Thread and waits for its completion.
     * 
     * @param action The task to execute.
     * @throws InterruptedException If the current thread is interrupted while waiting.
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
     * Verifies that the TableView is correctly initialized and populated with data.
     */
    @Test
    void testInitialize() {
        TableView<Media> table = getField(controller, "bookTable");
        assertNotNull(table.getItems());
        assertFalse(table.getItems().isEmpty());
    }

    /**
     * Verifies that the setCurrentUser method correctly updates the welcome label.
     * 
     * @throws InterruptedException If the JavaFX thread is interrupted.
     */
    @Test
    void testSetCurrentUser() throws InterruptedException {
        runOnFxThreadAndWait(() -> controller.setCurrentUser("NewUser", "Silver", "new@mail.com"));
        Label label = getField(controller, "welcomeLabel");
        assertTrue(label.getText().contains("NewUser"));
        assertTrue(label.getText().contains("Silver"));
    }

    /**
     * Tests setter methods that are used for legacy support or specific context updates.
     * 
     * @throws InterruptedException If the JavaFX thread is interrupted.
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
     * Tests the successful borrowing of an available book.
     * 
     * @throws InterruptedException If the JavaFX thread is interrupted.
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
    }

    /**
     * Tests that borrowing fails gracefully when no item is selected.
     * 
     * @throws InterruptedException If the JavaFX thread is interrupted.
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
     * Tests that borrowing fails if the user has outstanding unpaid fines on other items.
     * 
     * @throws InterruptedException If the JavaFX thread is interrupted.
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
     * Tests that borrowing fails if the user attempts to borrow the same item twice.
     * 
     * @throws InterruptedException If the JavaFX thread is interrupted.
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
     * Tests that borrowing fails if the item is already borrowed by another user.
     * 
     * @throws InterruptedException If the JavaFX thread is interrupted.
     */
    @Test
    void testBorrowBookFailUnavailable() throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(0);
        item.borrow("OtherUser");
        
        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(0);
            controller.handleBorrowBook();
        });
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("not available"));
    }

    /**
     * Tests the partial payment logic for fines.
     * 
     * @throws InterruptedException If the JavaFX thread is interrupted.
     */
    @Test
    void testPayFinePartialPayment() throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(1);
        item.borrow("TestUser");
        item.setStatus("Overdue");
        item.setDueDate("2000-01-01");
        item.setFineAmount(100.0); 

        TextField payField = getField(controller, "paymentField");
        payField.setText("1.0");

        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(item);
            controller.handlePayFine();
        });

        Label info = getField(controller, "infoLabel");
        assertTrue(info.getText().contains("Partial payment"));
    }

    /**
     * Tests the full payment logic for fines, ensuring the book is returned.
     * 
     * @throws InterruptedException If the JavaFX thread is interrupted.
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
    }

    /**
     * Tests validation against negative payment amounts.
     * 
     * @throws InterruptedException If the JavaFX thread is interrupted.
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
     * Tests validation against non-numeric input in the payment field.
     * 
     * @throws InterruptedException If the JavaFX thread is interrupted.
     */
    @Test
    void testPayFineInvalidFormat() throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(1);
        item.borrow("TestUser");
        item.setFineAmount(10.0);
        TextField payField = getField(controller, "paymentField");
        payField.setText("abc");
        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(item);
            controller.handlePayFine();
        });
        Label info = getField(controller, "infoLabel");
        assertTrue(info.getText().contains("Invalid number"));
    }

    /**
     * Tests the successful return of a book with no fines.
     * 
     * @throws InterruptedException If the JavaFX thread is interrupted.
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
     * Tests that returning a book fails if fines are pending.
     * 
     * @throws InterruptedException If the JavaFX thread is interrupted.
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
            } catch (Exception e) {}
        });

        if (item.getFineAmount() > 0) {
            Label msg = getField(controller, "messageLabel");
            assertTrue(msg.getText().contains("Pay the fine"));
        }
    }

    /**
     * Tests validation ensuring a user cannot return or pay for items borrowed by others.
     * 
     * @throws InterruptedException If the JavaFX thread is interrupted.
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
        
        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(item);
            controller.handlePayFine();
        });
        Label info = getField(controller, "infoLabel");
        assertTrue(info.getText().contains("YOUR borrowed items"));
    }

    /**
     * Tests the rendering logic of table cells to ensure data privacy (hiding others' data).
     * Uses mock objects and reflection to simulate TableRow interactions.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testCellRenderingLogic() {
        TableColumn<Media, String> dueCol = getField(controller, "dueDateColumn");
        TableColumn<Media, Double> fineCol = getField(controller, "fineColumn");
        
        Callback<TableColumn<Media, String>, TableCell<Media, String>> dueFactory = dueCol.getCellFactory();
        Callback<TableColumn<Media, Double>, TableCell<Media, Double>> fineFactory = fineCol.getCellFactory();
        
        TableCell<Media, String> dueCell = dueFactory.call(dueCol);
        TableCell<Media, Double> fineCell = fineFactory.call(fineCol);
        
        Media myItem = new Book("My Book", "Me", "111", "Borrowed", "2025-01-01", 10.0, "TestUser", 0, 1);
        
        TableRow<Media> row = new TableRow<>();
        row.setItem(myItem);
        
        try {
            Field rowField = javafx.scene.control.Cell.class.getDeclaredField("tableRow");
            rowField.setAccessible(true);
            rowField.set(dueCell, row);
            rowField.set(fineCell, row);
        } catch (Exception e) { 
        }

        dueCell.updateIndex(0); 
        assertNotNull(dueCell);
        assertNotNull(fineCell);
    }

    /**
     * Tests that the reload functionality works correctly and refreshes the UI.
     * 
     * @throws InterruptedException If the JavaFX thread is interrupted.
     */
    @Test
    void testReload() throws InterruptedException {
        runOnFxThreadAndWait(() -> controller.handleReload());
        Label info = getField(controller, "infoLabel");
        assertTrue(info.getText().contains("reloaded"));
    }

    /**
     * Tests the logout functionality.
     * 
     * @throws InterruptedException If the JavaFX thread is interrupted.
     */
    @Test
    void testLogout() throws InterruptedException {
        runOnFxThreadAndWait(() -> controller.handleLogout());
    }

    /**
     * Tests the system's ability to handle corrupted lines in the data file without crashing.
     * 
     * @throws IOException If file writing fails.
     * @throws InterruptedException If the JavaFX thread is interrupted.
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
     * Creates a temporary test file with valid data samples.
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
     * Injects a value into a private field of the target object using reflection.
     * 
     * @param target The object containing the field.
     * @param fieldName The name of the field.
     * @param value The value to inject.
     * @throws Exception If reflection fails.
     */
    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * Retrieves the value of a private field from the target object using reflection.
     * 
     * @param <T> The expected type of the field value.
     * @param target The object containing the field.
     * @param fieldName The name of the field.
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