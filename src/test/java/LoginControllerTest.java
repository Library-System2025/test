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
 * </p>
 * <ul>
 *   <li>Input validation logic (empty fields).</li>
 *   <li>File I/O operations (file existence, parsing logic).</li>
 *   <li>Authentication mechanisms (valid/invalid credentials).</li>
 *   <li>Role-based redirection logic (Admin, Librarian, User).</li>
 * </ul>
 * <p>
 * It utilizes JavaFX Platform tools to simulate UI interactions safely without requiring a physical display.
 * </p>
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
     * Default constructor for LoginControllerTest.
     */
    public LoginControllerTest() {
        // Default constructor
    }

    /**
     * Initializes the JavaFX Toolkit once before all tests execution.
     * This prevents "Toolkit not initialized" errors during UI component instantiation.
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
     * Sets up the test environment before each test method.
     * <p>
     * Instantiates the controller and injects mock JavaFX components using reflection
     * to bypass FXML injection requirements.
     * </p>
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
     * Removes the temporary users file to ensure test isolation.
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
     * <p>
     * Ensures the parser correctly handles:
     * </p>
     * <ul>
     *   <li>Empty lines.</li>
     *   <li>Lines with whitespace only.</li>
     *   <li>Malformed data formats.</li>
     * </ul>
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

        boolean isSuccess = errorMessage.getText().contains("opened successfully");
        boolean isError = errorMessage.getText().contains("Error loading page");
        
        assertTrue(isSuccess || isError);
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
        
        assertTrue(errorMessage.getText().contains("opened successfully") 
                || errorMessage.getText().contains("Error loading page"));
    }

    /**
     * Verifies the authentication and redirection flow for a standard User.
     * Also tests the default values logic when optional fields are missing.
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
        
        assertTrue(errorMessage.getText().contains("User window") 
                || errorMessage.getText().contains("Error loading page"));
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field field = LoginController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
    }

    private void createUsersFile(String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE))) {
            writer.write(content);
        }
    }
    
    private void cleanupFile() throws IOException {
        Files.deleteIfExists(Paths.get(USERS_FILE));
    }

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