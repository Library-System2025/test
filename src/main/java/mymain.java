import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
<<<<<<< HEAD
 * Main entry point for the Library Application.
 *
 * @author Zainab
 * @version 1.0
 */

public class mymain extends Application {

	
    
    /**
     * @param primaryStage 
     * @throws Exception 
     */

    @Override
    public void start(Stage primaryStage) throws Exception {
        
        Parent root = FXMLLoader.load(getClass().getResource("/login.fxml")); 

        
        Scene scene = new Scene(root);

        
        primaryStage.setTitle("Library Management - Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /** 
=======
* Main entry point for the Library Application.
 *
 * @author Zainab
 * @version 1.0
 */

public class mymain extends Application {

	
    
  /**
   * @param primaryStage 
     * @throws Exception 
    */

   @Override
   public void start(Stage primaryStage) throws Exception {
        
        Parent root = FXMLLoader.load(getClass().getResource("/login.fxml")); 

        
       Scene scene = new Scene(root);

        
        primaryStage.setTitle("Library Management - Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

   /** 
>>>>>>> branch 'main' of https://github.com/zainab-hashem/test.git
     * @param args 
     */
    
    public static void main(String[] args) {
        launch(args);
    }
    }
