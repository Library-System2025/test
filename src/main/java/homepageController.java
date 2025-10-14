
// الملف: DashboardController.java

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;
import java.io.IOException;

public class homepageController {
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField isbnField;
    @FXML private TextField searchField;
    @FXML private ListView<String> searchResults;
    @FXML private Label addBookMessage;

    private ObservableList<String> bookList = FXCollections.observableArrayList();

    @FXML
    private void handleLogout() {
        try {
            Parent login = FXMLLoader.load(getClass().getResource("login.fxml"));
            Stage stage = (Stage) titleField.getScene().getWindow();
            stage.setScene(new Scene(login));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddBook() {
        String title = titleField.getText();
        String author = authorField.getText();
        String isbn = isbnField.getText();

        if (!title.isEmpty() && !author.isEmpty() && !isbn.isEmpty()) {
            String book = title + " - " + author + " (ISBN: " + isbn + ")";
            bookList.add(book);
            addBookMessage.setText("Book added successfully.");
            titleField.clear(); authorField.clear(); isbnField.clear();
        } else {
            addBookMessage.setText("Please fill in all fields.");
        }
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().toLowerCase();
        ObservableList<String> filtered = FXCollections.observableArrayList();

        for (String book : bookList) {
            if (book.toLowerCase().contains(keyword)) {
                filtered.add(book);
            }
        }
        searchResults.setItems(filtered);
    }
}
