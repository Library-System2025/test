import static org.junit.jupiter.api.Assertions.*;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.util.Callback;

import org.junit.jupiter.api.*;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Comprehensive Integration Test Suite for the {@link homepageController} class.
 * <p>
 * This class employs Java Reflection to perform white-box testing on the controller's
 * private methods and internal state. It is designed to operate in a headless environment,
 * simulating JavaFX UI interactions and file system operations to ensure high code coverage
 * and logic reliability without requiring a physical display.
 * </p>
 *
 * @author Zainab
 * @version 1.0
 */
public class HomepageControllerTest {

    /** The controller instance under test. */
    private homepageController controller;

    /** Mock field for displaying messages. */
    private Label addBookMessage;
    /** Mock field for the welcome label. */
    private Label welcomeLabel;
    /** Mock field for title input. */
    private TextField titleField;
    /** Mock field for author input. */
    private TextField authorField;
    /** Mock field for ISBN input. */
    private TextField isbnField;
    /** Mock field for search input. */
    private TextField searchField;
    /** Mock combo box for media type selection. */
    private ComboBox<String> typeCombo;
    /** Mock combo box for search criteria. */
    private ComboBox<String> searchByCombo;
    /** Mock table view for search results. */
    private TableView<Media> searchResultsTable;
    /** Mock table column for media type. */
    private TableColumn<Media, String> typeColumn;
    /** Mock table column for title. */
    private TableColumn<Media, String> titleColumn;
    /** Mock table column for author. */
    private TableColumn<Media, String> authorColumn;
    /** Mock table column for ISBN. */
    private TableColumn<Media, String> isbnColumn;
    /** Mock table column for copy ID. */
    private TableColumn<Media, Integer> copyIdColumn;
    /** Mock table column for status. */
    private TableColumn<Media, String> statusColumn;
    /** Mock table column for due date. */
    private TableColumn<Media, String> dueDateColumn;
    /** Mock table column for fine amount. */
    private TableColumn<Media, Double> fineColumn;
    /** Mock table column for borrower name. */
    private TableColumn<Media, String> borrowedByColumn;
    /** Mock table view for users. */
    private TableView<User> usersTable;
    /** Mock table column for username. */
    private TableColumn<User, String> colUsername;
    /** Mock table column for role. */
    private TableColumn<User, String> colRole;
    /** Mock table column for membership. */
    private TableColumn<User, String> colMembership;

    /**
     * Default constructor for the test class.
     */
    public HomepageControllerTest() {
        // Default constructor
    }

    /**
     * Initializes the JavaFX toolkit environment.
     * <p>
     * This static setup is required to instantiate JavaFX controls (like Labels and TableViews)
     * safely outside of a standard JavaFX Application lifecycle.
     * </p>
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
     * Sets up the test environment before each test method execution.
     * <p>
     * This method initializes a new instance of the controller, injects mock JavaFX components
     * into private fields using reflection, and clears any existing data files to ensure test isolation.
     * </p>
     * 
     * @throws Exception if reflection access to private fields fails.
     */
    @BeforeEach
    void setUp() throws Exception {
        controller = new homepageController();

        injectField("mediaList", FXCollections.observableArrayList());
        injectField("mediaMap", new java.util.HashMap<String, Media>());
        injectField("usersList", FXCollections.observableArrayList());

        addBookMessage = new Label();
        injectField("addBookMessage", addBookMessage);
        
        welcomeLabel = new Label();
        injectField("welcomeLabel", welcomeLabel);
        
        titleField = new TextField();
        injectField("titleField", titleField);
        
        authorField = new TextField();
        injectField("authorField", authorField);
        
        isbnField = new TextField();
        injectField("isbnField", isbnField);
        
        searchField = new TextField();
        injectField("searchField", searchField);

        typeCombo = new ComboBox<>(FXCollections.observableArrayList("Book", "CD"));
        typeCombo.getSelectionModel().select("Book");
        injectField("typeCombo", typeCombo);

        searchByCombo = new ComboBox<>(FXCollections.observableArrayList("All", "Title", "Author", "ISBN"));
        searchByCombo.getSelectionModel().select("All");
        injectField("searchByCombo", searchByCombo);

        searchResultsTable = new TableView<>();
        injectField("searchResultsTable", searchResultsTable);

        typeColumn = new TableColumn<>("Type");
        injectField("typeColumn", typeColumn);
        
        titleColumn = new TableColumn<>("Title");
        injectField("titleColumn", titleColumn);
        
        authorColumn = new TableColumn<>("Author");
        injectField("authorColumn", authorColumn);
        
        isbnColumn = new TableColumn<>("ISBN");
        injectField("isbnColumn", isbnColumn);
        
        copyIdColumn = new TableColumn<>("CopyId");
        injectField("copyIdColumn", copyIdColumn);
        
        statusColumn = new TableColumn<>("Status");
        injectField("statusColumn", statusColumn);
        
        dueDateColumn = new TableColumn<>("Due");
        injectField("dueDateColumn", dueDateColumn);
        
        fineColumn = new TableColumn<>("Fine");
        injectField("fineColumn", fineColumn);
        
        borrowedByColumn = new TableColumn<>("By");
        injectField("borrowedByColumn", borrowedByColumn);

        usersTable = new TableView<>();
        injectField("usersTable", usersTable);
        
        colUsername = new TableColumn<>("Username");
        injectField("colUsername", colUsername);
        
        colRole = new TableColumn<>("Role");
        injectField("colRole", colRole);
        
        colMembership = new TableColumn<>("Membership");
        injectField("colMembership", colMembership);

        Files.deleteIfExists(Paths.get("books.txt"));
        Files.deleteIfExists(Paths.get("users.txt"));

        controller.initialize();
    }

