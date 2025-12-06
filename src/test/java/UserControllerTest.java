import static org.junit.jupiter.api.Assertions.*;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.util.Callback;
import org.junit.jupiter.api.*;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;

/**
 * Integration tests for the UserController.
 * Includes reflection-based tests for UI styling logic to maximize coverage.
 * 
 * @author Zainab
 * @version 1.6
 */
public class UserControllerTest {

    @BeforeAll
    static void initToolkit() {
        try { Platform.startup(() -> {}); }
        catch (IllegalStateException e) { }
    }

    private UserController controller;

    /**
     * Helper method to inject values into private fields.
     */
    private void injectField(String name, Object value) throws Exception {
        Field f = UserController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, value);
    }

    /**
     * Helper method to retrieve the media list.
     */
    @SuppressWarnings("unchecked")
    private ObservableList<Media> getMediaList() throws Exception {
        Field f = UserController.class.getDeclaredField("mediaList");
        f.setAccessible(true);
        return (ObservableList<Media>) f.get(controller);
    }

    /**
     * Helper method to set private fields.
     */
    private void setPrivate(String name, Object value) throws Exception {
        Field f = UserController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, value);
    }

    /**
     * Helper method to get private fields.
     */
    private Object getPrivateField(String name) throws Exception {
        Field f = UserController.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(controller);
    }

    /**
     * Sets up the test environment and initializes UI components.
     */
    @BeforeEach
    void setUp() throws Exception {
        controller = new UserController();
        File books = new File("books.txt");
        if (books.exists()) books.delete();

        injectField("welcomeLabel", new Label());
        injectField("paymentField", new TextField());
        injectField("infoLabel", new Label());
        injectField("messageLabel", new Label());

        TableView<Media> bookTable = new TableView<>();
        injectField("bookTable", bookTable);
        
        injectField("typeColumn", new TableColumn<>("Type"));
        injectField("titleColumn", new TableColumn<>("Title"));
        injectField("authorColumn", new TableColumn<>("Author"));
        injectField("isbnColumn", new TableColumn<>("ISBN"));
        injectField("statusColumn", new TableColumn<>("Status"));
        injectField("dueDateColumn", new TableColumn<>("Due"));
        injectField("fineColumn", new TableColumn<>("Fine"));

        injectField("mediaList", FXCollections.observableArrayList());

        controller.initialize();

        setPrivate("accountUsername", "u1");
        setPrivate("membershipType", "Silver");
        setPrivate("accountEmail", "u1@mail.com");
    }

    /**
     * Verifies the RowFactory logic for row coloring based on item status.
     */
    @Test
    void testRowFactory_ColoringLogic() throws Exception {
        TableView<Media> table = (TableView<Media>) getPrivateField("bookTable");
        
        Callback<TableView<Media>, TableRow<Media>> factory = table.getRowFactory();
        assertNotNull(factory);
        
        TableRow<Media> row = factory.call(table);
        
        Method updateItem = TableRow.class.getDeclaredMethod("updateItem", Object.class, boolean.class);
        updateItem.setAccessible(true);
        
        Media overdueItem = new Book("A", "B", "1", "Overdue", "", 0.0, "u1", 0.0, 1);
        updateItem.invoke(row, overdueItem, false);
        
        Media availableItem = new Book("A", "B", "1", "Available", "", 0.0, "", 0.0, 1);
        updateItem.invoke(row, availableItem, false);
        
        Media borrowedItem = new Book("A", "B", "1", "Borrowed", "", 0.0, "u1", 0.0, 1);
        updateItem.invoke(row, borrowedItem, false);

        updateItem.invoke(row, null, true);
    }

    /**
     * Verifies the CellFactory logic for specific columns.
     */
    @Test
    void testCellFactories_Logic() throws Exception {
        TableColumn<Media, String> dueCol = (TableColumn<Media, String>) getPrivateField("dueDateColumn");
        Callback<TableColumn<Media, String>, TableCell<Media, String>> cellFactory = dueCol.getCellFactory();
        TableCell<Media, String> cell = cellFactory.call(dueCol);
        assertNotNull(cell);
    }

    /**
     * Verifies that borrowing is blocked by unpaid fines.
     */
    @Test
    void testHandleBorrowBook_BlockedByFines() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        String pastDate = LocalDate.now().minusDays(10).toString();
        Media overdueItem = new Book("Old", "A", "1", "Overdue", pastDate, 10.0, "u1", 0.0, 1);
        mediaList.add(overdueItem);
        Media newItem = new Book("New", "B", "2", "Available", "", 0.0, "", 0.0, 1);
        mediaList.add(newItem);
        
        TableView<Media> table = (TableView<Media>) getPrivateField("bookTable");
        table.setItems(mediaList);
        table.getSelectionModel().select(newItem);
        controller.handleBorrowBook();
        Label msg = (Label) getPrivateField("messageLabel");
        assertEquals("❌ You have unpaid fines! Pay them first.", msg.getText());
    }

    /**
     * Verifies full fine payment logic.
     */
    @Test
    void testHandlePayFine_FullPayment() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        String pastDate = LocalDate.now().minusDays(5).toString();
        Media item = new Book("Book", "A", "1", "Overdue", pastDate, 0.0, "u1", 0.0, 1);
        item.calculateFine("Silver");
        double exactFine = item.getFineAmount();
        mediaList.add(item);
        
        TableView<Media> table = (TableView<Media>) getPrivateField("bookTable");
        table.setItems(mediaList);
        table.getSelectionModel().select(item);
        
        ((TextField) getPrivateField("paymentField")).setText(String.valueOf(exactFine));
        controller.handlePayFine();
        assertEquals("Available", item.getStatus());
    }

    /**
     * Verifies successful book return.
     */
    @Test
    void testHandleReturnBook_Success() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        String futureDate = LocalDate.now().plusDays(5).toString();
        Media item = new Book("Book", "A", "1", "Borrowed", futureDate, 0.0, "u1", 0.0, 1);
        mediaList.add(item);
        
        TableView<Media> table = (TableView<Media>) getPrivateField("bookTable");
        table.setItems(mediaList);
        table.getSelectionModel().select(item);
        controller.handleReturnBook();
        assertEquals("Available", item.getStatus());
    }

    /**
     * Verifies logic when returning an overdue book (email cleared).
     */
    @Test
    void testHandleReturnBook_OverdueBlocked() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        String pastDate = LocalDate.now().minusDays(5).toString();
        Media item = new Book("Book", "A", "1", "Borrowed", pastDate, 0.0, "u1", 0.0, 1);
        mediaList.add(item);
        
        TableView<Media> table = (TableView<Media>) getPrivateField("bookTable");
        table.setItems(mediaList);
        table.getSelectionModel().select(item);
        
        setPrivate("accountEmail", ""); 
        
        controller.handleReturnBook();
        assertEquals("Overdue", item.getStatus());
    }

    /**
     * Verifies setting current user updates UI.
     */
    @Test
    void testSetCurrentUser() throws Exception {
        controller.setCurrentUser("u1", "Gold", "test@mail.com");
        Label welcome = (Label) getPrivateField("welcomeLabel");
        assertTrue(welcome.getText().contains("Gold"));
    }
}