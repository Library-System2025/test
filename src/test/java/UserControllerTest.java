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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;

/**
 * Advanced Integration tests for {@link UserController}.
 * <p>
 * This test suite uses Java Reflection to bypass UI restrictions and simulate
 * user interactions directly against the controller logic. It achieves high
 * code coverage by targeting all branching logic (if/else) in borrowing,
 * returning, and paying fines.
 * </p>
 * 
 * @author Zainab
 * @version 2.0
 */
public class UserControllerTest {

    private UserController controller;

    /**
     * Initializes the JavaFX Toolkit. 
     * Essential for creating UI controls (Label, TableView) in a headless test environment.
     */
    @BeforeAll
    static void initToolkit() {
        try { Platform.startup(() -> {}); }
        catch (IllegalStateException e) { }
    }

    /**
     * Sets up the controller and injects Mock UI components before each test.
     */
    @BeforeEach
    void setUp() throws Exception {
        controller = new UserController();
        Files.deleteIfExists(Paths.get("books.txt"));

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

        controller.setCurrentUser("u1", "Silver", "u1@mail.com");
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Paths.get("books.txt"));
    }

    private void injectField(String name, Object value) throws Exception {
        Field f = UserController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, value);
    }

    @SuppressWarnings("unchecked")
    private ObservableList<Media> getMediaList() throws Exception {
        Field f = UserController.class.getDeclaredField("mediaList");
        f.setAccessible(true);
        return (ObservableList<Media>) f.get(controller);
    }

    private Object getPrivateField(String name) throws Exception {
        Field f = UserController.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(controller);
    }

    /**
     * Tests that the TableView correctly colors rows based on ownership and status.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testRowFactory_ColoringLogic() throws Exception {
        TableView<Media> table = (TableView<Media>) getPrivateField("bookTable");
        Callback<TableView<Media>, TableRow<Media>> factory = table.getRowFactory();
        assertNotNull(factory);
        
        TableRow<Media> row = factory.call(table);
        Method updateItem = javafx.scene.control.Cell.class.getDeclaredMethod("updateItem", Object.class, boolean.class);
        updateItem.setAccessible(true);
        
        Media myOverdue = new Book("A", "B", "1", "Overdue", "", 0.0, "u1", 0.0, 1);
        updateItem.invoke(row, myOverdue, false);
        assertEquals("-fx-background-color: #ffcccc;", row.getStyle());
        
        Media myOk = new Book("A", "B", "1", "Borrowed", "", 0.0, "u1", 0.0, 1);
        updateItem.invoke(row, myOk, false);
        assertEquals("-fx-background-color: #c8f7c5;", row.getStyle());
        
        Media otherBorrowed = new Book("A", "B", "1", "Borrowed", "", 0.0, "other", 0.0, 1);
        updateItem.invoke(row, otherBorrowed, false);
        assertEquals("-fx-background-color: #fff3cd;", row.getStyle());

        updateItem.invoke(row, null, true);
        assertEquals("", row.getStyle());
    }

    /**
     * Verifies that a user cannot borrow a book if they have unpaid fines.
     */
    @Test
    void testHandleBorrowBook_BlockedByFines() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        Media fineItem = new Book("Old", "A", "1", "Overdue", "", 10.0, "u1", 0.0, 1);
        mediaList.add(fineItem);
        
        Media targetItem = new Book("New", "B", "2", "Available", "", 0.0, "", 0.0, 1);
        mediaList.add(targetItem);
        
        TableView<Media> table = (TableView<Media>) getPrivateField("bookTable");
        table.setItems(mediaList);
        table.getSelectionModel().select(targetItem);
        
        controller.handleBorrowBook();
        
        Label msg = (Label) getPrivateField("messageLabel");
        assertTrue(msg.getText().contains("unpaid fines"));
    }

    /**
     * Verifies that a user cannot borrow a second copy of a book they already have.
     */
    @Test
    void testHandleBorrowBook_DuplicateCopy_Blocked() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        
        Media currentCopy = new Book("Java", "Me", "999", "Borrowed", "", 0.0, "u1", 0.0, 1);
        mediaList.add(currentCopy);
        
        Media newCopy = new Book("Java", "Me", "999", "Available", "", 0.0, "", 0.0, 2);
        mediaList.add(newCopy);
        
        TableView<Media> table = (TableView<Media>) getPrivateField("bookTable");
        table.setItems(mediaList);
        table.getSelectionModel().select(newCopy);
        
        controller.handleBorrowBook();
        
        Label msg = (Label) getPrivateField("messageLabel");
        assertTrue(msg.getText().contains("already borrowed a copy"));
    }

    /**
     * Verifies valid borrowing flow when no restrictions apply.
     */
    @Test
    void testHandleBorrowBook_Success() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        Media targetItem = new Book("New", "B", "555", "Available", "", 0.0, "", 0.0, 1);
        mediaList.add(targetItem);
        
        TableView<Media> table = (TableView<Media>) getPrivateField("bookTable");
        table.setItems(mediaList);
        table.getSelectionModel().select(targetItem);
        
        controller.handleBorrowBook();
        
        assertEquals("Borrowed", targetItem.getStatus());
        assertEquals("u1", targetItem.getBorrowedBy());
        Label msg = (Label) getPrivateField("messageLabel");
        assertTrue(msg.getText().contains("Borrowed successfully"));
    }
    
    /**
     * Tests borrowing when no item is selected.
     */
    @Test
    void testHandleBorrowBook_NoSelection() throws Exception {
        controller.handleBorrowBook();
        Label msg = (Label) getPrivateField("messageLabel");
        String text = msg.getText().toLowerCase();
        assertTrue(text.contains("select") || text.contains("item"), "Message should prompt to select an item");
    }

    /**
     * Tests borrowing an item that is already borrowed by someone else.
     */
    @Test
    void testHandleBorrowBook_NotAvailable() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        Media takenItem = new Book("New", "B", "555", "Borrowed", "", 0.0, "other", 0.0, 1);
        mediaList.add(takenItem);
        
        TableView<Media> table = (TableView<Media>) getPrivateField("bookTable");
        table.setItems(mediaList);
        table.getSelectionModel().select(takenItem);
        
        controller.handleBorrowBook();
        Label msg = (Label) getPrivateField("messageLabel");
        assertTrue(msg.getText().contains("not available"));
    }

    /**
     * Comprehensive test for Payment Validation Logic.
     */
    @Test
    void testHandlePayFine_AllCases() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        Media item = new Book("B", "A", "1", "Overdue", "", 0.0, "u1", 0.0, 1);
        item.calculateFine("Silver"); 
        double fine = item.getFineAmount(); 
        mediaList.add(item);
        
        TableView<Media> table = (TableView<Media>) getPrivateField("bookTable");
        table.setItems(mediaList);
        table.getSelectionModel().select(item);
        TextField payField = (TextField) getPrivateField("paymentField");
        Label info = (Label) getPrivateField("infoLabel");

        payField.setText("abc");
        controller.handlePayFine();
        assertTrue(info.getText().contains("Invalid number"));

        payField.setText("-5");
        controller.handlePayFine();
        assertTrue(info.getText().contains("positive"));
        
        payField.setText(String.valueOf(fine + 100));
        controller.handlePayFine();
        assertTrue(info.getText().contains("exceeds fine"));

        payField.setText("1.0");
        controller.handlePayFine();
        assertTrue(info.getText().contains("Partial payment"));
        assertTrue(item.getFineAmount() < fine);

        payField.setText(String.valueOf(item.getFineAmount()));
        controller.handlePayFine();
        String infoText = info.getText().toLowerCase();
        assertTrue(infoText.contains("returned") || infoText.contains("paid"), "Message should confirm return or payment");
        assertEquals("Available", item.getStatus());
    }

    /**
     * Verifies a user cannot pay for a book they do not own.
     */
    @Test
    void testHandlePayFine_NotOwner() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        Media item = new Book("B", "A", "1", "Overdue", "", 10.0, "other", 0.0, 1);
        mediaList.add(item);
        
        TableView<Media> table = (TableView<Media>) getPrivateField("bookTable");
        table.setItems(mediaList);
        table.getSelectionModel().select(item);
        
        controller.handlePayFine();
        Label info = (Label) getPrivateField("infoLabel");
        assertTrue(info.getText().contains("Select one of YOUR"));
    }

    /**
     * Verifies returning a book successfully.
     */
    @Test
    void testHandleReturnBook_Success() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        Media item = new Book("Book", "A", "1", "Borrowed", LocalDate.now().plusDays(5).toString(), 0.0, "u1", 0.0, 1);
        mediaList.add(item);
        
        TableView<Media> table = (TableView<Media>) getPrivateField("bookTable");
        table.setItems(mediaList);
        table.getSelectionModel().select(item);
        
        controller.handleReturnBook();
        
        assertEquals("Available", item.getStatus());
        Label msg = (Label) getPrivateField("messageLabel");
        assertTrue(msg.getText().contains("Returned successfully"));
    }

    /**
     * Verifies returning is blocked if the item is Overdue (must pay fine first).
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
        
        injectField("accountEmail", ""); 
        
        controller.handleReturnBook();
        
        assertEquals("Overdue", item.getStatus());
        Label msg = (Label) getPrivateField("messageLabel");
        assertTrue(msg.getText().contains("Pay the fine first"));
    }

    /**
     * Verifies user cannot return someone else's book.
     */
    @Test
    void testHandleReturnBook_NotOwner() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        Media item = new Book("Book", "A", "1", "Borrowed", "", 0.0, "other", 0.0, 1);
        mediaList.add(item);
        
        TableView<Media> table = (TableView<Media>) getPrivateField("bookTable");
        table.setItems(mediaList);
        table.getSelectionModel().select(item);
        
        controller.handleReturnBook();
        Label msg = (Label) getPrivateField("messageLabel");
        assertTrue(msg.getText().contains("only return your own"));
    }

    /**
     * Tests loading data from file, including parsing different media types (Book/CD).
     */
    @Test
    void testLoadMediaFromFile() throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("books.txt"))) {
            writer.write("Book,Title1,Auth1,111,1,Available,2025-01-01,0.0,,0.0");
            writer.newLine();
            writer.write("CD,Title2,Auth2,222,1,Borrowed,2025-01-01,0.0,u1,0.0");
        }
        
        injectField("accountEmail", "");
        
        controller.handleReload();
        
        ObservableList<Media> mediaList = getMediaList();
        assertEquals(2, mediaList.size());
        assertTrue(mediaList.get(0) instanceof Book);
        assertTrue(mediaList.get(1) instanceof CD);
    }
    
    /**
     * Verifies setCurrentUser updates the welcome label correctly.
     */
    @Test
    void testSetCurrentUser() throws Exception {
        controller.setCurrentUser("GoldUser", "Gold", "test@test.com");
        Label lbl = (Label) getPrivateField("welcomeLabel");
        assertTrue(lbl.getText().contains("GoldUser"));
        assertTrue(lbl.getText().contains("Gold"));
    }
    
    /**
     * Tests setMembershipType updates state.
     */
    @Test
    void testSetMembershipType() throws Exception {
        controller.setMembershipType("Silver");
        String type = (String) getPrivateField("membershipType");
        assertEquals("Silver", type);
    }
    
    /**
     * Tests setCurrentUsername updates state.
     */
    @Test
    void testSetCurrentUsername() throws Exception {
        controller.setCurrentUsername("NewName");
        String name = (String) getPrivateField("accountUsername");
        assertEquals("NewName", name);
    }
}