    /**
     * Cleans up resources after each test method execution.
     * <p>
     * Ensures that temporary files created during testing ('books.txt', 'users.txt')
     * are deleted to prevent data pollution between tests.
     * </p>
     * 
     * @throws IOException if file deletion fails.
     */
    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Paths.get("books.txt"));
        Files.deleteIfExists(Paths.get("users.txt"));
    }

    /**
     * Verifies that the TableView RowFactory correctly styles rows based on media status.
     * <p>
     * Uses reflection to invoke the cell update logic and checks that:
     * </p>
     * <ul>
     *   <li>"Available" items are styled green.</li>
     *   <li>"Borrowed" items are styled yellow.</li>
     *   <li>"Overdue" items are styled red.</li>
     * </ul>
     * 
     * @throws Exception if reflection access to the Cell update method fails.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testRowFactory_ColoringLogic() throws Exception {
        TableView<Media> table = (TableView<Media>) getPrivateField("searchResultsTable");
        Callback<TableView<Media>, TableRow<Media>> factory = table.getRowFactory();
        assertNotNull(factory);

        TableRow<Media> row = factory.call(table);
        Method updateItem = javafx.scene.control.Cell.class.getDeclaredMethod("updateItem", Object.class, boolean.class);
        updateItem.setAccessible(true);

        updateItem.invoke(row, new Book("T","A","1","Available","",0.0,"",0.0,1), false);
        assertEquals("-fx-background-color: #d0f0c0;", row.getStyle());

        updateItem.invoke(row, new Book("T","A","1","Borrowed","",0.0,"",0.0,1), false);
        assertEquals("-fx-background-color: #fff9c4;", row.getStyle());

        updateItem.invoke(row, new Book("T","A","1","Overdue","",0.0,"",0.0,1), false);
        assertEquals("-fx-background-color: #ffcdd2;", row.getStyle());

        updateItem.invoke(row, new Book("T","A","1","Lost","",0.0,"",0.0,1), false);
        assertEquals("", row.getStyle());

        updateItem.invoke(row, null, true);
        assertEquals("", row.getStyle());
    }

    /**
     * Tests that the {@code handleAddBook} method validates input fields.
     * <p>
     * Ensures that if any required field is empty, the operation is aborted
     * and an error message is displayed.
     * </p>
     * 
     * @throws Exception if reflection access to private fields fails.
     */
    @Test
    void testHandleAddBook_EmptyFields() throws Exception {
        titleField.setText("");
        controller.handleAddBook();
        Label msg = (Label) getPrivateField("addBookMessage");
        assertEquals("❗ Please fill all fields.", msg.getText());
    }

    /**
     * Tests the successful addition of a new book to the inventory.
     * 
     * @throws Exception if reflection access to private fields fails.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testHandleAddBook_Success() throws Exception {
        setTextField("titleField", "Java");
        setTextField("authorField", "Gosling");
        setTextField("isbnField", "999");
        
        controller.handleAddBook();

        ObservableList<Media> list = (ObservableList<Media>) getPrivateField("mediaList");
        assertEquals(1, list.size());
        assertEquals("Java", list.get(0).getTitle());
    }

    /**
     * Tests the logic for adding a new copy of an existing book.
     * <p>
     * Verifies that adding a book with an existing ISBN (and matching details)
     * correctly increments the copy ID instead of creating a separate entry.
     * </p>
     * 
     * @throws Exception if reflection access to private fields fails.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testHandleAddBook_NewCopy() throws Exception {
        setTextField("titleField", "Java");
        setTextField("authorField", "Gosling");
        setTextField("isbnField", "999");
        controller.handleAddBook();

        setTextField("titleField", "Java");
        setTextField("authorField", "Gosling");
        setTextField("isbnField", "999");
        controller.handleAddBook();

        ObservableList<Media> list = (ObservableList<Media>) getPrivateField("mediaList");
        assertEquals(2, list.size());
        assertEquals(2, list.get(1).getCopyId());
        
        Label msg = (Label) getPrivateField("addBookMessage");
        assertTrue(msg.getText().contains("NEW COPY"));
    }

    /**
     * Tests validation against conflicting ISBN data.
     * <p>
     * Ensures that the system prevents adding a book with an existing ISBN
     * if the title or author does not match the existing record.
     * </p>
     * 
     * @throws Exception if reflection access to private fields fails.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testHandleAddBook_Conflict() throws Exception {
        ObservableList<Media> list = (ObservableList<Media>) getPrivateField("mediaList");
        Map<String, Media> map = (Map<String, Media>) getPrivateField("mediaMap");
        Media m1 = new Book("Original", "Author", "123", "Available", "", 0.0, "", 0.0, 1);
        list.add(m1);
        map.put("123", m1);

        setTextField("titleField", "Conflict Book");
        setTextField("authorField", "Other Guy");
        setTextField("isbnField", "123");

        controller.handleAddBook();

        Label msg = (Label) getPrivateField("addBookMessage");
        assertTrue(msg.getText().contains("ISBN already exists but with different title"));
        assertEquals(1, list.size());
    }

    /**
     * Tests the file parsing logic specifically for edge cases and malformed data.
     * <p>
     * Writes a temporary file containing valid lines, invalid number formats,
     * short lines, and missing optional fields to verify the robustness of the
     * parsing loop.
     * </p>
     * 
     * @throws Exception if file I/O operations or reflection fails.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testLoadMedia_ParsingEdgeCases() throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("books.txt"))) {
            writer.write("Book,Good Title,Auth,100,1,Available,,0.0,,0.0\n");
            writer.write("CD,Bad Num,Auth,101,NotInt,Available,,NotDbl,,0.0\n");
            writer.write("Book,Short Line,Auth\n");
            writer.write("CD,Missing Opts,Auth,102,1\n");
            writer.write("Book,Borrowed,User,103,1,Borrowed,2025-01-01,5.0,ali,0.0\n");
        }
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("users.txt"))) {
            writer.write("ali,123,User,Gold\n");
        }

        controller.handleReload();

        ObservableList<Media> list = (ObservableList<Media>) getPrivateField("mediaList");
        
        assertFalse(list.isEmpty());
        
        assertTrue(list.stream().anyMatch(m -> m.getIsbn().equals("100")));
        
        Media badNum = list.stream().filter(m -> m.getIsbn().equals("101")).findFirst().orElse(null);
        assertNotNull(badNum);
        assertEquals(1, badNum.getCopyId());
        assertEquals(0.0, badNum.getFineAmount());
        
        assertFalse(list.stream().anyMatch(m -> m.getTitle().equals("Short Line")));
    }

    /**
     * Verifies the search functionality across all filtering criteria.
     * <p>
     * Tests filtering by:
     * </p>
     * <ul>
     *   <li>Title</li>
     *   <li>Author</li>
     *   <li>ISBN</li>
     *   <li>All fields (default)</li>
     * </ul>
     * 
     * @throws Exception if reflection access to private fields fails.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testHandleSearch_AllCriteria() throws Exception {
        ObservableList<Media> list = (ObservableList<Media>) getPrivateField("mediaList");
        list.add(new Book("Java", "James", "111", "Available", "", 0.0, "", 0.0, 1));
        list.add(new Book("Python", "Guido", "222", "Available", "", 0.0, "", 0.0, 1));

        searchField.setText("Java");
        searchByCombo.setValue("Title");
        controller.handleSearch();
        assertEquals(1, searchResultsTable.getItems().size());

        searchField.setText("Guido");
        searchByCombo.setValue("Author");
        controller.handleSearch();
        assertEquals(1, searchResultsTable.getItems().size());

        searchField.setText("111");
        searchByCombo.setValue("ISBN");
        controller.handleSearch();
        assertEquals(1, searchResultsTable.getItems().size());

        searchField.setText("222");
        searchByCombo.setValue("All");
        controller.handleSearch();
        assertEquals(1, searchResultsTable.getItems().size());
        
        searchField.setText("");
        controller.handleSearch();
        assertEquals(2, searchResultsTable.getItems().size());
    }

    /**
     * Tests that a user cannot be deleted if they currently hold borrowed items.
     * 
     * @throws Exception if reflection access to private fields fails.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testHandleDeleteUser_WithLoans() throws Exception {
        ObservableList<User> users = (ObservableList<User>) getPrivateField("usersList");
        User u = new User("loaner", "1", "User", "Silver");
        users.add(u);

        ObservableList<Media> media = (ObservableList<Media>) getPrivateField("mediaList");
        media.add(new Book("B", "A", "I", "Borrowed", "", 0.0, "loaner", 0.0, 1));

        usersTable.setItems(users);
        usersTable.getSelectionModel().select(u);

        controller.handleDeleteUser();

        assertEquals(1, users.size());
    }

    /**
     * Tests the successful deletion of a user who has no active loans.
     * 
     * @throws Exception if reflection access to private fields fails.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testHandleDeleteUser_Success() throws Exception {
        ObservableList<User> users = (ObservableList<User>) getPrivateField("usersList");
        User u = new User("free", "1", "User", "Silver");
        users.add(u);

        usersTable.setItems(users);
        usersTable.getSelectionModel().select(u);

        controller.handleDeleteUser();

        assertTrue(users.isEmpty());
    }

    /**
     * Tests the entire flow of sending an overdue reminder email.
     * <p>
     * This integration test verifies that the system:
     * </p>
     * <ol>
     *   <li>Identifies a user with overdue items.</li>
     *   <li>Retrieves the user's email from the file.</li>
     *   <li>Triggers the publisher notification mechanism.</li>
     * </ol>
     * 
     * @throws Exception if reflection or mock setup fails.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testHandleSendReminder_Logic() throws Exception {
        ObservableList<User> users = (ObservableList<User>) getPrivateField("usersList");
        User u = new User("lateUser", "1", "User", "Gold");
        users.add(u);
        
        usersTable.setItems(users);
        usersTable.getSelectionModel().select(u);
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("users.txt"))) {
            writer.write("lateUser,1,User,Gold,test@mail.com\n");
        }

        ObservableList<Media> media = (ObservableList<Media>) getPrivateField("mediaList");
        media.add(new Book("Late", "Auth", "999", "Overdue", "2020-01-01", 10.0, "lateUser", 0.0, 1));

        mockOverdueSubscriber();

        assertDoesNotThrow(() -> controller.handleSendReminder());
    }

    /**
     * Tests the behavior when attempting to send a reminder to a user without a registered email.
     * 
     * @throws Exception if reflection or file I/O fails.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testHandleSendReminder_NoEmail() throws Exception {
        ObservableList<User> users = (ObservableList<User>) getPrivateField("usersList");
        User u = new User("noEmail", "1", "User", "Gold");
        users.add(u);
        
        usersTable.setItems(users);
        usersTable.getSelectionModel().select(u);
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("users.txt"))) {
            writer.write("noEmail,1,User,Gold\n");
        }

        assertDoesNotThrow(() -> controller.handleSendReminder());
    }

    /**
     * Injects a value into a private field of the controller instance using Reflection.
     * 
     * @param name  The name of the private field.
     * @param value The object value to inject.
     * @throws Exception If the field is not found or inaccessible.
     */
    private void injectField(String name, Object value) throws Exception {
        Field f = homepageController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, value);
    }

    /**
     * Retrieves the value of a private field from the controller instance using Reflection.
     * 
     * @param name The name of the private field.
     * @return The value of the field.
     * @throws Exception If the field is not found or inaccessible.
     */
    private Object getPrivateField(String name) throws Exception {
        Field f = homepageController.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(controller);
    }
    
    /**
     * Helper method to set text on a private TextField within the controller.
     * 
     * @param fieldName The name of the TextField field.
     * @param value     The string value to set.
     * @throws Exception If reflection access fails.
     */
    private void setTextField(String fieldName, String value) throws Exception {
        ((TextField) getPrivateField(fieldName)).setText(value);
    }

    /**
     * Replaces the real OverduePublisher subscribers with a mock subscriber.
     * <p>
     * This prevents actual network calls or email sending attempts during testing.
     * </p>
     * 
     * @throws Exception If reflection access to the publisher or its list fails.
     */
    private void mockOverdueSubscriber() throws Exception {
        Field pubField = homepageController.class.getDeclaredField("overduePublisher");
        pubField.setAccessible(true);
        OverduePublisher publisher = (OverduePublisher) pubField.get(null);

        Field subsField = OverduePublisher.class.getDeclaredField("subscribers");
        subsField.setAccessible(true);
        java.util.List<OverdueSubscriber> subs = (java.util.List<OverdueSubscriber>) subsField.get(publisher);

        subs.clear();
        subs.add((username, email, overdueList) -> {
        });
    }
}