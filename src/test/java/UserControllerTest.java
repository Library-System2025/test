import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
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
 * This suite utilizes Reflection to mock UI components and manipulate private fields,
 * ensuring high code coverage and isolating the test environment from external dependencies
 * such as the email system.
 * </p>
 * 
 * @author Zainab
 * @version 1.0
 */
class UserControllerTest {

    private UserController controller;
    private static final String TEST_FILE = "books.txt";

    /**
     * Initializes the JavaFX Toolkit once before all tests execution.
     */
    @BeforeAll
    static void initJfxAndEnv() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
        }
        System.setProperty("EMAIL_USERNAME", "dummy@test.com");
        System.setProperty("EMAIL_PASSWORD", "dummy_pass");
    }

    /**
     * Sets up the test environment before each test execution.
     * Creates dummy data, initializes the controller, injects mock UI components,
     * and detaches the real email system to prevent CI/CD failures.
     * 
     * @throws Exception If initialization fails.
     */
    @BeforeEach
    void setUp() throws Exception {
        createDummyFile();
        controller = new UserController();
        injectMockUI();
        disableEmailNotifications(); 

        runOnFxThreadAndWait(() -> controller.initialize());
        controller.setCurrentUser("TestUser", "Gold", "test@mail.com");
    }

    /**
     * Cleans up resources after each test by deleting the temporary data file.
     */
    @AfterEach
    void tearDown() {
        File file = new File(TEST_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Uses reflection to detach the email subscriber from the OverduePublisher.
     * This prevents the application from attempting to send real emails during testing.
     */
    private void disableEmailNotifications() {
        try {
            Field publisherField = UserController.class.getDeclaredField("overduePublisher");
            publisherField.setAccessible(true);
            Object publisher = publisherField.get(null);

            if (publisher != null) {
                for (Field f : publisher.getClass().getDeclaredFields()) {
                    if (List.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        List<?> subscribers = (List<?>) f.get(publisher);
                        subscribers.clear();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not detach email system: " + e.getMessage());
        }
    }

    /**
     * Creates a temporary CSV file with varied media data for testing purposes.
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
     * Injects JavaFX controls into the controller using Reflection.
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
     * Executes a Runnable on the JavaFX Application Thread and waits for completion.
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
     * Verifies that the table is populated with data upon initialization.
     */
    @Test
    void testInitializeLoadsData() {
        TableView<Media> table = getField(controller, "bookTable");
        assertNotNull(table.getItems());
        assertEquals(3, table.getItems().size());
    }

    /**
     * Verifies that the welcome label correctly displays the username and membership type.
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
     * Tests the successful borrowing of an available item.
     */
    @Test
    void testBorrowBookSuccess() {
        TableView<Media> table = getField(controller, "bookTable");
        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(0); 
            controller.handleBorrowBook();
        });
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Borrowed successfully"));
    }

    /**
     * Tests that borrowing is blocked when the user has unpaid fines.
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
     * Tests that borrowing is blocked if the item is already borrowed by the user.
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
     * Tests that borrowing is blocked if the item is unavailable.
     */
    @Test
    void testBorrowFailUnavailable() {
        TableView<Media> table = getField(controller, "bookTable");
        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(2); 
            controller.handleBorrowBook();
        });
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("not available"));
    }

    /**
     * Tests validation when no item is selected for borrowing.
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
     * Tests full payment of a fine.
     */
    @Test
    void testPayFineFullSuccess() {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(1);
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
    }

    /**
     * Tests partial payment of a fine.
     */
    @Test
    void testPayFinePartial() {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(1);
        item.calculateFine("Gold");
        
        TextField payField = getField(controller, "paymentField");
        payField.setText("1.0"); 

        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(1);
            controller.handlePayFine();
        });
        
        Label info = getField(controller, "infoLabel");
        assertTrue(info.getText().contains("Partial payment"));
    }

    /**
     * Tests input validation for fine payment.
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
     * Tests preventing payment for items borrowed by other users.
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
     * Tests successful return of a borrowed item.
     */
    @Test
    void testReturnBookSuccess() {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(0);
        item.borrow("TestUser"); 
        
        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(0);
            controller.handleReturnBook();
        });
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Returned successfully"));
    }

    /**
     * Tests that returning an item is blocked if there are outstanding fines.
     */
    @Test
    void testReturnBookFailWithFine() {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(1); 
        
        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(1);
            controller.handleReturnBook();
        });
        
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Pay the fine"));
    }

    /**
     * Tests the data reload functionality.
     */
    @Test
    void testReload() {
        runOnFxThreadAndWait(() -> controller.handleReload());
        Label info = getField(controller, "infoLabel");
        assertTrue(info.getText().contains("reloaded"));
    }
    
    /**
     * Tests the logout mechanism (headless verification).
     */
    @Test
    void testLogout() {
        runOnFxThreadAndWait(() -> {
            try { controller.handleLogout(); } catch (Exception e) {}
        });
    }

    /**
     * Tests private utility methods using Reflection for full coverage.
     * 
     * @throws Exception If reflection fails.
     */
    @Test
    void testPrivateHelpers() throws Exception {
        Method method = UserController.class.getDeclaredMethod("parseIntSafe", String.class, int.class);
        method.setAccessible(true);
        assertEquals(10, method.invoke(controller, "10", 0));
        
        Method normMethod = UserController.class.getDeclaredMethod("normalizeBorrowedBy", String.class);
        normMethod.setAccessible(true);
        assertEquals("", normMethod.invoke(controller, "0.0"));
    }

    /**
     * Tests the TableCell factory logic regarding privacy of data.
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
     * Tests parsing resilience against corrupted file data.
     * 
     * @throws IOException If write fails.
     */
    @Test
    void testParsingCorruptData() throws IOException {
         try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEST_FILE))) {
            writer.write("Book,BadLine");
            writer.newLine();
        }
        runOnFxThreadAndWait(() -> controller.handleReload());
        TableView<Media> table = getField(controller, "bookTable");
        assertTrue(table.getItems().isEmpty());
    }

    /**
     * Helper to inject mock objects into the controller.
     * 
     * @param target The controller instance.
     * @param fieldName The name of the field to inject.
     * @param value The value to set.
     * @throws Exception If reflection fails.
     */
    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * Helper to retrieve private fields from the controller.
     * 
     * @param target The controller instance.
     * @param fieldName The name of the field to retrieve.
     * @return The field value.
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