import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.*;

/**
 * Controller class for the Login Screen.
 * Authenticates users and redirects them to the appropriate dashboard based on their role.
 * 
 * @author Zainab
 * @version 1.0
 */

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorMessage;

    
    private static final String USERS_FILE = "users.txt";

    /**
     * Inner class to hold user information after validation.
     */
    
    private static class UserInfo {
        String role;
        String membership;
        String email;

        UserInfo(String role, String membership, String email) {
            this.role = role;
            this.membership = membership;
            this.email = email;
        }
    }
    
    /**
     * Handles the login button click.
     * Validates credentials and opens the corresponding dashboard.
     * 
     * @param event The action event.
     */

    @FXML
     void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorMessage.setText("⚠️ Please fill in all fields.");
            return;
        }

        UserInfo userInfo = validateCredentials(username, password);

        if (userInfo == null) {
            errorMessage.setText("❌ Invalid username or password.");
            passwordField.clear();
            return;
        }

        String role = userInfo.role;
        String membership = userInfo.membership;
        String email = userInfo.email;   

        
        String fxmlToLoad;
        switch (role) {
            case "Admin":
                fxmlToLoad = "admin_home.fxml";
                break;
            case "Librarian":
                fxmlToLoad = "librarian_home.fxml";
                break;
            default: 
                fxmlToLoad = "user_home.fxml";
                break;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlToLoad));
            Parent dashboard = loader.load();
            if ("User".equals(role)) {
                UserController controller = loader.getController();
                controller.setCurrentUser(username, membership, email);  
            }
 else if ("Admin".equals(role)) {
                homepageController controller = loader.getController();
                controller.setCurrentUsername(username);

            } else if ("Librarian".equals(role)) {
                LibrarianController controller = loader.getController();
                controller.setCurrentUsername(username);
            }

            Stage newStage = new Stage();
            newStage.setTitle(role + " Dashboard");
            newStage.setScene(new Scene(dashboard));
            newStage.show();

            
            usernameField.clear();
            passwordField.clear();
            errorMessage.setText("✅ " + role + " window opened successfully!");

            
        } catch (IOException e) {
            e.printStackTrace();
            errorMessage.setText("⚠️ Error loading page.");
        }
    }

    /**
     * Validates user credentials against the 'users.txt' file.
     * Supported format: username,password,role[,membership,email]
     * 
     * @param username The entered username.
     * @param password The entered password.
     * @return UserInfo object if valid, null otherwise.
     */
    
    private UserInfo validateCredentials(String username, String password) {
        File file = new File(USERS_FILE);
        if (!file.exists()) {
            errorMessage.setText("⚠️ Users file not found!");
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;   

                
                String[] parts = line.split(",", 5);
                if (parts.length < 3) continue; 

                String fileUser = parts[0].trim();
                String filePass = parts[1].trim();
                String fileRole = parts[2].trim();

                
                String membership = (parts.length >= 4 && !parts[3].trim().isEmpty())
                        ? parts[3].trim()
                        : "Silver";

                
                String email = (parts.length == 5) ? parts[4].trim() : "";

                
                if (username.equals(fileUser) && password.equals(filePass)) {
                    System.out.println("✅ Login matched line: " + line);
                    return new UserInfo(fileRole, membership, email);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null; 
    }
}
