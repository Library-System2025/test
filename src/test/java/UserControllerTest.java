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
 * Uses dynamic dates to ensure tests pass regardless of execution year.
 * 
 * @author Zainab
 * @version 1.3
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
     * Verifies borrowing fails if user has unpaid fines.
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
     * Verifies full fine payment returns item.
     * Uses dynamic calculation to match the fine exactly.
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
        
        // Pay the EXACT calculated fine
        ((TextField) getPrivateField("paymentField")).setText(String.valueOf(exactFine));
        
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
     * Verifies returning item logic.
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

    @Test
    void testHandleReturnBook_OverdueBlocked() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        
        
        String pastDate = LocalDate.now().minusDays(5).toString();
        
        Media item = new Book("Book", "A", "1", "Borrowed", pastDate, 0.0, "u1", 0.0, 1);
        mediaList.add(item);
        
        TableView<Media> table = (TableView<Media>) getPrivateField("bookTable");
        table.setItems(mediaList);
        table.getSelectionModel().select(item);
        
        controller.handleReturnBook();
        
        assertEquals("Overdue", item.getStatus());
        Label msg = (Label) getPrivateField("messageLabel");
        assertTrue(msg.getText().contains("Pay the fine first"));
    }
    
    
    @Test
    void testHandleBorrowBook_NoSelection() throws Exception {
        controller.handleBorrowBook();
        Label msg = (Label) getPrivateField("messageLabel");
        assertEquals("⚠️ Please select an item to borrow.", msg.getText());
    }

    @Test
    void testHandlePayFine_NoSelection() throws Exception {
        controller.handlePayFine();
        Label info = (Label) getPrivateField("infoLabel");
        assertEquals("⚠️ Select an item.", info.getText());
    }
    
    private Object getPrivateField(String name) throws Exception {
        Field f = UserController.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(controller);
    }
}