import static org.junit.jupiter.api.Assertions.*;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import org.junit.jupiter.api.*;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Integration tests for homepageController (Admin Dashboard).
 * Uses Reflection to access private fields and methods for testing.
 */
public class HomepageControllerTest {

    // 1. Initialize JavaFX Toolkit ONCE to avoid "Toolkit not initialized" or "IllegalStateException"
    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // JavaFX is already initialized, which is fine.
        }
    }

    private homepageController controller;

    // ==========================================
    // Reflection Helpers
    // ==========================================

    private void injectField(String name, Object value) throws Exception {
        // NOTE: The string 'name' must match the variable name in homepageController.java EXACTLY.
        Field f = homepageController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, value);
    }

    private Object getPrivateField(String name) throws Exception {
        Field f = homepageController.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(controller);
    }

    private Object invokePrivate(String name, Class<?>[] types, Object... args) throws Exception {
        Method m = homepageController.class.getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(controller, args);
    }

    // ==========================================
    // Test Setup
    // ==========================================

    @BeforeEach
    void setUp() throws Exception {
        controller = new homepageController();

        // Initialize Lists using Reflection to prevent NullPointerExceptions
        injectField("mediaList", FXCollections.observableArrayList());
        injectField("usersList", FXCollections.observableArrayList());

        // Clean up files
        File books = new File("books.txt");
        if (books.exists()) books.delete();
        File users = new File("users.txt");
        if (users.exists()) users.delete();
    }

    @AfterEach
    void tearDown() {
        // Optional: clean up created files
        new File("books.txt").delete();
        new File("users.txt").delete();
    }

    // ==========================================
    // Test: Adding Books
    // ==========================================

    @Test
    void testHandleAddBook_validBook_addedOnce() throws Exception {
        // UI Setup
        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList("Book", "CD"));
        typeCombo.getSelectionModel().select("Book");

        TextField titleField = new TextField("Clean Code");
        TextField authorField = new TextField("Robert Martin");
        TextField isbnField = new TextField("111");
        Label addBookMessage = new Label();

        // Inject UI components
        injectField("typeCombo", typeCombo);
        injectField("titleField", titleField);
        injectField("authorField", authorField);
        injectField("isbnField", isbnField);
        injectField("addBookMessage", addBookMessage);

        // --- First Add ---
        controller.handleAddBook();

        // Verify First Add
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");
        assertEquals(1, mediaList.size(), "List should have 1 item");
        assertEquals("111", mediaList.get(0).getIsbn());
        assertEquals("✅ Book added successfully.", addBookMessage.getText());

        // --- Second Add (Duplicate Attempt) ---
        titleField.setText("Another Title");
        authorField.setText("Another Author");
        isbnField.setText("111"); // SAME ISBN

        controller.handleAddBook();

        // Verify Duplicate Rejection
        assertEquals("❌ Item with this ISBN exists.", addBookMessage.getText());
        assertEquals(1, mediaList.size(), "List size should remain 1");
    }

    @Test
    void testHandleAddBook_missingFields_showsError() throws Exception {
        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList("Book"));
        typeCombo.getSelectionModel().select("Book");

        injectField("typeCombo", typeCombo);
        injectField("titleField", new TextField("")); // Empty
        injectField("authorField", new TextField("Author"));
        injectField("isbnField", new TextField("111"));

        Label msgLabel = new Label();
        injectField("addBookMessage", msgLabel);

        controller.handleAddBook();

        assertEquals("❗ Please fill all fields.", msgLabel.getText());
    }

    // ==========================================
    // Test: Search
    // ==========================================

    @Test
    void testHandleSearch_emptyKeyword_showsAllItems() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");
        mediaList.add(new Book("Clean Code", "Robert", "111"));
        mediaList.add(new Book("Java FX", "Author", "222"));

        TableView<Media> table = new TableView<>();
        TextField searchField = new TextField("");
        ComboBox<String> searchBy = new ComboBox<>(FXCollections.observableArrayList("All"));
        searchBy.getSelectionModel().select("All");
        Label msg = new Label();

        injectField("searchResultsTable", table);
        injectField("searchField", searchField);
        injectField("searchByCombo", searchBy);
        injectField("addBookMessage", msg);

        controller.handleSearch();

        assertEquals(2, table.getItems().size());
        assertEquals("📚 Showing all items.", msg.getText());
    }

    // ==========================================
    // Test: User Management (Load/Delete)
    // ==========================================

    @Test
    void testLoadUsersFromFile_readsNonAdminUsers() throws Exception {
        // Create dummy users file
        try (PrintWriter out = new PrintWriter(new FileWriter("users.txt"))) {
            out.println("admin,123,Admin,Gold,admin@mail.com");
            out.println("user1,1,User,Silver,user1@mail.com");
        }

        TableView<User> usersTable = new TableView<>();
        injectField("usersTable", usersTable);

        // استدعاء الميثود الخاصة
        invokePrivate("loadUsersFromFile", new Class<?>[] {});

        @SuppressWarnings("unchecked")
        ObservableList<User> usersList = (ObservableList<User>) getPrivateField("usersList");

        // التأكد من أن القائمة تحتوي على المستخدم العادي
        assertEquals(1, usersList.size());
        assertEquals("user1", usersList.get(0).getUsername());
    }

    // ==========================================
    // Test: Reload Data
    // ==========================================

    @Test
    void testHandleReload_loadsFromFileAndUpdatesMessage() throws Exception {
        // إنشاء ملف كتب وهمي
        try (PrintWriter out = new PrintWriter(new FileWriter("books.txt"))) {
            // التنسيق: Type,Title,Author,ISBN,Status,DueDate,Fine,User,Rating
            out.println("Book,Clean Code,Robert Martin,111,Borrowed,2025-12-20,0.0,u1,0.0");
        }

        TableView<Media> table = new TableView<>();
        Label addBookMessage = new Label();

        injectField("searchResultsTable", table);
        injectField("addBookMessage", addBookMessage);

        // تنفيذ عملية التحديث
        controller.handleReload();

        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");

        assertFalse(mediaList.isEmpty(), "Media list should be populated from file");
        assertEquals("Clean Code", mediaList.get(0).getTitle());
        assertEquals("🔄 Reloaded.", addBookMessage.getText());
    }

    // ==========================================
    // Test: Delete User
    // ==========================================

    @Test
    void testHandleDeleteUser_userHasLoans_notDeleted() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<User> usersList = (ObservableList<User>) getPrivateField("usersList");
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");

        User u1 = new User("u1", "1", "User", "Gold");
        usersList.add(u1);

        // إضافة كتاب مستعار بواسطة هذا المستخدم
        Media m = new Book("Clean Code", "Robert", "111");
        m.setBorrowedBy("u1");
        mediaList.add(m);

        TableView<User> usersTable = new TableView<>();
        usersTable.setItems(usersList);
        usersTable.getSelectionModel().select(u1);

        injectField("usersTable", usersTable);

        controller.handleDeleteUser();

        assertEquals(1, usersList.size(), "User should NOT be deleted if they have active loans");
    }

    @Test
    void testHandleDeleteUser_success() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<User> usersList = (ObservableList<User>) getPrivateField("usersList");

        User u1 = new User("u1", "1", "User", "Gold");
        usersList.add(u1);

        // تجهيز الملف للتأكد من عملية الحذف والكتابة
        try (PrintWriter out = new PrintWriter(new FileWriter("users.txt"))) {
            out.println("m,123,Admin,Gold");
            out.println("u1,1,User,Gold");
        }

        TableView<User> usersTable = new TableView<>();
        usersTable.setItems(usersList);
        usersTable.getSelectionModel().select(u1);

        injectField("usersTable", usersTable);

        controller.handleDeleteUser();

        assertTrue(usersList.isEmpty(), "User list should be empty after deletion");

        // التأكد من الملف أنه تم حذف المستخدم وبقي الأدمن فقط
        try (BufferedReader reader = new BufferedReader(new FileReader("users.txt"))) {
            assertEquals("m,123,Admin,Gold", reader.readLine());
            assertNull(reader.readLine(), "Should be no more lines");
        }
    }
}
