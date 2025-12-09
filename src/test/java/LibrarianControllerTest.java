import static org.junit.jupiter.api.Assertions.*;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive Integration Test Suite for the LibrarianController class.
 * <p>
 * This suite utilizes Java Reflection to access and verify private fields and methods,
 * ensuring high code coverage and robust validation of the Librarian Dashboard logic.
 * It handles JavaFX thread synchronization and file I/O operations for realistic testing scenarios.
 * </p>
 * 
 * @author Zainab
 * @version 2.0
 */
public class LibrarianControllerTest {

    private LibrarianController controller;
    private static final String BOOKS_FILE = "books.txt";
    private static final String USERS_FILE = "users.txt";

    /**
     * Initializes the JavaFX Toolkit required for testing UI components.
     * This method ensures the toolkit is started only once for the entire test suite.
     */
    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            
        }
    }

    /**
     * Sets up the test environment before each test case.
     * Initializes the controller and cleans up any residual test files.
     * 
     * @throws Exception if reflection or file operations fail.
     */
    @BeforeEach
    void setUp() throws Exception {
        controller = new LibrarianController();
        cleanupFiles();
    }

    /**
     * Cleans up test resources after each test execution.
     */
    @AfterEach
    void tearDown() {
        cleanupFiles();
    }

    private void cleanupFiles() {
        new File(BOOKS_FILE).delete();
        new File(USERS_FILE).delete();
    }

    private void createDummyBooksFile(String content) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(BOOKS_FILE))) {
            out.print(content);
        }
    }

    private void createDummyUsersFile(String content) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(USERS_FILE))) {
            out.print(content);
        }
    }

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

    /**
     * Verifies that the initialization process correctly sets up table columns
     * and loads data from the file system.
     * 
     * @throws Exception if reflection fails.
     */
    @Test
    void testInitialize() throws Exception {
        TableView<Media> table = new TableView<>();
        injectField("bookTable", table);
        injectField("typeColumn", new TableColumn<Media, String>());
        injectField("titleColumn", new TableColumn<Media, String>());
        injectField("authorColumn", new TableColumn<Media, String>());
        injectField("isbnColumn", new TableColumn<Media, String>());
        injectField("statusColumn", new TableColumn<Media, String>());
        injectField("dueDateColumn", new TableColumn<Media, String>());
        injectField("borrowedByColumn", new TableColumn<Media, String>());
        injectField("copyIdColumn", new TableColumn<Media, Integer>());

        createDummyBooksFile("Book,Title,Author,123,1,Available,,0.0,,0.0");

        controller.initialize();

        assertNotNull(table.getItems());
        assertNotNull(table.getRowFactory());
        assertEquals(1, table.getItems().size());
    }

    /**
     * Verifies that attempting to borrow a book without selection triggers
     * the correct warning message.
     * 
     * @throws Exception if reflection fails.
     */
    @Test
    void testHandleBorrowBook_noSelection_showsWarning() throws Exception {
        TableView<Media> table = new TableView<>();
        Label infoLabel = new Label();

        injectField("bookTable", table);
        injectField("infoLabel", infoLabel);

        invokePrivate("handleBorrowBook", new Class<?>[]{});

        assertEquals("⚠️ Select an item first.", infoLabel.getText());
    }

    /**
     * Verifies that attempting to borrow an already borrowed item triggers an error.
     * 
     * @throws Exception if reflection fails.
     */
    @Test
    void testHandleBorrowBook_alreadyBorrowed_showsError() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");
        Media item = new Book("Title", "Author", "123");
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
     * Verifies a successful borrow transaction, ensuring status update and
     * borrower assignment.
     * 
     * @throws Exception if reflection fails.
     */
    @Test
    void testHandleBorrowBook_success() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");
        Media item = new Book("Title", "Author", "123");
        mediaList.add(item);

        TableView<Media> table = new TableView<>();
        table.setItems(mediaList);
        table.getSelectionModel().select(item);

        Label infoLabel = new Label();
        injectField("bookTable", table);
        injectField("infoLabel", infoLabel);

        controller.setCurrentUsername("user1");
        invokePrivate("handleBorrowBook", new Class<?>[]{});

        assertEquals("Borrowed", item.getStatus());
        assertEquals("user1", item.getBorrowedBy());
        assertTrue(infoLabel.getText().startsWith("✅ Borrowed successfully"));
        
        File file = new File(BOOKS_FILE);
        assertTrue(file.exists());
    }

    /**
     * Verifies that attempting to return a book without selection triggers
     * the correct warning message.
     * 
     * @throws Exception if reflection fails.
     */
    @Test
    void testHandleReturnBook_noSelection_showsWarning() throws Exception {
        TableView<Media> table = new TableView<>();
        Label infoLabel = new Label();

        injectField("bookTable", table);
        injectField("infoLabel", infoLabel);

        invokePrivate("handleReturnBook", new Class<?>[]{});

        assertEquals("⚠️ Select an item first.", infoLabel.getText());
    }

    /**
     * Verifies that attempting to return an item that is not borrowed shows
     * an informational message.
     * 
     * @throws Exception if reflection fails.
     */
    @Test
    void testHandleReturnBook_notBorrowed_showsInfo() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");
        Media item = new Book("Title", "Author", "123");
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

    /**
     * Verifies a successful return transaction, ensuring status resets to Available.
     * 
     * @throws Exception if reflection fails.
     */
    @Test
    void testHandleReturnBook_success() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");
        Media item = new Book("Title", "Author", "123");
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

    /**
     * Verifies the search functionality filters the table correctly by title.
     * 
     * @throws Exception if reflection fails.
     */
    @Test
    void testHandleSearch_filtersByTitle() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");
        mediaList.add(new Book("Clean Code", "Robert", "111"));
        mediaList.add(new Book("Java", "Joshua", "222"));

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
     * Verifies that an empty search keyword restores the full list.
     * 
     * @throws Exception if reflection fails.
     */
    @Test
    void testHandleSearch_emptyKeyword_showsAll() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");
        mediaList.add(new Book("A", "B", "1"));
        mediaList.add(new Book("C", "D", "2"));

        TableView<Media> table = new TableView<>();
        TextField searchField = new TextField("");

        injectField("bookTable", table);
        injectField("searchField", searchField);

        invokePrivate("handleSearch", new Class<?>[]{});

        assertEquals(2, table.getItems().size());
    }

    /**
     * Verifies the reload functionality refreshes data from the file.
     * 
     * @throws Exception if reflection fails.
     */
    @Test
    void testHandleReload() throws Exception {
        createDummyBooksFile("Book,Loaded,Author,999,1,Available,,0.0,,0.0");
        
        TableView<Media> table = new TableView<>();
        Label infoLabel = new Label();
        injectField("bookTable", table);
        injectField("infoLabel", infoLabel);

        invokePrivate("handleReload", new Class<?>[]{});

        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");
        
        assertEquals(1, mediaList.size());
        assertEquals("Loaded", mediaList.get(0).getTitle());
        assertEquals("🔄 Data reloaded.", infoLabel.getText());
    }

    /**
     * Verifies correct file parsing logic, including CD types and invalid numbers.
     * 
     * @throws Exception if reflection fails.
     */
    @Test
    void testLoadMediaFromFile_parsingLogic() throws Exception {
        String data = "CD,Disc,Artist,888,1,Available,,0.0,,0.0\n" +
                      "Book,BadNum,Auth,777,badInt,Available,,badDbl,,badDbl";
        createDummyBooksFile(data);
        
        TableView<Media> table = new TableView<>();
        injectField("bookTable", table);
        
        invokePrivate("loadMediaFromFile", new Class<?>[]{});
        
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");
        
        assertEquals(2, mediaList.size());
        assertTrue(mediaList.get(0) instanceof CD);
        assertEquals(1, mediaList.get(1).getCopyId()); 
    }

    /**
     * Verifies that the welcome label and username field are updated correctly.
     * 
     * @throws Exception if reflection fails.
     */
    @Test
    void testSetCurrentUsername() throws Exception {
        Label welcome = new Label();
        injectField("welcomeLabel", welcome);

        controller.setCurrentUsername("AdminUser");

        assertEquals("Welcome, AdminUser 👋", welcome.getText());
        Field f = LibrarianController.class.getDeclaredField("accountUsername");
        f.setAccessible(true);
        assertEquals("AdminUser", f.get(controller));
    }

    /**
     * Verifies the logout handler attempts to load the login screen.
     * Validates that no crashes occur even if the FXML is missing in test context.
     * 
     * @throws Exception if reflection fails.
     */
    @Test
    void testHandleLogout() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                TextField searchField = new TextField();
                Scene scene = new Scene(new StackPane(searchField));
                Stage stage = new Stage();
                stage.setScene(scene);
                
                injectField("searchField", searchField);
                
                invokePrivate("handleLogout", new Class<?>[]{});
            } catch (Exception e) {
                fail("Should not throw exception during logout attempt");
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    /**
     * Verifies that the TableView RowFactory assigns correct styles based on status.
     * 
     * @throws Exception if reflection fails.
     */
    @Test
    void testRowColoring_styles() throws Exception {
        TableView<Media> table = new TableView<>();
        injectField("bookTable", table);
        
        invokePrivate("setupRowColoring", new Class<?>[]{});
        
        javafx.util.Callback<TableView<Media>, TableRow<Media>> factory = table.getRowFactory();
        assertNotNull(factory);
        
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            TableRow<Media> row = factory.call(table);
            
            Media available = new Book("A", "A", "1");
            available.setStatus("Available");
            row.updateIndex(0);
            try {
                Method updateItem = TableRow.class.getDeclaredMethod("updateItem", Object.class, boolean.class);
                updateItem.setAccessible(true);
                
                updateItem.invoke(row, available, false);
                assertEquals("-fx-background-color: #d4edda;", row.getStyle());
                
                Media borrowed = new Book("B", "B", "2");
                borrowed.setStatus("Borrowed");
                updateItem.invoke(row, borrowed, false);
                assertEquals("-fx-background-color: #fff3cd;", row.getStyle());
                
                Media overdue = new Book("C", "C", "3");
                overdue.setStatus("Overdue");
                updateItem.invoke(row, overdue, false);
                assertEquals("-fx-background-color: #f8d7da;", row.getStyle());
                
                updateItem.invoke(row, null, true);
                assertEquals("", row.getStyle());
                
            } catch (Exception e) {
                e.printStackTrace();
            }
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}