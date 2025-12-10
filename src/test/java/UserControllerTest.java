import static org.junit.jupiter.api.Assertions.*;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;

import org.junit.jupiter.api.*;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;

/**
 * Advanced Integration Tests for the {@link UserController} class.
 * <p>
 * This suite validates critical business logic including book borrowing,
 * return policies, fine calculations, and payment processing.
 * It mocks the JavaFX UI components to ensure tests run efficiently
 * in a headless environment.
 * </p>
 *
 * @author Zainab
 * @version 2.0
 */
public class UserControllerTest {

    private UserController controller;
    private TableView<Media> bookTable;
    private ObservableList<Media> mediaList;
    
    private Label welcomeLabel;
    private Label messageLabel;
    private Label infoLabel;
    private TextField paymentField;

    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {}
    }

    @BeforeEach
    void setUp() throws Exception {
        controller = new UserController();
        Files.deleteIfExists(Paths.get("books.txt"));

        welcomeLabel = new Label();
        messageLabel = new Label();
        infoLabel = new Label();
        paymentField = new TextField();
        bookTable = new TableView<>();
        mediaList = FXCollections.observableArrayList();

        injectField("welcomeLabel", welcomeLabel);
        injectField("messageLabel", messageLabel);
        injectField("infoLabel", infoLabel);
        injectField("paymentField", paymentField);
        injectField("bookTable", bookTable);
        injectField("mediaList", mediaList);
        
        injectField("typeColumn", new TableColumn<>("Type"));
        injectField("titleColumn", new TableColumn<>("Title"));
        injectField("authorColumn", new TableColumn<>("Author"));
        injectField("isbnColumn", new TableColumn<>("ISBN"));
        injectField("statusColumn", new TableColumn<>("Status"));
        injectField("dueDateColumn", new TableColumn<>("Due"));
        injectField("fineColumn", new TableColumn<>("Fine"));

        controller.initialize();
        bookTable.setItems(mediaList);
        controller.setCurrentUser("testUser", "Gold", "test@email.com");
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

    /**
     * Verifies that a user can successfully borrow an available book.
     */
    @Test
    void testBorrow_Success() {
        Media book = new Book("Java", "Author", "123", "Available", "", 0.0, "", 0.0, 1);
        mediaList.add(book);
        bookTable.getSelectionModel().select(book);

        controller.handleBorrowBook();

        assertEquals("Borrowed", book.getStatus());
        assertEquals("testUser", book.getBorrowedBy());
        assertTrue(messageLabel.getText().contains("✅ Borrowed successfully"));
    }

    /**
     * Verifies that borrowing fails if no item is selected from the table.
     */
    @Test
    void testBorrow_Fail_NoSelection() {
        controller.handleBorrowBook();
        assertTrue(messageLabel.getText().contains("Select an item"));
    }

    /**
     * Verifies that a user is blocked from borrowing if they have unpaid fines on other items.
     */
    @Test
    void testBorrow_Fail_AlreadyHasFines() {
        Media overdueBook = new Book("Old", "Auth", "999", "Overdue", "", 10.0, "testUser", 0.0, 1);
        Media newBook = new Book("New", "Auth", "123", "Available", "", 0.0, "", 0.0, 1);
        mediaList.addAll(overdueBook, newBook);
        
        bookTable.getSelectionModel().select(newBook);

        controller.handleBorrowBook();
        
        assertTrue(messageLabel.getText().contains("unpaid fines"));
        assertEquals("Available", newBook.getStatus());
    }

    /**
     * Verifies that a user cannot borrow a second copy of the same book.
     */
    @Test
    void testBorrow_Fail_DuplicateCopy() {
        Media copy1 = new Book("Java", "Auth", "123", "Borrowed", "", 0.0, "testUser", 0.0, 1);
        Media copy2 = new Book("Java", "Auth", "123", "Available", "", 0.0, "", 0.0, 2);
        mediaList.addAll(copy1, copy2);
        
        bookTable.getSelectionModel().select(copy2);

        controller.handleBorrowBook();

        assertTrue(messageLabel.getText().contains("already borrowed a copy"));
    }

    /**
     * Verifies that borrowing is rejected if the item is already borrowed by another user.
     */
    @Test
    void testBorrow_Fail_ItemNotAvailable() {
        Media borrowedBook = new Book("Java", "Auth", "123", "Borrowed", "", 0.0, "otherUser", 0.0, 1);
        mediaList.add(borrowedBook);
        bookTable.getSelectionModel().select(borrowedBook);

        controller.handleBorrowBook();

        assertTrue(messageLabel.getText().contains("not available"));
    }

    /**
     * Verifies the successful return of a borrowed item.
     */
    @Test
    void testReturn_Success() {
        Media book = new Book("Java", "Auth", "123", "Borrowed", LocalDate.now().plusDays(1).toString(), 0.0, "testUser", 0.0, 1);
        mediaList.add(book);
        bookTable.getSelectionModel().select(book);

        controller.handleReturnBook();

        assertEquals("Available", book.getStatus());
        assertTrue(messageLabel.getText().contains("✅ Returned successfully"));
    }

    /**
     * Verifies that returning an item is blocked if it is overdue and has unpaid fines.
     */
    @Test
    void testReturn_Fail_HasFine() {
        String pastDate = LocalDate.now().minusDays(10).toString();
        Media book = new Book("Java", "Auth", "123", "Borrowed", pastDate, 0.0, "testUser", 0.0, 1);
        mediaList.add(book);
        bookTable.getSelectionModel().select(book);

        controller.handleReturnBook();

        assertEquals("Overdue", book.getStatus());
        assertTrue(messageLabel.getText().contains("Pay the fine first"));
    }

    /**
     * Verifies that a user cannot return an item belonging to someone else.
     */
    @Test
    void testReturn_Fail_NotOwner() {
        Media book = new Book("Java", "Auth", "123", "Borrowed", "", 0.0, "otherUser", 0.0, 1);
        mediaList.add(book);
        bookTable.getSelectionModel().select(book);

        controller.handleReturnBook();

        assertTrue(messageLabel.getText().contains("only return your own"));
    }

    /**
     * Verifies successful full payment of a fine.
     */
    @Test
    void testPayFine_FullPayment() {
        Media book = new Book("Java", "Auth", "123", "Overdue", "", 15.0, "testUser", 0.0, 1);
        mediaList.add(book);
        bookTable.getSelectionModel().select(book);
        
        paymentField.setText("15.0");

        controller.handlePayFine();

        assertEquals("Available", book.getStatus());
        assertTrue(infoLabel.getText().contains("Fine fully paid"));
    }

    /**
     * Verifies partial payment logic, ensuring the item remains overdue but debt decreases.
     */
    @Test
    void testPayFine_PartialPayment() {
        Media book = new Book("Java", "Auth", "123", "Overdue", "", 15.0, "testUser", 0.0, 1);
        mediaList.add(book);
        bookTable.getSelectionModel().select(book);
        
        paymentField.setText("5.0");

        controller.handlePayFine();

        assertEquals("Overdue", book.getStatus());
        assertTrue(book.getFineAmount() > 0);
        assertTrue(infoLabel.getText().contains("Partial payment accepted"));
    }

    /**
     * Verifies input validation for the payment field (non-numeric, negative, excessive amounts).
     */
    @Test
    void testPayFine_InvalidInput() {
        Media book = new Book("Java", "Auth", "123", "Overdue", "", 15.0, "testUser", 0.0, 1);
        mediaList.add(book);
        bookTable.getSelectionModel().select(book);
        
        paymentField.setText("abc");
        controller.handlePayFine();
        assertTrue(infoLabel.getText().contains("Invalid number"));

        paymentField.setText("-5");
        controller.handlePayFine();
        assertTrue(infoLabel.getText().contains("must be positive"));

        paymentField.setText("100");
        controller.handlePayFine();
        assertTrue(infoLabel.getText().contains("exceeds fine"));
    }

    /**
     * Verifies that data is correctly loaded from the local storage file.
     *
     * @throws IOException if file creation fails.
     */
    @Test
    void testReloadAndFileIO() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("books.txt"))) {
            writer.write("Book,Title1,Auth1,111,1,Available,2025-01-01,0.0,,0.0\n");
            writer.write("CD,Title2,Auth2,222,1,Borrowed,2025-01-01,0.0,testUser,0.0\n");
        }

        controller.handleReload();

        assertEquals(2, mediaList.size());
        assertTrue(mediaList.get(0) instanceof Book);
        assertTrue(mediaList.get(1) instanceof CD);
        assertEquals("Title1", mediaList.get(0).getTitle());
    }

    /**
     * Verifies that updating the user membership correctly updates the UI label.
     */
    @Test
    void testSetMembershipUpdatesLabel() {
        controller.setMembershipType("Silver");
        assertTrue(welcomeLabel.getText().contains("Silver"));
    }
}