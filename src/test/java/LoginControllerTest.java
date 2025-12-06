import static org.junit.jupiter.api.Assertions.*;

import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.application.Platform;

import org.junit.jupiter.api.*;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Unit tests for LoginController.
 * Verifies authentication logic and file reading.
 * 
 * @author Zainab
 * @version 1.2
 */
public class LoginControllerTest {

    @BeforeAll
    static void initToolkit() {
        try { Platform.startup(() -> {}); } catch (IllegalStateException e) {}
    }

    private LoginController controller;
    private TextField usernameField;
    private PasswordField passwordField;
    private Label errorMessageLabel;

    @BeforeEach
    void setUp() throws Exception {
        controller = new LoginController();

        usernameField = new TextField();
        passwordField = new PasswordField();
        errorMessageLabel = new Label();

        setPrivateField("usernameField", usernameField);
        setPrivateField("passwordField", passwordField);
        setPrivateField("errorMessage", errorMessageLabel);
        
        
        File f = new File("users.txt");
        if (f.exists()) f.delete();
    }

    private void setPrivateField(String fieldName, Object value) throws Exception {
        Field f = LoginController.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(controller, value);
    }
    
    private Object invokeValidateCredentials(String username, String password) throws Exception {
        Method m = LoginController.class.getDeclaredMethod("validateCredentials", String.class, String.class);
        m.setAccessible(true);
        return m.invoke(controller, username, password);
    }

    /**
     * Verifies empty fields show error.
     */
    @Test
    void testHandleLogin_EmptyFields() throws Exception {
        usernameField.setText("");
        passwordField.setText("");
        controller.handleLogin(null);
        assertEquals("⚠️ Please fill in all fields.", errorMessageLabel.getText());
    }

    /**
     * Verifies invalid credentials show error.
     */
    @Test
    void testHandleLogin_InvalidCredentials() throws Exception {
        try (PrintWriter out = new PrintWriter(new FileWriter("users.txt"))) {
            out.println("admin,123,Admin");
        }
        
        usernameField.setText("wrong");
        passwordField.setText("wrong");
        controller.handleLogin(null);
        
        assertEquals("❌ Invalid username or password.", errorMessageLabel.getText());
    }

    /**
     * Verifies valid user validation.
     */
    @Test
    void testValidateCredentials_ValidUser() throws Exception {
        try (PrintWriter out = new PrintWriter(new FileWriter("users.txt"))) {
            out.println("u1,1,User,Gold,u1@mail.com");
        }

        Object result = invokeValidateCredentials("u1", "1");
        assertNotNull(result);
        
        Field emailField = result.getClass().getDeclaredField("email");
        emailField.setAccessible(true);
        assertEquals("u1@mail.com", emailField.get(result));
    }

    /**
     * Verifies Admin validation.
     */
    @Test
    void testValidateCredentials_Admin() throws Exception {
        try (PrintWriter out = new PrintWriter(new FileWriter("users.txt"))) {
            out.println("admin,123,Admin");
        }
        
        Object result = invokeValidateCredentials("admin", "123");
        assertNotNull(result);
        
        Field roleField = result.getClass().getDeclaredField("role");
        roleField.setAccessible(true);
        assertEquals("Admin", roleField.get(result));
    }

    /**
     * Verifies malformed lines in users file.
     */
    @Test
    void testValidateCredentials_MalformedLines() throws Exception {
        try (PrintWriter out = new PrintWriter(new FileWriter("users.txt"))) {
            out.println("");
            out.println("badline");
            out.println("u1,1,User");
        }
        
        Object result = invokeValidateCredentials("u1", "1");
        assertNotNull(result);
    }
    
    /**
     * Verifies file not found scenario.
     */
    @Test
    void testValidateCredentials_NoFile() throws Exception {
        File f = new File("users.txt");
        if (f.exists()) f.delete();
        
        Object result = invokeValidateCredentials("u", "p");
        assertNull(result);
        assertEquals("⚠️ Users file not found!", errorMessageLabel.getText());
    }
}