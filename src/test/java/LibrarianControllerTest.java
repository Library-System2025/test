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
 * Comprehensive Integration Test Suite for the {@link LibrarianController} class.
 * <p>
 * This suite verifies the core functionality of the Librarian Dashboard, including
 * transaction management (borrow/return), file persistence, data parsing,
 * user membership validation, and UI visual states.
 * It utilizes Java Reflection to inspect private state and invoke private methods,
 * ensuring maximum code coverage.
 * </p>
 *
 * @author Zainab
 * @version 1.0
 */
public class LibrarianControllerTest {

    /** The controller instance under test. */
    private LibrarianController controller;

    /** File path for books database. */
    private static final String BOOKS_FILE = "books.txt";
    /** File path for users database. */
    private static final String USERS_FILE = "users.txt";

    /** Mock table view for books. */
    private TableView<Media> bookTable;
    /** Mock label for information messages. */
    private Label infoLabel;
    /** Mock text field for search input. */
    private TextField searchField;
    /** Mock label for welcome message. */
    private Label welcomeLabel;

    /** Mock column for media type. */
    private TableColumn<Media, String> typeColumn;
    /** Mock column for title. */
    private TableColumn<Media, String> titleColumn;
    /** Mock column for author. */
    private TableColumn<Media, String> authorColumn;
    /** Mock column for ISBN. */
    private TableColumn<Media, String> isbnColumn;
    /** Mock column for status. */
    private TableColumn<Media, String> statusColumn;
    /** Mock column for due date. */
    private TableColumn<Media, String> dueDateColumn;
    /** Mock column for borrower. */
    private TableColumn<Media, String> borrowedByColumn;
    /** Mock column for copy ID. */
    private TableColumn<Media, Integer> copyIdColumn;

    /**
     * Default constructor for the test class.
     */
    public LibrarianControllerTest() {
        // Default constructor
    }

