import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.control.cell.PropertyValueFactory;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class homepageController {

    // 🧩 عناصر الواجهة
    @FXML private Label welcomeLabel;
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField isbnField;
    @FXML private TextField searchField;
    @FXML private Label addBookMessage;
    @FXML private TableView<Book> searchResultsTable;
    @FXML private TableColumn<Book, String> titleColumn;
    @FXML private TableColumn<Book, String> authorColumn;
    @FXML private TableColumn<Book, String> isbnColumn;
    @FXML private TableColumn<Book, String> statusColumn;
    @FXML private TableColumn<Book, String> dueDateColumn;
    @FXML private TableColumn<Book, String> borrowedByColumn;
    @FXML private TableColumn<Book, Double> fineColumn;
    @FXML private ComboBox<String> searchByCombo;



    // 🧠 بيانات البرنامج
    private Map<String, Book> bookMap = new HashMap<>();
    private ObservableList<Book> booksList = FXCollections.observableArrayList();
    private static final String FILE_PATH = "books.txt";

    // 🧾 المستخدم الحالي
    private String currentUsername;

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        fineColumn.setCellValueFactory(new PropertyValueFactory<>("fineAmount"));
        borrowedByColumn.setCellValueFactory(new PropertyValueFactory<>("borrowedBy"));

        searchResultsTable.setItems(booksList);

        searchByCombo.setItems(FXCollections.observableArrayList("All", "Title", "Author", "ISBN"));
        searchByCombo.getSelectionModel().select("All");

        loadBooksFromFile();
    }

    // 👋 استقبال اسم المستخدم من شاشة الدخول
    public void setCurrentUsername(String username) {
        this.currentUsername = username;
        System.out.println("✅ Logged in as: " + username);
        if (welcomeLabel != null)
            welcomeLabel.setText("Welcome, " + username + " 👋");
    }

    // 🚪 تسجيل الخروج
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

    // ➕ إضافة كتاب جديد
    @FXML
    private void handleAddBook() {
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String isbn = isbnField.getText().trim();

        if (title.isEmpty() || author.isEmpty() || isbn.isEmpty()) {
            addBookMessage.setText("❗ Please fill in all fields.");
            return;
        }

        if (bookMap.containsKey(isbn)) {
            addBookMessage.setText("❌ Book with this ISBN already exists.");
            return;
        }

        Book newBook = new Book(title, author, isbn, "Available", "", 0.0, "");
        bookMap.put(isbn, newBook);
        booksList.add(newBook);
        saveAllBooksToFile();

        addBookMessage.setText("✅ Book added successfully.");

        titleField.clear();
        authorField.clear();
        isbnField.clear();
    }

    // 🔍 البحث
    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().toLowerCase().trim();
        String mode = (searchByCombo.getValue() == null) ? "All" : searchByCombo.getValue();

        if (keyword.isEmpty()) {
            searchResultsTable.setItems(booksList);
            addBookMessage.setText("📚 Showing all books.");
            return;
        }

        ObservableList<Book> filtered = FXCollections.observableArrayList();

        for (Book b : booksList) {
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
        addBookMessage.setText("🔍 Found " + filtered.size() + " matching book(s).");
    }

    // 🔄 إعادة التحميل
    @FXML
    private void handleReload() {
        loadBooksFromFile();
        searchResultsTable.refresh();
        addBookMessage.setText("🔄 Books reloaded.");
    }

    // 📂 تحميل الكتب من الملف (7 أعمدة متوافقة)
    private void loadBooksFromFile() {
        booksList.clear();
        bookMap.clear();

        File file = new File(FILE_PATH);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 7);
                if (parts.length >= 3) {
                    String title = parts[0];
                    String author = parts[1];
                    String isbn = parts[2];
                    String status = (parts.length >= 4) ? parts[3] : "Available";
                    String dueDate = (parts.length >= 5) ? parts[4] : "";
                    double fine = (parts.length >= 6) ? Double.parseDouble(parts[5]) : 0.0;
                    String borrowedBy = (parts.length == 7) ? parts[6] : "";

                    Book book = new Book(title, author, isbn, status, dueDate, fine, borrowedBy);
                    booksList.add(book);
                    bookMap.put(isbn, book);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 💾 حفظ جميع الكتب إلى الملف (7 أعمدة)
    private void saveAllBooksToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (Book b : booksList) {
                writer.write(b.toFileFormat());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
