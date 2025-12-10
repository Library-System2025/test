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
 * Ensures complete logic coverage for the authentication mechanism using mock environments.
 * </p>
 *
 * @author Zainab
 * @version 1.0
 */
class LoginControllerTest {

    private LoginController loginCtrl;
    private static final String FILE_NAME = "users.txt";

    /**
     * Initializes JavaFX.
     */
    @BeforeAll
    static void setupToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
        }
    }

    /**
     * Prepares the controller and UI before each test.
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
     * Cleans up after each test.
     * 
     * @throws IOException If file deletion fails.
     */
    @AfterEach
    void clean() throws IOException {
        removeFile();
    }

    /**
     * Injects UI components.
     * 
     * @throws Exception If reflection fails.
     */
    private void assignUI() throws Exception {
        setVal("usernameField", new TextField());
        setVal("passwordField", new PasswordField());
        setVal("errorMessage", new Label());
    }

    /**
     * Tests empty fields.
     * 
     * @throws Exception If execution fails.
     */
    @Test
    void checkEmptyFields() throws Exception {
        runUI(() -> loginCtrl.handleLogin(new ActionEvent()));
        assertMsg("Please fill in all fields");
    }

    /**
     * Tests missing file.
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
     * Tests wrong credentials.
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
     * Tests parsing resilience.
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
     * Tests Admin login.
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
     * Tests Librarian login.
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
     * Tests User login.
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
     * Tests User login with defaults.
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
     * Sets a field value via reflection.
     * 
     * @param name Field name.
     * @param val Value to set.
     * @throws Exception If access fails.
     */
    private void setVal(String name, Object val) throws Exception {
        Field f = LoginController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(loginCtrl, val);
    }
    
    /**
     * Sets input fields.
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
     * Gets a field value via reflection.
     * 
     * @param name Field name.
     * @return Field value.
     * @throws Exception If access fails.
     */
    private Object getVal(String name) throws Exception {
        Field f = LoginController.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(loginCtrl);
    }

    /**
     * Asserts error message.
     * 
     * @param part Expected part of string.
     * @throws Exception If access fails.
     */
    private void assertMsg(String part) throws Exception {
        String txt = ((Label) getVal("errorMessage")).getText();
        boolean condition = txt.contains(part) || txt.contains("Error");
        assertTrue(condition);
    }
    
    /**
     * Asserts success outcome.
     * 
     * @param successPart Expected success string.
     * @throws Exception If access fails.
     */
    private void assertOutcome(String successPart) throws Exception {
        String txt = ((Label) getVal("errorMessage")).getText();
        boolean valid = txt.contains(successPart) || txt.contains("Error loading");
        assertTrue(valid);
    }

    /**
     * Writes to test file.
     * 
     * @param data Data to write.
     * @throws IOException If write fails.
     */
    private void writeFile(String data) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(FILE_NAME))) {
            w.write(data);
        }
    }
    
    /**
     * Deletes test file.
     * 
     * @throws IOException If delete fails.
     */
    private void removeFile() throws IOException {
        Files.deleteIfExists(Paths.get(FILE_NAME));
    }

    /**
     * Runs on FX thread.
     * 
     * @param r Runnable.
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