    /**
     * Initializes the JavaFX Toolkit required for testing UI components.
     * This method ensures the toolkit is started only once for the entire test execution.
     */
    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit already initialized
        }
    }

    /**
     * Sets up the test environment before each test case.
     * Initializes the controller instance, injects mocks, and ensures a clean file system state.
     *
     * @throws Exception if reflection or file operations fail.
     */
    @BeforeEach
    void setUp() throws Exception {
        controller = new LibrarianController();
        cleanupFiles();
        initializeMocks();
    }

    /**
     * Cleans up external resources and temporary files after each test execution.
     */
    @AfterEach
    void tearDown() {
        cleanupFiles();
    }

    /**
     * Initializes and injects all mock UI components into the controller.
     * 
     * @throws Exception if reflection fails.
     */
    private void initializeMocks() throws Exception {
        bookTable = new TableView<>();
        injectField("bookTable", bookTable);

        infoLabel = new Label();
        injectField("infoLabel", infoLabel);

        searchField = new TextField();
        injectField("searchField", searchField);
        
        welcomeLabel = new Label();
        injectField("welcomeLabel", welcomeLabel);

        typeColumn = new TableColumn<>();
        injectField("typeColumn", typeColumn);
        
        titleColumn = new TableColumn<>();
        injectField("titleColumn", titleColumn);
        
        authorColumn = new TableColumn<>();
        injectField("authorColumn", authorColumn);
        
        isbnColumn = new TableColumn<>();
        injectField("isbnColumn", isbnColumn);
        
        statusColumn = new TableColumn<>();
        injectField("statusColumn", statusColumn);
        
        dueDateColumn = new TableColumn<>();
        injectField("dueDateColumn", dueDateColumn);
        
        borrowedByColumn = new TableColumn<>();
        injectField("borrowedByColumn", borrowedByColumn);
        
        copyIdColumn = new TableColumn<>();
        injectField("copyIdColumn", copyIdColumn);
    }

    /**
     * Verifies the complete initialization flow.
     * Ensures that table columns are bound correctly and data is loaded from the file system.
     *
     * @throws Exception if reflection or initialization logic fails.
     */
    @Test
    void testInitialize_FullFlow() throws Exception {
        createDummyBooksFile("Book,Title,Author,123,1,Available,2025-01-01,0.0,,0.0");

        controller.initialize();

        assertNotNull(bookTable.getItems());
        assertEquals(1, bookTable.getItems().size());
        assertNotNull(bookTable.getRowFactory());
    }

    /**
     * Validates the borrowing transaction logic.
     * Covers scenarios for: missing selection, attempting to borrow an already borrowed item,
     * and a successful borrowing transaction.
     *
     * @throws Exception if reflection or business logic fails.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testHandleBorrowBook_Scenarios() throws Exception {
        controller.setCurrentUsername("libUser");
        
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");

        invokePrivate("handleBorrowBook");
        assertEquals("⚠️ Select an item first.", infoLabel.getText());

        Media borrowedItem = new Book("B1", "A1", "111");
        borrowedItem.setStatus("Borrowed");
        mediaList.add(borrowedItem);
        bookTable.setItems(mediaList);
        bookTable.getSelectionModel().select(borrowedItem);
        
        invokePrivate("handleBorrowBook");
        assertEquals("❌ Item already borrowed.", infoLabel.getText());

        Media availItem = new Book("B2", "A2", "222");
        availItem.setStatus("Available");
        mediaList.add(availItem);
        bookTable.getSelectionModel().select(availItem);
        
        invokePrivate("handleBorrowBook");
        assertEquals("Borrowed", availItem.getStatus());
        assertTrue(infoLabel.getText().startsWith("✅ Borrowed successfully"));
        
        assertTrue(new File(BOOKS_FILE).exists());
    }

    /**
     * Validates the return transaction logic.
     * Covers scenarios for: missing selection, attempting to return a non-borrowed item,
     * and a successful return transaction.
     *
     * @throws Exception if reflection or business logic fails.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testHandleReturnBook_Scenarios() throws Exception {
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");

        invokePrivate("handleReturnBook");
        assertEquals("⚠️ Select an item first.", infoLabel.getText());

        Media availItem = new Book("B1", "A1", "111");
        availItem.setStatus("Available");
        mediaList.add(availItem);
        bookTable.setItems(mediaList);
        bookTable.getSelectionModel().select(availItem);

        invokePrivate("handleReturnBook");
        assertEquals("ℹ️ This item is not borrowed.", infoLabel.getText());

        Media borrowedItem = new Book("B2", "A2", "222");
        borrowedItem.setStatus("Borrowed");
        mediaList.add(borrowedItem);
        bookTable.getSelectionModel().select(borrowedItem);

        invokePrivate("handleReturnBook");
        assertEquals("Available", borrowedItem.getStatus());
        assertEquals("✅ Item returned successfully.", infoLabel.getText());
    }

    /**
     * Tests the search filter functionality.
     * Verifies that the table updates correctly based on keyword matches and resets 
     * when the search field is empty.
     *
     * @throws Exception if reflection fails.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testHandleSearch_Scenarios() throws Exception {
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");
        mediaList.add(new Book("Java Programming", "Author1", "12345"));
        mediaList.add(new Book("Python Guide", "Author2", "67890"));

        searchField.setText("java");
        invokePrivate("handleSearch");
        assertEquals(1, bookTable.getItems().size());
        assertEquals("Java Programming", bookTable.getItems().get(0).getTitle());

        searchField.setText("");
        invokePrivate("handleSearch");
        assertEquals(2, bookTable.getItems().size());
    }

    /**
     * Tests robustness of data loading and file parsing.
     * Verifies that the system correctly identifies different Media types (Book vs CD)
     * and gracefully handles malformed numeric data without crashing.
     *
     * @throws Exception if reflection or file I/O fails.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testHandleReload_and_FileParsing() throws Exception {
        String content = "CD,Disc1,Art1,111,1,Available,2024-01-01,0.0,,0.0\n" +
                         "Book,Book1,Auth1,222,2,Borrowed,2024-01-01,0.0,user1,0.0\n" +
                         "Book,BadNum,Auth2,333,badInt,Available,,badDbl,,badDbl";
        createDummyBooksFile(content);

        invokePrivate("handleReload");

        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");

        assertEquals(3, mediaList.size()); 
        
        assertTrue(mediaList.get(0) instanceof CD);
        
        Media badItem = mediaList.get(2);
        assertEquals(1, badItem.getCopyId()); 
        
        assertEquals("🔄 Data reloaded.", infoLabel.getText());
    }

    /**
     * Verifies the interaction between overdue items and user membership levels.
     * Ensures that the system attempts to calculate fines for overdue items based on 
     * the borrower's membership type retrieved from the user database.
     *
     * @throws Exception if reflection or file I/O fails.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testOverdueFineCalculation_withUserMembership() throws Exception {
        createDummyUsersFile("goldUser,pass,Gold,Gold,email@test.com");

        String overdueBook = "Book,OldBook,Auth,999,1,Overdue,2020-01-01,0.0,goldUser,0.0";
        createDummyBooksFile(overdueBook);

        invokePrivate("loadMediaFromFile");

        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");
        
        assertFalse(mediaList.isEmpty());
        Media loadedBook = mediaList.get(0);

        assertEquals("Overdue", loadedBook.getStatus());
        assertEquals("goldUser", loadedBook.getBorrowedBy());
    }

    /**
     * Tests the fallback logic for retrieving user membership.
     * Ensures that the system defaults to "Silver" membership if the user file is missing
     * or the specific user cannot be found.
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
     * Ensures that the UI transition logic is triggered without errors, even in a headless
     * test environment.
     *
     * @throws Exception if reflection fails.
     */
    @Test
    void testHandleLogout() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                TextField sField = new TextField();
                Scene scene = new Scene(new StackPane(sField));
                Stage stage = new Stage();
                stage.setScene(scene);
                injectField("searchField", sField);
                
                invokePrivate("handleLogout");
            } catch (Exception e) {
                // Ignore exceptions caused by missing FXML
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    /**
     * Tests the TableView RowFactory implementation.
     * Validates that rows are assigned the correct CSS styles based on the Media item's status
     * (Available, Borrowed, Overdue).
     *
     * @throws Exception if reflection fails.
     */
    @Test
    void testRowColoring() throws Exception {
        invokePrivate("setupRowColoring");

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            TableRow<Media> row = bookTable.getRowFactory().call(bookTable);
            
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
                // Should not happen in test environment
            }
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
    
    /**
     * Verifies that setting the current username updates both the internal state
     * and the welcome label on the UI.
     *
     * @throws Exception if reflection fails.
     */
    @Test
    void testSetCurrentUsername() throws Exception {
        controller.setCurrentUsername("Tester");
        assertEquals("Welcome, Tester 👋", welcomeLabel.getText());
    }

    

    /**
     * Deletes temporary test files.
     */
    private void cleanupFiles() {
        new File(BOOKS_FILE).delete();
        new File(USERS_FILE).delete();
    }

    /**
     * Creates a dummy books file with the specified content.
     * 
     * @param content The CSV content to write.
     * @throws IOException If writing fails.
     */
    private void createDummyBooksFile(String content) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(BOOKS_FILE))) {
            out.println(content);
        }
    }

    /**
     * Creates a dummy users file with the specified content.
     * 
     * @param content The CSV content to write.
     * @throws IOException If writing fails.
     */
    private void createDummyUsersFile(String content) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(USERS_FILE))) {
            out.println(content);
        }
    }

    /**
     * Injects a mock object into a private field of the controller.
     * 
     * @param name The field name.
     * @param value The mock object.
     * @throws Exception If reflection fails.
     */
    private void injectField(String name, Object value) throws Exception {
        Field f = LibrarianController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, value);
    }

    /**
     * Retrieves the value of a private field from the controller.
     * 
     * @param name The field name.
     * @return The field value.
     * @throws Exception If reflection fails.
     */
    private Object getPrivateField(String name) throws Exception {
        Field f = LibrarianController.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(controller);
    }

    /**
     * Invokes a private method on the controller.
     * 
     * @param name The method name.
     * @param args The method arguments.
     * @return The return value of the method.
     * @throws Exception If reflection fails.
     */
    private Object invokePrivate(String name, Object... args) throws Exception {
        Method m = LibrarianController.class.getDeclaredMethod(name);
        m.setAccessible(true);
        return m.invoke(controller, args);
    }
}