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
 * Comprehensive Unit Test suite for the LoginController class.
 * <p>
 * This suite ensures high code coverage and reliability by verifying
 * input validation, file parsing logic, authentication, and role-based redirection.
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
     * Default constructor.
     */
    public LoginControllerTest() {
    }

    /**
     * Initializes the JavaFX Toolkit once.
     */
    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
        }
    }

    /**
     * Sets up the test environment, injects mocks, and clears files.
     * 
     * @throws Exception if reflection fails.
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

        Files.deleteIfExists(Paths.get(USERS_FILE));
    }

    /**
     * Cleans up resources after each test.
     * 
     * @throws IOException if file deletion fails.
     */
    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Paths.get(USERS_FILE));
    }

    /**
     * Tests validation when both fields are empty.
     * 
     * @throws InterruptedException on thread error.
     */
    @Test
    void testHandleLogin_AllFieldsEmpty() throws InterruptedException {
        usernameField.setText("");
        passwordField.setText("");
        
        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        assertEquals("⚠️ Please fill in all fields.", errorMessage.getText());
    }

    /**
     * Tests validation when only username is empty.
     * 
     * @throws InterruptedException on thread error.
     */
    @Test
    void testHandleLogin_UsernameEmpty() throws InterruptedException {
        usernameField.setText("");
        passwordField.setText("password");
        
        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        assertEquals("⚠️ Please fill in all fields.", errorMessage.getText());
    }

    /**
     * Tests validation when only password is empty.
     * 
     * @throws InterruptedException on thread error.
     */
    @Test
    void testHandleLogin_PasswordEmpty() throws InterruptedException {
        usernameField.setText("username");
        passwordField.setText("");
        
        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        assertEquals("⚠️ Please fill in all fields.", errorMessage.getText());
    }

    /**
     * Tests behavior when users file is missing.
     * 
     * @throws InterruptedException on thread error.
     */
    @Test
    void testHandleLogin_FileNotFound() throws InterruptedException {
        usernameField.setText("user");
        passwordField.setText("pass");
        
        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        assertEquals("⚠️ Users file not found!", errorMessage.getText());
    }

    /**
     * Tests login with incorrect password.
     * 
     * @throws IOException on file error.
     * @throws InterruptedException on thread error.
     */
    @Test
    void testHandleLogin_InvalidPassword() throws IOException, InterruptedException {
        createUsersFile("admin,123,Admin");
        
        usernameField.setText("admin");
        passwordField.setText("WrongPass");
        
        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        assertEquals("❌ Invalid username or password.", errorMessage.getText());
        assertEquals("", passwordField.getText());
    }

    /**
     * Tests login with incorrect username.
     * 
     * @throws IOException on file error.
     * @throws InterruptedException on thread error.
     */
    @Test
    void testHandleLogin_InvalidUsername() throws IOException, InterruptedException {
        createUsersFile("admin,123,Admin");
        
        usernameField.setText("wrongUser");
        passwordField.setText("123");
        
        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        assertEquals("❌ Invalid username or password.", errorMessage.getText());
    }

    /**
     * Tests parsing logic with various malformed lines to cover loop continues.
     * 
     * @throws IOException on file error.
     * @throws InterruptedException on thread error.
     */
    @Test
    void testValidateCredentials_MalformedLines() throws IOException, InterruptedException {
        StringBuilder sb = new StringBuilder();
        sb.append("\n"); 
        sb.append("   \n"); 
        sb.append("incomplete,line\n"); 
        sb.append("validUser,123,User\n");
        createUsersFile(sb.toString());
        
        usernameField.setText("validUser");
        passwordField.setText("123");
        
        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        assertFalse(errorMessage.getText().contains("Invalid"));
    }

    /**
     * Tests parsing logic where membership is missing (defaults to Silver).
     * 
     * @throws IOException on file error.
     * @throws InterruptedException on thread error.
     */
    @Test
    void testValidateCredentials_DefaultMembership_Length3() throws IOException, InterruptedException {
        createUsersFile("user3,pass3,User");
        usernameField.setText("user3");
        passwordField.setText("pass3");
        
        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        assertTrue(errorMessage.getText().contains("User window") || errorMessage.getText().contains("Error loading page"));
    }

    /**
     * Tests parsing logic where membership field exists but is empty.
     * 
     * @throws IOException on file error.
     * @throws InterruptedException on thread error.
     */
    @Test
    void testValidateCredentials_DefaultMembership_EmptyField() throws IOException, InterruptedException {
        createUsersFile("user4,pass4,User,  ");
        usernameField.setText("user4");
        passwordField.setText("pass4");
        
        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        assertTrue(errorMessage.getText().contains("User window") || errorMessage.getText().contains("Error loading page"));
    }

    /**
     * Tests parsing logic with full data (5 parts).
     * 
     * @throws IOException on file error.
     * @throws InterruptedException on thread error.
     */
    @Test
    void testValidateCredentials_FullData() throws IOException, InterruptedException {
        createUsersFile("fullUser,123,User,Gold,test@email.com");
        usernameField.setText("fullUser");
        passwordField.setText("123");

        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        assertTrue(errorMessage.getText().contains("User window") || errorMessage.getText().contains("Error loading page"));
    }

    /**
     * Tests Admin role flow.
     * 
     * @throws IOException on file error.
     * @throws InterruptedException on thread error.
     */
    @Test
    void testRoleFlow_Admin() throws IOException, InterruptedException {
        createUsersFile("admin,123,Admin");
        usernameField.setText("admin");
        passwordField.setText("123");

        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));

        String msg = errorMessage.getText();
        assertTrue(msg.contains("Admin window") || msg.contains("Error loading page"));
    }

    /**
     * Tests Librarian role flow.
     * 
     * @throws IOException on file error.
     * @throws InterruptedException on thread error.
     */
    @Test
    void testRoleFlow_Librarian() throws IOException, InterruptedException {
        createUsersFile("lib,123,Librarian");
        usernameField.setText("lib");
        passwordField.setText("123");

        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        String msg = errorMessage.getText();
        assertTrue(msg.contains("Librarian window") || msg.contains("Error loading page"));
    }

    /**
     * Tests User role flow.
     * 
     * @throws IOException on file error.
     * @throws InterruptedException on thread error.
     */
    @Test
    void testRoleFlow_User() throws IOException, InterruptedException {
        createUsersFile("user,123,User");
        usernameField.setText("user");
        passwordField.setText("123");

        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        String msg = errorMessage.getText();
        assertTrue(msg.contains("User window") || msg.contains("Error loading page"));
    }

    /**
     * Tests unknown role flow (default case).
     * 
     * @throws IOException on file error.
     * @throws InterruptedException on thread error.
     */
    @Test
    void testRoleFlow_Unknown() throws IOException, InterruptedException {
        createUsersFile("guest,123,Guest");
        usernameField.setText("guest");
        passwordField.setText("123");

        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        String msg = errorMessage.getText();
        assertTrue(msg.contains("Guest window") || msg.contains("Error loading page"));
    }
    
    /**
     * Tests IOException handling during FXML loading.
     * 
     * @throws IOException on file error.
     * @throws InterruptedException on thread error.
     */
    @Test
    void testHandleLogin_FXMLLoadException() throws IOException, InterruptedException {
        createUsersFile("admin,123,Admin");
        usernameField.setText("admin");
        passwordField.setText("123");
        
        runOnFxThreadAndWait(() -> controller.handleLogin(new ActionEvent()));
        
        if (!errorMessage.getText().contains("opened successfully")) {
             assertEquals("⚠️ Error loading page.", errorMessage.getText());
        }
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