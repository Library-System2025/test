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
 * Integration tests for the {@link homepageController} class.
 * <p>
 * This class utilizes Java Reflection to access private fields and methods
 * within the controller, allowing for comprehensive white-box testing of
 * UI logic, data persistence, and validation rules without relying on the
 * full JavaFX application lifecycle.
 * </p>
 *
 * @author Zainab
 * @version 2.0
 */
public class HomepageControllerTest {

    private homepageController controller;

    /**
     * Initializes the JavaFX toolkit environment.
     * This is required to instantiate JavaFX controls like Labels and TableViews
     * outside of a standard Application start method.
     */
    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Intentionally ignored: toolkit may already be initialized in some environments.
        }
    }

    /**
     * Sets up the test environment before each test execution.
     * <p>
     * This method initializes the controller, injects mock UI components using reflection,
     * and clears any existing data files to ensure test isolation.
     * </p>
     *
     * @throws Exception if reflection access fails.
     */
    @BeforeEach
    void setUp() throws Exception {
        controller = new homepageController();

        injectField("mediaList", FXCollections.observableArrayList());
        injectField("mediaMap", new java.util.HashMap<String, Media>());
        injectField("usersList", FXCollections.observableArrayList());

        injectField("addBookMessage", new Label());
        injectField("welcomeLabel", new Label());
        injectField("titleField", new TextField());
        injectField("authorField", new TextField());
        injectField("isbnField", new TextField());
        injectField("searchField", new TextField());

        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList("Book", "CD"));
        typeCombo.getSelectionModel().select("Book");
        injectField("typeCombo", typeCombo);

        ComboBox<String> searchByCombo = new ComboBox<>(FXCollections.observableArrayList("All", "Title", "Author", "ISBN"));
        searchByCombo.getSelectionModel().select("All");
        injectField("searchByCombo", searchByCombo);

        TableView<Media> searchResultsTable = new TableView<>();
        injectField("searchResultsTable", searchResultsTable);

        injectField("typeColumn", new TableColumn<>("Type"));
        injectField("titleColumn", new TableColumn<>("Title"));
        injectField("authorColumn", new TableColumn<>("Author"));
        injectField("isbnColumn", new TableColumn<>("ISBN"));
        injectField("copyIdColumn", new TableColumn<>("CopyId"));
        injectField("statusColumn", new TableColumn<>("Status"));
        injectField("dueDateColumn", new TableColumn<>("Due"));
        injectField("fineColumn", new TableColumn<>("Fine"));
        injectField("borrowedByColumn", new TableColumn<>("By"));

        TableView<User> usersTable = new TableView<>();
        injectField("usersTable", usersTable);
        injectField("colUsername", new TableColumn<>("Username"));
        injectField("colRole", new TableColumn<>("Role"));
        injectField("colMembership", new TableColumn<>("Membership"));

        Files.deleteIfExists(Paths.get("books.txt"));
        Files.deleteIfExists(Paths.get("users.txt"));

        controller.initialize();
    }

    /**
     * Cleans up resources after each test execution.
     * Deletes temporary files created during testing.
     *
     * @throws IOException if file deletion fails.
     */
    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Paths.get("books.txt"));
        Files.deleteIfExists(Paths.get("users.txt"));
    }

    /**
     * Injects a value into a private field of the controller using reflection.
     *
     * @param name  the name of the field.
     * @param value the value to inject.
     * @throws Exception if the field cannot be accessed.
     */
    private void injectField(String name, Object value) throws Exception {
        Field f = homepageController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, value);
    }

    /**
     * Retrieves the value of a private field from the controller using reflection.
     *
     * @param name the name of the field.
     * @return the value of the field.
     * @throws Exception if the field cannot be accessed.
     */
    private Object getPrivateField(String name) throws Exception {
        Field f = homepageController.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(controller);
    }

    /**
     * Verifies that the TableView RowFactory correctly styles rows based on media status.
     * Checks logic for 'Available', 'Borrowed', and 'Overdue' statuses.
     *
     * @throws Exception if reflection or method invocation fails.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testRowFactory_Coloring() throws Exception {
        TableView<Media> table = (TableView<Media>) getPrivateField("searchResultsTable");
        Callback<TableView<Media>, TableRow<Media>> factory = table.getRowFactory();
        assertNotNull(factory);

        TableRow<Media> row = factory.call(table);
        Method updateItem = javafx.scene.control.Cell.class.getDeclaredMethod("updateItem", Object.class, boolean.class);
        updateItem.setAccessible(true);

        updateItem.invoke(row, new Book("A","B","1","Available","",0.0,"",0.0,1), false);
        assertEquals("-fx-background-color: #d0f0c0;", row.getStyle());

        updateItem.invoke(row, new Book("A","B","1","Borrowed","",0.0,"",0.0,1), false);
        assertEquals("-fx-background-color: #fff9c4;", row.getStyle());

        updateItem.invoke(row, new Book("A","B","1","Overdue","",0.0,"",0.0,1), false);
        assertEquals("-fx-background-color: #ffcdd2;", row.getStyle());

        updateItem.invoke(row, null, true);
        assertEquals("", row.getStyle());
    }

    @Test
    void testHandleAddBook_EmptyFields() throws Exception {
        ((TextField) getPrivateField("titleField")).setText("");
        controller.handleAddBook();
        Label msg = (Label) getPrivateField("addBookMessage");
        assertEquals("❗ Please fill all fields.", msg.getText());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testHandleAddBook_Success_And_NewCopy() throws Exception {
        TextField t = (TextField) getPrivateField("titleField");
        TextField a = (TextField) getPrivateField("authorField");
        TextField i = (TextField) getPrivateField("isbnField");
        ComboBox<String> type = (ComboBox<String>) getPrivateField("typeCombo");

        t.setText("Java 101"); a.setText("Gosling"); i.setText("999"); type.setValue("Book");
        controller.handleAddBook();

        ObservableList<Media> list = (ObservableList<Media>) getPrivateField("mediaList");
        assertEquals(1, list.size());
        assertEquals(1, list.get(0).getCopyId());

        t.setText("Java 101"); a.setText("Gosling"); i.setText("999");
        controller.handleAddBook();

        assertEquals(2, list.size());
        assertEquals(2, list.get(1).getCopyId());
        Label msg = (Label) getPrivateField("addBookMessage");
        assertTrue(msg.getText().contains("NEW COPY"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testHandleAddBook_Conflict_ISBN() throws Exception {
        ObservableList<Media> list = (ObservableList<Media>) getPrivateField("mediaList");
        Map<String, Media> map = (Map<String, Media>) getPrivateField("mediaMap");
        Media m1 = new Book("Original", "Author", "123", "Available", "", 0.0, "", 0.0, 1);
        list.add(m1);
        map.put("123", m1);

        ((TextField) getPrivateField("titleField")).setText("Fake Book");
        ((TextField) getPrivateField("authorField")).setText("Other Guy");
        ((TextField) getPrivateField("isbnField")).setText("123");

        controller.handleAddBook();

        Label msg = (Label) getPrivateField("addBookMessage");
        assertTrue(msg.getText().contains("ISBN already exists but with different title"));
        assertEquals(1, list.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testHandleAddBook_CD_Logic() throws Exception {
        ComboBox<String> type = (ComboBox<String>) getPrivateField("typeCombo");
        type.setValue("CD");

        ((TextField) getPrivateField("titleField")).setText("Best Hits");
        ((TextField) getPrivateField("authorField")).setText("Singer");
        ((TextField) getPrivateField("isbnField")).setText("CD-001");

        controller.handleAddBook();

        ObservableList<Media> list = (ObservableList<Media>) getPrivateField("mediaList");
        assertTrue(list.get(0) instanceof CD);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testHandleSearch_AllModes() throws Exception {
        ObservableList<Media> list = (ObservableList<Media>) getPrivateField("mediaList");
        list.add(new Book("Java", "James", "111", "Available", "", 0.0, "", 0.0, 1));
        list.add(new Book("Python", "Guido", "222", "Available", "", 0.0, "", 0.0, 1));

        TextField searchField = (TextField) getPrivateField("searchField");
        ComboBox<String> combo = (ComboBox<String>) getPrivateField("searchByCombo");
        TableView<Media> table = (TableView<Media>) getPrivateField("searchResultsTable");

        searchField.setText("Java");
        combo.setValue("Title");
        controller.handleSearch();
        assertEquals(1, table.getItems().size());

        searchField.setText("Guido");
        combo.setValue("Author");
        controller.handleSearch();
        assertEquals(1, table.getItems().size());

        searchField.setText("111");
        combo.setValue("ISBN");
        controller.handleSearch();
        assertEquals(1, table.getItems().size());

        searchField.setText("");
        controller.handleSearch();
        assertEquals(2, table.getItems().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testHandleReload_And_LoadFromFile() throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("books.txt"))) {
            writer.write("Book,Clean Code,Uncle Bob,555,1,Available,,0.0,,0.0");
            writer.newLine();
            writer.write("CD,Music,Artist,666,1,Borrowed,2025-01-01,0.0,ali,0.0");
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("users.txt"))) {
            writer.write("ali,123,User,Gold");
        }

        controller.handleReload();

        ObservableList<Media> list = (ObservableList<Media>) getPrivateField("mediaList");
        assertEquals(2, list.size());
        assertEquals("Clean Code", list.get(0).getTitle());
        assertEquals("Music", list.get(1).getTitle());
    }

    @Test
    void testHandleReload_SetsReloadMessage() throws Exception {
        controller.handleReload();
        Label msg = (Label) getPrivateField("addBookMessage");
        assertEquals("🔄 Reloaded.", msg.getText());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testHandleDeleteUser_NoSelection() throws Exception {
        TableView<User> table = (TableView<User>) getPrivateField("usersTable");
        table.getSelectionModel().clearSelection();

        controller.handleDeleteUser();
        // Implicit assertion: no exception thrown
    }

    @Test
    @SuppressWarnings("unchecked")
    void testHandleDeleteUser_WithLoans() throws Exception {
        ObservableList<User> users = (ObservableList<User>) getPrivateField("usersList");
        User u = new User("loaner", "1", "User", "Silver");
        users.add(u);

        ObservableList<Media> media = (ObservableList<Media>) getPrivateField("mediaList");
        media.add(new Book("B", "A", "I", "Borrowed", "", 0.0, "loaner", 0.0, 1));

        TableView<User> table = (TableView<User>) getPrivateField("usersTable");
        table.setItems(users);
        table.getSelectionModel().select(u);

        controller.handleDeleteUser();

        assertEquals(1, users.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testHandleDeleteUser_Success() throws Exception {
        ObservableList<User> users = (ObservableList<User>) getPrivateField("usersList");
        User u = new User("free", "1", "User", "Silver");
        users.add(u);

        TableView<User> table = (TableView<User>) getPrivateField("usersTable");
        table.setItems(users);
        table.getSelectionModel().select(u);

        controller.handleDeleteUser();

        assertTrue(users.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSendReminder_Validation() throws Exception {

        controller.handleSendReminder();

        ObservableList<User> users = (ObservableList<User>) getPrivateField("usersList");
        User noEmailUser = new User("noemail", "1", "User", "Silver");
        users.add(noEmailUser);

        TableView<User> table = (TableView<User>) getPrivateField("usersTable");
        table.setItems(users);
        table.getSelectionModel().select(noEmailUser);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("users.txt"))) {
            writer.write("noemail,1,User,Silver,");
        }
        controller.handleSendReminder();

        User goodUser = new User("good", "1", "User", "Gold");
        users.add(goodUser);
        table.getSelectionModel().select(goodUser);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("users.txt"))) {
            writer.write("good,1,User,Gold,test@test.com");
        }

        controller.handleSendReminder();
    }

    /**
     * Verifies the success path of handleSendReminder:
     * user has an email AND at least one overdue item,
     * while stubbing the real email sending via subscribers list.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testHandleSendReminder_SuccessPath() throws Exception {

        ObservableList<User> users =
                (ObservableList<User>) getPrivateField("usersList");
        User u = new User("good2", "1", "User", "Gold");
        users.add(u);

        TableView<User> table =
                (TableView<User>) getPrivateField("usersTable");
        table.setItems(users);
        table.getSelectionModel().select(u);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("users.txt"))) {
            writer.write("good2,1,User,Gold,good2@test.com");
            writer.newLine();
        }

        ObservableList<Media> media =
                (ObservableList<Media>) getPrivateField("mediaList");

        Media overdueItem = new Book(
                "Late Book",
                "Author",
                "XYZ",
                "Overdue",
                "2000-01-01",
                0.0,
                "good2",
                0.0,
                1
        );
        media.add(overdueItem);

        // 3) replace subscribers in OverduePublisher with a no-op subscriber
        Field pubField = homepageController.class.getDeclaredField("overduePublisher");
        pubField.setAccessible(true);
        OverduePublisher publisher = (OverduePublisher) pubField.get(null); // static field

        Field subsField = OverduePublisher.class.getDeclaredField("subscribers");
        subsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.List<OverdueSubscriber> subs =
                (java.util.List<OverdueSubscriber>) subsField.get(publisher);

        subs.clear();
        subs.add(new OverdueSubscriber() {
            @Override
            public void update(String username, String email, java.util.List<Media> overdueList) {
                // Intentionally left blank for this test:
                // we only need a no-op subscriber to verify that publish()
                // is invoked without sending real emails or throwing exceptions.
            }
        });

        assertDoesNotThrow(() -> controller.handleSendReminder());
    }

    @Test
    void testGetUserMembership_FoundAndDefault() throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("users.txt"))) {
            writer.write("ali,123,User,Gold");
            writer.newLine();
        }

        Method m = homepageController.class.getDeclaredMethod("getUserMembership", String.class);
        m.setAccessible(true);

        String membershipExisting = (String) m.invoke(controller, "ali");
        String membershipMissing = (String) m.invoke(controller, "someone");

        assertEquals("Gold", membershipExisting);
        assertEquals("Silver", membershipMissing);
    }

    @Test
    void testGetUserEmail_FoundAndEmpty() throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("users.txt"))) {
            writer.write("ali,123,User,Gold,ali@mail.com");
            writer.newLine();
        }

        Method m = homepageController.class.getDeclaredMethod("getUserEmail", String.class);
        m.setAccessible(true);

        String emailExisting = (String) m.invoke(controller, "ali");
        String emailMissing = (String) m.invoke(controller, "x");

        assertEquals("ali@mail.com", emailExisting);
        assertEquals("", emailMissing);
    }

    @Test
    void testShowAlert_NonFxThread_DoesNotThrow() throws Exception {
        Method m = homepageController.class.getDeclaredMethod("showAlert", String.class, String.class);
        m.setAccessible(true);

        assertDoesNotThrow(() -> {
            try {
                m.invoke(controller, "Title", "Content");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testSetCurrentUsername() {
        controller.setCurrentUsername("Zainab");
        try {
            Label l = (Label) getPrivateField("welcomeLabel");
            assertEquals("Welcome, Zainab 👋", l.getText());
        } catch (Exception e) {
            fail("Failed to access welcomeLabel");
        }
    }
}
