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
 * @version 1.2
 */
public class HomepageControllerTest {

    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            
        }
    }

    private homepageController controller;

    /**
     * Helper method to inject values into private fields.
     */
    private void injectField(String name, Object value) throws Exception {
        Field f = homepageController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, value);
    }

    /**
     * Helper method to retrieve values from private fields.
     */
    private Object getPrivateField(String name) throws Exception {
        Field f = homepageController.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(controller);
    }

    /**
     * Helper method to invoke private methods.
     */
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
        
        
        injectField("addBookMessage", new Label());
        injectField("welcomeLabel", new Label());
        injectField("titleField", new TextField());
        injectField("authorField", new TextField());
        injectField("isbnField", new TextField());
        
        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList("Book", "CD"));
        typeCombo.getSelectionModel().select("Book");
        injectField("typeCombo", typeCombo);

        
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
     * Verifies that adding a Book works correctly.
     */
    @Test
    void testHandleAddBook_ValidBook_AddedSuccessfully() throws Exception {
        ((TextField) getPrivateField("titleField")).setText("Clean Code");
        ((TextField) getPrivateField("authorField")).setText("Robert Martin");
        ((TextField) getPrivateField("isbnField")).setText("111");

        controller.handleAddBook();

        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");
        Label msg = (Label) getPrivateField("addBookMessage");

        assertEquals(1, mediaList.size());
        assertTrue(mediaList.get(0) instanceof Book);
        assertTrue(msg.getText().contains("New Book Added"));
    }

    /**
     * Verifies that adding a CD works correctly (Testing the else/if branch).
     */
    @Test
    void testHandleAddBook_ValidCD_AddedSuccessfully() throws Exception {
        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList("Book", "CD"));
        typeCombo.getSelectionModel().select("CD");
        injectField("typeCombo", typeCombo);

        ((TextField) getPrivateField("titleField")).setText("Greatest Hits");
        ((TextField) getPrivateField("authorField")).setText("Queen");
        ((TextField) getPrivateField("isbnField")).setText("999");

        controller.handleAddBook();

        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");
        
        assertEquals(1, mediaList.size());
        assertTrue(mediaList.get(0) instanceof CD);
    }

    /**
     * Verifies that duplicate ISBN with different details is rejected.
     */
    @Test
    void testHandleAddBook_DuplicateIsbnDifferentDetails_Rejected() throws Exception {
        
        ((TextField) getPrivateField("titleField")).setText("Book 1");
        ((TextField) getPrivateField("authorField")).setText("Author 1");
        ((TextField) getPrivateField("isbnField")).setText("123");
        controller.handleAddBook();

        
        ((TextField) getPrivateField("titleField")).setText("Book 2");
        ((TextField) getPrivateField("authorField")).setText("Author 1");
        ((TextField) getPrivateField("isbnField")).setText("123");
        controller.handleAddBook();

        Label msg = (Label) getPrivateField("addBookMessage");
        assertTrue(msg.getText().contains("ISBN already exists but with different title"));
        
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");
        assertEquals(1, mediaList.size());
    }

    /**
     * Verifies that adding a second copy works.
     */
    @Test
    void testHandleAddBook_SecondCopy_IncrementsCopyId() throws Exception {
        
        ((TextField) getPrivateField("titleField")).setText("Book 1");
        ((TextField) getPrivateField("authorField")).setText("Author 1");
        ((TextField) getPrivateField("isbnField")).setText("123");
        controller.handleAddBook();

        
        ((TextField) getPrivateField("titleField")).setText("Book 1");
        ((TextField) getPrivateField("authorField")).setText("Author 1");
        ((TextField) getPrivateField("isbnField")).setText("123");
        controller.handleAddBook();

        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");
        assertEquals(2, mediaList.size());
        assertEquals(2, mediaList.get(1).getCopyId());
    }

    /**
     * Verifies that empty fields prevent addition.
     */
    @Test
    void testHandleAddBook_MissingFields_ShowsError() throws Exception {
        ((TextField) getPrivateField("titleField")).setText("");
        controller.handleAddBook();

        Label msg = (Label) getPrivateField("addBookMessage");
        assertEquals("❗ Please fill all fields.", msg.getText());
    }

    /**
     * Verifies search functionality filters by specific criteria (Title).
     */
    @Test
    void testHandleSearch_FilterByTitle() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");
        mediaList.add(new Book("Java", "Author A", "111"));
        mediaList.add(new Book("Python", "Author B", "222"));

        TableView<Media> table = new TableView<>();
        injectField("searchResultsTable", table);
        injectField("searchField", new TextField("Java"));
        
        ComboBox<String> searchBy = new ComboBox<>(FXCollections.observableArrayList("Title"));
        searchBy.getSelectionModel().select("Title");
        injectField("searchByCombo", searchBy);

        controller.handleSearch();

        assertEquals(1, table.getItems().size());
        assertEquals("Java", table.getItems().get(0).getTitle());
    }
    
    /**
     * Verifies search functionality filters by specific criteria (Author).
     */
    @Test
    void testHandleSearch_FilterByAuthor() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");
        mediaList.add(new Book("Book 1", "John", "111"));
        mediaList.add(new Book("Book 2", "Jane", "222"));

        TableView<Media> table = new TableView<>();
        injectField("searchResultsTable", table);
        injectField("searchField", new TextField("Jane"));
        
        ComboBox<String> searchBy = new ComboBox<>(FXCollections.observableArrayList("Author"));
        searchBy.getSelectionModel().select("Author");
        injectField("searchByCombo", searchBy);

        controller.handleSearch();

        assertEquals(1, table.getItems().size());
        assertEquals("Jane", table.getItems().get(0).getAuthor());
    }

    /**
     * Verifies reloading data from file.
     */
    @Test
    void testHandleReload_LoadsFromFile() throws Exception {
        try (PrintWriter out = new PrintWriter(new FileWriter("books.txt"))) {
            out.println("Book,Reloaded Book,Author,123,1,Available,2025-01-01,0.0,,0.0");
        }

        TableView<Media> table = new TableView<>();
        injectField("searchResultsTable", table);
        
        injectField("typeColumn", new TableColumn<>("Type"));
        injectField("titleColumn", new TableColumn<>("Title"));
        injectField("authorColumn", new TableColumn<>("Author"));
        injectField("isbnColumn", new TableColumn<>("ISBN"));
        injectField("copyIdColumn", new TableColumn<>("CopyId"));
        injectField("statusColumn", new TableColumn<>("Status"));
        injectField("dueDateColumn", new TableColumn<>("Due"));
        injectField("fineColumn", new TableColumn<>("Fine"));
        injectField("borrowedByColumn", new TableColumn<>("By"));

        controller.handleReload();

        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");
        assertFalse(mediaList.isEmpty());
        assertEquals("Reloaded Book", mediaList.get(0).getTitle());
    }

    /**
     * Verifies deleting a user fails if they have active loans.
     */
    @Test
    void testHandleDeleteUser_WithActiveLoans_Fails() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<User> usersList = (ObservableList<User>) getPrivateField("usersList");
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");

        User u1 = new User("u1", "1", "User", "Gold");
        usersList.add(u1);

        Media m = new Book("B1", "A1", "111");
        m.setBorrowedBy("u1");
        mediaList.add(m);

        TableView<User> usersTable = new TableView<>();
        usersTable.setItems(usersList);
        usersTable.getSelectionModel().select(u1);
        injectField("usersTable", usersTable);

        controller.handleDeleteUser();

        assertEquals(1, usersList.size());
    }

    /**
     * Verifies successful user deletion.
     */
    @Test
    void testHandleDeleteUser_Success() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<User> usersList = (ObservableList<User>) getPrivateField("usersList");
        User u1 = new User("u1", "1", "User", "Gold");
        usersList.add(u1);

        // Prepare file for saving
        try (PrintWriter out = new PrintWriter(new FileWriter("users.txt"))) {
            out.println("admin,123,Admin,Gold");
        }

        TableView<User> usersTable = new TableView<>();
        usersTable.setItems(usersList);
        usersTable.getSelectionModel().select(u1);
        injectField("usersTable", usersTable);

        controller.handleDeleteUser();

        assertTrue(usersList.isEmpty());
    }

    /**
     * Verifies logic for sending reminders when no user is selected.
     */
    @Test
    void testHandleSendReminder_NoSelection() throws Exception {
        TableView<User> usersTable = new TableView<>();
        injectField("usersTable", usersTable);

        
        invokePrivate("handleSendReminder", new Class<?>[]{});
    }

    /**
     * Verifies logic for sending reminders when user has no email.
     */
    @Test
    void testHandleSendReminder_NoEmail() throws Exception {
        // Create user in file without email
        try (PrintWriter out = new PrintWriter(new FileWriter("users.txt"))) {
            out.println("uNoEmail,123,User,Silver"); 
        }

        User u = new User("uNoEmail", "1", "User", "Silver");
        
        TableView<User> usersTable = new TableView<>();
        usersTable.setItems(FXCollections.observableArrayList(u));
        usersTable.getSelectionModel().select(u);
        injectField("usersTable", usersTable);

        invokePrivate("handleSendReminder", new Class<?>[]{});
    }

    /**
     * Verifies logic for sending reminders when user has no overdue books.
     */
    @Test
    void testHandleSendReminder_NoOverdueBooks() throws Exception {
        // Create user with email
        try (PrintWriter out = new PrintWriter(new FileWriter("users.txt"))) {
            out.println("uGood,123,User,Silver,test@mail.com"); 
        }

        User u = new User("uGood", "1", "User", "Silver");
        
        TableView<User> usersTable = new TableView<>();
        usersTable.setItems(FXCollections.observableArrayList(u));
        usersTable.getSelectionModel().select(u);
        injectField("usersTable", usersTable);

        invokePrivate("handleSendReminder", new Class<?>[]{});
    }

    /**
     * Verifies setting current username updates the label.
     */
    @Test
    void testSetCurrentUsername() throws Exception {
        controller.setCurrentUsername("Admin");
        Label welcome = (Label) getPrivateField("welcomeLabel");
        assertEquals("Welcome, Admin 👋", welcome.getText());
    }
}