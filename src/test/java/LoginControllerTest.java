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
 * Unit tests for the {@link LoginController} class.
 * <p>
 * This suite validates the authentication logic, file parsing for user credentials,
 * and error handling for various login scenarios (empty fields, invalid credentials, etc.).
 * </p>
 * 
 * @author Zainab
 * @version 1.3
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
     * Verifies that attempting to login with empty fields triggers an appropriate warning message.
     */
    @Test
    void testHandleLogin_EmptyFields() throws Exception {
        usernameField.setText("");
        passwordField.setText("");
        controller.handleLogin(null);
        assertEquals("⚠️ Please fill in all fields.", errorMessageLabel.getText());
    }

    /**
     * Verifies that invalid credentials result in an error message and clear the password field.
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
        assertEquals("", passwordField.getText(), "Password field should be cleared on invalid login");
    }

    /**
     * Verifies that a valid user is correctly validated, parsing extra fields like membership and email.
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

        Field membershipField = result.getClass().getDeclaredField("membership");
        membershipField.setAccessible(true);
        assertEquals("Gold", membershipField.get(result));
    }

    /**
     * Verifies that an Admin user is correctly identified with the "Admin" role.
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
     * Verifies robustness against malformed lines in the users file.
     * The parser should skip bad lines and still find valid users.
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
     * Verifies behavior when the users file is missing.
     * Should return null and display a file not found error.
     */
    @Test
    void testValidateCredentials_NoFile() throws Exception {
        File f = new File("users.txt");
        if (f.exists()) f.delete();

        Object result = invokeValidateCredentials("u", "p");
        assertNull(result);
        assertEquals("⚠️ Users file not found!", errorMessageLabel.getText());
    }

    /**
     * Verifies that membership defaults to "Silver" when the field is missing in the file.
     * Line format: username,password,role
     */
    @Test
    void testValidateCredentials_DefaultMembership_WhenMissing() throws Exception {
        try (PrintWriter out = new PrintWriter(new FileWriter("users.txt"))) {
            out.println("u2,2,User");
        }

        Object result = invokeValidateCredentials("u2", "2");
        assertNotNull(result);

        Field membershipField = result.getClass().getDeclaredField("membership");
        membershipField.setAccessible(true);
        assertEquals("Silver", membershipField.get(result));
    }

    /**
     * Verifies that membership defaults to "Silver" when the field is explicitly empty,
     * but subsequent fields (like email) are present.
     * Line format: username,password,role,,email
     */
    @Test
    void testValidateCredentials_DefaultMembership_WhenEmptyField() throws Exception {
        try (PrintWriter out = new PrintWriter(new FileWriter("users.txt"))) {
            out.println("u3,3,User,,u3@mail.com");
        }

        Object result = invokeValidateCredentials("u3", "3");
        assertNotNull(result);

        Field membershipField = result.getClass().getDeclaredField("membership");
        membershipField.setAccessible(true);
        assertEquals("Silver", membershipField.get(result));

        Field emailField = result.getClass().getDeclaredField("email");
        emailField.setAccessible(true);
        assertEquals("u3@mail.com", emailField.get(result));
    }

    /**
     * Verifies successful login flow up to Stage creation.
     * <p>
     * This test confirms that `handleLogin` proceeds through validation successfully.
     * An {@link IllegalStateException} is expected because opening a new Stage 
     * requires the JavaFX Application Thread, which is not available in this unit test context.
     * This exception confirms the code reached the UI transition point.
     * </p>
     */
    @Test
    void testHandleLogin_ValidAdmin_throwsIllegalStateOnStageCreation() throws Exception {
        try (PrintWriter out = new PrintWriter(new FileWriter("users.txt"))) {
            out.println("admin,123,Admin");
        }

        usernameField.setText("admin");
        passwordField.setText("123");

        assertThrows(IllegalStateException.class, () -> controller.handleLogin(null));
    }
}
