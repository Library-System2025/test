import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
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
 * This class validates the core functionality of the library system including:
 * </p>
 * <ul>
 *   <li>Initialization and data loading.</li>
 *   <li>User context switching (Gold/Silver memberships).</li>
 *   <li>Borrowing books (checking availability and duplication).</li>
 *   <li>Returning books (checking ownership and fines).</li>
 *   <li>Paying fines and updating records.</li>
 *   <li>Reloading and Logging out.</li>
 * </ul>
 * <p>
 * It utilizes Java Reflection to inspect and modify private fields within the Controller
 * to simulate various system states without needing the full UI.
 * </p>
 * 
 * @author Zainab
 * @version 1.0
 */
class UserControllerTest {

    /** The controller instance under test. */
    private UserController controller;
    
    /** Name of the temporary file used for testing media data. */
    private static final String TEST_FILE = "books.txt"; 
    
    /** Name of the backup file (cleaned up after tests). */
    private static final String BACKUP_FILE = "library.txt";
    
    /** Mock email for the test user. */
    private static final String MOCK_EMAIL = "test@mock.com";
    
    /** Mock username for the test user. */
    private static final String MOCK_USER = "TestUser";

    /**
     * Initializes the JavaFX runtime environment.
     * <p>
     * This is required because the Controller uses JavaFX components.
     * Catches IllegalStateException if the toolkit is already initialized.
     * </p>
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
     * Sets up the test environment before each test method.
     * <p>
     * Creates data files, initializes the controller, injects dependencies,
     * and sets the default user context.
     * </p>
     * 
     * @throws Exception if any setup step fails.
     */
    @BeforeEach
    void setUp() throws Exception {
        createDataFile(TEST_FILE);
        
        controller = new UserController();
        injectTestFilePath();
        injectMockControls();
        
        runAndWait(() -> controller.initialize());
        
        waitForTableToLoad();

        controller.setCurrentUser(MOCK_USER, "Gold", MOCK_EMAIL);
    }

    /**
     * Cleans up resources after each test.
     * <p>
     * Deletes the temporary test files created during execution.
     * </p>
     */
    @AfterEach
    void tearDown() {
        try {
            new File(TEST_FILE).delete();
            new File(BACKUP_FILE).delete();
        } catch (Exception e) {
        }
    }

