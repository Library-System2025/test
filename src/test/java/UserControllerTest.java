import static org.junit.jupiter.api.Assertions.*;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import org.junit.jupiter.api.*;

import java.io.*;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Integration tests for the UserController.
 * Verifies borrowing, returning, and fine payment logic.
 * 
 * @author Zainab
 * @version 1.1
 */
public class UserControllerTest {

    @BeforeAll
    static void initToolkit() {
        try { Platform.startup(() -> {}); }
        catch (IllegalStateException e) { }
    }

    private UserController controller;
    private Label welcomeLabel;
    private TextField paymentField;
    private Label infoLabel;
    private Label messageLabel;
    private TableView<Media> bookTable;
    private TableColumn<Media, String> typeColumn;
    private TableColumn<Media, String> titleColumn;
    private TableColumn<Media, String> authorColumn;
    private TableColumn<Media, String> isbnColumn;
    private TableColumn<Media, String> statusColumn;
    private TableColumn<Media, String> dueDateColumn;
    private TableColumn<Media, Double> fineColumn;

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

        welcomeLabel = new Label();
        paymentField = new TextField();
        infoLabel = new Label();
        messageLabel = new Label();

        bookTable = new TableView<>();
        typeColumn = new TableColumn<>("Type");
        titleColumn = new TableColumn<>("Title");
        authorColumn = new TableColumn<>("Author");
        isbnColumn = new TableColumn<>("ISBN");
        statusColumn = new TableColumn<>("Status");
        dueDateColumn = new TableColumn<>("Due");
        fineColumn = new TableColumn<>("Fine");

        injectField("welcomeLabel", welcomeLabel);
        injectField("paymentField", paymentField);
        injectField("infoLabel", infoLabel);
        injectField("messageLabel", messageLabel);

        injectField("bookTable", bookTable);
        injectField("typeColumn", typeColumn);
        injectField("titleColumn", titleColumn);
        injectField("authorColumn", authorColumn);
        injectField("isbnColumn", isbnColumn);
        injectField("statusColumn", statusColumn);
        injectField("dueDateColumn", dueDateColumn);
        injectField("fineColumn", fineColumn);

        ObservableList<Media> mediaList = FXCollections.observableArrayList();
        injectField("mediaList", mediaList);

        controller.initialize();

