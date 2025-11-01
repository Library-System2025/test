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
import java.time.LocalDate;
import java.util.*;

public class LibrarianController {
    @FXML private Label welcomeLabel;
    @FXML private TextField searchField;
    @FXML private TableView<Book> bookTable;
    @FXML private TableColumn<Book, String> titleColumn;
    @FXML private TableColumn<Book, String> authorColumn;
    @FXML private TableColumn<Book, String> isbnColumn;
    @FXML private TableColumn<Book, String> statusColumn;
    @FXML private TableColumn<Book, String> dueDateColumn;
    @FXML private TableColumn<Book, String> borrowedByColumn;
    @FXML private Label infoLabel;
   

    private ObservableList<Book> booksList = FXCollections.observableArrayList();
    private Map<String, Book> bookMap = new HashMap<>();
    private static final String FILE_PATH = "books.txt";
    private String accountUsername;

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        borrowedByColumn.setCellValueFactory(new PropertyValueFactory<>("borrowedBy"));

        bookTable.setItems(booksList);
        loadBooksFromFile();

        // 🎨 تلوين الصفوف حسب الحالة
        bookTable.setRowFactory(tv -> new TableRow<Book>() {
            @Override
            protected void updateItem(Book book, boolean empty) {
                super.updateItem(book, empty);

                if (book == null || empty) {
                    setStyle("");
                    return;
                }

                switch (book.getStatus()) {
                    case "Available":
                        setStyle("-fx-background-color: #d4edda;"); // أخضر فاتح
                        break;
                    case "Borrowed":
                        setStyle("-fx-background-color: #fff3cd;"); // أصفر باهت
                        break;
                    case "Overdue":
                        setStyle("-fx-background-color: #f8d7da;"); // أحمر باهت
                        break;
                    default:
                        setStyle("");
                        break;
                }
            }
        });
    }


    @FXML
    private void handleLogout() {
        try {
            Parent login = FXMLLoader.load(getClass().getResource("login.fxml"));
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(new Scene(login));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 📚 استعارة كتاب من طرف الأمين
    @FXML
    private void handleBorrowBook() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            infoLabel.setText("⚠️ Please select a book to borrow.");
            return;
        }

        if (selected.getStatus().equals("Borrowed")) {
            infoLabel.setText("❌ Book already borrowed.");
            return;
        }

        selected.borrow(accountUsername);
        saveAllBooksToFile();
        reloadBooks();

        infoLabel.setText("✅ Book borrowed by " + accountUsername + ". Due date: " + selected.getDueDate());
    }

    // 🔁 إرجاع كتاب
    @FXML
    private void handleReturnBook() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            infoLabel.setText("⚠️ Select a book to return.");
            return;
        }

        if (!selected.getStatus().equals("Borrowed") && !selected.getStatus().equals("Overdue")) {
            infoLabel.setText("ℹ️ This book is not borrowed.");
            return;
        }

        selected.returnBook();
        saveAllBooksToFile();
        reloadBooks();

        infoLabel.setText("✅ Book returned successfully.");
    }

    // ⏰ فحص الكتب المتأخرة
    @FXML
    private void handleOverdueCheck() {
        int count = 0;
        for (Book b : booksList) {
            if (b.isOverdue() && !b.getStatus().equals("Overdue")) {
                b.setStatus("Overdue");
                count++;
            }
        }
        saveAllBooksToFile();
        reloadBooks();

        infoLabel.setText("⚠️ Marked " + count + " book(s) as overdue.");
    }

    // 🔍 البحث
    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().toLowerCase().trim();
        if (keyword.isEmpty()) {
            bookTable.setItems(booksList);
            return;
        }

        ObservableList<Book> filtered = FXCollections.observableArrayList();
        for (Book b : booksList) {
            if (b.getTitle().toLowerCase().contains(keyword) ||
                b.getIsbn().toLowerCase().contains(keyword)) {
                filtered.add(b);
            }
        }
        bookTable.setItems(filtered);
    }

    @FXML
    private void handleReload() {
        reloadBooks();
        infoLabel.setText("🔄 All books reloaded.");
    }

    // 📂 تحميل الكتب من الملف (7 أعمدة كاملة)
    private void loadBooksFromFile() {
        booksList.clear();
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

    // 💾 حفظ الكتب إلى الملف (7 أعمدة)
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

    // 🔁 إعادة تحميل البيانات بعد كل عملية
    private void reloadBooks() {
        loadBooksFromFile();
        bookTable.refresh();
    }

    public void setCurrentUsername(String username) {
        this.accountUsername = username;
        System.out.println("✅ Logged in as: " + username);
        if (welcomeLabel != null)
            welcomeLabel.setText("Welcome, " + username + " 👋");
    }
}
