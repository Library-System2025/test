import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorMessage;

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        String correctUsername = "m";
        String correctPassword = "123";

        if (username.isEmpty() || password.isEmpty()) {
            errorMessage.setText("Please fill in all fields.");
            usernameField.clear();
            passwordField.clear();
            return;
        }

        if (!username.equals(correctUsername)) {
            errorMessage.setText("Username not found or incorrect.");
            usernameField.clear();
            passwordField.clear();
            return;
        }

        if (!password.equals(correctPassword)) {
            errorMessage.setText("Incorrect password.");
            passwordField.clear();
            return;
        }

        try {
            Parent dashboard = FXMLLoader.load(getClass().getResource("homepage.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(dashboard));
        } catch (IOException e) {
            e.printStackTrace();
            errorMessage.setText("Error loading homepage.");
        }
    }
}
