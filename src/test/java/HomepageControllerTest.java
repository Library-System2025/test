import static org.junit.jupiter.api.Assertions.*;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import org.junit.jupiter.api.*;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class HomepageControllerTest {

    @BeforeAll
    static void initToolkit() {
        try { Platform.startup(() -> {}); }
        catch (IllegalStateException e) { }
    }

    private homepageController controller;

    // ====== helpers لسهولة التعامل مع الحقول private والـ methods private ======

    private void injectField(String name, Object value) throws Exception {
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

    @BeforeEach
    void setUp() throws Exception {
        controller = new homepageController();

        // ننظّف الملفات عشان كل test يبدأ من الصفر
        File books = new File("books.txt");
        if (books.exists()) books.delete();
        File users = new File("users.txt");
        if (users.exists()) users.delete();
    }

    // ================== handleAddBook ==================

    @Test
    void testHandleAddBook_missingFields_showsError() throws Exception {
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.setItems(FXCollections.observableArrayList("Book", "CD"));
        typeCombo.getSelectionModel().select("Book");

        TextField titleField = new TextField(""); // فاضي
        TextField authorField = new TextField("Author");
        TextField isbnField = new TextField("111");
        Label addBookMessage = new Label();

        injectField("typeCombo", typeCombo);
        injectField("titleField", titleField);
        injectField("authorField", authorField);
        injectField("isbnField", isbnField);
        injectField("addBookMessage", addBookMessage);

        controller.handleAddBook();

        assertEquals("❗ Please fill all fields.", addBookMessage.getText());
    }

    @Test
    void testHandleAddBook_validBook_addedOnce() throws Exception {
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.setItems(FXCollections.observableArrayList("Book", "CD"));
        typeCombo.getSelectionModel().select("Book");

        TextField titleField = new TextField("Clean Code");
        TextField authorField = new TextField("Robert Martin");
        TextField isbnField = new TextField("111");
        Label addBookMessage = new Label();

        injectField("typeCombo", typeCombo);
        injectField("titleField", titleField);
        injectField("authorField", authorField);
        injectField("isbnField", isbnField);
        injectField("addBookMessage", addBookMessage);

        // أول مرة: المفروض يضيف الكتاب
        controller.handleAddBook();

        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList =
                (ObservableList<Media>) getPrivateField("mediaList");

        assertEquals(1, mediaList.size());
        assertEquals("111", mediaList.get(0).getIsbn());
        assertEquals("Clean Code", mediaList.get(0).getTitle());
        assertEquals("✅ Book added successfully.", addBookMessage.getText());

        // ثاني مرة بنفس الـ ISBN → رسالة "exists"
        titleField.setText("Another");
        authorField.setText("Someone");
        isbnField.setText("111");

        controller.handleAddBook();
        assertEquals("❌ Item with this ISBN exists.", addBookMessage.getText());
        assertEquals(1, mediaList.size(), "Should still have only one item");
    }

    // ================== handleSearch ==================

    @Test
    void testHandleSearch_emptyKeyword_showsAllItems() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList =
                (ObservableList<Media>) getPrivateField("mediaList");

        mediaList.add(new Book("Clean Code", "Robert Martin", "111"));
        mediaList.add(new Book("Effective Java", "Joshua Bloch", "222"));

        TableView<Media> table = new TableView<>();
        TextField searchField = new TextField(""); // فاضي
        ComboBox<String> searchByCombo = new ComboBox<>();
        searchByCombo.setItems(FXCollections.observableArrayList("All", "Title", "Author", "ISBN"));
        searchByCombo.getSelectionModel().select("All");
        Label addBookMessage = new Label();

        injectField("searchResultsTable", table);
        injectField("searchField", searchField);
        injectField("searchByCombo", searchByCombo);
        injectField("addBookMessage", addBookMessage);

        controller.handleSearch();

        assertSame(mediaList, table.getItems(), "Table items should reference full mediaList");
        assertEquals("📚 Showing all items.", addBookMessage.getText());
    }

    @Test
    void testHandleSearch_byTitle_filtersCorrectly() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList =
                (ObservableList<Media>) getPrivateField("mediaList");

        mediaList.add(new Book("Clean Code", "Robert Martin", "111"));
        mediaList.add(new Book("Effective Java", "Joshua Bloch", "222"));

        TableView<Media> table = new TableView<>();
        TextField searchField = new TextField("clean");
        ComboBox<String> searchByCombo = new ComboBox<>();
        searchByCombo.setItems(FXCollections.observableArrayList("All", "Title", "Author", "ISBN"));
        searchByCombo.getSelectionModel().select("Title");
        Label addBookMessage = new Label();

        injectField("searchResultsTable", table);
        injectField("searchField", searchField);
        injectField("searchByCombo", searchByCombo);
        injectField("addBookMessage", addBookMessage);

        controller.handleSearch();

        ObservableList<Media> result = table.getItems();
        assertEquals(1, result.size());
        assertEquals("Clean Code", result.get(0).getTitle());
    }

    // ================== getUserMembership ==================

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

    // ================== getUserEmail ==================

    @Test
    void testGetUserEmail_missingOrEmpty_returnsEmpty() throws Exception {
        String email1 = (String) invokePrivate(
                "getUserEmail",
                new Class<?>[]{String.class},
                "u1"
        );
        String email2 = (String) invokePrivate(
                "getUserEmail",
                new Class<?>[]{String.class},
                ""
        );
        assertEquals("", email1);
        assertEquals("", email2);
    }

    @Test
    void testGetUserEmail_readsFromFile() throws Exception {
        try (PrintWriter out = new PrintWriter(new FileWriter("users.txt"))) {
            out.println("u1,1,User,Gold,user1@mail.com");
            out.println("u2,2,User,Silver,user2@mail.com");
        }

        String email = (String) invokePrivate(
                "getUserEmail",
                new Class<?>[]{String.class},
                "u1"
        );

        assertEquals("user1@mail.com", email);
    }

    // ================== loadUsersFromFile (private) ==================

    @Test
    void testLoadUsersFromFile_readsNonAdminUsers() throws Exception {
        try (PrintWriter out = new PrintWriter(new FileWriter("users.txt"))) {
            out.println("m,123,Admin,Gold");
            out.println("u1,1,User,Gold,user1@mail.com");
        }

        invokePrivate("loadUsersFromFile", new Class<?>[]{});

        @SuppressWarnings("unchecked")
        ObservableList<User> usersList =
                (ObservableList<User>) getPrivateField("usersList");

        assertEquals(1, usersList.size());
        assertEquals("u1", usersList.get(0).getUsername());
    }

    // ================== handleReload ==================

    @Test
    void testHandleReload_loadsFromFileAndUpdatesMessage() throws Exception {
        // نكتب كتاب للملف
        try (PrintWriter out = new PrintWriter(new FileWriter("books.txt"))) {
            out.println("Book,Clean Code,Robert Martin,111,Borrowed,2025-12-20,0.0,u1,0.0");
        }

        TableView<Media> table = new TableView<>();
        Label addBookMessage = new Label();

        injectField("searchResultsTable", table);
        injectField("addBookMessage", addBookMessage);

        // نربط الجدول مع mediaList الداخلي
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList =
                (ObservableList<Media>) getPrivateField("mediaList");
        table.setItems(mediaList);

        controller.handleReload();

        assertFalse(mediaList.isEmpty(), "mediaList should be loaded from file");
        assertEquals("🔄 Reloaded.", addBookMessage.getText());
    }

    // ================== handleDeleteUser ==================

    @Test
    void testHandleDeleteUser_noSelection_doesNotChangeList() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<User> usersList =
                (ObservableList<User>) getPrivateField("usersList");

        usersList.add(new User("u1", "1", "User", "Gold"));

        TableView<User> usersTable = new TableView<>();
        usersTable.setItems(usersList); // بدون selection

        injectField("usersTable", usersTable);

        controller.handleDeleteUser();

        // لسه فيه يوزر لأن مافي selection
        assertEquals(1, usersList.size());
    }

    @Test
    void testHandleDeleteUser_userHasLoans_notDeleted() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<User> usersList =
                (ObservableList<User>) getPrivateField("usersList");
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList =
                (ObservableList<Media>) getPrivateField("mediaList");

        User u1 = new User("u1", "1", "User", "Gold");
        usersList.add(u1);

        Media m = new Book("Clean Code", "Robert", "111");
        m.setBorrowedBy("u1");
        mediaList.add(m);

        TableView<User> usersTable = new TableView<>();
        usersTable.setItems(usersList);
        usersTable.getSelectionModel().select(u1);

        injectField("usersTable", usersTable);

        controller.handleDeleteUser();

        // ما انمسح لأنه عنده كتب
        assertEquals(1, usersList.size());
    }

    @Test
    void testHandleDeleteUser_success_removesUserAndWritesFile() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<User> usersList =
                (ObservableList<User>) getPrivateField("usersList");

        User u1 = new User("u1", "1", "User", "Gold");
        usersList.add(u1);

        TableView<User> usersTable = new TableView<>();
        usersTable.setItems(usersList);
        usersTable.getSelectionModel().select(u1);

        injectField("usersTable", usersTable);

        controller.handleDeleteUser();

        assertTrue(usersList.isEmpty(), "User list should be empty after delete");

        // نتأكد من ملف users.txt
        try (BufferedReader reader = new BufferedReader(new FileReader("users.txt"))) {
            String firstLine = reader.readLine();
            assertEquals("m,123,Admin,Gold", firstLine);
            assertNull(reader.readLine(), "No extra user lines expected");
        }
    }

    // ================== handleSendReminder ==================

    @Test
    void testHandleSendReminder_noSelection_doesNotThrow() throws Exception {
        TableView<User> usersTable = new TableView<>();
        injectField("usersTable", usersTable);

        // بس نتأكد إنه ما يكرّش
        controller.handleSendReminder();
    }

    @Test
    void testHandleSendReminder_userWithoutEmail_showsErrorAndReturns() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<User> usersList =
                (ObservableList<User>) getPrivateField("usersList");

        User u1 = new User("u1", "1", "User", "Gold");
        usersList.add(u1);

        // users.txt بدون إيميل
        try (PrintWriter out = new PrintWriter(new FileWriter("users.txt"))) {
            out.println("u1,1,User,Gold");
        }

        TableView<User> usersTable = new TableView<>();
        usersTable.setItems(usersList);
        usersTable.getSelectionModel().select(u1);

        injectField("usersTable", usersTable);

        // المفروض يوقف بعد ما يكتشف إنه ما في إيميل
        controller.handleSendReminder();

        // لا تغيير على القائمة
        assertEquals(1, usersList.size());
    }

    @Test
    void testHandleSendReminder_userHasNoOverdue_books() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<User> usersList =
                (ObservableList<User>) getPrivateField("usersList");
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList =
                (ObservableList<Media>) getPrivateField("mediaList");

        User u1 = new User("u1", "1", "User", "Gold");
        usersList.add(u1);

        // users.txt مع إيميل
        try (PrintWriter out = new PrintWriter(new FileWriter("users.txt"))) {
            out.println("u1,1,User,Gold,u1@mail.com");
        }

        // كتاب مش متأخر (due بالمستقبل)
        Media m = new Book("Clean Code", "Robert", "111");
        m.setBorrowedBy("u1");
        m.setStatus("Borrowed");
        m.setDueDate("2999-12-31"); // مستقبل → مش Overdue
        mediaList.add(m);

        TableView<User> usersTable = new TableView<>();
        usersTable.setItems(usersList);
        usersTable.getSelectionModel().select(u1);

        injectField("usersTable", usersTable);

        controller.handleSendReminder();

        // لسه ما في أي تعديل، بس المهم إنه الكود مشى على فرع
        assertEquals(1, usersList.size());
        assertEquals(1, mediaList.size());
    }
}
