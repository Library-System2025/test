import static org.junit.jupiter.api.Assertions.*;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import org.junit.jupiter.api.*;

import java.io.*;
import java.lang.reflect.Field;
import java.time.LocalDate;

/**
 * Integration tests for the UserController.
 * Verifies borrowing, returning, and fine payment logic using dynamic dates.
 * 
 * @author Zainab
 * @version 1.5
 */
public class UserControllerTest {

    @BeforeAll
    static void initToolkit() {
        try { Platform.startup(() -> {}); }
        catch (IllegalStateException e) { }
    }

    private UserController controller;

    /**
     * Helper method to inject values into private fields using Reflection.
     */
    private void injectField(String name, Object value) throws Exception {
        Field f = UserController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, value);
    }

    /**
     * Helper method to retrieve the media list from the controller.
     */
    @SuppressWarnings("unchecked")
    private ObservableList<Media> getMediaList() throws Exception {
        Field f = UserController.class.getDeclaredField("mediaList");
        f.setAccessible(true);
        return (ObservableList<Media>) f.get(controller);
    }

    /**
     * Helper method to set private fields using Reflection.
     */
    private void setPrivate(String name, Object value) throws Exception {
        Field f = UserController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, value);
    }

    /**
     * Helper method to get private fields using Reflection.
     */
    private Object getPrivateField(String name) throws Exception {
        Field f = UserController.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(controller);
    }

    /**
     * Sets up the test environment, initializes the controller,
     * injects mock UI components, and sets a default user context.
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
     * Verifies that borrowing is blocked if the user has unpaid overdue fines.
     * Uses dynamic dates to ensure test validity.
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
     * Verifies that paying the full fine amount returns the item to available status.
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
        Label info = (Label) getPrivateField("infoLabel");
        assertEquals("✅ Fine fully paid. Item returned.", info.getText());
    }

    /**
     * Verifies that partial payment reduces the fine amount without returning the item.
     */
    @Test
    void testHandlePayFine_PartialPayment() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        
        String pastDate = LocalDate.now().minusDays(10).toString();
        
        Media item = new Book("Book", "A", "1", "Overdue", pastDate, 0.0, "u1", 0.0, 1);
        item.calculateFine("Silver");
        double totalFine = item.getFineAmount();
        double payAmount = totalFine / 2;
        
        mediaList.add(item);
        
        TableView<Media> table = (TableView<Media>) getPrivateField("bookTable");
        table.setItems(mediaList);
        table.getSelectionModel().select(item);
        
        ((TextField) getPrivateField("paymentField")).setText(String.valueOf(payAmount));
        
        controller.handlePayFine();
        
        double expectedRemaining = totalFine - payAmount;
        assertEquals(expectedRemaining, item.getFineAmount(), 0.01);
        
        Label info = (Label) getPrivateField("infoLabel");
        assertTrue(info.getText().contains("Partial payment accepted"));
    }

    /**
     * Verifies successful return of a borrowed item that is not overdue.
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
        Label msg = (Label) getPrivateField("messageLabel");
        assertEquals("✅ Returned successfully!", msg.getText());
    }

    /**
     * Verifies that returning an overdue item is blocked and requires fine payment.
     * Clears the user email to prevent RuntimeException during email sending.
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
        Label msg = (Label) getPrivateField("messageLabel");
        assertTrue(msg.getText().contains("Pay the fine first"));
    }

    /**
     * Verifies that setting the current user updates the UI labels correctly.
     */
    @Test
    void testSetCurrentUser() throws Exception {
        controller.setCurrentUser("u1", "Gold", "test@mail.com");
        Label welcome = (Label) getPrivateField("welcomeLabel");
        assertTrue(welcome.getText().contains("Gold"));
    }
    
    /**
     * Verifies warning message when borrowing without selection.
     */
    @Test
    void testHandleBorrowBook_NoSelection() throws Exception {
        controller.handleBorrowBook();
        Label msg = (Label) getPrivateField("messageLabel");
        assertEquals("⚠️ Please select an item to borrow.", msg.getText());
    }

    /**
     * Verifies warning message when paying fine without selection.
     */
    @Test
    void testHandlePayFine_NoSelection() throws Exception {
        controller.handlePayFine();
        Label info = (Label) getPrivateField("infoLabel");
        assertEquals("⚠️ Select an item.", info.getText());
    }
}