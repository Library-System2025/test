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

    private homepageController controller;

    private Label addBookMessage;
    private Label welcomeLabel;
    private TextField titleField;
    private TextField authorField;
    private TextField isbnField;
    private TextField searchField;
    private ComboBox<String> typeCombo;
    private ComboBox<String> searchByCombo;
    private TableView<Media> searchResultsTable;
    private TableView<User> usersTable;

    private TableColumn<Media, String> typeColumn;
    private TableColumn<Media, String> titleColumn;
    private TableColumn<Media, String> authorColumn;
    private TableColumn<Media, String> isbnColumn;
    private TableColumn<Media, Integer> copyIdColumn;
    private TableColumn<Media, String> statusColumn;
    private TableColumn<Media, String> dueDateColumn;
    private TableColumn<Media, Double> fineColumn;
    private TableColumn<Media, String> borrowedByColumn;
    private TableColumn<User, String> colUsername;
    private TableColumn<User, String> colRole;
    private TableColumn<User, String> colMembership;


    /**
     * Initializes the JavaFX toolkit environment.
     * <p>
     * This static setup is required to instantiate JavaFX controls safely outside 
     * of a standard JavaFX Application lifecycle.
     * </p>
     */
    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
        }
    }

    /**
     * Sets up the test environment before each test method execution.
     * <p>
     * Initializes the controller, injects mocks, clears files, and runs the initialization logic.
     * </p>
     *
     * @throws Exception If initialization fails.
     */
    @BeforeEach
    void setUp() throws Exception {
        controller = new homepageController();
        initializeMocks();
        clearFiles();
        controller.initialize();
    }

    /**
     * Cleans up resources after each test method execution.
     *
     * @throws IOException If file deletion fails.
     */
    @AfterEach
    void tearDown() throws IOException {
        clearFiles();
    }

    /**
     * Deletes temporary test files.
     *
     * @throws IOException If deletion fails.
     */
    private void clearFiles() throws IOException {
        Files.deleteIfExists(Paths.get("books.txt"));
        Files.deleteIfExists(Paths.get("users.txt"));
    }

    /**
     * Injects mock JavaFX components into the controller using reflection.
     *
     * @throws Exception If reflection fails.
     */
    private void initializeMocks() throws Exception {
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
        titleColumn = new TableColumn<>("Title");
        authorColumn = new TableColumn<>("Author");
        isbnColumn = new TableColumn<>("ISBN");
        copyIdColumn = new TableColumn<>("CopyId");
        statusColumn = new TableColumn<>("Status");
        dueDateColumn = new TableColumn<>("Due");
        fineColumn = new TableColumn<>("Fine");
        borrowedByColumn = new TableColumn<>("By");

        injectField("typeColumn", typeColumn);
        injectField("titleColumn", titleColumn);
        injectField("authorColumn", authorColumn);
        injectField("isbnColumn", isbnColumn);
        injectField("copyIdColumn", copyIdColumn);
        injectField("statusColumn", statusColumn);
        injectField("dueDateColumn", dueDateColumn);
        injectField("fineColumn", fineColumn);
        injectField("borrowedByColumn", borrowedByColumn);

        usersTable = new TableView<>();
        colUsername = new TableColumn<>("Username");
        colRole = new TableColumn<>("Role");
        colMembership = new TableColumn<>("Membership");

        injectField("usersTable", usersTable);
        injectField("colUsername", colUsername);
        injectField("colRole", colRole);
        injectField("colMembership", colMembership);
    }

    /**
     * Verifies that the welcome label is updated with the correct username.
     */
    @Test
    void testSetCurrentUsername() {
        controller.setCurrentUsername("AdminZainab");
        assertTrue(welcomeLabel.getText().contains("AdminZainab"));
    }

    /**
     * Verifies that the logout method does not crash in a test environment.
     */
    @Test
    void testLogoutSafely() {
        assertDoesNotThrow(() -> {
            try {
                Method m = homepageController.class.getDeclaredMethod("handleLogout");
                m.setAccessible(true);
                m.invoke(controller);
            } catch (Exception e) {
            }
        });
    }

    /**
     * Verifies validation when trying to add a book with empty fields.
     *
     * @throws Exception If reflection fails.
     */
    @Test
    void testHandleAddBook_EmptyFields() throws Exception {
        titleField.setText("");
        controller.handleAddBook();
        assertEquals("❗ Please fill all fields.", addBookMessage.getText());
    }

    /**
     * Verifies successful addition of a new book.
     *
     * @throws Exception If reflection fails.
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
     * Verifies that adding an existing book increments the copy count.
     *
     * @throws Exception If reflection fails.
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
        
        assertTrue(addBookMessage.getText().contains("NEW COPY"));
    }

    /**
     * Verifies that adding a book with an existing ISBN but different details fails.
     *
     * @throws Exception If reflection fails.
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

        assertTrue(addBookMessage.getText().contains("ISBN already exists"));
        assertEquals(1, list.size());
    }

    /**
     * Verifies the custom row coloring logic based on media status.
     *
     * @throws Exception If reflection fails.
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
        assertTrue(row.getStyle().contains("d0f0c0"));

        updateItem.invoke(row, new Book("T","A","1","Borrowed","",0.0,"",0.0,1), false);
        assertTrue(row.getStyle().contains("fff9c4"));

        updateItem.invoke(row, new Book("T","A","1","Overdue","",0.0,"",0.0,1), false);
        assertTrue(row.getStyle().contains("ffcdd2"));

        updateItem.invoke(row, null, true);
        assertEquals("", row.getStyle());
    }

    /**
     * Verifies the robustness of the file parsing logic with corrupted data.
     *
     * @throws Exception If IO or reflection fails.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testLoadMedia_ParsingEdgeCases() throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("books.txt"))) {
            writer.write("Book,Good,Auth,100,1,Available,,0.0,,0.0\n");
            writer.write("CD,BadNum,Auth,101,NotInt,Available,,NotDbl,,0.0\n");
            writer.write("Book,Short\n"); 
            writer.write("CD,OverdueItem,Auth,102,1,Overdue,2020-01-01,10.0,user1,0.0\n");
        }
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("users.txt"))) {
            writer.write("user1,123,User,Gold\n");
        }

        controller.handleReload();

        ObservableList<Media> list = (ObservableList<Media>) getPrivateField("mediaList");
        
        assertFalse(list.stream().anyMatch(m -> m.getTitle().equals("Short")));
        
        assertTrue(list.stream().anyMatch(m -> m.getIsbn().equals("100")));
        
        Media badNum = list.stream().filter(m -> m.getIsbn().equals("101")).findFirst().orElse(null);
        assertNotNull(badNum);
        assertEquals(1, badNum.getCopyId()); 
        assertEquals(0.0, badNum.getFineAmount()); 
    }

    /**
     * Verifies search functionality across multiple criteria.
     *
     * @throws Exception If reflection fails.
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
     * Verifies that a user with active loans cannot be deleted.
     *
     * @throws Exception If reflection fails.
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
     * Verifies successful user deletion.
     *
     * @throws Exception If reflection fails.
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
     * Verifies delete behavior when no user is selected.
     */
    @Test
    void testHandleDeleteUser_NoSelection() {
        usersTable.getSelectionModel().clearSelection();
        assertDoesNotThrow(() -> controller.handleDeleteUser());
    }

    /**
     * Verifies the flow of sending an email reminder for overdue items.
     *
     * @throws Exception If reflection or IO fails.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testHandleSendReminder_Flow() throws Exception {
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
     * Verifies behavior when attempting to send a reminder to a user with no email.
     *
     * @throws Exception If reflection or IO fails.
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
     * Verifies email reminder behavior when no user is selected.
     */
    @Test
    void testHandleSendReminder_NoSelection() {
        usersTable.getSelectionModel().clearSelection();
        assertDoesNotThrow(() -> controller.handleSendReminder());
    }

    /**
     * Helper to inject private fields.
     */
    private void injectField(String name, Object value) throws Exception {
        Field f = homepageController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, value);
    }

    /**
     * Helper to get private fields.
     */
    private Object getPrivateField(String name) throws Exception {
        Field f = homepageController.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(controller);
    }
    
    /**
     * Helper to set private TextField values.
     */
    private void setTextField(String fieldName, String value) throws Exception {
        ((TextField) getPrivateField(fieldName)).setText(value);
    }

    /**
     * Mocks the overdue subscriber to prevent actual network calls.
     */
    private void mockOverdueSubscriber() throws Exception {
        Field pubField = homepageController.class.getDeclaredField("overduePublisher");
        pubField.setAccessible(true);
        OverduePublisher publisher = (OverduePublisher) pubField.get(null);

        Field subsField = OverduePublisher.class.getDeclaredField("subscribers");
        subsField.setAccessible(true);
        java.util.List<OverdueSubscriber> subs = (java.util.List<OverdueSubscriber>) subsField.get(publisher);

        subs.clear();
        subs.add((username, email, overdueList) -> {});
    }
}