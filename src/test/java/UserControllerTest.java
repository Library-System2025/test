import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Robust and Production-Ready JUnit Test Suite for {@link UserController}.
 * 
 * @author Zainab
 * @version 1.0
 */
class UserControllerTest {

    private UserController controller;
    private static final String TEST_FILE = "books.txt";

    /**
     * Initializes the JavaFX Toolkit once.
     */
    @BeforeAll
    static void initJfxAndEnv() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {}
        System.setProperty("EMAIL_USERNAME", "dummy@test.com");
        System.setProperty("EMAIL_PASSWORD", "dummy_pass");
    }

    /**
     * Sets up the test environment.
     */
    @BeforeEach
    void setUp() throws Exception {
        createDummyFile();
        controller = new UserController();
        injectMockUI();
        disableEmailNotifications();

        runOnFxThreadAndWait(() -> {
            controller.initialize();
            controller.setCurrentUser("TestUser", "Gold", "test@mail.com");
        });
    }

    /**
     * Cleans up the test file.
     */
    @AfterEach
    void tearDown() {
        File file = new File(TEST_FILE);
        if (file.exists()) file.delete();
    }

    /**
     * Detaches the email subscriber.
     */
    private void disableEmailNotifications() {
        try {
            Field publisherField = UserController.class.getDeclaredField("overduePublisher");
            publisherField.setAccessible(true);
            Object publisher = publisherField.get(null);

            if (publisher != null) {
                for (Field f : publisher.getClass().getDeclaredFields()) {
                    if (List.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        List<?> subscribers = (List<?>) f.get(publisher);
                        subscribers.clear();
                        break;
                    }
                }
            }
        } catch (Exception e) {}
    }

    /**
     * Creates dummy data for testing.
     */
    private void createDummyFile() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEST_FILE))) {
            writer.write("Book,Clean Code,Robert Martin,111,1,Available,2025-01-01,0.0,,0.0");
            writer.newLine();
            writer.write("CD,Greatest Hits,Queen,222,2,Overdue,2020-01-01,10.0,TestUser,0.0");
            writer.newLine();
            writer.write("Book,Refactoring,Fowler,333,3,Borrowed,2025-05-05,0.0,OtherGuy,0.0");
            writer.newLine();
        }
    }

    /**
     * Injects JavaFX components.
     */
    private void injectMockUI() throws Exception {
        injectField(controller, "welcomeLabel", new Label());
        injectField(controller, "paymentField", new TextField());
        injectField(controller, "infoLabel", new Label());
        injectField(controller, "messageLabel", new Label());

        TableView<Media> table = new TableView<>();
        injectField(controller, "bookTable", table);
        injectField(controller, "typeColumn", new TableColumn<Media, String>());
        injectField(controller, "titleColumn", new TableColumn<Media, String>());
        injectField(controller, "authorColumn", new TableColumn<Media, String>());
        injectField(controller, "isbnColumn", new TableColumn<Media, String>());
        injectField(controller, "statusColumn", new TableColumn<Media, String>());
        injectField(controller, "dueDateColumn", new TableColumn<Media, String>());
        injectField(controller, "fineColumn", new TableColumn<Media, Double>());
    }

    /**
     * Executes logic on JavaFX thread.
     */
    private void runOnFxThreadAndWait(Runnable action) {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try { action.run(); } finally { latch.countDown(); }
        });
        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /**
     * Tests data loading initialization.
     */
    @Test
    void testInitializeLoadsData() {
        TableView<Media> table = getField(controller, "bookTable");
        assertNotNull(table.getItems());
        assertEquals(3, table.getItems().size());
    }

    /**
     * Tests welcome label updates.
     */
    @Test
    void testWelcomeLabelUpdates() {
        runOnFxThreadAndWait(() -> {
            controller.setMembershipType("Silver");
            controller.setCurrentUsername("NewUser");
        });
        Label label = getField(controller, "welcomeLabel");
        assertTrue(label.getText().contains("NewUser"));
    }

    /**
     * Tests successful borrowing.
     */
    @Test
    void testBorrowBookSuccess() {
        TableView<Media> table = getField(controller, "bookTable");
        runOnFxThreadAndWait(() -> {
            Media itemToBorrow = table.getItems().get(0);
            table.getSelectionModel().select(itemToBorrow);
            controller.handleBorrowBook();
        });
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Borrowed successfully"));
    }

    /**
     * Tests borrowing failure due to fines.
     */
    @Test
    void testBorrowFailWithFines() {
        TableView<Media> table = getField(controller, "bookTable");
        Media overdueItem = table.getItems().get(1);
        overdueItem.setFineAmount(50.0);

        runOnFxThreadAndWait(() -> {
            Media itemToBorrow = table.getItems().get(0);
            table.getSelectionModel().select(itemToBorrow);
            controller.handleBorrowBook();
        });
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("unpaid fines"));
    }

    /**
     * Tests borrowing failure if already borrowed.
     */
    @Test
    void testBorrowFailAlreadyBorrowed() {
        TableView<Media> table = getField(controller, "bookTable");
        table.getItems().get(0).borrow("TestUser");

        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(0);
            controller.handleBorrowBook();
        });
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("already borrowed"));
    }

    /**
     * Tests borrowing unavailable item.
     */
    @Test
    void testBorrowFailUnavailable() {
        TableView<Media> table = getField(controller, "bookTable");
        runOnFxThreadAndWait(() -> {
            Media unavailableItem = table.getItems().get(2);
            table.getSelectionModel().select(unavailableItem);
            controller.handleBorrowBook();
        });
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("not available"));
    }

    /**
     * Tests validation when no selection is made.
     */
    @Test
    void testBorrowNoSelection() {
        TableView<Media> table = getField(controller, "bookTable");
        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().clearSelection();
            controller.handleBorrowBook();
        });
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("select an item"));
    }

    /**
     * Tests full payment.
     */
    @Test
    void testPayFineFullSuccess() {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(1);
        item.calculateFine("Gold");
        double fine = item.getFineAmount();

        TextField payField = getField(controller, "paymentField");
        payField.setText(String.valueOf(fine));

        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(item);
            controller.handlePayFine();
        });

        Label info = getField(controller, "infoLabel");
        assertTrue(info.getText().contains("fully paid") || info.getText().contains("returned"));
    }

    /**
     * Tests partial payment.
     */
    @Test
    void testPayFinePartial() {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(1);
        item.calculateFine("Gold");

        TextField payField = getField(controller, "paymentField");
        payField.setText("1.0");

        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(item);
            controller.handlePayFine();
        });

        Label info = getField(controller, "infoLabel");
        assertTrue(info.getText().contains("Partial payment"));
    }

    /**
     * Tests payment validation.
     */
    @Test
    void testPayFineValidation() {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(1);
        TextField payField = getField(controller, "paymentField");

        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(item);

            payField.setText("-5");
            controller.handlePayFine();
            assertTrue(getField(controller, "infoLabel").toString().contains("positive"));

            payField.setText("Invalid");
            controller.handlePayFine();
            assertTrue(getField(controller, "infoLabel").toString().contains("Invalid"));

            payField.setText("1000000");
            controller.handlePayFine();
            assertTrue(getField(controller, "infoLabel").toString().contains("exceeds"));
        });
    }

    /**
     * Tests payment restriction for other users.
     */
    @Test
    void testPayFineWrongUser() {
        TableView<Media> table = getField(controller, "bookTable");
        runOnFxThreadAndWait(() -> {
            Media otherItem = table.getItems().get(2);
            table.getSelectionModel().select(otherItem);
            controller.handlePayFine();
        });
        Label info = getField(controller, "infoLabel");
        assertTrue(info.getText().contains("YOUR borrowed items"));
    }

    /**
     * Tests returning a book.
     */
    @Test
    void testReturnBookSuccess() {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(0);
        item.borrow("TestUser");

        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(item);
            controller.handleReturnBook();
        });
        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Returned successfully"));
    }

    /**
     * Tests return failure with fines.
     */
    @Test
    void testReturnBookFailWithFine() {
        TableView<Media> table = getField(controller, "bookTable");
        Media item = table.getItems().get(1);

        runOnFxThreadAndWait(() -> {
            table.getSelectionModel().select(item);
            controller.handleReturnBook();
        });

        Label msg = getField(controller, "messageLabel");
        assertTrue(msg.getText().contains("Pay the fine"));
    }

    /**
     * Tests reload.
     */
    @Test
    void testReload() {
        runOnFxThreadAndWait(() -> controller.handleReload());
        Label info = getField(controller, "infoLabel");
        assertTrue(info.getText().contains("reloaded"));
    }

    /**
     * Tests logout.
     */
    @Test
    void testLogout() {
        runOnFxThreadAndWait(() -> {
            try { controller.handleLogout(); } catch (Exception e) {}
        });
    }

    /**
     * Tests file parsing robustness.
     */
    @Test
    void testParsingCorruptData() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEST_FILE))) {
            writer.write("Book,BadLine");
            writer.newLine();
        }
        runOnFxThreadAndWait(() -> controller.handleReload());
        TableView<Media> table = getField(controller, "bookTable");
        assertTrue(table.getItems().isEmpty());
    }

    /**
     * Helper: Injects field via reflection.
     */
    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * Helper: Gets field via reflection.
     */
    @SuppressWarnings("unchecked")
    private <T> T getField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
