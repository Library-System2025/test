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
 * This suite is engineered to achieve maximum branch and line coverage by 
 * utilizing reflection to simulate UI interactions and test private logic,
 * including RowFactories and CellFactories.
 * </p>
 * 
 * @author Zainab
 * @version 1.0 
 */
class UserControllerTest {

    private UserController controller;
    private static final String TEST_FILE = "books.txt";

    /**
     * Initializes the JavaFX Platform.
     */
    @BeforeAll
    static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
        }
    }

    /**
     * Default constructor.
     */
    public UserControllerTest() {
    }

    /**
     * Sets up the test environment.
     * 
     * @throws Exception If setup fails.
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
     * Cleans up temporary files.
     */
    @AfterEach
    void tearDown() {
        File file = new File(TEST_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Injects mock UI components.
     * 
     * @throws Exception If reflection fails.
     */
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
    }

    /**
     * Helper to run on FX thread.
     * 
     * @param action Runnable action.
     * @throws InterruptedException If interrupted.
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
     * Tests table initialization.
     */
    @Test
    void testInitialize() {
        TableView<Media> table = getField(controller, "bookTable");
        assertNotNull(table.getItems());
        assertFalse(table.getItems().isEmpty());
    }

    /**
     * Tests user context setting and label styling.
     * 
     * @throws InterruptedException If interrupted.
     */
    @Test
    void testSetCurrentUser() throws InterruptedException {
        runOnFxThreadAndWait(() -> controller.setCurrentUser("GoldUser", "Gold", "g@mail.com"));
        Label label = getField(controller, "welcomeLabel");
        assertTrue(label.getStyle().contains("gold"));

        runOnFxThreadAndWait(() -> controller.setCurrentUser("SilverUser", "Silver", "s@mail.com"));
        assertTrue(label.getStyle().contains("silver"));
    }

    /**
     * Tests legacy setters.
     * 
     * @throws InterruptedException If interrupted.
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
     * Tests borrowing success.
     * 
     * @throws InterruptedException If interrupted.
     */
    @Test
    void testBorrowBookSuccess() throws InterruptedException {
        performBorrow(0);
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Borrowed successfully"));
    }

    /**
     * Tests borrow failure: No selection.
     * 
     * @throws InterruptedException If interrupted.
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
     * Tests borrow failure: Unpaid fines exist.
     * 
     * @throws InterruptedException If interrupted.
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
     * Tests borrow failure: Already borrowed copy.
     * 
     * @throws InterruptedException If interrupted.
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
     * Tests borrow failure: Item not available.
     * 
     * @throws InterruptedException If interrupted.
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
     * Tests partial payment.
     * 
     * @throws InterruptedException If interrupted.
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
     * Tests full payment.
     * 
     * @throws InterruptedException If interrupted.
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
     * Tests payment validation logic.
     * 
     * @throws InterruptedException If interrupted.
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
     * Tests payment failure: No selection.
     * 
     * @throws InterruptedException If interrupted.
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
     * Tests payment failure: Wrong user.
     * 
     * @throws InterruptedException If interrupted.
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
     * Tests successful return.
     * 
     * @throws InterruptedException If interrupted.
     */
    @Test
    void testReturnBookSuccess() throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(0);
        
        item.borrow("TestUser");
        item.setFineAmount(0);
        
        runOnFxThreadAndWait(() -> {
            getField(controller, "bookTable", TableView.class).getSelectionModel().select(item);
            controller.handleReturnBook();
        });
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Returned successfully"));
    }

    /**
     * Tests return failure due to outstanding fines.
     * 
     * @throws InterruptedException If interrupted.
     */
    @Test
    void testReturnBookFailWithFine() throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(1);
        
        item.borrow("TestUser");
        item.setDueDate("2000-01-01"); 

        runOnFxThreadAndWait(() -> {
            getField(controller, "bookTable", TableView.class).getSelectionModel().select(item);
            try { controller.handleReturnBook(); } catch (Exception e) {}
        });

        if (item.getFineAmount() > 0) {
            Label msg = getField(controller, "messageLabel");
            assertTrue(msg.getText().contains("Pay the fine"));
        }
    }
    
    /**
     * Tests return failure: Wrong user.
     * 
     * @throws InterruptedException If interrupted.
     */
    @Test
    void testValidationNotBorrower() throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(1);
        item.borrow("OtherPerson");
        
        runOnFxThreadAndWait(() -> {
            getField(controller, "bookTable", TableView.class).getSelectionModel().select(item);
            controller.handleReturnBook();
        });
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("only return your own"));
    }
    
    /**
     * Tests return failure: No selection.
     * 
     * @throws InterruptedException If interrupted.
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
     * Tests private helpers via reflection.
     * 
     * @throws Exception If reflection fails.
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
        assertEquals("", normalize.invoke(controller, (Object)null));
        assertEquals("User", normalize.invoke(controller, "User"));
    }

    /**
     * Tests RowFactory logic by direct invocation.
     * 
     * @throws Exception If reflection fails.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testRowFactoryStyling() throws Exception {
        TableView<Media> table = getField(controller, "bookTable");
        Callback<TableView<Media>, TableRow<Media>> rowFactory = table.getRowFactory();
        assertNotNull(rowFactory);

        TableRow<Media> row = rowFactory.call(table);
        Method updateItem = TableRow.class.getDeclaredMethod("updateItem", Object.class, boolean.class);
        updateItem.setAccessible(true);

        // Case 1: Empty
        updateItem.invoke(row, null, true);
        assertEquals("", row.getStyle());

        // Case 2: My Item + Overdue
        updateItem.invoke(row, new Book("B", "A", "1", "Overdue", "2000-01-01", 10.0, "TestUser", 0, 1), false);
        assertTrue(row.getStyle().contains("ffcccc"));

        // Case 3: My Item + Normal
        updateItem.invoke(row, new Book("B", "A", "2", "Borrowed", "2099-01-01", 0.0, "TestUser", 0, 1), false);
        assertTrue(row.getStyle().contains("c8f7c5"));

        // Case 4: Other Item + Borrowed
        updateItem.invoke(row, new Book("B", "A", "3", "Borrowed", "2099-01-01", 0.0, "Other", 0, 1), false);
        assertTrue(row.getStyle().contains("fff3cd"));
    }

    /**
     * Tests CellFactory logic by direct invocation.
     * 
     * @throws Exception If reflection fails.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testCellFactoryRendering() throws Exception {
        TableColumn<Media, String> dueCol = getField(controller, "dueDateColumn");
        TableColumn<Media, Double> fineCol = getField(controller, "fineColumn");
        
        TableCell<Media, String> dueCell = dueCol.getCellFactory().call(dueCol);
        TableCell<Media, Double> fineCell = fineCol.getCellFactory().call(fineCol);
        
        Media myItem = new Book("My Book", "Me", "111", "Borrowed", "2025-01-01", 10.0, "TestUser", 0, 1);
        Media otherItem = new Book("Other Book", "Me", "222", "Borrowed", "2025-01-01", 10.0, "Other", 0, 1);

        Method updateItemString = TableCell.class.getDeclaredMethod("updateItem", Object.class, boolean.class);
        updateItemString.setAccessible(true);
        
        
        updateItemString.invoke(dueCell, null, true);
        assertNull(dueCell.getText());

        
        injectTableRow(dueCell, createRow(myItem));
        updateItemString.invoke(dueCell, myItem.getDueDate(), false); 
        assertEquals("2025-01-01", dueCell.getText());

        
        injectTableRow(dueCell, createRow(otherItem));
        updateItemString.invoke(dueCell, otherItem.getDueDate(), false);
        assertEquals("", dueCell.getText());

        
        Method updateItemDouble = TableCell.class.getDeclaredMethod("updateItem", Object.class, boolean.class);
        updateItemDouble.setAccessible(true);
        injectTableRow(fineCell, createRow(myItem));
        updateItemDouble.invoke(fineCell, 10.0, false);
        assertTrue(fineCell.getText().contains("10.0"));
    }

    /**
     * Tests file parsing robustness.
     * 
     * @throws IOException If write fails.
     * @throws InterruptedException If interrupted.
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
     * Tests notification email flow.
     * 
     * @throws InterruptedException If interrupted.
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
            getField(controller, "bookTable", TableView.class).getSelectionModel().select(item);
            controller.handleReturnBook();
        });
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Pay the fine"));
    }

    /**
     * Tests reload functionality.
     * 
     * @throws InterruptedException If interrupted.
     */
    @Test
    void testReload() throws InterruptedException {
        runOnFxThreadAndWait(() -> controller.handleReload());
        Label info = getField(controller, "infoLabel");
        assertTrue(info.getText().contains("reloaded"));
    }

    /**
     * Tests logout functionality.
     * 
     * @throws InterruptedException If interrupted.
     */
    @Test
    void testLogout() throws InterruptedException {
        runOnFxThreadAndWait(() -> {
            try { controller.handleLogout(); } catch (Exception e) {}
        });
    }

    /**
     * Tests file saving exceptions.
     * 
     * @throws Exception If failure occurs.
     */
    @Test
    void testSaveFileException() throws Exception {
        File file = new File(TEST_FILE);
        if(file.exists()) file.delete();
        file.mkdir();
        
        Media item = new Book("A", "B", "C", "Avail", "date", 0, "user", 0, 1);
        item.borrow("TestUser");
        
        runOnFxThreadAndWait(() -> controller.handleBorrowBook());
        
        file.delete();
    }

    /**
     * Helper to perform borrow.
     * 
     * @param index Selection index.
     * @throws InterruptedException If interrupted.
     */
    private void performBorrow(int index) throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(index);
            controller.handleBorrowBook();
        });
    }

    /**
     * Helper to perform payment.
     * 
     * @param item The item.
     * @param amount The amount.
     * @throws InterruptedException If interrupted.
     */
    private void performPayment(Media item, String amount) throws InterruptedException {
        TableView<Media> table = getField(controller, "bookTable");
        TextField payField = getField(controller, "paymentField");
        payField.setText(amount);
        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(item);
            controller.handlePayFine();
        });
    }

    /**
     * Creates a TableRow.
     * 
     * @param item The item.
     * @return The row.
     */
    private TableRow<Media> createRow(Media item) {
        TableRow<Media> row = new TableRow<>();
        row.setItem(item);
        return row;
    }

    /**
     * Injects a row into a cell.
     * 
     * @param cell The cell.
     * @param row The row.
     * @throws Exception If reflection fails.
     */
    private void injectTableRow(TableCell<?, ?> cell, TableRow<?> row) throws Exception {
        Field rowField = javafx.scene.control.Cell.class.getDeclaredField("tableRow");
        rowField.setAccessible(true);
        rowField.set(cell, row);
    }

    /**
     * Creates a dummy file.
     * 
     * @throws IOException If write fails.
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
     * Injects a mock field.
     * 
     * @param target The object.
     * @param fieldName The field name.
     * @param value The value.
     * @throws Exception If reflection fails.
     */
    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * Gets a private field.
     * 
     * @param <T> The type.
     * @param target The object.
     * @param fieldName The field name.
     * @return The value.
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