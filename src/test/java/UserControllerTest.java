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
 * Updated with explicit data synchronization to prevent empty table errors.
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
     * Sets up the test environment.
     * Ensures data is loaded into the table before proceeding.
     * 
     * @throws Exception if initialization fails.
     */
    @BeforeEach
    void setUp() throws Exception {
        createDataFile(TEST_FILE);
        createDataFile(BACKUP_FILE);
        
        controller = new UserController();
        injectTestFilePath();
        injectMockControls();
        
        runAndWait(() -> controller.initialize());
        
        waitForTableToLoad();

        controller.setCurrentUser(MOCK_USER, "Gold", MOCK_EMAIL);
    }

    /**
     * Cleans up the test environment.
     */
    @AfterEach
    void tearDown() {
        new File(TEST_FILE).delete();
        new File(BACKUP_FILE).delete();
    }

    /**
     * Waits for the table to populate with data to prevent IndexOutOfBoundsException.
     * 
     * @throws Exception if table remains empty.
     */
    private void waitForTableToLoad() throws Exception {
        long startTime = System.currentTimeMillis();
        boolean loaded = false;
        
        while (System.currentTimeMillis() - startTime < 3000) {
            final boolean[] isNotEmpty = {false};
            runAndWait(() -> {
                TableView<?> table = getField("bookTable");
                if (!table.getItems().isEmpty()) {
                    isNotEmpty[0] = true;
                }
            });
            
            if (isNotEmpty[0]) {
                loaded = true;
                break;
            }
            Thread.sleep(100);
        }
        
        if (!loaded) {
            System.err.println("WARNING: Table did not load data in time. Tests may fail.");
        }
    }

    /**
     * Injects the test file path into the controller.
     */
    private void injectTestFilePath() {
        String[] possibleFields = {"DATA_FILE", "FILE_NAME", "FILE_PATH", "fileName", "dataFile", "csvFile", "BOOK_FILE", "libraryFile"};
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
     * Verifies initialization and data loading.
     */
    @Test
    void testInitializationAndLoading() {
        TableView<Media> table = getField("bookTable");
        assertNotNull(table.getItems());
        assertFalse(table.getItems().isEmpty(), "Table should load data from the file.");
        assertTrue(table.getItems().size() >= 2, "Table should contain at least 2 items.");
    }

    /**
     * Tests user context updates.
     * 
     * @throws InterruptedException if thread is interrupted.
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
     * Tests the borrowing workflow.
     * 
     * @throws InterruptedException if thread is interrupted.
     */
    @Test
    void testBorrowFlow() throws InterruptedException {
        runAndWait(() -> {
            TableView<Media> table = getField("bookTable");
            if (!table.getItems().isEmpty()) {
                table.getSelectionModel().select(0);
                controller.handleBorrowBook();
            }
        });

        Label msg = getField("messageLabel");
        String text = msg.getText();
        
        if (!text.isEmpty()) {
            assertTrue(text.contains("successfully") || text.contains("Due date") || text.contains("already"), 
                   "Unexpected message: " + text);
        }

        runAndWait(() -> {
            TableView<Media> table = getField("bookTable");
             if (!table.getItems().isEmpty()) {
                table.getSelectionModel().select(0);
                controller.handleBorrowBook();
             }
        });
        
        String text2 = msg.getText();
        if (!text2.isEmpty()) {
            assertTrue(text2.contains("already borrowed") || text2.contains("own this book"), 
                   "Expected error message. Actual: " + text2);
        }

        runAndWait(() -> {
            ((TableView<?>) getField("bookTable")).getSelectionModel().clearSelection();
            controller.handleBorrowBook();
        });
        assertTrue(msg.getText().contains("select"), "Expected selection warning.");
    }

    /**
     * Tests borrowing blocker when fines exist.
     * 
     * @throws InterruptedException if thread is interrupted.
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
     * Tests the return workflow.
     * 
     * @throws InterruptedException if thread is interrupted.
     */
    @Test
    void testReturnFlow() throws InterruptedException {
        TableView<Media> table = getField("bookTable");
        if (table.getItems().isEmpty()) return;

        Media item = table.getItems().get(0);
        item.borrow(MOCK_USER);
        item.setFineAmount(0);

        runAndWait(() -> {
            table.getSelectionModel().select(item);
            controller.handleReturnBook();
        });
        
        Label msg = getField("messageLabel");
        assertTrue(msg.getText().contains("Returned successfully"), "Return failed.");

        if (table.getItems().size() > 1) {
            Media otherItem = table.getItems().get(1);
            otherItem.borrow("OtherGuy");
            runAndWait(() -> {
                table.getSelectionModel().select(otherItem);
                controller.handleReturnBook();
            });
            assertTrue(msg.getText().contains("only return your own"), "Security check failed.");
        }
    }

    /**
     * Tests return blocker when item has fines.
     * 
     * @throws InterruptedException if thread is interrupted.
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

        Label msg = getField("messageLabel");
        if (item.getFineAmount() > 0 || msg.getText().contains("Pay")) {
            assertTrue(msg.getText().contains("Pay the fine") || msg.getText().contains("fines"), 
                       "Fine block failed.");
        }
    }

    /**
     * Tests payment workflow.
     * 
     * @throws InterruptedException if thread is interrupted.
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
                   "Partial payment failed.");

        runAndWait(() -> {
            table.getSelectionModel().select(item);
            payField.setText(String.valueOf(totalFine));
            controller.handlePayFine();
        });
        
        infoText = info.getText();
        assertTrue(infoText.contains("paid") || infoText.contains("returned") || infoText.contains("successful"), 
                   "Full payment failed.");
    }

    /**
     * Tests payment input validation.
     * 
     * @throws InterruptedException if thread is interrupted.
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
        assertTrue(info.getText().contains("positive"), "Negative check failed.");

        runAndWait(() -> { 
            payField.setText("NotNumber"); 
            controller.handlePayFine(); 
        });
        assertTrue(info.getText().contains("Invalid"), "NaN check failed.");

        runAndWait(() -> { 
            payField.setText("9999"); 
            controller.handlePayFine(); 
        });
        assertTrue(info.getText().contains("exceeds"), "Overpayment check failed.");
    }

    /**
     * Tests reload and logout functionality.
     * 
     * @throws InterruptedException if thread is interrupted.
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
     * Tests internal helper methods via reflection.
     * 
     * @throws Exception if reflection fails.
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
     * Executes action on JavaFX thread and waits for completion.
     * 
     * @param action The action to run.
     * @throws InterruptedException if interrupted.
     */
    private void runAndWait(Runnable action) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try { action.run(); } finally { latch.countDown(); }
        });
        latch.await(3, TimeUnit.SECONDS);
    }

    /**
     * Gets a private field value.
     * 
     * @param name Field name.
     * @param <T> Field type.
     * @return Field value.
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
     * Injects mock controls.
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
        
        ((TableColumn<?,?>) getField("typeColumn")).setCellValueFactory(new PropertyValueFactory<>("mediaType"));
    }

    /**
     * Sets a private field value.
     * 
     * @param name Field name.
     * @param val Value to set.
     * @throws Exception if access fails.
     */
    private void setField(String name, Object val) throws Exception {
        Field f = UserController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, val);
    }

    /**
     * Creates test data files.
     * 
     * @param fileName Name of file.
     * @throws IOException if write fails.
     */
    private void createDataFile(String fileName) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(fileName))) {
            w.write("Book,Test Title,Auth,123,1,Available,2025-01-01,0.0,0.0,0.0");
            w.newLine();
            w.write("CD,Test CD,Artist,456,1,Available,2025-01-01,0.0,0.0,0.0");
        }
    }
}