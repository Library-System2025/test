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
        
        bookTable.setRowFactory(tv -> new TableRow<Book>() {
            @Override
            protected void updateItem(Book book, boolean empty) {
                super.updateItem(book, empty);

                if (empty || book == null) {
                    setStyle("");
                } else {
                    // إذا المستخدم الحالي هو اللي مستعير الكتاب
                    if (book.getBorrowedBy() != null && book.getBorrowedBy().equals(accountUsername)) {
                        setStyle("-fx-background-color: #d0f0c0;"); // 💚 أخضر فاتح
                    }
                    // إذا الكتاب مستعار من شخص آخر
                    else if (book.getStatus().equals("Borrowed") || book.getStatus().equals("Overdue")) {
                        setStyle("-fx-background-color: #ffd6d6;"); // ❤️ أحمر فاتح
                    }
                    // الكتب المتاحة
                    else {
                        setStyle("");
                    }
                }
            }
        });

        
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
    	
    	// 🚫 تحقق أولاً هل المستخدم عليه أي غرامة
        for (Book b : booksList) {
            if (b.getBorrowedBy().equals(accountUsername) && b.getFineAmount() > 0) {
                messageLabel.setText("❌ You have unpaid fines. Please pay them before borrowing.");
                return;
            }
        }
    	
    	 // 🔁 حفظ الكتاب المحدد قبل التحديث
        Book selectedBeforeReload = bookTable.getSelectionModel().getSelectedItem();

        // 🔁 تحديث البيانات من الملف
        reloadBooks();

        // ✅ إعادة اختيار الكتاب في الجدول بعد التحديث
        if (selectedBeforeReload != null) {
            for (Book b : booksList) {
                if (b.getIsbn().equals(selectedBeforeReload.getIsbn())) {
                    bookTable.getSelectionModel().select(b);
                    selectedBeforeReload = b;
                    break;
                }
            }
        }

        // ⚠️ التحقق بعد التحديث
        if (selectedBeforeReload == null) {
            messageLabel.setText("⚠️ Please select a book first.");
            return;
        }

        if (!selectedBeforeReload.getStatus().equals("Available")) {
            messageLabel.setText("❌ This book is not available.");
            return;
        }

        // ✅ تحديث حالة الكتاب
        selectedBeforeReload.setStatus("Borrowed");
        selectedBeforeReload.setDueDate(LocalDate.now().plusDays(28).toString());
        selectedBeforeReload.setFineAmount(0.0);
        selectedBeforeReload.setBorrowedBy(accountUsername);

        // ✅ حفظ و إعادة تحميل الملف بعد التعديل
        saveAllBooksToFile();
        reloadBooks();

        messageLabel.setText("✅ Book borrowed successfully! Due date: " + selectedBeforeReload.getDueDate());
    }

    // 🔁 دالة لإعادة تحميل الكتب بعد أي تعديل
    private void reloadBooks() {
        booksList.clear();
        loadBooksFromFile();
        bookTable.setItems(booksList); // ✅ أعد ربط الجدول بالقائمة
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
        
        // 🚫 تحقق هل الكتاب مستعار من المستخدم الحالي فقط
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

        // 🧮 احسب المبلغ المتبقي
        double currentFine = selected.getFineAmount();
        double remaining = currentFine - amountToPay;

        if (remaining < 0) {
            infoLabel.setText("❌ Payment exceeds fine amount!");
            return;
        }

     // 🟢 البحث عن فهرس الكتاب في القائمة لتحديثه بشكل صريح
        int selectedIndex = booksList.indexOf(selected);
        if (selectedIndex != -1) {
            selected.setFineAmount(remaining);

            if (remaining <= 0) {
                selected.returnBook();
            } else {
                selected.setStatus("Overdue");
            }
            // 🟢 إبلاغ الـ ObservableList بالتغيير
            booksList.set(selectedIndex, selected);
        }

        // 💾 احفظ الملف فورًا بعد التعديل
        saveAllBooksToFile();
        
        System.out.println("DEBUG: Books saved. Current fine for selected book: " + selected.getFineAmount());
        
        // ✅ حدّث الجدول فورًا
        bookTable.refresh();

        // ✅ رسالة الحالة
        if (remaining <= 0) {
            infoLabel.setText("✅ Fine fully paid for '" + selected.getTitle() + "'. Book is now available!");
        } else {
            infoLabel.setText("💰 Partial payment recorded. Remaining fine: $" + String.format("%.2f", remaining));
        }

        // 🔄 تنظيف الحقل
        paymentField.clear();
    }
    
    
    // 🔄 تحديث حالة الغرامات / التأخير
    @FXML
    private void handleReload() {
    	booksList.clear();          // احذف البيانات القديمة
        loadBooksFromFile();        // أعد تحميلها من الملف
        bookTable.refresh();        // حدّث الجدول في الواجهة
        infoLabel.setText("🔄 Data reloaded from file successfully!");
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

        if (!selected.getBorrowedBy().equals(accountUsername)) {
            messageLabel.setText("❌ You can only return books you borrowed.");
            return;
        }

        selected.calculateFine();

        // ❗ إذا عليه غرامة لا يرجع كمتاح
        if (selected.getFineAmount() > 0) {
            messageLabel.setText("⚠️ Book returned but fine must be paid before it's available.");
            selected.setStatus("Overdue");
        } else {
            selected.returnBook();
            messageLabel.setText("✅ Book returned successfully!");
        }

        saveAllBooksToFile();
        reloadBooks();
    }

}
