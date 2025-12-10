import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Comprehensive Unit Test suite for the {@link LoginController} class.
 * <p>
 * This class verifies the authentication logic, file parsing mechanisms,
 * and error handling scenarios. It utilizes Java Reflection to inject
 * JavaFX dependencies and handles JavaFX threading constraints to ensure
 * robust execution in a headless environment without relying on Swing interoperability.
 * </p>
 *
 * @author Zainab
 * @version 2.0
 */
public class LoginControllerTest {

    private LoginController controller;
    private TextField usernameField;
    private PasswordField passwordField;
    private Label errorMessage;

    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        controller = new LoginController();
        
        usernameField = new TextField();
        passwordField = new PasswordField();
        errorMessage = new Label();

        setPrivateField("usernameField", usernameField);
        setPrivateField("passwordField", passwordField);
        setPrivateField("errorMessage", errorMessage);

        Files.deleteIfExists(Paths.get("users.txt"));
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Paths.get("users.txt"));
    }

    private void setPrivateField(String fieldName, Object value) throws Exception {
        Field field = LoginController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
    }

    private void createUsersFile(String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("users.txt"))) {
            writer.write(content);
        }
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

    /**
     * Tests that the login process halts and displays an error when fields are empty.
     * 
     * @throws InterruptedException if the thread is interrupted.
     */
    @Test
    void testHandleLogin_EmptyFields() throws InterruptedException {
        usernameField.setText("");
        passwordField.setText("");
        
        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        assertEquals("⚠️ Please fill in all fields.", errorMessage.getText());
    }

    /**
     * Tests that valid Admin credentials are correctly parsed.
     * <p>
     * Reaching the "Error loading page" state confirms that authentication was successful
     * but the FXML loader failed (which is expected in a unit test environment).
     * </p>
     *
     * @throws IOException if file creation fails.
     * @throws InterruptedException if the thread is interrupted.
     */
    @Test
    void testHandleLogin_Success_Admin() throws IOException, InterruptedException {
        createUsersFile("admin,123,Admin");
        
        usernameField.setText("admin");
        passwordField.setText("123");
        
        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        assertEquals("⚠️ Error loading page.", errorMessage.getText());
    }

    /**
     * Tests the authentication flow for a standard User with additional details.
     *
     * @throws IOException if file creation fails.
     * @throws InterruptedException if the thread is interrupted.
     */
    @Test
    void testHandleLogin_Success_User_WithDetails() throws IOException, InterruptedException {
        createUsersFile("user1,123,User,Gold,test@mail.com");
        
        usernameField.setText("user1");
        passwordField.setText("123");
        
        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        assertEquals("⚠️ Error loading page.", errorMessage.getText());
    }

    /**
     * Tests that providing incorrect credentials results in an error message 
     * and clears the password field.
     *
     * @throws IOException if file creation fails.
     * @throws InterruptedException if the thread is interrupted.
     */
    @Test
    void testHandleLogin_InvalidCredentials() throws IOException, InterruptedException {
        createUsersFile("admin,123,Admin");
        
        usernameField.setText("wrongUser");
        passwordField.setText("wrongPass");
        
        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        assertEquals("❌ Invalid username or password.", errorMessage.getText());
        assertEquals("", passwordField.getText());
    }

    /**
     * Tests the behavior when the users data file is missing.
     * <p>
     * Note: The controller logic overwrites the file not found message with
     * the generic invalid credentials message upon returning null validation.
     * </p>
     * 
     * @throws InterruptedException if the thread is interrupted.
     */
    @Test
    void testHandleLogin_FileNotFound() throws InterruptedException {
        usernameField.setText("user");
        passwordField.setText("pass");
        
        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        assertEquals("❌ Invalid username or password.", errorMessage.getText());
    }

    /**
     * Tests the robustness of the file parser against malformed lines and empty rows.
     *
     * @throws IOException if file creation fails.
     * @throws InterruptedException if the thread is interrupted.
     */
    @Test
    void testValidateCredentials_ParsingEdgeCases() throws IOException, InterruptedException {
        String badData = "\n\nbad_line_no_commas\nuser,pass\nvalid,123,User";
        createUsersFile(badData);
        
        usernameField.setText("valid");
        passwordField.setText("123");
        
        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        assertEquals("⚠️ Error loading page.", errorMessage.getText());
    }
}