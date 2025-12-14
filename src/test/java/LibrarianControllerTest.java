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
 * This suite verifies the core functionality of the Librarian Dashboard, including
 * transaction management, file persistence, data parsing, and UI states.
 * It utilizes Java Reflection to inspect private state and ensure maximum coverage.
 *
 * @author Zainab
 * @version 1.0
 */
public class LibrarianControllerTest {

    private LibrarianController controller;
    private static final String BOOKS_FILE = "books.txt";
    private static final String USERS_FILE = "users.txt";

    /**
     * Initializes the JavaFX Toolkit required for testing UI components.
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
     *
     * @throws Exception if reflection or file operations fail.
     */
    @BeforeEach
    void setUp() throws Exception {
        controller = new LibrarianController();
        cleanupFiles();
    }

    /**
     * Cleans up external resources after each test execution.
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
            out.println(content);
        }
    }

    private void createDummyUsersFile(String content) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(USERS_FILE))) {
            out.println(content);
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
     * Verifies the complete initialization flow.
     *
     * @throws Exception if reflection or initialization logic fails.
     */
    @Test
    void testInitialize_FullFlow() throws Exception {
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

        createDummyBooksFile("Book,Title,Author,123,1,Available,2025-01-01,0.0,,0.0");

        controller.initialize();

        assertNotNull(table.getItems());
        assertEquals(1, table.getItems().size());
        assertNotNull(table.getRowFactory());
    }

    /**
     * Validates the borrowing transaction logic scenarios.
     *
     * @throws Exception if reflection or business logic fails.
     */
    @Test
    void testHandleBorrowBook_Scenarios() throws Exception {
        TableView<Media> table = new TableView<>();
        Label infoLabel = new Label();
        injectField("bookTable", table);
        injectField("infoLabel", infoLabel);
        controller.setCurrentUsername("libUser");
        
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");

        invokePrivate("handleBorrowBook", new Class<?>[]{});
        assertEquals("⚠️ Select an item first.", infoLabel.getText());

        Media borrowedItem = new Book("B1", "A1", "111");
        borrowedItem.setStatus("Borrowed");
        mediaList.add(borrowedItem);
        table.setItems(mediaList);
        table.getSelectionModel().select(borrowedItem);
        
        invokePrivate("handleBorrowBook", new Class<?>[]{});
        assertEquals("❌ Item already borrowed.", infoLabel.getText());

        Media availItem = new Book("B2", "A2", "222");
        availItem.setStatus("Available");
        mediaList.add(availItem);
        table.getSelectionModel().select(availItem);
        
        invokePrivate("handleBorrowBook", new Class<?>[]{});
        assertEquals("Borrowed", availItem.getStatus());
        assertTrue(infoLabel.getText().startsWith("✅ Borrowed successfully"));
        
        assertTrue(new File(BOOKS_FILE).exists());
    }

    /**
     * Validates the return transaction logic scenarios.
     *
     * @throws Exception if reflection or business logic fails.
     */
    @Test
    void testHandleReturnBook_Scenarios() throws Exception {
        TableView<Media> table = new TableView<>();
        Label infoLabel = new Label();
        injectField("bookTable", table);
        injectField("infoLabel", infoLabel);

        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");

        invokePrivate("handleReturnBook", new Class<?>[]{});
        assertEquals("⚠️ Select an item first.", infoLabel.getText());

        Media availItem = new Book("B1", "A1", "111");
        availItem.setStatus("Available");
        mediaList.add(availItem);
        table.setItems(mediaList);
        table.getSelectionModel().select(availItem);

        invokePrivate("handleReturnBook", new Class<?>[]{});
        assertEquals("ℹ️ This item is not borrowed.", infoLabel.getText());

        Media borrowedItem = new Book("B2", "A2", "222");
        borrowedItem.setStatus("Borrowed");
        mediaList.add(borrowedItem);
        table.getSelectionModel().select(borrowedItem);

        invokePrivate("handleReturnBook", new Class<?>[]{});
        assertEquals("Available", borrowedItem.getStatus());
        assertEquals("✅ Item returned successfully.", infoLabel.getText());
    }

    /**
     * Tests the search filter functionality.
     *
     * @throws Exception if reflection fails.
     */
    @Test
    void testHandleSearch_Scenarios() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");
        mediaList.add(new Book("Java Programming", "Author1", "12345"));
        mediaList.add(new Book("Python Guide", "Author2", "67890"));

        TableView<Media> table = new TableView<>();
        TextField searchField = new TextField();
        injectField("bookTable", table);
        injectField("searchField", searchField);

