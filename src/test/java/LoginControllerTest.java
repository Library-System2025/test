import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive Unit Test suite for the {@link LoginController} class.
 * <p>
 * This suite ensures high code coverage and reliability by verifying:
 * <ul>
 *   <li>Input validation logic.</li>
 *   <li>File I/O operations and parsing resilience.</li>
 *   <li>Authentication mechanisms.</li>
 *   <li>Role-based redirection logic.</li>
 * </ul>
 * It utilizes JavaFX Platform tools to simulate UI interactions safely.
 *
 * @author Zainab
 * @version 1.0
 */
public class LoginControllerTest {

    private LoginController controller;
    private TextField usernameField;
    private PasswordField passwordField;
    private Label errorMessage;
    private static final String USERS_FILE = "users.txt";

    /**
     * Initializes the JavaFX Toolkit once before all tests execution.
     */
    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
        }
    }

    /**
     * Sets up the test environment before each test method.
     * 
     * @throws Exception if reflection access fails.
     */
    @BeforeEach
    void setUp() throws Exception {
        controller = new LoginController();
        
        usernameField = new TextField();
        passwordField = new PasswordField();
        errorMessage = new Label();

        injectField("usernameField", usernameField);
        injectField("passwordField", passwordField);
        injectField("errorMessage", errorMessage);

        cleanupFile();
    }

    /**
     * Cleans up resources after each test method.
     * 
     * @throws IOException if file deletion fails.
     */
    @AfterEach
    void tearDown() throws IOException {
        cleanupFile();
    }

    /**
     * Verifies that the login handler rejects empty username or password fields.
     * 
     * @throws InterruptedException if the JavaFX thread is interrupted.
     */
    @Test
    void testHandleLogin_EmptyFields() throws InterruptedException {
        usernameField.setText("");
        passwordField.setText("");
        
        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        assertEquals("⚠️ Please fill in all fields.", errorMessage.getText());
    }

    /**
     * Verifies behavior when the user database file does not exist.
     * 
     * @throws InterruptedException if the JavaFX thread is interrupted.
     */
    @Test
    void testHandleLogin_FileNotFound() throws InterruptedException {
        usernameField.setText("user");
        passwordField.setText("pass");
        
        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        assertEquals("❌ Invalid username or password.", errorMessage.getText());
    }

    /**
     * Verifies that valid inputs but incorrect credentials result in a failure message.
     * 
     * @throws IOException if creating the test file fails.
     * @throws InterruptedException if the JavaFX thread is interrupted.
     */
    @Test
    void testHandleLogin_InvalidCredentials() throws IOException, InterruptedException {
        createUsersFile("admin,123,Admin");
        
        usernameField.setText("admin");
        passwordField.setText("WrongPass");
        
        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        assertEquals("❌ Invalid username or password.", errorMessage.getText());
        assertEquals("", passwordField.getText());
    }

    /**
     * Verifies the robustness of the file parsing logic.
     * 
     * @throws IOException if creating the test file fails.
     * @throws InterruptedException if the JavaFX thread is interrupted.
     */
    @Test
    void testValidateCredentials_ParsingEdgeCases() throws IOException, InterruptedException {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("   \n");
        sb.append("bad_line_no_comma\n");
        sb.append("user,pass\n");
        sb.append("realUser,123,User\n");
        createUsersFile(sb.toString());
        
        usernameField.setText("realUser");
        passwordField.setText("123");
        
        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        assertNotEquals("❌ Invalid username or password.", errorMessage.getText());
    }

    /**
     * Verifies the authentication and redirection flow for an Admin user.
     * 
     * @throws IOException if creating the test file fails.
     * @throws InterruptedException if the JavaFX thread is interrupted.
     */
    @Test
    void testRoleFlow_Admin() throws IOException, InterruptedException {
        createUsersFile("admin,123,Admin,Gold,admin@test.com");
        usernameField.setText("admin");
        passwordField.setText("123");

        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));

        String msg = errorMessage.getText();
        assertTrue(msg.contains("opened successfully") || msg.contains("Error loading page"), 
                   "Should attempt to load Admin page");
    }

    /**
     * Verifies the authentication and redirection flow for a Librarian user.
     * 
     * @throws IOException if creating the test file fails.
     * @throws InterruptedException if the JavaFX thread is interrupted.
     */
    @Test
    void testRoleFlow_Librarian() throws IOException, InterruptedException {
        createUsersFile("lib,123,Librarian");
        usernameField.setText("lib");
        passwordField.setText("123");

        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        String msg = errorMessage.getText();
        assertTrue(msg.contains("opened successfully") || msg.contains("Error loading page"),
                   "Should attempt to load Librarian page");
    }

    /**
     * Verifies the authentication and redirection flow for a standard User.
     * 
     * @throws IOException if creating the test file fails.
     * @throws InterruptedException if the JavaFX thread is interrupted.
     */
    @Test
    void testRoleFlow_User_Defaults() throws IOException, InterruptedException {
        createUsersFile("simpleUser,123,User"); 
        usernameField.setText("simpleUser");
        passwordField.setText("123");

        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        String msg = errorMessage.getText();
        assertTrue(msg.contains("opened successfully") || msg.contains("Error loading page"),
                   "Should attempt to load User page");
    }

    /**
     * Injects mock objects into private fields using reflection.
     */
    private void injectField(String fieldName, Object value) throws Exception {
        Field field = LoginController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
    }

    /**
     * Creates a dummy users file with specified content.
     */
    private void createUsersFile(String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE))) {
            writer.write(content);
        }
    }
    
    /**
     * Deletes the dummy users file.
     */
    private void cleanupFile() throws IOException {
        Files.deleteIfExists(Paths.get(USERS_FILE));
    }

    /**
     * Runs the given runnable on the JavaFX thread and waits for it to complete.
     */
    private void runOnFxThreadAndWait(Runnable action) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
    }
}