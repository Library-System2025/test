// مشروع JavaFX - إدارة المستخدمين والكتب

// الملف: LoginController.java

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
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.equals("admin") && password.equals("admin123")) {
            try {
                Parent dashboard = FXMLLoader.load(getClass().getResource("homepage.fxml"));
                Stage stage = (Stage) usernameField.getScene().getWindow();
                stage.setScene(new Scene(dashboard));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            errorMessage.setText("Invalid username or password.");
        }
    }
}

