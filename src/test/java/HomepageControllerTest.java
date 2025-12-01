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
 * @version 1.0
 */

public class HomepageControllerTest {

	/**
     * Initializes the JavaFX Toolkit before all tests.
     * This is necessary to create UI controls (Labels, TextFields) in a test environment.
     */
    
    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            
        }
    }

    private homepageController controller;

    /**
     * Injects a value into a private field of the controller.
     * 
     * @param name  The name of the field.
     * @param value The value to set.
     * @throws Exception If reflection fails.
     */

    private void injectField(String name, Object value) throws Exception {
        
        Field f = homepageController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, value);
    }

    /**
     * Retrieves the value of a private field from the controller.
     * 
     * @param name The name of the field.
     * @return The value of the field.
     * @throws Exception If reflection fails.
     */
    
    private Object getPrivateField(String name) throws Exception {
        Field f = homepageController.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(controller);
    }

    /**
     * Invokes a private method of the controller.
     * 
     * @param name  The name of the method.
     * @param types The parameter types of the method.
     * @param args  The arguments to pass.
     * @return The result of the method invocation.
     * @throws Exception If reflection fails.
     */
    
    private Object invokePrivate(String name, Class<?>[] types, Object... args) throws Exception {
        Method m = homepageController.class.getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(controller, args);
    }

    /**
     * Sets up the test environment before each test.
     * Initializes the controller and cleans up any existing data files.
     * 
     * @throws Exception If setup fails.
     */

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

    /**
     * Cleans up resources after each test.
     * Deletes temporary data files.
     */
    
    @AfterEach
    void tearDown() {
        
        new File("books.txt").delete();
        new File("users.txt").delete();
    }

    /**
     * Verifies that a valid book is added successfully and that duplicate ISBNs are rejected.
     * 
     * @throws Exception If reflection or IO fails.
     */
    
    @Test
    void testHandleAddBook_validBook_addedOnce() throws Exception {
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

        
        controller.handleAddBook();

        
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");
        assertEquals(1, mediaList.size(), "List should have 1 item");
        assertEquals("111", mediaList.get(0).getIsbn());
        assertEquals("✅ Book added successfully.", addBookMessage.getText());

        
        titleField.setText("Another Title");
        authorField.setText("Another Author");
        isbnField.setText("111"); 

        controller.handleAddBook();

        
        assertEquals("❌ Item with this ISBN exists.", addBookMessage.getText());
        assertEquals(1, mediaList.size(), "List size should remain 1");
    }

    /**
     * Verifies that the system prevents adding a book with missing fields.
     * 
     * @throws Exception If reflection fails.
     */
    
    @Test
    void testHandleAddBook_missingFields_showsError() throws Exception {
        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList("Book"));
        typeCombo.getSelectionModel().select("Book");

        injectField("typeCombo", typeCombo);
        injectField("titleField", new TextField("")); 
        injectField("authorField", new TextField("Author"));
        injectField("isbnField", new TextField("111"));

        Label msgLabel = new Label();
        injectField("addBookMessage", msgLabel);

        controller.handleAddBook();

        assertEquals("❗ Please fill all fields.", msgLabel.getText());
    }

    /**
     * Verifies that an empty search keyword returns all items in the library.
     * 
     * @throws Exception If reflection fails.
     */

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

    /**
     * Verifies that the system correctly loads non-admin users from the 'users.txt' file.
     * 
     * @throws Exception If reflection or IO fails.
     */

    @Test
    void testLoadUsersFromFile_readsNonAdminUsers() throws Exception {
        // Create dummy users file
        try (PrintWriter out = new PrintWriter(new FileWriter("users.txt"))) {
            out.println("admin,123,Admin,Gold,admin@mail.com");
            out.println("user1,1,User,Silver,user1@mail.com");
        }

        TableView<User> usersTable = new TableView<>();
        injectField("usersTable", usersTable);

        
        invokePrivate("loadUsersFromFile", new Class<?>[] {});

        @SuppressWarnings("unchecked")
        ObservableList<User> usersList = (ObservableList<User>) getPrivateField("usersList");

        
        assertEquals(1, usersList.size());
        assertEquals("user1", usersList.get(0).getUsername());
    }

    /**
     * Verifies that the 'Reload' functionality correctly reads data from 'books.txt'
     * and updates the UI.
     * 
     * @throws Exception If reflection or IO fails.
     */

    @Test
    void testHandleReload_loadsFromFileAndUpdatesMessage() throws Exception {
        
        try (PrintWriter out = new PrintWriter(new FileWriter("books.txt"))) {
            
            out.println("Book,Clean Code,Robert Martin,111,Borrowed,2025-12-20,0.0,u1,0.0");
        }

        TableView<Media> table = new TableView<>();
        Label addBookMessage = new Label();

        injectField("searchResultsTable", table);
        injectField("addBookMessage", addBookMessage);

        
        controller.handleReload();

        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");

        assertFalse(mediaList.isEmpty(), "Media list should be populated from file");
        assertEquals("Clean Code", mediaList.get(0).getTitle());
        assertEquals("🔄 Reloaded.", addBookMessage.getText());
    }

    /**
     * Verifies that a user cannot be deleted if they have active loans.
     * 
     * @throws Exception If reflection fails.
     */

    @Test
    void testHandleDeleteUser_userHasLoans_notDeleted() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<User> usersList = (ObservableList<User>) getPrivateField("usersList");
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");

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

        assertEquals(1, usersList.size(), "User should NOT be deleted if they have active loans");
    }

    /**
     * Verifies that a user with no active loans is successfully deleted from the system
     * and the file is updated.
     * 
     * @throws Exception If reflection or IO fails.
     */
    
    @Test
    void testHandleDeleteUser_success() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<User> usersList = (ObservableList<User>) getPrivateField("usersList");

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
