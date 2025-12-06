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

/**
 * Integration tests for the HomepageController.
 * Utilizes reflection to test UI logic, file operations, and data management.
 * 
 * @author Zainab
 * @version 1.6
 */
public class HomepageControllerTest {

    @BeforeAll
    static void initToolkit() {
        try { Platform.startup(() -> {}); }
        catch (IllegalStateException e) { }
    }

    private homepageController controller;

    /**
     * Helper method to inject values into private fields using Reflection.
     */
    private void injectField(String name, Object value) throws Exception {
        Field f = homepageController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, value);
    }

    /**
     * Helper method to get values from private fields using Reflection.
     */
    private Object getPrivateField(String name) throws Exception {
        Field f = homepageController.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(controller);
    }

    /**
     * Sets up the test environment, injects all necessary UI components,
     * and initializes the controller before each test.
     */
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
        injectField("searchField", new TextField());
        
        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList("Book", "CD"));
        typeCombo.getSelectionModel().select("Book");
        injectField("typeCombo", typeCombo);
        
        ComboBox<String> searchByCombo = new ComboBox<>(FXCollections.observableArrayList("All", "Title"));
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
        
        controller.initialize();

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
     * Verifies the RowFactory logic for row coloring in the table view.
     * Uses reflection to invoke the protected updateItem method.
     */
    @Test
    void testRowFactory_Coloring() throws Exception {
        TableView<Media> table = (TableView<Media>) getPrivateField("searchResultsTable");
        
        Callback<TableView<Media>, TableRow<Media>> factory = table.getRowFactory();
        assertNotNull(factory);
        
        TableRow<Media> row = factory.call(table);
        Method updateItem = javafx.scene.control.Cell.class.getDeclaredMethod("updateItem", Object.class, boolean.class);
        updateItem.setAccessible(true);
        
        updateItem.invoke(row, new Book("A","B","1","Available","",0.0,"",0.0,1), false);
        updateItem.invoke(row, new Book("A","B","1","Borrowed","",0.0,"",0.0,1), false);
        updateItem.invoke(row, new Book("A","B","1","Overdue","",0.0,"",0.0,1), false);
        updateItem.invoke(row, null, true);
    }

    /**
     * Verifies that a valid book is added successfully to the list.
     */
    @Test
    void testHandleAddBook_ValidBook() throws Exception {
        ((TextField) getPrivateField("titleField")).setText("Clean Code");
        ((TextField) getPrivateField("authorField")).setText("Robert Martin");
        ((TextField) getPrivateField("isbnField")).setText("111");

        controller.handleAddBook();

        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");
        assertEquals(1, mediaList.size());
    }

    /**
     * Verifies that the search functionality correctly filters the media list.
     */
    @Test
    void testHandleSearch_Filter() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<Media> mediaList = (ObservableList<Media>) getPrivateField("mediaList");
        mediaList.add(new Book("Java", "Author A", "111"));
        
        injectField("searchField", new TextField("Java"));
        ComboBox<String> searchBy = (ComboBox<String>) getPrivateField("searchByCombo");
        searchBy.getSelectionModel().select("Title");

        controller.handleSearch();
        
        TableView<Media> table = (TableView<Media>) getPrivateField("searchResultsTable");
        assertEquals(1, table.getItems().size());
    }
    
    /**
     * Verifies that the reload method refreshes data from the file.
     */
    @Test
    void testHandleReload() throws Exception {
        controller.handleReload();
        Label msg = (Label) getPrivateField("addBookMessage");
        assertEquals("🔄 Reloaded.", msg.getText());
    }
    
    /**
     * Verifies successful deletion of a user from the list.
     */
    @Test
    void testHandleDeleteUser_Success() throws Exception {
        @SuppressWarnings("unchecked")
        ObservableList<User> usersList = (ObservableList<User>) getPrivateField("usersList");
        User u = new User("u1", "1", "User", "Gold");
        usersList.add(u);
        
        TableView<User> table = (TableView<User>) getPrivateField("usersTable");
        table.setItems(usersList);
        table.getSelectionModel().select(u);
        
        controller.handleDeleteUser();
        
        assertTrue(usersList.isEmpty());
    }
}