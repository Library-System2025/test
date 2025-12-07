import static org.junit.jupiter.api.Assertions.*;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.util.Callback;
import org.junit.jupiter.api.*;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Integration tests for the LibrarianController.
 * Uses Reflection to access private FXML fields and methods.
 * 
 * @author Zainab
 * @version 1.2
 */
public class LibrarianControllerTest {

    private LibrarianController controller;

    @BeforeAll
    static void initToolkit() {
        try { Platform.startup(() -> {}); }
        catch (IllegalStateException e) { }
    }

    @BeforeEach
    void setUp() throws Exception {
        controller = new LibrarianController();

        File books = new File("books.txt");
        if (books.exists()) books.delete();
        File users = new File("users.txt");
        if (users.exists()) users.delete();
    }

    // ==========================================
    // Helper Methods (Reflection)
    // ==========================================

    private void injectField(String name, Object value) throws Exception {
        Field f = LibrarianController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, value);
    }

    private Object getPrivateField(String name) throws Exception {
        Field f = LibrarianController.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(controller);
    }

    private Object invokePrivate(String name, Class<?>[] types, Object... args) throws Exception {
        Method m = LibrarianController.class.getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(controller, args);
    }

    // ==========================================
    // Test Cases: handleBorrowBook
    // ==========================================

    /**
     * Verifies that attempting to borrow without selection shows a warning.
     */
    @Test
    void testHandleBorrowBook_noSelection_showsWarning() throws Exception {
        TableView<Media> table = new TableView<>();
        Label infoLabel = new Label();

        injectField("bookTable", table);
        injectField("infoLabel", infoLabel);

        invokePrivate("handleBorrowBook", new Class<?>[]{});   

        assertEquals("⚠️ Select an item to borrow.", infoLabel.getText());
    }

    /**
     * Verifies that attempting to borrow an already borrowed item shows an error.
     */
    @Test
    void testHandleBorrowBook_alreadyBorrowed_showsError() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");

        Media item = new Book("Clean Code", "Robert Martin", "111");
        item.setStatus("Borrowed");
        mediaList.add(item);

        TableView<Media> table = new TableView<>();
        table.setItems(mediaList);
        table.getSelectionModel().select(item);

        Label infoLabel = new Label();

        injectField("bookTable", table);
        injectField("infoLabel", infoLabel);

        invokePrivate("handleBorrowBook", new Class<?>[]{});

        assertEquals("❌ Item already borrowed.", infoLabel.getText());
    }

    /**
     * Verifies successful borrowing logic.
     */
    @Test
    void testHandleBorrowBook_success_setsStatusAndBorrower() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");

        Media item = new Book("Clean Code", "Robert Martin", "111");
        mediaList.add(item);

        TableView<Media> table = new TableView<>();
        table.setItems(mediaList);
        table.getSelectionModel().select(item);

        Label infoLabel = new Label();
        injectField("bookTable", table);
        injectField("infoLabel", infoLabel);

        controller.setCurrentUsername("lib1");

        invokePrivate("handleBorrowBook", new Class<?>[]{});

        assertEquals("Borrowed", item.getStatus());
        assertEquals("lib1", item.getBorrowedBy());
        assertNotNull(item.getDueDate());
        assertFalse(item.getDueDate().isEmpty());
        assertTrue(infoLabel.getText().startsWith("✅ Borrowed successfully"));
    }

    // ==========================================
    // Test Cases: handleReturnBook
    // ==========================================

    @Test
    void testHandleReturnBook_noSelection_showsWarning() throws Exception {
        TableView<Media> table = new TableView<>();
        Label infoLabel = new Label();

        injectField("bookTable", table);
        injectField("infoLabel", infoLabel);

        invokePrivate("handleReturnBook", new Class<?>[]{});

        assertEquals("⚠️ Select an item to return.", infoLabel.getText());
    }

    @Test
    void testHandleReturnBook_notBorrowed_showsInfo() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");

        Media item = new Book("Clean Code", "Robert Martin", "111");
        item.setStatus("Available");
        mediaList.add(item);

        TableView<Media> table = new TableView<>();
        table.setItems(mediaList);
        table.getSelectionModel().select(item);

        Label infoLabel = new Label();
        injectField("bookTable", table);
        injectField("infoLabel", infoLabel);

        invokePrivate("handleReturnBook", new Class<?>[]{});

        assertEquals("ℹ️ This item is not borrowed.", infoLabel.getText());
    }

    @Test
    void testHandleReturnBook_success_setsAvailable() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");

        Media item = new Book("Clean Code", "Robert Martin", "111");
        item.setStatus("Borrowed");
        mediaList.add(item);

        TableView<Media> table = new TableView<>();
        table.setItems(mediaList);
        table.getSelectionModel().select(item);

        Label infoLabel = new Label();
        injectField("bookTable", table);
        injectField("infoLabel", infoLabel);

        invokePrivate("handleReturnBook", new Class<?>[]{});

        assertEquals("Available", item.getStatus());
        assertEquals("✅ Item returned successfully.", infoLabel.getText());
    }

    // ==========================================
    // Test Cases: handleSearch
    // ==========================================

    @Test
    void testHandleSearch_emptyKeyword_showsAll() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");

        mediaList.add(new Book("Clean Code", "Robert Martin", "111"));
        mediaList.add(new Book("Effective Java", "Joshua Bloch", "222"));

        TableView<Media> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList());
        TextField searchField = new TextField("");

        injectField("bookTable", table);
        injectField("searchField", searchField);

        invokePrivate("handleSearch", new Class<?>[]{});

        assertEquals(mediaList, table.getItems());
    }

    @Test
    void testHandleSearch_filtersByTitle() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");

        mediaList.add(new Book("Clean Code", "Robert Martin", "111"));
        mediaList.add(new Book("Effective Java", "Joshua Bloch", "222"));

        TableView<Media> table = new TableView<>();
        TextField searchField = new TextField("clean");

        injectField("bookTable", table);
        injectField("searchField", searchField);

        invokePrivate("handleSearch", new Class<?>[]{});

        ObservableList<Media> result = table.getItems();
        assertEquals(1, result.size());
        assertEquals("Clean Code", result.get(0).getTitle());
    }

    /**
     * إضافي: بحث باستخدام ISBN فقط.
     */
    @Test
    void testHandleSearch_filtersByIsbn() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");

        mediaList.add(new Book("Clean Code", "Robert Martin", "111"));
        mediaList.add(new Book("Another Book", "X", "999"));

        TableView<Media> table = new TableView<>();
        TextField searchField = new TextField("999");

        injectField("bookTable", table);
        injectField("searchField", searchField);

        invokePrivate("handleSearch", new Class<?>[]{});

        ObservableList<Media> result = table.getItems();
        assertEquals(1, result.size());
        assertEquals("999", result.get(0).getIsbn());
    }

    // ==========================================
    // Test Cases: Private Helpers & Initialization
    // ==========================================

    @Test
    void testGetUserMembership_fileMissing_returnsSilver() throws Exception {
        String membership = (String) invokePrivate(
                "getUserMembership",
                new Class<?>[]{String.class},
                "u1"
        );
        assertEquals("Silver", membership);
    }

    @Test
    void testGetUserMembership_readsFromFile() throws Exception {
        try (PrintWriter out = new PrintWriter(new FileWriter("users.txt"))) {
            out.println("u1,1,User,Gold,user1@mail.com");
            out.println("u2,2,User,Platinum,user2@mail.com");
        }

        String membership1 = (String) invokePrivate(
                "getUserMembership",
                new Class<?>[]{String.class},
                "u1"
        );
        String membership2 = (String) invokePrivate(
                "getUserMembership",
                new Class<?>[]{String.class},
                "u2"
        );

        assertEquals("Gold", membership1);
        assertEquals("Platinum", membership2);
    }

    /**
     * يتأكد أن initialize يضبط الأعمدة، الـ items، والـ rowFactory.
     */
    @Test
    void testInitialize_setsUpTableAndRowFactory() throws Exception {
        TableView<Media> table = new TableView<>();

        injectField("bookTable", table);
        injectField("typeColumn", new TableColumn<Media, String>("Type"));
        injectField("titleColumn", new TableColumn<Media, String>("Title"));
        injectField("authorColumn", new TableColumn<Media, String>("Author"));
        injectField("isbnColumn", new TableColumn<Media, String>("ISBN"));
        injectField("statusColumn", new TableColumn<Media, String>("Status"));
        injectField("dueDateColumn", new TableColumn<Media, String>("Due"));
        injectField("borrowedByColumn", new TableColumn<Media, String>("By"));
        injectField("copyIdColumn", new TableColumn<Media, Integer>("CopyId"));

        controller.initialize();

        assertNotNull(table.getItems());
        assertNotNull(table.getRowFactory());
    }

    /**
     * يغطي تلوين الصفوف حسب الـ status في الـ rowFactory.
     */
    @Test
    void testRowFactory_coloringByStatus() throws Exception {
        TableView<Media> table = new TableView<>();

        injectField("bookTable", table);
        injectField("typeColumn", new TableColumn<Media, String>("Type"));
        injectField("titleColumn", new TableColumn<Media, String>("Title"));
        injectField("authorColumn", new TableColumn<Media, String>("Author"));
        injectField("isbnColumn", new TableColumn<Media, String>("ISBN"));
        injectField("statusColumn", new TableColumn<Media, String>("Status"));
        injectField("dueDateColumn", new TableColumn<Media, String>("Due"));
        injectField("borrowedByColumn", new TableColumn<Media, String>("By"));
        injectField("copyIdColumn", new TableColumn<Media, Integer>("CopyId"));

        controller.initialize();

        Callback<TableView<Media>, TableRow<Media>> factory = table.getRowFactory();
        assertNotNull(factory);

        TableRow<Media> row = factory.call(table);
        Method updateItem = javafx.scene.control.Cell.class
                .getDeclaredMethod("updateItem", Object.class, boolean.class);
        updateItem.setAccessible(true);

        updateItem.invoke(row, new Book("A","B","1","Available","",0.0,"",0.0,1), false);
        assertEquals("-fx-background-color: #d4edda;", row.getStyle());

        updateItem.invoke(row, new Book("A","B","1","Borrowed","",0.0,"",0.0,1), false);
        assertEquals("-fx-background-color: #fff3cd;", row.getStyle());

        updateItem.invoke(row, new Book("A","B","1","Overdue","",0.0,"",0.0,1), false);
        assertEquals("-fx-background-color: #f8d7da;", row.getStyle());

        updateItem.invoke(row, null, true);
        assertEquals("", row.getStyle());
    }

    /**
     * يتأكد أن handleReload يعيد تحميل البيانات ويحدث الرسالة.
     */
    @Test
    void testHandleReload_loadsFromFileAndUpdatesLabel() throws Exception {

        try (PrintWriter out = new PrintWriter(new FileWriter("books.txt"))) {
            out.println("Book,Clean Code,Robert Martin,111,1,Borrowed,2025-12-20,0.0,lib1,0.0");
        }

        TableView<Media> table = new TableView<>();
        Label infoLabel = new Label();

        injectField("bookTable", table);
        injectField("typeColumn", new TableColumn<>("Type"));
        injectField("titleColumn", new TableColumn<>("Title"));
        injectField("authorColumn", new TableColumn<>("Author"));
        injectField("isbnColumn", new TableColumn<>("ISBN"));
        injectField("statusColumn", new TableColumn<>("Status"));
        injectField("dueDateColumn", new TableColumn<>("Due"));
        injectField("borrowedByColumn", new TableColumn<>("By"));
        injectField("copyIdColumn", new TableColumn<>("CopyId"));
        injectField("infoLabel", infoLabel);

        controller.initialize();
        controller.handleReload();

        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");

        assertFalse(mediaList.isEmpty(), "mediaList should be loaded from file");
        assertEquals("🔄 Data reloaded.", infoLabel.getText());
        assertEquals(1, ((Book) mediaList.get(0)).getCopyId());
    }

    /**
     * يغطي فرع حساب الغرامة لـ Book متأخر + membership من الملف.
     */
    @Test
    void testLoadMediaFromFile_overdueBook_calculatesFine() throws Exception {
        try (PrintWriter out = new PrintWriter(new FileWriter("users.txt"))) {
            out.println("lib1,123,User,Gold,lib1@mail.com");
        }

        try (PrintWriter out = new PrintWriter(new FileWriter("books.txt"))) {
            out.println("Book,Clean Code,Robert Martin,111,1,Borrowed,2024-01-01,0.0,lib1,0.0");
        }

        TableView<Media> table = new TableView<>();
        injectField("bookTable", table);
        injectField("typeColumn", new TableColumn<>("Type"));
        injectField("titleColumn", new TableColumn<>("Title"));
        injectField("authorColumn", new TableColumn<>("Author"));
        injectField("isbnColumn", new TableColumn<>("ISBN"));
        injectField("statusColumn", new TableColumn<>("Status"));
        injectField("dueDateColumn", new TableColumn<>("Due"));
        injectField("borrowedByColumn", new TableColumn<>("By"));
        injectField("copyIdColumn", new TableColumn<>("CopyId"));

        controller.initialize();

        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");

        assertEquals(1, mediaList.size());
        Media m = mediaList.get(0);

        assertEquals("Overdue", m.getStatus());
        assertTrue(m.getFineAmount() > 0.0, "fineAmount should be > 0 for overdue item");
    }

    /**
     * يغطي فرع تحميل CD من الملف + borrowedBy = "0.0" والـ amountPaid.
     */
    @Test
    void testLoadMediaFromFile_loadsCD_andNormalizesBorrowedBy() throws Exception {
        try (PrintWriter out = new PrintWriter(new FileWriter("books.txt"))) {
            // type=CD, borrowedBy=0.0, amountPaid=5.5
            out.println("CD,Top Hits,Artist,999,2,Available,,0.0,0.0,5.5");
        }

        TableView<Media> table = new TableView<>();
        injectField("bookTable", table);
        injectField("typeColumn", new TableColumn<>("Type"));
        injectField("titleColumn", new TableColumn<>("Title"));
        injectField("authorColumn", new TableColumn<>("Author"));
        injectField("isbnColumn", new TableColumn<>("ISBN"));
        injectField("statusColumn", new TableColumn<>("Status"));
        injectField("dueDateColumn", new TableColumn<>("Due"));
        injectField("borrowedByColumn", new TableColumn<>("By"));
        injectField("copyIdColumn", new TableColumn<>("CopyId"));

        controller.initialize();

        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");

        assertEquals(1, mediaList.size());
        Media m = mediaList.get(0);
        assertTrue(m instanceof CD);
        assertEquals("Top Hits", m.getTitle());
        assertEquals(2, m.getCopyId());
        assertEquals("", m.getBorrowedBy(), "borrowedBy should be cleared when equal to 0.0");
    }

    /**
     * يتأكد أن setCurrentUsername يحدّث الـ label والـ accountUsername.
     */
    @Test
    void testSetCurrentUsername_updatesLabelAndField() throws Exception {
        Label welcome = new Label();
        injectField("welcomeLabel", welcome);

        controller.setCurrentUsername("LibrarianX");

        assertEquals("Welcome, LibrarianX 👋", welcome.getText());

        Field f = LibrarianController.class.getDeclaredField("accountUsername");
        f.setAccessible(true);
        String stored = (String) f.get(controller);
        assertEquals("LibrarianX", stored);
    }
}
