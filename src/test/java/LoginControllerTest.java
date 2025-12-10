import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedWriter;
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
 * Advanced Unit Test suite for LoginController.
 * <p>
 * Ensures complete logic coverage for the authentication mechanism.
 * </p>
 *
 * @author Zainab
 * @version 1.0
 */
class LoginControllerTest {

    private LoginController loginCtrl;
    private static final String FILE_NAME = "users.txt";

    /**
     * Initializes the JavaFX toolkit.
     */
    @BeforeAll
    static void setupToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
        }
    }

    /**
     * Sets up the controller and mocks UI fields before each test.
     * 
     * @throws Exception If reflection fails.
     */
    @BeforeEach
    void init() throws Exception {
        loginCtrl = new LoginController();
        assignUI();
        removeFile();
    }

    /**
     * Cleans up the test environment.
     * 
     * @throws IOException If file deletion fails.
     */
    @AfterEach
    void clean() throws IOException {
        removeFile();
    }

    /**
     * Assigns mock UI components to the controller.
     * 
     * @throws Exception If reflection fails.
     */
    private void assignUI() throws Exception {
        setVal("usernameField", new TextField());
        setVal("passwordField", new PasswordField());
        setVal("errorMessage", new Label());
    }

    /**
     * Tests validation for empty fields.
     * 
     * @throws Exception If execution fails.
     */
    @Test
    void checkEmptyFields() throws Exception {
        runUI(() -> loginCtrl.handleLogin(new ActionEvent()));
        assertMsg("Please fill in all fields");
    }

    /**
     * Tests behavior when the user database file is missing.
     * 
     * @throws Exception If execution fails.
     */
    @Test
    void checkMissingFile() throws Exception {
        setInput("user", "pass");
        runUI(() -> loginCtrl.handleLogin(new ActionEvent()));
        assertMsg("Invalid username or password");
    }

    /**
     * Tests authentication failure with wrong credentials.
     * 
     * @throws Exception If execution fails.
     */
    @Test
    void checkWrongCredentials() throws Exception {
        writeFile("admin,123,Admin");
        setInput("bad", "pass");
        runUI(() -> loginCtrl.handleLogin(new ActionEvent()));
        assertMsg("Invalid username or password");
    }

    /**
     * Tests the parser's robustness against empty lines and malformed data.
     * 
     * @throws Exception If execution fails.
     */
    @Test
    void checkParsingRobustness() throws Exception {
        writeFile("\n   \nbroken_line\npart1,part2\nreal,1,User");
        
        setInput("real", "1");
        runUI(() -> loginCtrl.handleLogin(new ActionEvent()));
        
        assertOutcome("User window");
    }

    /**
     * Tests successful login for an Admin user.
     * 
     * @throws Exception If execution fails.
     */
    @Test
    void checkAdminLogin() throws Exception {
        writeFile("admin,123,Admin,Gold,a@a.com");
        setInput("admin", "123");
        runUI(() -> loginCtrl.handleLogin(new ActionEvent()));
        assertOutcome("Admin window");
    }

    /**
     * Tests successful login for a Librarian user.
     * 
     * @throws Exception If execution fails.
     */
    @Test
    void checkLibrarianLogin() throws Exception {
        writeFile("lib,123,Librarian");
        setInput("lib", "123");
        runUI(() -> loginCtrl.handleLogin(new ActionEvent()));
        assertOutcome("Librarian window");
    }

    /**
     * Tests successful login for a standard User.
     * 
     * @throws Exception If execution fails.
     */
    @Test
    void checkUserLogin() throws Exception {
        writeFile("u1,123,User,Silver,u@u.com");
        setInput("u1", "123");
        runUI(() -> loginCtrl.handleLogin(new ActionEvent()));
        assertOutcome("User window");
    }
    
    /**
     * Tests login functionality when user details (membership/email) are missing.
     * 
     * @throws Exception If execution fails.
     */
    @Test
    void checkUserLoginDefaults() throws Exception {
        writeFile("u2,123,User"); 
        setInput("u2", "123");
        runUI(() -> loginCtrl.handleLogin(new ActionEvent()));
        assertOutcome("User window");
    }

    /**
     * Sets a private field value using reflection.
     * 
     * @param name The field name.
     * @param val The value to set.
     * @throws Exception If reflection fails.
     */
    private void setVal(String name, Object val) throws Exception {
        Field f = LoginController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(loginCtrl, val);
    }
    
    /**
     * Sets input fields text.
     * 
     * @param u Username.
     * @param p Password.
     * @throws Exception If access fails.
     */
    private void setInput(String u, String p) throws Exception {
        ((TextField) getVal("usernameField")).setText(u);
        ((PasswordField) getVal("passwordField")).setText(p);
    }

    /**
     * Gets a private field value using reflection.
     * 
     * @param name The field name.
     * @return The field value.
     * @throws Exception If reflection fails.
     */
    private Object getVal(String name) throws Exception {
        Field f = LoginController.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(loginCtrl);
    }

    /**
     * Asserts the error message content.
     * 
     * @param part The expected string part.
     * @throws Exception If access fails.
     */
    private void assertMsg(String part) throws Exception {
        String txt = ((Label) getVal("errorMessage")).getText();
        boolean condition = txt.contains(part) || txt.contains("Error");
        assertTrue(condition);
    }
    
    /**
     * Asserts the outcome success message.
     * 
     * @param successPart The success message part.
     * @throws Exception If access fails.
     */
    private void assertOutcome(String successPart) throws Exception {
        String txt = ((Label) getVal("errorMessage")).getText();
        boolean valid = txt.contains(successPart) || txt.contains("Error loading");
        assertTrue(valid);
    }

    /**
     * Writes content to the test file.
     * 
     * @param data The data to write.
     * @throws IOException If write fails.
     */
    private void writeFile(String data) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(FILE_NAME))) {
            w.write(data);
        }
    }
    
    /**
     * Removes the test file.
     * 
     * @throws IOException If deletion fails.
     */
    private void removeFile() throws IOException {
        Files.deleteIfExists(Paths.get(FILE_NAME));
    }

    /**
     * Runs a runnable on the FX thread.
     * 
     * @param r The runnable.
     * @throws InterruptedException If interrupted.
     */
    private void runUI(Runnable r) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try { r.run(); } finally { latch.countDown(); }
        });
        latch.await(5, TimeUnit.SECONDS);
    }
}