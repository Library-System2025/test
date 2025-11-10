import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.*;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorMessage;

    // 🔹 ملف المستخدمين (username,password,role,membershipType)
    private static final String USERS_FILE = "users.txt";

    // ✅ كلاس داخلي لتجميع بيانات المستخدم (الدور + العضوية)
    private static class UserInfo {
        String role;
        String membership;

        UserInfo(String role, String membership) {
            this.role = role;
            this.membership = membership;
        }
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        // ✅ تحقق من الإدخال
        if (username.isEmpty() || password.isEmpty()) {
            errorMessage.setText("⚠️ Please fill in all fields.");
            return;
        }

        // ✅ تحقق من صحة البيانات
        UserInfo userInfo = validateCredentials(username, password);

        if (userInfo == null) {
            errorMessage.setText("❌ Invalid username or password.");
            passwordField.clear();
            return;
        }

        String role = userInfo.role;
        String membership = userInfo.membership;

        // ✅ تحديد واجهة المستخدم حسب الدور
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

            // ✅ تمرير اسم المستخدم والعضوية للكنترولر المناسب
            if (role.equals("User")) {
                UserController controller = loader.getController();
                controller.setCurrentUsername(username);
                controller.setMembershipType(membership); 
                System.out.println("✅ Logged in as: " + username + " | Role: " + role + " | Membership: " + membership);

            } else if (role.equals("Admin")) {
                homepageController controller = loader.getController();
                controller.setCurrentUsername(username);
            } else if (role.equals("Librarian")) {
                LibrarianController controller = loader.getController();
                controller.setCurrentUsername(username);
            }

            // ✅ فتح النافذة
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
     * 🔍 التحقق من صحة بيانات الدخول
     * الملف users.txt بالشكل:
     * username,password,role,membershipType
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
                String[] parts = line.split(",", 4); // 🔸 الآن نقرأ 4 أعمدة
                if (parts.length >= 3) {
                    String fileUser = parts[0].trim();
                    String filePass = parts[1].trim();
                    String fileRole = parts[2].trim();
                    String membership = (parts.length == 4) ? parts[3].trim() : "Silver"; // الافتراضي

                    if (username.equals(fileUser) && password.equals(filePass)) {
                        return new UserInfo(fileRole, membership);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
