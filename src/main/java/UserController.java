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
    private String membershipType;

    // 👤 يأتي من LoginController
    public void setMembershipType(String membershipType) {
        this.membershipType = membershipType;
        System.out.println("✅ Membership Type received: " + membershipType);
        updateWelcomeLabel();
        tryLoadBooks(); // تحميل بعد وصول العضوية
    }

    public void setCurrentUsername(String username) {
        this.accountUsername = username;
        System.out.println("✅ Username received: " + username);
        updateWelcomeLabel();
        tryLoadBooks(); // تحميل بعد وصول اسم المستخدم
    }

    // ⚙️ تحميل الكتب بعد التأكد أن المعلومات كاملة
    private void tryLoadBooks() {
        if (accountUsername != null && membershipType != null) {
            System.out.println("📘 Loading books for user " + accountUsername +
                    " (" + membershipType + ")");
            loadBooksFromFile();
            bookTable.refresh();
        }
    }

    // 🟢 تحديث رسالة الترحيب
    private void updateWelcomeLabel() {
        if (welcomeLabel != null && accountUsername != null && membershipType != null) {
            welcomeLabel.setText("Welcome, " + accountUsername + " (" + membershipType + ") 👋");

            if (membershipType.equalsIgnoreCase("Gold")) {
                welcomeLabel.setStyle("-fx-text-fill: gold; -fx-font-weight: bold;");
            } else {
                welcomeLabel.setStyle("-fx-text-fill: silver; -fx-font-weight: bold;");
            }

            System.out.println("🟢 Label updated → " + membershipType);
        }
    }

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        fineColumn.setCellValueFactory(new PropertyValueFactory<>("fineAmount"));
        bookTable.setItems(booksList);

        // ✅ تلوين الصفوف
        bookTable.setRowFactory(tv -> new TableRow<Book>() {
            @Override
            protected void updateItem(Book book, boolean empty) {
                super.updateItem(book, empty);
                if (empty || book == null) {
                    setStyle("");
                } else if (book.getBorrowedBy() != null && book.getBorrowedBy().equals(accountUsername)) {
                    setStyle("-fx-background-color: #d0f0c0;"); // 💚
                } else if (book.getStatus().equals("Borrowed") || book.getStatus().equals("Overdue")) {
                    setStyle("-fx-background-color: #ffd6d6;"); // ❤️
                } else {
                    setStyle("");
                }
            }
        });

        // ✅ إظهار التاريخ فقط للمستخدم
        dueDateColumn.setCellFactory(column -> new TableCell<Book, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    Book book = getTableRow().getItem();
                    if (book.getBorrowedBy() != null && book.getBorrowedBy().equals(accountUsername)) {
                        setText(book.getDueDate());
                    } else {
                        setText("");
                    }
                }
            }
        });

        // ✅ إظهار الغرامة فقط للمستخدم
        fineColumn.setCellFactory(column -> new TableCell<Book, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    Book book = getTableRow().getItem();
                    if (book.getBorrowedBy() != null && book.getBorrowedBy().equals(accountUsername)) {
                        setText(String.format("$%.2f", book.getFineAmount()));
                    } else {
                        setText("");
                    }
                }
            }
        });
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
        for (Book b : booksList) {
            if (b.getBorrowedBy().equals(accountUsername) && b.getFineAmount() > 0) {
                messageLabel.setText("❌ You have unpaid fines. Please pay them before borrowing.");
                return;
            }
        }

        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("⚠️ Please select a book first.");
            return;
        }

        if (!selected.getStatus().equals("Available")) {
            messageLabel.setText("❌ This book is not available.");
            return;
        }

        selected.setStatus("Borrowed");
        selected.setDueDate(LocalDate.now().plusDays(28).toString());
        selected.setFineAmount(0.0);
        selected.setBorrowedBy(accountUsername);

        saveAllBooksToFile();
        reloadBooks();

        messageLabel.setText("✅ Book borrowed successfully! Due date: " + selected.getDueDate());
    }

    // 💰 دفع الغرامة
    @FXML
    private void handlePayFine() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            infoLabel.setText("⚠️ Select a book first.");
            return;
        }

        if (!selected.getBorrowedBy().equals(accountUsername)) {
            infoLabel.setText("❌ You can only pay fines for books you borrowed.");
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

        double currentFine = selected.getFineAmount();
        double remaining = currentFine - amountToPay;

        if (remaining < 0) {
            infoLabel.setText("❌ Payment exceeds fine amount!");
            return;
        }

        int index = booksList.indexOf(selected);
        if (index != -1) {
            selected.setFineAmount(remaining);
            if (remaining <= 0) selected.returnBook();
            else selected.setStatus("Overdue");
            booksList.set(index, selected);
        }

        saveAllBooksToFile();
        bookTable.refresh();

        if (remaining <= 0)
            infoLabel.setText("✅ Fine fully paid for '" + selected.getTitle() + "'.");
        else
            infoLabel.setText("💰 Partial payment recorded. Remaining fine: $" + String.format("%.2f", remaining));

        paymentField.clear();
    }

    // 📂 تحميل الكتب
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

                    if (book.isOverdue()) {
                        if (borrowedBy.equals(accountUsername))
                            book.calculateFine(membershipType);
                        else
                            book.calculateFine("Silver");
                    }

                    booksList.add(book);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        bookTable.setItems(booksList);
        bookTable.refresh();
    }

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

    private void reloadBooks() {
        booksList.clear();
        loadBooksFromFile();
        bookTable.refresh();
    }
    @FXML
    private void handleReload() {
        booksList.clear();
        loadBooksFromFile();
        bookTable.refresh();
        infoLabel.setText("🔄 Data reloaded from file successfully!");
    }

    // 📦 إرجاع الكتاب
    @FXML
    private void handleReturnBook() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("⚠️ Please select a book to return.");
            return;
        }

        if (!selected.getStatus().equals("Borrowed") && !selected.getStatus().equals("Overdue")) {
            messageLabel.setText("This book is not borrowed.");
            return;
        }

        if (!selected.getBorrowedBy().equals(accountUsername)) {
            messageLabel.setText("❌ You can only return books you borrowed.");
            return;
        }

        selected.calculateFine(membershipType);

        if (selected.getFineAmount() > 0) {
            messageLabel.setText("⚠️ Book returned but fine must be paid.");
            selected.setStatus("Overdue");
        } else {
            selected.returnBook();
            messageLabel.setText("✅ Book returned successfully!");
        }

        saveAllBooksToFile();
        reloadBooks();
    }
}
