import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class homepageController {
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField isbnField;
    @FXML private TextField searchField;
    @FXML private Label addBookMessage;

    @FXML private TableView<Book> searchResultsTable;
    @FXML private TableColumn<Book, String> titleColumn;
    @FXML private TableColumn<Book, String> authorColumn;
    @FXML private TableColumn<Book, String> isbnColumn;

    @FXML private ComboBox<String> searchByCombo;

    private Map<String, Book> bookMap = new HashMap<>();
    private ObservableList<Book> booksList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));

        searchResultsTable.setItems(booksList);

        searchByCombo.setItems(FXCollections.observableArrayList("All", "Title", "Author", "ISBN"));
        searchByCombo.getSelectionModel().select("All");
    }

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
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String isbn = isbnField.getText().trim();

        if (!title.isEmpty() && !author.isEmpty() && !isbn.isEmpty()) {
            if (bookMap.containsKey(isbn)) {
                addBookMessage.setText("❌ Book with this ISBN already exists.");
            } else {
                Book newBook = new Book(title, author, isbn);
                bookMap.put(isbn, newBook);
                booksList.add(newBook);
                addBookMessage.setText("✅ Book added successfully.");
                titleField.clear();
                authorField.clear();
                isbnField.clear();
            }
        } else {
            addBookMessage.setText("❗ Please fill in all fields.");
        }
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().toLowerCase().trim();
        String mode = (searchByCombo.getValue() == null) ? "All" : searchByCombo.getValue();

        if (keyword.isEmpty()) {
            searchResultsTable.setItems(booksList);
            return;
        }

        ObservableList<Book> filtered = FXCollections.observableArrayList();

        for (Book b : bookMap.values()) {
            String t = b.getTitle().toLowerCase();
            String a = b.getAuthor().toLowerCase();
            String i = b.getIsbn().toLowerCase();

            boolean match = false;

            switch (mode) {
                case "Title":
                    match = t.contains(keyword);
                    break;
                case "Author":
                    match = a.contains(keyword);
                    break;
                case "ISBN":
                    match = i.contains(keyword);
                    break;
                default:
                    match = t.contains(keyword) || a.contains(keyword) || i.contains(keyword);
                    break;
            }

            if (match) filtered.add(b);
        }

        searchResultsTable.setItems(filtered);
    }
}
