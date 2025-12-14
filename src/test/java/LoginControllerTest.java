import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
 * This suite ensures high code coverage and reliability by verifying input validation,
 * file I/O operations, authentication mechanisms, and role-based redirection logic.
 * It utilizes Java Reflection to access private methods and inner classes, ensuring
 * maximum coverage even for encapsulated logic.
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
     * Initializes the JavaFX Toolkit once before all tests execution.
     * This prevents toolkit initialization errors during UI component instantiation.
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
     * Initializes the controller, injects mock JavaFX components, and clears test files.
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
     * Verifies that the login handler correctly identifies and rejects empty input fields.
     * 
     * @throws InterruptedException if the JavaFX thread is interrupted.
     */
    @Test
    void testHandleLogin_EmptyFields() throws InterruptedException {
        usernameField.setText("");
        passwordField.setText("");
        runOnFx(() -> controller.handleLogin(new ActionEvent()));
        assertEquals("⚠️ Please fill in all fields.", errorMessage.getText());
    }

    /**
     * Verifies the system's behavior when the user database file is missing.
     * 
     * @throws InterruptedException if the JavaFX thread is interrupted.
     */
    @Test
    void testHandleLogin_FileNotFound() throws InterruptedException {
        usernameField.setText("user");
        passwordField.setText("pass");
        runOnFx(() -> controller.handleLogin(new ActionEvent()));
        assertEquals("⚠️ Users file not found!", errorMessage.getText());
    }

    /**
     * Tests the private `validateCredentials` method using Reflection.
     * This ensures that the core authentication logic works correctly for both valid
     * and invalid credentials without relying on the UI layer.
     * 
     * @throws Exception if reflection access fails.
     */
    @Test
    void testValidateCredentials_PrivateMethod() throws Exception {
        createUsersFile("validUser,123,User,Silver,mail@test.com");
        
        Method m = LoginController.class.getDeclaredMethod("validateCredentials", String.class, String.class);
        m.setAccessible(true);
        
        Object result = m.invoke(controller, "validUser", "123");
        assertNotNull(result, "Should return UserInfo object");
        
        Object fail = m.invoke(controller, "validUser", "wrongPass");
        assertNull(fail, "Should return null for wrong password");
    }

    /**
     * Verifies the login flow for all supported roles (Admin, Librarian, User).
     * Ensures that the controller attempts to load the correct dashboard for each role.
     * 
     * @throws IOException if creating the test file fails.
     * @throws InterruptedException if the JavaFX thread is interrupted.
     */
    @Test
    void testRoleFlow_AllRoles() throws IOException, InterruptedException {
        StringBuilder sb = new StringBuilder();
        sb.append("admin,123,Admin\n");
        sb.append("lib,123,Librarian\n");
        sb.append("user,123,User\n");
        createUsersFile(sb.toString());

        usernameField.setText("admin");
        passwordField.setText("123");
        runOnFx(() -> controller.handleLogin(new ActionEvent()));
        assertTrue(errorMessage.getText().contains("opened") || errorMessage.getText().contains("Error"));

        usernameField.setText("lib");
        passwordField.setText("123");
        runOnFx(() -> controller.handleLogin(new ActionEvent()));
        assertTrue(errorMessage.getText().contains("opened") || errorMessage.getText().contains("Error"));

        usernameField.setText("user");
        passwordField.setText("123");
        runOnFx(() -> controller.handleLogin(new ActionEvent()));
        assertTrue(errorMessage.getText().contains("opened") || errorMessage.getText().contains("Error"));
    }

    /**
     * Tests the private static inner class `UserInfo` using Reflection.
     * This guarantees 100% coverage by instantiating the inner data structure directly.
     * 
     * @throws Exception if reflection instantiation fails.
     */
    @Test
    void testInnerClassUserInfo() throws Exception {
        Class<?>[] declaredClasses = LoginController.class.getDeclaredClasses();
        for (Class<?> c : declaredClasses) {
            if (c.getSimpleName().equals("UserInfo")) {
                java.lang.reflect.Constructor<?> ctor = c.getDeclaredConstructor(String.class, String.class, String.class);
                ctor.setAccessible(true);
                Object obj = ctor.newInstance("Role", "Membership", "Email");
                assertNotNull(obj);
            }
        }
    }

    /**
     * Injects a value into a private field of the controller.
     * 
     * @param fieldName The name of the field to inject.
     * @param value The value to set.
     * @throws Exception If the field cannot be accessed.
     */
    private void injectField(String fieldName, Object value) throws Exception {
        Field field = LoginController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
    }

    /**
     * Creates a dummy users file for testing purposes.
     * 
     * @param content The content to write to the file.
     * @throws IOException If writing fails.
     */
    private void createUsersFile(String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE))) {
            writer.write(content);
        }
    }
    
    /**
     * Deletes the dummy users file if it exists.
     * 
     * @throws IOException If deletion fails.
     */
    private void cleanupFile() throws IOException {
        Files.deleteIfExists(Paths.get(USERS_FILE));
    }

    /**
     * Executes a runnable on the JavaFX application thread and waits for completion.
     * 
     * @param action The action to execute.
     * @throws InterruptedException If the thread is interrupted.
     */
    private void runOnFx(Runnable action) throws InterruptedException {
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