        setPrivate("accountUsername", "u1");
        setPrivate("membershipType", "Silver");
        setPrivate("accountEmail", "u1@mail.com");
    }

    // ===================== handleBorrowBook =====================

    @Test
    void testHandleBorrowBook_noSelection_showsWarning() throws Exception {
        controller.handleBorrowBook();
        assertEquals("⚠️ Please select an item to borrow.", messageLabel.getText());
    }

    @Test
    void testHandleBorrowBook_itemNotAvailable_showsError() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        Media item = new Book("Clean Code", "Robert Martin", "111",
                "Borrowed", "", 0.0, "other", 0.0, 1);
        mediaList.add(item);
        bookTable.setItems(mediaList);
        bookTable.getSelectionModel().select(item);

        controller.handleBorrowBook();

        assertEquals("❌ This item is not available.", messageLabel.getText());
    }

    @Test
    void testHandleBorrowBook_success_updatesStatusAndDueDate() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        Media item = new Book("Clean Code", "Robert Martin", "111",
                "Available", "", 0.0, "", 0.0, 1);
        mediaList.add(item);
        bookTable.setItems(mediaList);
        bookTable.getSelectionModel().select(item);

        controller.handleBorrowBook();

        assertEquals("Borrowed", item.getStatus());
        assertEquals("u1", item.getBorrowedBy());
        assertNotNull(item.getDueDate());
        assertFalse(item.getDueDate().isEmpty());
        assertTrue(messageLabel.getText().startsWith("✅ Borrowed successfully!"));
    }

    @Test
    void testHandleBorrowBook_blockedWhenUnpaidFinesExist() throws Exception {
        ObservableList<Media> mediaList = getMediaList();

        Media owed = new Book("Old Loan", "A", "X",
                "Overdue", "2025-11-01", 5.0, "u1", 0.0, 1);
        mediaList.add(owed);

        Media available = new Book("Clean Code", "Robert Martin", "111",
                "Available", "", 0.0, "", 0.0, 1);
        mediaList.add(available);

        bookTable.setItems(mediaList);
        bookTable.getSelectionModel().select(available);

        controller.handleBorrowBook();

        assertEquals("❌ You have unpaid fines! Pay them first.", messageLabel.getText());
        assertEquals("Available", available.getStatus(), "Should not borrow when fines exist");
    }

    /**
     * يتأكد إن اليوزر ما بقدر يستعير نسخة ثانية من نفس الكتاب (نفس ISBN)
     * وهو أصلاً مستعير نسخة منه.
     */
    @Test
    void testHandleBorrowBook_cannotBorrowSecondCopyOfSameBook() throws Exception {
        ObservableList<Media> mediaList = getMediaList();

        // نسخة أولى مستعارة من نفس اليوزر
        Media copy1 = new Book("Clean Code", "Robert Martin", "111",
                "Borrowed", "2025-12-20", 0.0, "u1", 0.0, 1);
        mediaList.add(copy1);

        // نسخة ثانية Available بنفس الـ ISBN
        Media copy2 = new Book("Clean Code", "Robert Martin", "111",
                "Available", "", 0.0, "", 0.0, 2);
        mediaList.add(copy2);

        bookTable.setItems(mediaList);
        bookTable.getSelectionModel().select(copy2);

        controller.handleBorrowBook();

        assertEquals("❌ You already borrowed a copy of this item.", messageLabel.getText());
        assertEquals("Available", copy2.getStatus(), "Second copy should remain available");
        assertEquals("", copy2.getBorrowedBy());
    }

    // ===================== handlePayFine =====================

    @Test
    void testHandlePayFine_noSelection_showsWarning() {
        controller.handlePayFine();
        assertEquals("⚠️ Select an item.", infoLabel.getText());
    }

    @Test
    void testHandlePayFine_notUserItem_showsError() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        Media item = new Book("Clean Code", "Robert Martin", "111",
                "Overdue", "2025-11-01", 5.0, "other", 0.0, 1);
        mediaList.add(item);
        bookTable.setItems(mediaList);
        bookTable.getSelectionModel().select(item);

        paymentField.setText("2.0");

        controller.handlePayFine();

        assertEquals("❌ Select one of YOUR borrowed items.", infoLabel.getText());
    }

    @Test
    void testHandlePayFine_invalidNumber_showsError() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        Media item = new Book("Clean Code", "Robert Martin", "111",
                "Overdue", "2025-11-01", 5.0, "u1", 0.0, 1);
        mediaList.add(item);
        bookTable.setItems(mediaList);
        bookTable.getSelectionModel().select(item);

        paymentField.setText("abc");

        controller.handlePayFine();

        assertEquals("❌ Invalid number.", infoLabel.getText());
    }

    @Test
    void testHandlePayFine_exceedsFine_showsError() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        Media item = new Book("Clean Code", "Robert Martin", "111",
                "Overdue", "2025-11-01", 5.0, "u1", 0.0, 1);
        mediaList.add(item);
        bookTable.setItems(mediaList);
        bookTable.getSelectionModel().select(item);

        paymentField.setText("10.0");

        controller.handlePayFine();

        assertEquals("❌ Payment exceeds fine amount!", infoLabel.getText());
    }

    @Test
    void testHandlePayFine_fullPayment_returnsItem() throws Exception {
        ObservableList<Media> mediaList = getMediaList();

        String yesterday = LocalDate.now().minusDays(1)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Media item = new Book("Clean Code", "Robert Martin", "111",
                "Borrowed", yesterday, 1.0, "u1", 0.0, 1);
        mediaList.add(item);
        bookTable.setItems(mediaList);
        bookTable.getSelectionModel().select(item);

        paymentField.setText("1.0");

        controller.handlePayFine();

        assertEquals("Available", item.getStatus());
        assertEquals("", item.getBorrowedBy());
        assertEquals("", item.getDueDate());
        assertEquals(0.0, item.getFineAmount());
        assertEquals("✅ Fine fully paid. Item returned.", infoLabel.getText());
        assertEquals("", paymentField.getText());
    }

    // ===================== handleReturnBook =====================

    @Test
    void testHandleReturnBook_noSelection_showsWarning() {
        controller.handleReturnBook();
        assertEquals("⚠️ Select an item to return.", messageLabel.getText());
    }

    @Test
    void testHandleReturnBook_notUserItem_showsError() throws Exception {
        ObservableList<Media> mediaList = getMediaList();
        Media item = new Book("Clean Code", "Robert Martin", "111",
                "Borrowed", "", 0.0, "other", 0.0, 1);
        mediaList.add(item);
        bookTable.setItems(mediaList);
        bookTable.getSelectionModel().select(item);

        controller.handleReturnBook();

        assertEquals("❌ You can only return your own items.", messageLabel.getText());
    }

    @Test
    void testHandleReturnBook_success_whenNoFine() throws Exception {
        ObservableList<Media> mediaList = getMediaList();

        String future = LocalDate.now().plusDays(3)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Media item = new Book("Clean Code", "Robert Martin", "111",
                "Borrowed", future, 0.0, "u1", 0.0, 1);
        mediaList.add(item);
        bookTable.setItems(mediaList);
        bookTable.getSelectionModel().select(item);

        controller.handleReturnBook();

        assertEquals("Available", item.getStatus());
        assertEquals("", item.getBorrowedBy());
        assertEquals("", item.getDueDate());
        assertEquals("✅ Returned successfully!", messageLabel.getText());
    }

    @Test
    void testHandleReload_callsReloadBooksAndSetsInfoLabel() {
        controller.handleReload();
        assertEquals("🔄 Data reloaded.", infoLabel.getText());
    }
}
