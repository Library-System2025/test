import static org.junit.jupiter.api.Assertions.*;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import org.junit.jupiter.api.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive Test Suite for the {@link LibrarianController} class.
 * <p>
 * This suite focuses on achieving maximum code coverage by testing private helper methods
 * via Reflection, verifying transaction flows, and handling edge cases in data parsing.
 * </p>
 *
 * @author Zainab
 * @version 1.0
 */
public class LibrarianControllerTest {

    private LibrarianController controller;
    private static final String BOOKS_FILE = "books.txt";

    /**
     * Initializes the JavaFX Toolkit.
     */
    @BeforeAll
    static void initToolkit() {
        try { Platform.startup(() -> {}); } catch (IllegalStateException e) {}
    }

    /**
     * Sets up the controller and cleans files before each test.
     * 
     * @throws Exception if initialization fails.
     */
    @BeforeEach
    void setUp() throws Exception {
        controller = new LibrarianController();
        new File(BOOKS_FILE).delete();
    }

    /**
     * Cleans up resources after each test.
     */
    @AfterEach
    void tearDown() {
        new File(BOOKS_FILE).delete();
    }

    /**
     * Tests private parsing helper methods using Reflection.
     * Validates `parseIntSafe`, `parseDoubleSafe`, `getPart`, and `extractBorrowedBy`
     * to ensure robustness against malformed data.
     * 
     * @throws Exception if reflection access fails.
     */
    @Test
    void testPrivateParsers() throws Exception {
        Method parseInt = LibrarianController.class.getDeclaredMethod("parseIntSafe", String.class, int.class);
        parseInt.setAccessible(true);
        assertEquals(5, parseInt.invoke(controller, "5", 0));
        assertEquals(0, parseInt.invoke(controller, "bad", 0));

        Method parseDouble = LibrarianController.class.getDeclaredMethod("parseDoubleSafe", String.class, double.class);
        parseDouble.setAccessible(true);
        assertEquals(5.5, parseDouble.invoke(controller, "5.5", 0.0));
        assertEquals(0.0, parseDouble.invoke(controller, "bad", 0.0));

        Method getPart = LibrarianController.class.getDeclaredMethod("getPart", String[].class, int.class);
        getPart.setAccessible(true);
        String[] arr = {"A", "B"};
        assertEquals("A", getPart.invoke(controller, arr, 0));
        assertEquals("", getPart.invoke(controller, arr, 5));
        
        Method extract = LibrarianController.class.getDeclaredMethod("extractBorrowedBy", String[].class);
        extract.setAccessible(true);
        String[] parts1 = {"","","","","","","","","user"};
        assertEquals("user", extract.invoke(controller, (Object)parts1));
        String[] parts2 = {"","","","","","","","","0.0"};
        assertEquals("", extract.invoke(controller, (Object)parts2));
    }

    /**
     * Tests the full initialization process, including file loading and column setup.
     * 
     * @throws Exception if file creation or reflection injection fails.
     */
    @Test
    void testInitializeFull() throws Exception {
        createFile("Book,T,A,1,1,Avail,d,0,0,0");
        
        injectField("bookTable", new TableView<>());
        injectField("typeColumn", new TableColumn<>());
        injectField("titleColumn", new TableColumn<>());
        injectField("authorColumn", new TableColumn<>());
        injectField("isbnColumn", new TableColumn<>());
        injectField("statusColumn", new TableColumn<>());
        injectField("dueDateColumn", new TableColumn<>());
        injectField("borrowedByColumn", new TableColumn<>());
        injectField("copyIdColumn", new TableColumn<>());
        
        runOnFx(() -> controller.initialize());
        
        ObservableList<?> list = (ObservableList<?>) getPrivateField("mediaList");
        assertFalse(list.isEmpty());
    }

    /**
     * Simulates a complete Borrow and Return transaction cycle.
     * Verifies that the item status updates correctly in the backend list.
     * 
     * @throws Exception if reflection invocation fails.
     */
    @Test
    void testBorrowReturnFlow() throws Exception {
        TableView<Media> table = new TableView<>();
        Label label = new Label();
        injectField("bookTable", table);
        injectField("infoLabel", label);
        
        ObservableList<Media> list = (ObservableList<Media>) getPrivateField("mediaList");
        Media m = new Book("T","A","1");
        m.setStatus("Available");
        list.add(m);
        table.setItems(list);
        
        table.getSelectionModel().select(m);
        runOnFx(() -> {
            try {
                Method b = LibrarianController.class.getDeclaredMethod("handleBorrowBook");
                b.setAccessible(true);
                b.invoke(controller);
            } catch(Exception e){}
        });
        assertEquals("Borrowed", m.getStatus());

        runOnFx(() -> {
            try {
                Method r = LibrarianController.class.getDeclaredMethod("handleReturnBook");
                r.setAccessible(true);
                r.invoke(controller);
            } catch(Exception e){}
        });
        assertEquals("Available", m.getStatus());
    }

    /**
     * Injects a value into a private field using reflection.
     * 
     * @param name The field name.
     * @param value The value to set.
     * @throws Exception If access fails.
     */
    private void injectField(String name, Object value) throws Exception {
        Field f = LibrarianController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, value);
    }

    /**
     * Retrieves the value of a private field using reflection.
     * 
     * @param name The field name.
     * @return The field value.
     * @throws Exception If access fails.
     */
    private Object getPrivateField(String name) throws Exception {
        Field f = LibrarianController.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(controller);
    }

    /**
     * Creates a dummy data file.
     * 
     * @param content The string content to write.
     * @throws IOException If writing fails.
     */
    private void createFile(String content) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(BOOKS_FILE))) {
            bw.write(content);
        }
    }

    /**
     * Executes an action on the JavaFX thread safely.
     * 
     * @param action The runnable task.
     */
    private void runOnFx(Runnable action) {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try { action.run(); } finally { latch.countDown(); }
        });
        try { latch.await(2, TimeUnit.SECONDS); } catch (InterruptedException e) {}
    }
}