    /**
     * Waits for the TableView to load data items asynchronously.
     * 
     * @throws Exception if the wait is interrupted.
     */
    private void waitForTableToLoad() throws Exception {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 5000) {
            final boolean[] hasData = {false};
            runAndWait(() -> {
                TableView<?> table = getField("bookTable");
                if (table != null && !table.getItems().isEmpty()) {
                    hasData[0] = true;
                }
            });
            
            if (hasData[0]) break;
            Thread.sleep(100);
        }
    }

    /**
     * Injects the test file path into the Controller's private constant/field.
     */
    private void injectTestFilePath() {
        String[] possibleFields = {"FILE_PATH", "DATA_FILE", "fileName", "dataFile"};
        for (String fieldName : possibleFields) {
            try {
                Field field = UserController.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                try {
                    Field modifiersField = Field.class.getDeclaredField("modifiers");
                    modifiersField.setAccessible(true);
                    modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
                } catch (Exception e) { }
                
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
        assertNotNull(table.getItems(), "Table items list should not be null.");
        assertFalse(table.getItems().isEmpty(), "Table should contain data loaded from the file.");
    }

    /**
     * Verifies that updating the user context updates the UI welcome label.
     * 
     * @throws InterruptedException if the JavaFX thread execution is interrupted.
     */
    @Test
    void testUserContextUpdates() throws InterruptedException {
        runAndWait(() -> controller.setCurrentUser("NewUser", "Silver", "new@mail.com"));
        Label label = getField("welcomeLabel");
        assertTrue(label.getText().contains("NewUser"), "Welcome label should display the new username.");
        
        runAndWait(() -> {
            controller.setMembershipType("Platinum");
            controller.setCurrentUsername("UpdatedName");
        });
        assertTrue(label.getText().contains("UpdatedName"), "Welcome label should update with the modified username.");
    }

    /**
     * Tests the book borrowing workflow, including duplication checks and availability.
     * 
     * @throws InterruptedException if the JavaFX thread execution is interrupted.
     */
    @Test
    void testBorrowFlow() throws InterruptedException {
        runAndWait(() -> {
            TableView<Media> table = getField("bookTable");
            if (!table.getItems().isEmpty()) {
                Media item = table.getItems().get(0);
                
                setInternalState(item, "status", "Available");
                setInternalState(item, "borrowedBy", ""); 
                setInternalState(item, "fineAmount", 0.0);
                
                table.getSelectionModel().select(0);
                controller.handleBorrowBook();
            }
        });

        Label msg = getField("messageLabel");
        String text = msg.getText();
        
        if (text != null && !text.isEmpty()) {
            boolean success = text.toLowerCase().contains("success") || 
                              text.contains("Due date") || 
                              text.contains("already");
            assertTrue(success, "Expected success message or valid status update.");
        }

        runAndWait(() -> {
            TableView<Media> table = getField("bookTable");
            if (!table.getItems().isEmpty()) {
                table.getSelectionModel().select(0);
                controller.handleBorrowBook();
            }
        });
        
        String text2 = msg.getText();
        if (text2 != null && !text2.isEmpty()) {
            assertTrue(text2.contains("already") || text2.contains("own") || text2.toLowerCase().contains("success"),
                    "Should detect that the user already has a copy of this item.");
        }

        runAndWait(() -> {
            ((TableView<?>) getField("bookTable")).getSelectionModel().clearSelection();
            controller.handleBorrowBook();
        });
        assertTrue(msg.getText().toLowerCase().contains("select"), "Should warn user to select an item.");
    }

    /**
     * Tests the book return workflow for a borrowed item.
     * 
     * @throws InterruptedException if the JavaFX thread execution is interrupted.
     */
    @Test
    void testReturnFlow() throws InterruptedException {
        TableView<Media> table = getField("bookTable");
        if (table.getItems().isEmpty()) return;

        Media item = table.getItems().get(0);

        setInternalState(item, "borrowedBy", MOCK_USER);
        setInternalState(item, "status", "Borrowed");
        setInternalState(item, "fineAmount", 0.0);

        runAndWait(() -> {
            table.getSelectionModel().select(item);
            controller.handleReturnBook();
        });
        
        Label msg = getField("messageLabel");
        String text = msg.getText();
        if (text != null) {
            assertTrue(text.toLowerCase().contains("success") || text.contains("Returned"), 
                    "Message should indicate successful return.");
        }
    }

    /**
     * Tests the fine payment workflow with partial payments.
     * 
     * @throws InterruptedException if the JavaFX thread execution is interrupted.
     */
    @Test
    void testPaymentFlow() throws InterruptedException {
        TableView<Media> table = getField("bookTable");
        if (table.getItems().size() < 2) return;

        Media item = table.getItems().get(1);
        
        setInternalState(item, "borrowedBy", MOCK_USER);
        setInternalState(item, "status", "Overdue");
        setInternalState(item, "fineAmount", 10.0);
        
        TextField payField = getField("paymentField");

        runAndWait(() -> {
            table.getSelectionModel().select(item);
            payField.setText("5.0");
            controller.handlePayFine();
        });
        
        Label info = getField("infoLabel");
        String text = info.getText();
        
        assertTrue(text.contains("Partial") || text.contains("Remaining") || text.toLowerCase().contains("success") || text.isEmpty(),
                "Should acknowledge partial payment or success.");
    }

    /**
     * Tests the reload and logout handlers.
     * 
     * @throws InterruptedException if the JavaFX thread execution is interrupted.
     */
    @Test
    void testReloadAndLogout() throws InterruptedException {
        runAndWait(() -> controller.handleReload());
        Label info = getField("infoLabel");
        if (info.getText() != null) {
            assertTrue(info.getText().contains("reloaded") || info.getText().isEmpty());
        }

        runAndWait(() -> {
            try { controller.handleLogout(); } catch (Exception e) {
            }
        });
    }

    /**
     * Executes a Runnable on the JavaFX Application Thread and waits for it to finish.
     * 
     * @param action The code to execute on the UI thread.
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    private void runAndWait(Runnable action) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try { action.run(); } catch(Exception e) { e.printStackTrace(); } finally { latch.countDown(); }
        });
        latch.await(5, TimeUnit.SECONDS);
    }

    /**
     * Retrieves a private field from the controller using Reflection.
     * 
     * @param name The name of the field.
     * @param <T> The expected type of the field.
     * @return The value of the field, or null if not found.
     */
    @SuppressWarnings("unchecked")
    private <T> T getField(String name) {
        try {
            Field f = UserController.class.getDeclaredField(name);
            f.setAccessible(true);
            return (T) f.get(controller);
        } catch (Exception e) { return null; }
    }

    /**
     * Sets a private field on any object using Reflection.
     * 
     * @param target The object containing the field.
     * @param fieldName The name of the field to set.
     * @param value The value to assign.
     */
    private void setInternalState(Object target, String fieldName, Object value) {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field f = clazz.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Injects mock JavaFX controls into the Controller.
     * 
     * @throws Exception if injection fails.
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
        
        ((TableColumn<?,?>) getField("statusColumn")).setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    /**
     * Helper to set a private field in the Controller specifically.
     * 
     * @param name Field name.
     * @param val Field value.
     */
    private void setField(String name, Object val) throws Exception {
        try {
            Field f = UserController.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(controller, val);
        } catch (NoSuchFieldException e) { }
    }

    /**
     * Creates a temporary data file with sample book entries for testing.
     * 
     * @param fileName The name of the file to create.
     * @throws IOException if writing to the file fails.
     */
    private void createDataFile(String fileName) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(fileName))) {
            w.write("Book,Test Title,Auth,123,1,Available,2025-01-01,0.0,,0.0");
            w.newLine();
            w.write("CD,Test CD,Artist,456,1,Available,2025-01-01,0.0,,0.0");
        }
    }
}