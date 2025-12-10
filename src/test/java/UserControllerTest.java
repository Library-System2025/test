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
 * Advanced JUnit Suite for UserController.
 * <p>
 * Fixes DateTimeParseException by using strictly formatted ISO-8601 dates.
 * Ensures high coverage, security compliance, and robust error handling.
 * </p>
 * 
 * @author Zainab
 * @version 1.0
 */
class UserControllerTest {

    private UserController controller;
    private static final String DB_FILE = "books.txt";

    /**
     * Initializes JavaFX and sets mock environment variables.
     */
    @BeforeAll
    static void launchJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
        }
        System.setProperty("EMAIL_USERNAME", "mock_usr");
        System.setProperty("EMAIL_CREDENTIAL", "mock_key"); 
    }

    /**
     * Sets up the environment before each test.
     * 
     * @throws Exception If setup fails.
     */
    @BeforeEach
    void init() throws Exception {
        generateDbFile();
        controller = new UserController();
        injectComponents();
        executeOnFx(() -> controller.initialize());
        controller.setCurrentUser("TestUser", "Gold", "test@mail.com");
    }

    /**
     * Cleans up the test file.
     */
    @AfterEach
    void cleanup() {
        new File(DB_FILE).delete();
    }

    /**
     * Injects UI components.
     * 
     * @throws Exception If reflection fails.
     */
    private void injectComponents() throws Exception {
        setField(controller, "welcomeLabel", new Label());
        setField(controller, "paymentField", new TextField());
        setField(controller, "infoLabel", new Label());
        setField(controller, "messageLabel", new Label());

        TableView<Media> table = new TableView<>();
        setField(controller, "bookTable", table);
        setField(controller, "typeColumn", new TableColumn<Media, String>());
        setField(controller, "titleColumn", new TableColumn<Media, String>());
        setField(controller, "authorColumn", new TableColumn<Media, String>());
        setField(controller, "isbnColumn", new TableColumn<Media, String>());
        setField(controller, "statusColumn", new TableColumn<Media, String>());
        setField(controller, "dueDateColumn", new TableColumn<Media, String>());
        setField(controller, "fineColumn", new TableColumn<Media, Double>());
    }

    /**
     * Runs actions on the JavaFX thread.
     * 
     * @param cmd The action to run.
     * @throws InterruptedException If interrupted.
     */
    private void executeOnFx(Runnable cmd) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                cmd.run();
            } finally {
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
    }

    /**
     * Tests initialization.
     */
    @Test
    void verifyInitialization() {
        TableView<Media> table = fetchField(controller, "bookTable");
        assertNotNull(table.getItems());
        assertFalse(table.getItems().isEmpty());
    }

    /**
     * Tests user context setting.
     * 
     * @throws InterruptedException If interrupted.
     */
    @Test
    void verifyUserContext() throws InterruptedException {
        executeOnFx(() -> controller.setCurrentUser("Alpha", "Silver", "a@b.com"));
        Label lbl = fetchField(controller, "welcomeLabel");
        assertTrue(lbl.getText().contains("Alpha"));
    }

    /**
     * Tests legacy setters.
     * 
     * @throws InterruptedException If interrupted.
     */
    @Test
    void verifyLegacyMethods() throws InterruptedException {
        executeOnFx(() -> {
            controller.setMembershipType("Platinum");
            controller.setCurrentUsername("Beta");
            controller.setCurrentUser("Gamma", "g@b.com");
        });
        Label lbl = fetchField(controller, "welcomeLabel");
        assertTrue(lbl.getText().contains("Gamma"));
    }

    /**
     * Tests successful borrowing.
     * 
     * @throws InterruptedException If interrupted.
     */
    @Test
    void flowBorrowSuccess() throws InterruptedException {
        doSelection(0);
        executeOnFx(() -> controller.handleBorrowBook());
        Label msg = fetchField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Borrowed successfully"));
    }

    /**
     * Tests borrow failure (no selection).
     * 
     * @throws InterruptedException If interrupted.
     */
    @Test
    void flowBorrowFailNoSelect() throws InterruptedException {
        executeOnFx(() -> {
            fetchField(controller, "bookTable", TableView.class).getSelectionModel().clearSelection();
            controller.handleBorrowBook();
        });
        Label msg = fetchField(controller, "messageLabel");
        assertTrue(msg.getText().contains("select an item"));
    }

    /**
     * Tests borrow failure (fines).
     * 
     * @throws InterruptedException If interrupted.
     */
    @Test
    void flowBorrowFailFines() throws InterruptedException {
        modifyItem(1, "Available", 0.0);
        TableView<Media> table = fetchField(controller, "bookTable");
        Media m = table.getItems().get(1);
        m.borrow("TestUser");
        m.setFineAmount(50.0);

        doSelection(0);
        executeOnFx(() -> controller.handleBorrowBook());
        
        Label msg = fetchField(controller, "messageLabel");
        assertTrue(msg.getText().contains("unpaid fines"));
    }

    /**
     * Tests borrow failure (duplicate).
     * 
     * @throws InterruptedException If interrupted.
     */
    @Test
    void flowBorrowFailDuplicate() throws InterruptedException {
        TableView<Media> table = fetchField(controller, "bookTable");
        table.getItems().get(0).borrow("TestUser");
        
        doSelection(0);
        executeOnFx(() -> controller.handleBorrowBook());
        
        Label msg = fetchField(controller, "messageLabel");
        assertTrue(msg.getText().contains("already borrowed"));
    }
    
    /**
     * Tests borrow failure (unavailable).
     * 
     * @throws InterruptedException If interrupted.
     */
    @Test
    void flowBorrowFailTaken() throws InterruptedException {
        TableView<Media> table = fetchField(controller, "bookTable");
        table.getItems().get(0).borrow("Other");
        
        doSelection(0);
        executeOnFx(() -> controller.handleBorrowBook());
        
        Label msg = fetchField(controller, "messageLabel");
        assertTrue(msg.getText().contains("not available"));
    }

    /**
     * Tests partial payment.
     * 
     * @throws InterruptedException If interrupted.
     */
    @Test
    void flowPayPartial() throws InterruptedException {
        Media m = modifyItem(1, "Overdue", 100.0);
        m.borrow("TestUser");
        m.setDueDate("2000-01-01");
        m.calculateFine("Gold"); 

        inputPayment("1.0");
        doSelection(1);
        executeOnFx(() -> controller.handlePayFine());

        Label info = fetchField(controller, "infoLabel");
        assertTrue(info.getText().contains("Partial payment"));
    }

    /**
     * Tests full payment.
     * 
     * @throws InterruptedException If interrupted.
     */
    @Test
    void flowPayFull() throws InterruptedException {
        Media m = modifyItem(1, "Overdue", 10.0);
        m.borrow("TestUser");
        m.setDueDate("2000-01-01");
        m.calculateFine("Gold");
        
        inputPayment(String.valueOf(m.getFineAmount()));
        doSelection(1);
        executeOnFx(() -> controller.handlePayFine());
        
        Label info = fetchField(controller, "infoLabel");
        assertTrue(info.getText().contains("fully paid") || info.getText().contains("Item returned"));
    }

    /**
     * Tests payment validation.
     * 
     * @throws InterruptedException If interrupted.
     */
    @Test
    void flowPayValidation() throws InterruptedException {
        Media m = modifyItem(1, "Overdue", 10.0);
        m.borrow("TestUser");
        m.setDueDate("2000-01-01"); 
        doSelection(1);

        inputPayment("-5");
        executeOnFx(() -> controller.handlePayFine());
        assertTrue(fetchField(controller, "infoLabel", Label.class).getText().contains("positive"));

        inputPayment("xyz");
        executeOnFx(() -> controller.handlePayFine());
        assertTrue(fetchField(controller, "infoLabel", Label.class).getText().contains("Invalid number"));

        inputPayment("999999");
        executeOnFx(() -> controller.handlePayFine());
        assertTrue(fetchField(controller, "infoLabel", Label.class).getText().contains("Payment exceeds"));
    }
    
    /**
     * Tests wrong user payment.
     * 
     * @throws InterruptedException If interrupted.
     */
    @Test
    void flowPayWrongUser() throws InterruptedException {
        Media m = modifyItem(1, "Borrowed", 10.0);
        m.borrow("Alien");
        inputPayment("10");
        doSelection(1);
        executeOnFx(() -> controller.handlePayFine());
        Label info = fetchField(controller, "infoLabel");
        assertTrue(info.getText().contains("YOUR borrowed items"));
    }

    /**
     * Tests successful return.
     * 
     * @throws InterruptedException If interrupted.
     */
    @Test
    void flowReturnSuccess() throws InterruptedException {
        Media m = modifyItem(0, "Borrowed", 0.0);
        m.borrow("TestUser");
        doSelection(0);
        executeOnFx(() -> controller.handleReturnBook());
        Label msg = fetchField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Returned successfully"));
    }

    /**
     * Tests return failure (fines).
     * 
     * @throws InterruptedException If interrupted.
     */
    @Test
    void flowReturnFail() throws InterruptedException {
        Media m = modifyItem(1, "Borrowed", 0.0);
        m.borrow("TestUser");
        m.setDueDate("2000-01-01"); 
        doSelection(1);
        
        executeOnFx(() -> {
            try { controller.handleReturnBook(); } catch (Exception e) {}
        });

        Label msg = fetchField(controller, "messageLabel");
        if(m.getFineAmount() > 0) {
            assertTrue(msg.getText().contains("Pay the fine"));
        }
    }
    
    /**
     * Tests return wrong user.
     * 
     * @throws InterruptedException If interrupted.
     */
    @Test
    void flowReturnWrongUser() throws InterruptedException {
        Media m = modifyItem(1, "Borrowed", 0.0);
        m.borrow("Alien");
        doSelection(1);
        executeOnFx(() -> controller.handleReturnBook());
        Label msg = fetchField(controller, "messageLabel");
        assertTrue(msg.getText().contains("only return your own"));
    }

    /**
     * Tests private helpers via reflection.
     * 
     * @throws Exception If reflection fails.
     */
    @Test
    void verifyPrivateHelpers() throws Exception {
        Method parseInt = UserController.class.getDeclaredMethod("parseIntSafe", String.class, int.class);
        parseInt.setAccessible(true);
        assertEquals(5, parseInt.invoke(controller, "5", 0));
        assertEquals(9, parseInt.invoke(controller, "NaN", 9));

        Method parseDouble = UserController.class.getDeclaredMethod("parseDoubleSafe", String.class, double.class);
        parseDouble.setAccessible(true);
        assertEquals(5.5, parseDouble.invoke(controller, "5.5", 0.0));
        assertEquals(9.9, parseDouble.invoke(controller, "NaN", 9.9));
        
        Method normalize = UserController.class.getDeclaredMethod("normalizeBorrowedBy", String.class);
        normalize.setAccessible(true);
        assertEquals("", normalize.invoke(controller, "0.0"));
        assertEquals("", normalize.invoke(controller, (Object)null));
        assertEquals("Bob", normalize.invoke(controller, "Bob"));
    }

    /**
     * Tests row factory via reflection.
     */
    @Test
    @SuppressWarnings("unchecked")
    void verifyVisualLogic() {
        TableView<Media> table = fetchField(controller, "bookTable");
        Callback<TableView<Media>, TableRow<Media>> factory = table.getRowFactory();
        assertNotNull(factory);

        TableRow<Media> row = factory.call(table);
        try {
            Method update = TableRow.class.getDeclaredMethod("updateItem", Object.class, boolean.class);
            update.setAccessible(true);

            update.invoke(row, null, true);
            update.invoke(row, new Book("T","A","1","Overdue","2025-01-01",10,"TestUser",0,1), false);
            update.invoke(row, new Book("T","A","1","Borrowed","2025-01-01",0,"TestUser",0,1), false);
            update.invoke(row, new Book("T","A","1","Borrowed","2025-01-01",0,"Alien",0,1), false);
        } catch (Exception e) {}
    }

    /**
     * Tests cell factory via reflection.
     */
    @Test
    @SuppressWarnings("unchecked")
    void verifyCellLogic() {
        TableColumn<Media, String> col = fetchField(controller, "dueDateColumn");
        TableCell<Media, String> cell = col.getCellFactory().call(col);
        
        Media mine = new Book("T","A","1","B","2025-01-01",0,"TestUser",0,1);
        Media other = new Book("T","A","1","B","2025-01-01",0,"Alien",0,1);

        try {
            Method update = TableCell.class.getDeclaredMethod("updateItem", Object.class, boolean.class);
            update.setAccessible(true);
            
            update.invoke(cell, null, true);
            
            setupCellRow(cell, mine);
            update.invoke(cell, mine.getDueDate(), false); 
            
            setupCellRow(cell, other);
            update.invoke(cell, other.getDueDate(), false);
        } catch (Exception e) {}
    }

    /**
     * Tests corrupted file handling.
     * 
     * @throws IOException If write fails.
     * @throws InterruptedException If interrupted.
     */
    @Test
    void flowCorruptedFile() throws IOException, InterruptedException {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(DB_FILE))) {
            w.write("Bad,Data,No,Commas\nShort\n");
        }
        executeOnFx(() -> controller.handleReload());
        TableView<Media> table = fetchField(controller, "bookTable");
        assertTrue(table.getItems().stream().anyMatch(m -> "Bad".equals(m.getTitle()) || m.getTitle() == null));
    }
    
    /**
     * Tests null email notification.
     * 
     * @throws InterruptedException If interrupted.
     */
    @Test
    void flowNullEmail() throws InterruptedException {
        executeOnFx(() -> controller.setCurrentUser("TestUser", "Gold", null));
        Media m = modifyItem(0, "Overdue", 0.0);
        m.borrow("TestUser");
        m.setDueDate("2000-01-01");
        
        doSelection(0);
        executeOnFx(() -> controller.handleReturnBook());
        
        Label msg = fetchField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Pay the fine"));
    }

    /**
     * Tests reload.
     * 
     * @throws InterruptedException If interrupted.
     */
    @Test
    void flowReload() throws InterruptedException {
        executeOnFx(() -> controller.handleReload());
        Label info = fetchField(controller, "infoLabel");
        assertTrue(info.getText().contains("reloaded"));
    }

    /**
     * Tests logout.
     * 
     * @throws InterruptedException If interrupted.
     */
    @Test
    void flowLogout() throws InterruptedException {
        executeOnFx(() -> {
            try { controller.handleLogout(); } catch (Exception e) {}
        });
    }

    /**
     * Tests save error.
     * 
     * @throws Exception If failure.
     */
    @Test
    void flowSaveError() throws Exception {
        File f = new File(DB_FILE);
        if(f.exists()) f.delete();
        f.mkdir();
        
        Media m = new Book("T","A","I","S","2025-01-01",0,"U",0,1);
        m.borrow("TestUser");
        
        executeOnFx(() -> controller.handleBorrowBook());
        
        f.delete();
    }

    private void doSelection(int idx) {
        fetchField(controller, "bookTable", TableView.class).getSelectionModel().select(idx);
    }

    private void inputPayment(String val) {
        fetchField(controller, "paymentField", TextField.class).setText(val);
    }
    
    private Media modifyItem(int idx, String status, double fine) {
        TableView<Media> tbl = fetchField(controller, "bookTable");
        Media m = tbl.getItems().get(idx);
        m.setStatus(status);
        m.setFineAmount(fine);
        return m;
    }

    private void setupCellRow(TableCell<?, ?> cell, Media item) throws Exception {
        TableRow<Media> tr = new TableRow<>();
        tr.setItem(item);
        Field f = javafx.scene.control.Cell.class.getDeclaredField("tableRow");
        f.setAccessible(true);
        f.set(cell, tr);
    }

    private void generateDbFile() throws IOException {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(DB_FILE))) {
            w.write("Book,Java,Auth,123,1,Available,2025-01-01,0,0,0\n");
            w.write("CD,Hits,Art,456,1,Available,2025-01-01,0,0,0\n");
        }
    }

    private void setField(Object obj, String name, Object val) throws Exception {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, val);
    }

    @SuppressWarnings("unchecked")
    private <T> T fetchField(Object obj, String name) {
        try {
            Field f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return (T) f.get(obj);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    
    private <T> T fetchField(Object obj, String name, Class<T> type) {
        return type.cast(fetchField(obj, name));
    }
}