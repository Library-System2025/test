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
    @FXML private Label addBookMessage;
    
    // عناصر الإضافة
    @FXML private ComboBox<String> typeCombo; // ✅ جديد: لاختيار النوع
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField isbnField;

    // عناصر البحث والجدول
    @FXML private TextField searchField;
    @FXML private ComboBox<String> searchByCombo;
    @FXML private TableView<Media> searchResultsTable; // ✅ صار Media
    @FXML private TableColumn<Media, String> typeColumn; // ✅ عمود النوع
    @FXML private TableColumn<Media, String> titleColumn;
    @FXML private TableColumn<Media, String> authorColumn;
    @FXML private TableColumn<Media, String> isbnColumn;
    @FXML private TableColumn<Media, String> statusColumn;
    @FXML private TableColumn<Media, String> dueDateColumn;
    @FXML private TableColumn<Media, Double> fineColumn;
    @FXML private TableColumn<Media, String> borrowedByColumn;

    // 👤 عناصر إدارة المستخدمين (Tab 2) - كما هي
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colMembership;
    private ObservableList<User> usersList = FXCollections.observableArrayList();

    // 🧠 البيانات (Media Map & List)
    private Map<String, Media> mediaMap = new HashMap<>();
    private ObservableList<Media> mediaList = FXCollections.observableArrayList();
    private static final String FILE_PATH = "books.txt";

    @FXML
    public void initialize() {
        // 1. إعداد الأعمدة (بما فيها النوع)
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("mediaType")); // يقرأ من دالة getMediaType()
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        fineColumn.setCellValueFactory(new PropertyValueFactory<>("fineAmount"));
        borrowedByColumn.setCellValueFactory(new PropertyValueFactory<>("borrowedBy"));

        searchResultsTable.setItems(mediaList);

        // 2. إعداد القوائم
        searchByCombo.setItems(FXCollections.observableArrayList("All", "Title", "Author", "ISBN"));
        searchByCombo.getSelectionModel().select("All");

        typeCombo.setItems(FXCollections.observableArrayList("Book", "CD")); // ✅ الخيارات
        typeCombo.getSelectionModel().selectFirst();

        // 3. تلوين الصفوف
        searchResultsTable.setRowFactory(tv -> new TableRow<Media>() {
            @Override
            protected void updateItem(Media item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    switch (item.getStatus()) {
                        case "Available": setStyle("-fx-background-color: #d0f0c0;"); break;
                        case "Borrowed": setStyle("-fx-background-color: #fff9c4;"); break;
                        case "Overdue": setStyle("-fx-background-color: #ffcdd2;"); break;
                        default: setStyle(""); break;
                    }
                }
            }
        });

        // 4. تحميل البيانات
        loadMediaFromFile();

        // 5. إعداد جدول المستخدمين
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colMembership.setCellValueFactory(new PropertyValueFactory<>("membership"));
        usersTable.setItems(usersList);
        loadUsersFromFile();
    }

    public void setCurrentUsername(String username) {
        if (welcomeLabel != null) welcomeLabel.setText("Welcome, " + username + " 👋");
    }

    @FXML
    private void handleLogout() {
        try {
            Parent login = FXMLLoader.load(getClass().getResource("login.fxml"));
            Stage stage = (Stage) titleField.getScene().getWindow();
            stage.setScene(new Scene(login));
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ✅ دالة الإضافة الذكية (Polymorphism)
    @FXML
    private void handleAddBook() {
        String type = typeCombo.getValue();
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String isbn = isbnField.getText().trim();

        if (title.isEmpty() || author.isEmpty() || isbn.isEmpty() || type == null) {
            addBookMessage.setText("❗ Please fill all fields.");
            return;
        }

        if (mediaMap.containsKey(isbn)) {
            addBookMessage.setText("❌ Item with this ISBN exists.");
            return;
        }

        // 🔥 إنشاء الكائن حسب الاختيار
        Media newItem;
        if (type.equals("CD")) {
            newItem = new CD(title, author, isbn); // مدة 7 أيام، غرامة 2$
        } else {
            newItem = new Book(title, author, isbn); // مدة 28 يوم، غرامة 1$
        }

        mediaMap.put(isbn, newItem);
        mediaList.add(newItem);
        saveAllMediaToFile();

        addBookMessage.setText("✅ " + type + " added successfully.");
        titleField.clear(); authorField.clear(); isbnField.clear();
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().toLowerCase().trim();
        String mode = (searchByCombo.getValue() == null) ? "All" : searchByCombo.getValue();

        if (keyword.isEmpty()) {
            searchResultsTable.setItems(mediaList);
            addBookMessage.setText("📚 Showing all items.");
            return;
        }

        ObservableList<Media> filtered = FXCollections.observableArrayList();
        for (Media m : mediaList) {
            String t = m.getTitle().toLowerCase();
            String a = m.getAuthor().toLowerCase();
            String i = m.getIsbn().toLowerCase();
            boolean match = false;
            switch (mode) {
                case "Title": match = t.contains(keyword); break;
                case "Author": match = a.contains(keyword); break;
                case "ISBN": match = i.contains(keyword); break;
                default: match = t.contains(keyword) || a.contains(keyword) || i.contains(keyword); break;
            }
            if (match) filtered.add(m);
        }
        searchResultsTable.setItems(filtered);
    }

    @FXML
    private void handleReload() {
        loadMediaFromFile();
        searchResultsTable.refresh();
        addBookMessage.setText("🔄 Reloaded.");
    }

    // ✅ دالة التحميل المحدثة (9 أعمدة)
    private void loadMediaFromFile() {
        mediaList.clear();
        mediaMap.clear();
        File file = new File(FILE_PATH);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // التقسيم لـ 9 أجزاء (الأول هو النوع)
                String[] parts = line.split(",", 9);
                if (parts.length >= 4) {
                    String type = parts[0].trim(); // النوع
                    String title = parts[1].trim();
                    String author = parts[2].trim();
                    String isbn = parts[3].trim();
                    String status = (parts.length >= 5) ? parts[4].trim() : "Available";
                    String dueDate = (parts.length >= 6) ? parts[5].trim() : "";
                    
                    double fine = 0.0;
                    try { if(parts.length >= 7) fine = Double.parseDouble(parts[6]); } catch (Exception e) {}
                    
                    String borrowedBy = "";
                    if (parts.length >= 8) {
                        borrowedBy = parts[7].trim();
                        if (borrowedBy.equals("0.0")) borrowedBy = "";
                    }
                    
                    double amountPaid = 0.0;
                    if (parts.length == 9) {
                        try { amountPaid = Double.parseDouble(parts[8]); } catch (Exception e) {}
                    }

                    // 🔥 إنشاء الكائن المناسب (Factory Pattern بسيط)
                    Media item;
                    if (type.equalsIgnoreCase("CD")) {
                        item = new CD(title, author, isbn, status, dueDate, fine, borrowedBy, amountPaid);
                    } else {
                        item = new Book(title, author, isbn, status, dueDate, fine, borrowedBy, amountPaid);
                    }

                    // حساب الغرامة للأدمن
                    if (item.isOverdue() && !borrowedBy.isEmpty()) {
                        String membership = getUserMembership(borrowedBy);
                        item.calculateFine(membership);
                    }

                    mediaList.add(item);
                    mediaMap.put(isbn, item);
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        if (searchResultsTable != null) searchResultsTable.refresh();
    }

    private void saveAllMediaToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (Media m : mediaList) {
                writer.write(m.toFileFormat());
                writer.newLine();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    // --- دوال المستخدمين ---
    private String getUserMembership(String username) {
        File file = new File("users.txt");
        if (!file.exists() || username.isEmpty()) return "Silver";
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4 && parts[0].trim().equals(username.trim())) return parts[3].trim();
            }
        } catch (IOException e) {}
        return "Silver";
    }

    private void loadUsersFromFile() {
        usersList.clear();
        File file = new File("users.txt");
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4 && !parts[2].trim().equalsIgnoreCase("Admin")) {
                    usersList.add(new User(parts[0], parts[1], parts[2], parts[3]));
                }
            }
        } catch (IOException e) {}
    }

    @FXML
    private void handleDeleteUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Warning", "Select a user."); return; }
        
        String target = selected.getUsername();
        boolean hasLoans = false;
        // فحص الميديا بدلاً من الكتب فقط
        for (Media m : mediaList) {
            if (m.getBorrowedBy() != null && m.getBorrowedBy().equals(target)) { hasLoans = true; break; }
        }

        if (hasLoans) showAlert("Error", "User has active loans.");
        else {
            usersList.remove(selected);
            saveUsersToFile();
            showAlert("Success", "User deleted.");
        }
    }

    private void saveUsersToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("users.txt"))) {
            writer.write("m,123,Admin,Gold"); writer.newLine();
            for (User u : usersList) { writer.write(u.toFileFormat()); writer.newLine(); }
        } catch (IOException e) {}
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content);
        if (title.equals("Error") || title.equals("Warning")) alert.setAlertType(Alert.AlertType.ERROR);
        alert.showAndWait();
    }
}