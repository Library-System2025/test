import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class mymain extends Application {

    public int add(int x, int y) {
        return x + y;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // تحميل واجهة تسجيل الدخول
        Parent root = FXMLLoader.load(getClass().getResource("/login.fxml")); // ✔️ تأكد أن login.fxml موجود بنفس المسار

        // إعداد المشهد
        Scene scene = new Scene(root);

        // إعداد النافذة
        primaryStage.setTitle("Library Management - Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
    }
