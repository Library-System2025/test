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

public class UserController {

    @FXML private Label welcomeLabel;
    @FXML private TableView<Book> bookTable;
    @FXML private TableColumn<Book, String> titleColumn;
    @FXML private TableColumn<Book, String> authorColumn;
    @FXML private TableColumn<Book, String> isbnColumn;
    @FXML private TableColumn<Book, String> statusColumn;
    @FXML private TableColumn<Book, String> dueDateColumn;
    @FXML private TableColumn<Book, Double> fineColumn;
    @FXML private TableColumn<Book, String> borrowedByColumn;
    @FXML private TextField paymentField;
    @FXML private Label infoLabel;
    @FXML private Label messageLabel;

    private ObservableList<Book> booksList = FXCollections.observableArrayList();
    private static final String FILE_PATH = "books.txt";
    private String accountUsername;

    // ✅ عند فتح الصفحة
    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        fineColumn.setCellValueFactory(new PropertyValueFactory<>("fineAmount"));
    

        bookTable.setItems(booksList);
        loadBooksFromFile();
    }

    // 👋 ضبط اسم المستخدم الحالي بعد تسجيل الدخول
    public void setCurrentUsername(String username) {
        this.accountUsername = username;
        System.out.println("✅ Logged in as: " + username);
        if (welcomeLabel != null)
            welcomeLabel.setText("Welcome, " + username + " 👋");
    }

    // 🚪 تسجيل الخروج
    @FXML
    private void handleLogout() {
        try {
            Parent login = FXMLLoader.load(getClass().getResource("login.fxml"));
            Stage stage = (Stage) bookTable.getScene().getWindow();
            stage.setScene(new Scene(login));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 📚 استعارة كتاب
    @FXML
    private void handleBorrowBook() {
        Book selectedBook = bookTable.getSelectionModel().getSelectedItem();

        if (selectedBook == null) {
            messageLabel.setText("⚠️ Please select a book first.");
            return;
        }

        if (!selectedBook.getStatus().equals("Available")) {
            messageLabel.setText("❌ This book is not available.");
            return;
        }

        // ✅ تحديث حالة الكتاب
        selectedBook.setStatus("Borrowed");
        selectedBook.setDueDate(LocalDate.now().plusDays(28).toString());
        selectedBook.setFineAmount(0.0);
        selectedBook.setBorrowedBy(accountUsername);

        // ✅ حفظ و إعادة تحميل الملف
        saveAllBooksToFile();
        reloadBooks();

        messageLabel.setText("✅ Book borrowed successfully! Due date: " + selectedBook.getDueDate());
    }

    // 🔁 دالة لإعادة تحميل الكتب بعد أي تعديل
    private void reloadBooks() {
        booksList.clear();
        loadBooksFromFile();
        bookTable.refresh();
    }

    // 💰 دفع الغرامة
    @FXML
    private void handlePayFine() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            infoLabel.setText("⚠️ Select a book first.");
            return;
        }

        if (selected.getFineAmount() <= 0) {
            infoLabel.setText("✅ No fine for this book.");
            return;
        }

        double amountToPay;
        try {
            amountToPay = Double.parseDouble(paymentField.getText());
        } catch (NumberFormatException e) {
            infoLabel.setText("❌ Enter a valid number.");
            return;
        }

        if (amountToPay <= 0) {
            infoLabel.setText("❌ Amount must be positive.");
            return;
        }

        double remaining = selected.getFineAmount() - amountToPay;
        if (remaining <= 0) {
            selected.setFineAmount(0);
            selected.setStatus("Available");
            selected.setBorrowedBy("");
            infoLabel.setText("✅ Fine fully paid. You can borrow again!");
        } else {
            selected.setFineAmount(remaining);
            infoLabel.setText("💰 Partial payment done. Remaining: $" + remaining);
        }

        saveAllBooksToFile();
        bookTable.refresh();
        paymentField.clear();
    }

    // 🔄 تحديث حالة الغرامات / التأخير
    @FXML
    private void handleReload() {
        for (Book b : booksList) {
            b.calculateFine();
        }
        saveAllBooksToFile();
        bookTable.refresh();
        infoLabel.setText("🔄 Refreshed fine and status info.");
    }

    // 📂 تحميل الكتب من الملف
    private void loadBooksFromFile() {
        File file = new File(FILE_PATH);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 7); // ✅ نقرأ 7 أعمدة
                if (parts.length >= 3) {
                    String title = parts[0];
                    String author = parts[1];
                    String isbn = parts[2];
                    String status = (parts.length >= 4) ? parts[3] : "Available";
                    String dueDate = (parts.length >= 5) ? parts[4] : "";
                    double fine = (parts.length >= 6) ? Double.parseDouble(parts[5]) : 0.0;
                    String borrowedBy = (parts.length == 7) ? parts[6] : "";

                    Book book = new Book(title, author, isbn, status, dueDate, fine, borrowedBy);
                    book.calculateFine();
                    booksList.add(book);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 💾 حفظ جميع الكتب في الملف
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

    @FXML
    private void handleReturnBook() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            messageLabel.setText("⚠️ Please select a book to return.");
            return;
        }

        if (!selected.getStatus().equals("Borrowed") && !selected.getStatus().equals("Overdue")) {
            messageLabel.setText("ℹ️ This book is not borrowed.");
            return;
        }

        // ✅ التحقق إذا هذا المستخدم هو فعلاً اللي استعار الكتاب
        if (!selected.getBorrowedBy().equals(accountUsername)) {
            messageLabel.setText("❌ You can only return books you borrowed.");
            return;
        }

        // ✅ تنفيذ عملية الإرجاع
        selected.returnBook();
        saveAllBooksToFile();
        reloadBooks();

        messageLabel.setText("✅ Book returned successfully!");
    }

}
