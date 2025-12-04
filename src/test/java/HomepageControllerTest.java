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
 * Integration tests for the HomepageController (Admin Dashboard).
 * <p>
 * This class uses Java Reflection to access private fields and methods
 * within the controller to ensure comprehensive testing of logic
 * without exposing internal members.
 * </p>
 * 
 * @author Zainab
 * @version 1.1
 */
public class HomepageControllerTest {

    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // JavaFX toolkit already initialized
        }
    }

    private homepageController controller;

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

        injectField("mediaList", FXCollections.observableArrayList());
        injectField("usersList", FXCollections.observableArrayList());

        File books = new File("books.txt");
        if (books.exists()) books.delete();
        File users = new File("users.txt");
        if (users.exists()) users.delete();
    }

    @AfterEach
    void tearDown() {
        new File("books.txt").delete();
        new File("users.txt").delete();
    }

    /**
     * Verifies that a valid book is added successfully and that
     * a second book with same ISBN but different title/author is rejected.
     */
    @Test
    void testHandleAddBook_validBook_addedOnce_andRejectsDifferentTitleForSameIsbn()
            throws Exception {

        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList("Book", "CD"));
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

        // أول إضافة
        controller.handleAddBook();

        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList =
                (ObservableList<Media>) getPrivateField("mediaList");

        assertEquals(1, mediaList.size(), "List should have 1 item");
        assertEquals("111", mediaList.get(0).getIsbn());
        // ✅ الرسالة الجديدة للنسخة الأولى
        assertEquals("📗 New Book Added (Copy #1).", addBookMessage.getText());

        // محاولة إضافة بنفس ISBN لكن بعنوان/مؤلف مختلف → مرفوض
        titleField.setText("Another Title");
        authorField.setText("Another Author");
        isbnField.setText("111");

        controller.handleAddBook();

        assertEquals("❌ ISBN already exists but with different title/author!",
                     addBookMessage.getText());
        assertEquals(1, mediaList.size(), "List size should remain 1");
    }

    /**
     * Verifies that adding a second copy with the same title/author/ISBN
     * is allowed and increases mediaList size and copyId.
     */
    @Test
    void testHandleAddBook_secondCopy_sameBook_allowed() throws Exception {
        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList("Book", "CD"));
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

        // النسخة الأولى
        controller.handleAddBook();

        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList =
                (ObservableList<Media>) getPrivateField("mediaList");

        assertEquals(1, mediaList.size());
        Media first = mediaList.get(0);
        assertEquals(1, first.getCopyId());

        // النسخة الثانية، نفس العنوان والمؤلف و ISBN
     // بعد أول handleAddBook()
        controller.handleAddBook();

        // رجّعي تعبي الفيلدز قبل النداء الثاني
        titleField.setText("Clean Code");
        authorField.setText("Robert Martin");
        isbnField.setText("111");

        controller.handleAddBook();

        assertEquals(2, mediaList.size(), "Should now have 2 copies");
        Media second = mediaList.get(1);
        assertEquals("111", second.getIsbn());
        assertEquals("Clean Code", second.getTitle());
        assertEquals("Robert Martin", second.getAuthor());
        assertEquals(2, second.getCopyId(), "Second copy should have copyId=2");

        assertEquals("📚 Added NEW COPY (Copy #2) of this book.",
                     addBookMessage.getText());
    }

    @Test
    void testHandleAddBook_missingFields_showsError() throws Exception {
        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList("Book"));
        typeCombo.getSelectionModel().select("Book");

        injectField("typeCombo", typeCombo);
        injectField("titleField", new TextField(""));   // empty title
        injectField("authorField", new TextField("Author"));
        injectField("isbnField", new TextField("111"));

        Label msgLabel = new Label();
        injectField("addBookMessage", msgLabel);

        controller.handleAddBook();

        assertEquals("❗ Please fill all fields.", msgLabel.getText());
    }

    @Test
    void testHandleSearch_emptyKeyword_showsAllItems() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList =
                (ObservableList<Media>) getPrivateField("mediaList");

        mediaList.add(new Book("Clean Code", "Robert", "111"));
        mediaList.add(new Book("Java FX", "Author", "222"));

        TableView<Media> table = new TableView<>();
        TextField searchField = new TextField("");
        ComboBox<String> searchBy =
                new ComboBox<>(FXCollections.observableArrayList("All"));
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

    @Test
    void testLoadUsersFromFile_readsNonAdminUsers() throws Exception {
        try (PrintWriter out = new PrintWriter(new FileWriter("users.txt"))) {
            out.println("admin,123,Admin,Gold,admin@mail.com");
            out.println("user1,1,User,Silver,user1@mail.com");
        }

        TableView<User> usersTable = new TableView<>();
        injectField("usersTable", usersTable);

        invokePrivate("loadUsersFromFile", new Class<?>[] {});

        @SuppressWarnings("unchecked")
        ObservableList<User> usersList =
                (ObservableList<User>) getPrivateField("usersList");

        assertEquals(1, usersList.size());
        assertEquals("user1", usersList.get(0).getUsername());
    }

    @Test
    void testHandleReload_loadsFromFileAndUpdatesMessage() throws Exception {
        // سطر بالـ format الجديد: type,title,author,isbn,copyId,status,dueDate,fine,borrowedBy,amountPaid
        try (PrintWriter out = new PrintWriter(new FileWriter("books.txt"))) {
            out.println("Book,Clean Code,Robert Martin,111,1,Borrowed,2025-12-20,0.0,u1,0.0");
        }

        TableView<Media> table = new TableView<>();
        Label addBookMessage = new Label();

        injectField("searchResultsTable", table);
        injectField("addBookMessage", addBookMessage);

        controller.handleReload();

        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList =
                (ObservableList<Media>) getPrivateField("mediaList");

        assertFalse(mediaList.isEmpty(), "Media list should be populated from file");
        assertEquals("Clean Code", mediaList.get(0).getTitle());
        assertEquals("🔄 Reloaded.", addBookMessage.getText());
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

        assertEquals(1, usersList.size(),
                "User should NOT be deleted if they have active loans");
    }

    @Test
    void testHandleDeleteUser_success() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<User> usersList =
                (ObservableList<User>) getPrivateField("usersList");

        User u1 = new User("u1", "1", "User", "Gold");
        usersList.add(u1);

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

        try (BufferedReader reader = new BufferedReader(new FileReader("users.txt"))) {
            assertEquals("m,123,Admin,Gold", reader.readLine());
            assertNull(reader.readLine(), "Should be no more lines");
        }
    }
}
