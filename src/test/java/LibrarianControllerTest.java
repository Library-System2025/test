import static org.junit.jupiter.api.Assertions.*;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import org.junit.jupiter.api.*;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class LibrarianControllerTest {

    @BeforeAll
    static void initToolkit() {
        try { Platform.startup(() -> {}); }
        catch (IllegalStateException e) { }
    }

    private LibrarianController controller;

    // ===== Helpers عامة =====

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

    @BeforeEach
    void setUp() throws Exception {
        controller = new LibrarianController();

        // ننضف الملفات اللي الكلاس بستخدمها
        File books = new File("books.txt");
        if (books.exists()) books.delete();
        File users = new File("users.txt");
        if (users.exists()) users.delete();
    }

    // ================== handleBorrowBook ==================

    @Test
    void testHandleBorrowBook_noSelection_showsWarning() throws Exception {
        TableView<Media> table = new TableView<>();
        Label infoLabel = new Label();

        injectField("bookTable", table);
        injectField("infoLabel", infoLabel);

        // مافي selection
        invokePrivate("handleBorrowBook", new Class<?>[]{});   // يعادل controller.handleBorrowBook()

        assertEquals("⚠️ Select an item to borrow.", infoLabel.getText());
    }

    @Test
    void testHandleBorrowBook_alreadyBorrowed_showsError() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList =
                (ObservableList<Media>) getPrivateField("mediaList");

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

    @Test
    void testHandleBorrowBook_success_setsStatusAndBorrower() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList =
                (ObservableList<Media>) getPrivateField("mediaList");

        Media item = new Book("Clean Code", "Robert Martin", "111");
        mediaList.add(item);

        TableView<Media> table = new TableView<>();
        table.setItems(mediaList);
        table.getSelectionModel().select(item);

        Label infoLabel = new Label();
        injectField("bookTable", table);
        injectField("infoLabel", infoLabel);

        // نعين اسم المستخدم الحالي
        controller.setCurrentUsername("lib1");

        invokePrivate("handleBorrowBook", new Class<?>[]{});

        assertEquals("Borrowed", item.getStatus());
        assertEquals("lib1", item.getBorrowedBy());
        assertNotNull(item.getDueDate());
        assertFalse(item.getDueDate().isEmpty());
        assertTrue(infoLabel.getText().startsWith("✅ Borrowed successfully"));
    }

    // ================== handleReturnBook ==================

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
        ObservableList<Media> mediaList =
                (ObservableList<Media>) getPrivateField("mediaList");

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
        ObservableList<Media> mediaList =
                (ObservableList<Media>) getPrivateField("mediaList");

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

    // ================== handleSearch ==================

    @Test
    void testHandleSearch_emptyKeyword_showsAll() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList =
                (ObservableList<Media>) getPrivateField("mediaList");

        mediaList.add(new Book("Clean Code", "Robert Martin", "111"));
        mediaList.add(new Book("Effective Java", "Joshua Bloch", "222"));

        TableView<Media> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList()); // مؤقتاً
        TextField searchField = new TextField(""); // فاضي

        injectField("bookTable", table);
        injectField("searchField", searchField);

        invokePrivate("handleSearch", new Class<?>[]{});

        assertEquals(mediaList, table.getItems());
    }

    @Test
    void testHandleSearch_filtersByTitleOrIsbn() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList =
                (ObservableList<Media>) getPrivateField("mediaList");

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

    // ================== getUserMembership (private) ==================

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
    
    @Test
    void testInitialize_setsUpTableAndRowFactory() throws Exception {
        // نحضّر العناصر اللي عادة بتيجي من FXML
        TableView<Media> table = new TableView<>();
        TableColumn<Media, String> typeCol = new TableColumn<>("Type");
        TableColumn<Media, String> titleCol = new TableColumn<>("Title");
        TableColumn<Media, String> authorCol = new TableColumn<>("Author");
        TableColumn<Media, String> isbnCol = new TableColumn<>("ISBN");
        TableColumn<Media, String> statusCol = new TableColumn<>("Status");
        TableColumn<Media, String> dueDateCol = new TableColumn<>("Due");
        TableColumn<Media, String> borrowedCol = new TableColumn<>("By");

        injectField("bookTable", table);
        injectField("typeColumn", typeCol);
        injectField("titleColumn", titleCol);
        injectField("authorColumn", authorCol);
        injectField("isbnColumn", isbnCol);
        injectField("statusColumn", statusCol);
        injectField("dueDateColumn", dueDateCol);
        injectField("borrowedByColumn", borrowedCol);

        // act
        controller.initialize();

        // assert بسيط: اتربط items
        assertNotNull(table.getItems());
    }

    @Test
    void testHandleReload_loadsFromFileAndUpdatesLabel() throws Exception {
        // نكتب كتاب للملف
        try (PrintWriter out = new PrintWriter(new FileWriter("books.txt"))) {
            out.println("Book,Clean Code,Robert Martin,111,Borrowed,2025-12-20,0.0,lib1,0.0");
        }

        // نجهّز الجدول والأعمدة زي ما بيصير من الـ FXML
        TableView<Media> table = new TableView<>();
        TableColumn<Media, String> typeCol     = new TableColumn<>("Type");
        TableColumn<Media, String> titleCol    = new TableColumn<>("Title");
        TableColumn<Media, String> authorCol   = new TableColumn<>("Author");
        TableColumn<Media, String> isbnCol     = new TableColumn<>("ISBN");
        TableColumn<Media, String> statusCol   = new TableColumn<>("Status");
        TableColumn<Media, String> dueDateCol  = new TableColumn<>("Due");
        TableColumn<Media, String> borrowedCol = new TableColumn<>("By");

        Label infoLabel = new Label();

        // inject للحقول اللي عادة بتيجي من الـ FXML
        injectField("bookTable", table);
        injectField("typeColumn", typeCol);
        injectField("titleColumn", titleCol);
        injectField("authorColumn", authorCol);
        injectField("isbnColumn", isbnCol);
        injectField("statusColumn", statusCol);
        injectField("dueDateColumn", dueDateCol);
        injectField("borrowedByColumn", borrowedCol);
        injectField("infoLabel", infoLabel);

        // أولاً: نستدعي initialize عشان يربط الأعمدة والـ mediaList بالجدول
        controller.initialize();

        // act
        controller.handleReload();

        // نقرأ mediaList الداخلي للتأكد أنه انقرأ من الملف
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList =
                (ObservableList<Media>) getPrivateField("mediaList");

        // assert
        assertFalse(mediaList.isEmpty(), "mediaList should be loaded from file");
        assertEquals(mediaList, table.getItems(), "table items should be same list");
        assertEquals("🔄 Data reloaded.", infoLabel.getText());
    }

    
    @Test
    void testLoadMediaFromFile_overdueBook_calculatesFine() throws Exception {
        // نكتب user عنده عضوية Gold عشان نشغّل getUserMembership + calculateFine
        try (PrintWriter out = new PrintWriter(new FileWriter("users.txt"))) {
            out.println("lib1,123,User,Gold,lib1@mail.com");
        }

        // كتاب متأخر: dueDate قديم و status Borrowed و borrowedBy = lib1
        try (PrintWriter out = new PrintWriter(new FileWriter("books.txt"))) {
            out.println("Book,Clean Code,Robert Martin,111,Borrowed,2024-01-01,0.0,lib1,0.0");
        }

        // نجهّز الجدول والأعمدة زي ما بنعمل في التستات الثانية
        TableView<Media> table = new TableView<>();
        TableColumn<Media, String> typeCol     = new TableColumn<>("Type");
        TableColumn<Media, String> titleCol    = new TableColumn<>("Title");
        TableColumn<Media, String> authorCol   = new TableColumn<>("Author");
        TableColumn<Media, String> isbnCol     = new TableColumn<>("ISBN");
        TableColumn<Media, String> statusCol   = new TableColumn<>("Status");
        TableColumn<Media, String> dueDateCol  = new TableColumn<>("Due");
        TableColumn<Media, String> borrowedCol = new TableColumn<>("By");

        injectField("bookTable", table);
        injectField("typeColumn", typeCol);
        injectField("titleColumn", titleCol);
        injectField("authorColumn", authorCol);
        injectField("isbnColumn", isbnCol);
        injectField("statusColumn", statusCol);
        injectField("dueDateColumn", dueDateCol);
        injectField("borrowedByColumn", borrowedCol);

        // نستدعي initialize → جواته loadMediaFromFile
        controller.initialize();

        // نقرأ mediaList من داخل الكلاس
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList =
                (ObservableList<Media>) getPrivateField("mediaList");

        assertEquals(1, mediaList.size());
        Media m = mediaList.get(0);

        // لازم يكون الكتاب متأخر و عليه غرامة > 0 بعد الحساب
        assertEquals("111", m.getIsbn());
        assertEquals("Overdue", m.getStatus());        // من calculateFine
        assertTrue(m.getFineAmount() > 0.0, "fineAmount should be > 0 for overdue item");
    }

}
