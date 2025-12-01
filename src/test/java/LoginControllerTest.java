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
 * @version 1.0
 */

public class LoginControllerTest {

    
    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            
        }
    }

    private LoginController controller;
    private TextField usernameField;
    private PasswordField passwordField;
    private Label errorMessageLabel;

    
    private void setPrivateField(String fieldName, Object value) throws Exception {
        Field f = LoginController.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(controller, value);
    }

    
    private Object invokeValidateCredentials(String username, String password) throws Exception {
        Method m = LoginController.class.getDeclaredMethod(
                "validateCredentials",
                String.class,
                String.class
        );
        m.setAccessible(true);
        return m.invoke(controller, username, password);
    }

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
        if (f.exists()) {
            f.delete();
        }
    }

    // ---------------  handleLogin ---------------

    @Test
    void testHandleLogin_emptyFields_showsErrorMessage() throws Exception {
        
        usernameField.setText("");
        passwordField.setText("");

        
        controller.handleLogin(null);

        assertEquals("⚠️ Please fill in all fields.", errorMessageLabel.getText());
    }

    @Test
    void testHandleLogin_invalidCredentials_showsErrorMessage() throws Exception {
        
        File f = new File("users.txt");
        try (PrintWriter out = new PrintWriter(new FileWriter(f))) {
            out.println("m,123,Admin");
        }

        
        usernameField.setText("wrongUser");
        passwordField.setText("wrongPass");

        controller.handleLogin(null);

        assertEquals("❌ Invalid username or password.", errorMessageLabel.getText());
        assertEquals("", passwordField.getText(), "Password field should be cleared");
    }

    // ---------------  validateCredentials (بالـ reflection) ---------------

    @Test
    void testValidateCredentials_usersFileNotFound_returnsNullAndSetsError() throws Exception {
        
        File f = new File("users.txt");
        if (f.exists()) {
            f.delete();
        }

        Object result = invokeValidateCredentials("m", "123");
        assertNull(result);
        assertEquals("⚠️ Users file not found!", errorMessageLabel.getText());
    }

    @Test
    void testValidateCredentials_validAdminLine_parsedCorrectly() throws Exception {
        
        try (PrintWriter out = new PrintWriter(new FileWriter("users.txt"))) {
            out.println("m,123,Admin");
        }

        Object result = invokeValidateCredentials("m", "123");
        assertNotNull(result, "UserInfo should not be null for valid credentials");

        
        Class<?> userInfoClass = result.getClass();

        Field roleField = userInfoClass.getDeclaredField("role");
        roleField.setAccessible(true);
        String role = (String) roleField.get(result);

        Field membershipField = userInfoClass.getDeclaredField("membership");
        membershipField.setAccessible(true);
        String membership = (String) membershipField.get(result);

        Field emailField = userInfoClass.getDeclaredField("email");
        emailField.setAccessible(true);
        String email = (String) emailField.get(result);

        assertEquals("Admin", role);
        
        assertEquals("Silver", membership);
        assertEquals("", email);
    }

    @Test
    void testValidateCredentials_validUserLine_parsedWithMembershipAndEmail() throws Exception {
        
        try (PrintWriter out = new PrintWriter(new FileWriter("users.txt"))) {
            out.println("u1,1,User,Gold,manar@gmail.com");
        }

        Object result = invokeValidateCredentials("u1", "1");
        assertNotNull(result, "UserInfo should not be null for valid user");

        Class<?> userInfoClass = result.getClass();

        Field roleField = userInfoClass.getDeclaredField("role");
        roleField.setAccessible(true);
        String role = (String) roleField.get(result);

        Field membershipField = userInfoClass.getDeclaredField("membership");
        membershipField.setAccessible(true);
        String membership = (String) membershipField.get(result);

        Field emailField = userInfoClass.getDeclaredField("email");
        emailField.setAccessible(true);
        String email = (String) emailField.get(result);

        assertEquals("User", role);
        assertEquals("Gold", membership);
        assertEquals("manar@gmail.com", email);
    }

    @Test
    void testValidateCredentials_wrongPassword_returnsNull() throws Exception {
        try (PrintWriter out = new PrintWriter(new FileWriter("users.txt"))) {
            out.println("m,123,Admin");
        }

        Object result = invokeValidateCredentials("m", "999");
        assertNull(result, "Should return null if password does not match");
    }

    @Test
    void testValidateCredentials_skipsEmptyAndMalformedLines() throws Exception {
        try (PrintWriter out = new PrintWriter(new FileWriter("users.txt"))) {
            out.println("");                      
            out.println("invalidLineWithoutComma"); 
            out.println("x,111");                 
            out.println("user1,pass1,User,Silver,user1@mail.com");
        }

        Object result = invokeValidateCredentials("user1", "pass1");
        assertNotNull(result, "Should still find valid user after bad lines");

        Class<?> userInfoClass = result.getClass();
        Field emailField = userInfoClass.getDeclaredField("email");
        emailField.setAccessible(true);
        String email = (String) emailField.get(result);

        assertEquals("user1@mail.com", email);
    }
}