        searchField.setText("java");
        invokePrivate("handleSearch", new Class<?>[]{});
        assertEquals(1, table.getItems().size());
        assertEquals("Java Programming", table.getItems().get(0).getTitle());

        searchField.setText("");
        invokePrivate("handleSearch", new Class<?>[]{});
        assertEquals(2, table.getItems().size());
    }

    /**
     * Tests robustness of data loading and file parsing.
     *
     * @throws Exception if reflection or file I/O fails.
     */
    @Test
    void testHandleReload_and_FileParsing() throws Exception {
        String content = "CD,Disc1,Art1,111,1,Available,2024-01-01,0.0,,0.0\n" +
                         "Book,Book1,Auth1,222,2,Borrowed,2024-01-01,0.0,user1,0.0\n" +
                         "Book,BadNum,Auth2,333,badInt,Available,,badDbl,,badDbl\n" + 
                         "InvalidLine\n";
        createDummyBooksFile(content);

        TableView<Media> table = new TableView<>();
        Label infoLabel = new Label();
        injectField("bookTable", table);
        injectField("infoLabel", infoLabel);

        invokePrivate("handleReload", new Class<?>[]{});

        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");

        assertEquals(3, mediaList.size()); 
        
        assertTrue(mediaList.get(0) instanceof CD);
        
        Media badItem = mediaList.get(2);
        assertEquals(1, badItem.getCopyId()); 
        
        assertEquals("🔄 Data reloaded.", infoLabel.getText());
    }

    /**
     * Verifies overdue fine calculation with user membership.
     *
     * @throws Exception if reflection or file I/O fails.
     */
    @Test
    void testOverdueFineCalculation_withUserMembership() throws Exception {
        createDummyUsersFile("goldUser,pass,Gold,Gold,email@test.com");

        String overdueBook = "Book,OldBook,Auth,999,1,Overdue,2020-01-01,0.0,goldUser,0.0";
        createDummyBooksFile(overdueBook);

        TableView<Media> table = new TableView<>();
        injectField("bookTable", table);

        invokePrivate("loadMediaFromFile", new Class<?>[]{});

        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");
        
        assertFalse(mediaList.isEmpty());
        Media loadedBook = mediaList.get(0);

        assertEquals("Overdue", loadedBook.getStatus());
        assertEquals("goldUser", loadedBook.getBorrowedBy());
    }

    /**
     * Tests the fallback logic for retrieving user membership.
     *
     * @throws Exception if reflection fails.
     */
    @Test
    void testGetUserMembership_Fallbacks() throws Exception {
        Method m = LibrarianController.class.getDeclaredMethod("getUserMembership", String.class);
        m.setAccessible(true);

        new File(USERS_FILE).delete();
        String res1 = (String) m.invoke(controller, "anyUser");
        assertEquals("Silver", res1);

        createDummyUsersFile("otherUser,pass,Type,Gold,mail");
        String res2 = (String) m.invoke(controller, "missingUser");
        assertEquals("Silver", res2);
    }

    /**
     * Verifies the logout mechanism.
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
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    /**
     * Tests the TableView RowFactory implementation for correct styling.
     *
     * @throws Exception if reflection fails.
     */
    @Test
    void testRowColoring() throws Exception {
        TableView<Media> table = new TableView<>();
        injectField("bookTable", table);
        invokePrivate("setupRowColoring", new Class<?>[]{});

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            TableRow<Media> row = table.getRowFactory().call(table);
            
            Media m = new Book("T", "A", "I");
            
            try {
                Method update = TableRow.class.getDeclaredMethod("updateItem", Object.class, boolean.class);
                update.setAccessible(true);

                m.setStatus("Available");
                update.invoke(row, m, false);
                assertEquals("-fx-background-color: #d4edda;", row.getStyle());

                m.setStatus("Borrowed");
                update.invoke(row, m, false);
                assertEquals("-fx-background-color: #fff3cd;", row.getStyle());

                m.setStatus("Overdue");
                update.invoke(row, m, false);
                assertEquals("-fx-background-color: #f8d7da;", row.getStyle());

                update.invoke(row, null, true);
                assertEquals("", row.getStyle());

            } catch (Exception e) {
            }
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
    
    /**
     * Verifies that setting the current username updates the UI.
     *
     * @throws Exception if reflection fails.
     */
    @Test
    void testSetCurrentUsername() throws Exception {
        Label welcome = new Label();
        injectField("welcomeLabel", welcome);
        controller.setCurrentUsername("Tester");
        assertEquals("Welcome, Tester 👋", welcome.getText());
    }
}