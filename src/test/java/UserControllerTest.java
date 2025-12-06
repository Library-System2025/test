import static org.junit.jupiter.api.Assertions.*;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import org.junit.jupiter.api.*;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Integration tests for the UserController.
 * Uses Reflection to verify borrowing, returning, and fine payment logic.
 * 
 * @author Zainab
 * @version 1.2
 */
public class UserControllerTest {

    @BeforeAll
    static void initToolkit() {
        try { Platform.startup(() -> {}); }
        catch (IllegalStateException e) { }
    }

    private UserController controller;

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

    private void setPrivate(String name, Object value) throws Exception {
        Field f = UserController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, value);
    }

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
     * Verifies setCurrentUser methods update labels and load books.
     */
    @Test
    void testSetCurrentUser() throws Exception {
        controller.setCurrentUser("u1", "Gold", "test@mail.com");
        Label welcome = (Label) getPrivateField("welcomeLabel");
        assertTrue(welcome.getText().contains("Gold"));

        controller.setCurrentUsername("u2");
        assertTrue(welcome.getText().contains("u2"));
        
        controller.setMembershipType("Platinum");
        assertTrue(welcome.getText().contains("Platinum"));
        
        controller.setCurrentUser("u3", "u3@mail.com");
        assertTrue(welcome.getText().contains("u3"));
    }

    /**
     * Verifies borrowing fails if no item is selected.
     */
    @Test
    void testHandleBorrowBook_NoSelection_ShowsWarning() throws Exception {
        controller.handleBorrowBook();
        Label msg = (Label) getPrivateField("messageLabel");
        assertEquals("⚠️ Please select an item to borrow.", msg.getText());
    }

    /**
     * Verifies borrowing fails if user has unpaid fines.
     */
    @Test
    void testHandleBorrowBook_BlockedByFines() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        
        Media overdueItem = new Book("Old", "A", "1", "Overdue", "2020-01-01", 10.0, "u1", 0.0, 1);
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
     * Verifies borrowing fails if trying to borrow a second copy of same book.
     */
    @Test
    void testHandleBorrowBook_SecondCopy_Rejected() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        
        Media copy1 = new Book("Book", "Auth", "111", "Borrowed", "2025-01-01", 0.0, "u1", 0.0, 1);
        mediaList.add(copy1);
        
        Media copy2 = new Book("Book", "Auth", "111", "Available", "", 0.0, "", 0.0, 2);
        mediaList.add(copy2);
        
        TableView<Media> table = (TableView<Media>) getPrivateField("bookTable");
        table.setItems(mediaList);
        table.getSelectionModel().select(copy2);

        controller.handleBorrowBook();
        
        Label msg = (Label) getPrivateField("messageLabel");
        assertEquals("❌ You already borrowed a copy of this item.", msg.getText());
    }

    /**
     * Verifies successful borrowing.
     */
    @Test
    void testHandleBorrowBook_Success() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        Media item = new Book("Java", "Auth", "999", "Available", "", 0.0, "", 0.0, 1);
        mediaList.add(item);
        
        TableView<Media> table = (TableView<Media>) getPrivateField("bookTable");
        table.setItems(mediaList);
        table.getSelectionModel().select(item);

        controller.handleBorrowBook();
        
        assertEquals("Borrowed", item.getStatus());
        assertEquals("u1", item.getBorrowedBy());
        Label msg = (Label) getPrivateField("messageLabel");
        assertTrue(msg.getText().contains("successfully"));
    }

    /**
     * Verifies pay fine validations.
     */
    @Test
    void testHandlePayFine_Validations() throws Exception {
        
        controller.handlePayFine();
        Label info = (Label) getPrivateField("infoLabel");
        assertEquals("⚠️ Select an item.", info.getText());
        
        
        ObservableList<Media> mediaList = getMediaList();
        Media otherItem = new Book("Other", "A", "1", "Borrowed", "", 0.0, "u2", 0.0, 1);
        mediaList.add(otherItem);
        
        TableView<Media> table = (TableView<Media>) getPrivateField("bookTable");
        table.setItems(mediaList);
        table.getSelectionModel().select(otherItem);
        
        controller.handlePayFine();
        assertEquals("❌ Select one of YOUR borrowed items.", info.getText());
    }

    /**
     * Verifies full fine payment returns item.
     */
    @Test
    void testHandlePayFine_FullPayment() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        Media item = new Book("Book", "A", "1", "Overdue", "2020-01-01", 5.0, "u1", 0.0, 1);
        mediaList.add(item);
        
        TableView<Media> table = (TableView<Media>) getPrivateField("bookTable");
        table.setItems(mediaList);
        table.getSelectionModel().select(item);
        
        ((TextField) getPrivateField("paymentField")).setText("5.0");
        
        controller.handlePayFine();
        
        assertEquals("Available", item.getStatus());
        Label info = (Label) getPrivateField("infoLabel");
        assertEquals("✅ Fine fully paid. Item returned.", info.getText());
    }

    /**
     * Verifies partial fine payment.
     */
    @Test
    void testHandlePayFine_PartialPayment() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        Media item = new Book("Book", "A", "1", "Overdue", "2020-01-01", 10.0, "u1", 0.0, 1);
        mediaList.add(item);
        
        TableView<Media> table = (TableView<Media>) getPrivateField("bookTable");
        table.setItems(mediaList);
        table.getSelectionModel().select(item);
        
        ((TextField) getPrivateField("paymentField")).setText("5.0");
        
        controller.handlePayFine();
        
        assertEquals(5.0, item.getFineAmount());
        Label info = (Label) getPrivateField("infoLabel");
        assertTrue(info.getText().contains("Partial payment accepted"));
    }

    /**
     * Verifies returning item logic.
     */
    @Test
    void testHandleReturnBook() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        
        
        Media item = new Book("Book", "A", "1", "Borrowed", "2030-01-01", 0.0, "u1", 0.0, 1);
        mediaList.add(item);
        
        TableView<Media> table = (TableView<Media>) getPrivateField("bookTable");
        table.setItems(mediaList);
        table.getSelectionModel().select(item);
        
        controller.handleReturnBook();
        
        assertEquals("Available", item.getStatus());
        Label msg = (Label) getPrivateField("messageLabel");
        assertEquals("✅ Returned successfully!", msg.getText());
        
        
        item.setBorrowedBy("u1");
        item.setDueDate("2020-01-01"); 
        item.setStatus("Borrowed");
        
        controller.handleReturnBook();
        assertEquals("Overdue", item.getStatus());
        assertTrue(msg.getText().contains("Pay the fine first"));
    }
    
    private Object getPrivateField(String name) throws Exception {
        Field f = UserController.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(controller);
    }
}