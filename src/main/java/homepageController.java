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
import java.util.List;
import java.util.ArrayList;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Controller class for the main dashboard (Home Page).
 *
 * This class handles the core administrative functionalities of the library system, including:
 * <ul>
 *   <li>Adding new Books and CDs.</li>
 *   <li>Searching for media items.</li>
 *   <li>Managing registered users (listing, deleting).</li>
 *   <li>Sending overdue email notifications.</li>
 * </ul>
 *
 * @author Zainab
 * @version 1.0
 */

public class homepageController {

    @FXML private Label welcomeLabel;
    @FXML private Label addBookMessage;

    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField isbnField;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> searchByCombo;
    @FXML private TableView<Media> searchResultsTable;
    @FXML private TableColumn<Media, String> typeColumn;
    @FXML private TableColumn<Media, String> titleColumn;
    @FXML private TableColumn<Media, String> authorColumn;
    @FXML private TableColumn<Media, String> isbnColumn;
    @FXML private TableColumn<Media, String> statusColumn;
    @FXML private TableColumn<Media, String> dueDateColumn;
    @FXML private TableColumn<Media, Double> fineColumn;
    @FXML private TableColumn<Media, String> borrowedByColumn;
    @FXML private TableColumn<Media, Integer> copyIdColumn;

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colMembership;
    private ObservableList<User> usersList = FXCollections.observableArrayList();

    private Map<String, Media> mediaMap = new HashMap<>();
    private ObservableList<Media> mediaList = FXCollections.observableArrayList();
    private static final String FILE_PATH = "books.txt";
    private static final String USERS_FILE_PATH = "users.txt";

    // Constants for statuses
    private static final String STATUS_AVAILABLE = "Available";
    private static final String STATUS_BORROWED  = "Borrowed";
    private static final String STATUS_OVERDUE   = "Overdue";

    private static final OverduePublisher overduePublisher = new OverduePublisher();
    private static EmailService emailService;

    /**
     * Static initialization block to setup the Email Service.
     * Loads credentials from the .env file and subscribes the email service to overdue notifications.
     */
    static {
        try {
            Dotenv dotenv = Dotenv.load();
            String serviceEmail = dotenv.get("EMAIL_USERNAME");
            String servicePass  = dotenv.get("EMAIL_PASSWORD");

            emailService = new EmailService(serviceEmail, servicePass);

            EmailOverdueSubscriber emailSub =
                    new EmailOverdueSubscriber(emailService, serviceEmail);

            overduePublisher.subscribe(emailSub);
        } catch (Exception e) {
            System.err.println("Failed to init email service / subscribers in homepageController: " + e.getMessage());
        }
    }

    /**
     * Initializes the controller class.
     * This method is automatically called after the FXML file has been loaded.
     * It sets up table columns, loads data, and configures UI behavior.
     */
    @FXML
    public void initialize() {
        configureMediaTable();
        configureSearchControls();
        configureTypeCombo();
        configureMediaRowFactory();

        loadMediaFromFile();

        configureUsersTable();
    }

    // ---------- initialization helpers (reduce cognitive complexity) ----------

    private void configureMediaTable() {
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("mediaType"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        copyIdColumn.setCellValueFactory(new PropertyValueFactory<>("copyId"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        fineColumn.setCellValueFactory(new PropertyValueFactory<>("fineAmount"));
        borrowedByColumn.setCellValueFactory(new PropertyValueFactory<>("borrowedBy"));

        searchResultsTable.setItems(mediaList);
    }

    private void configureSearchControls() {
        searchByCombo.setItems(FXCollections.observableArrayList("All", "Title", "Author", "ISBN"));
        searchByCombo.getSelectionModel().select("All");
    }

    private void configureTypeCombo() {
        typeCombo.setItems(FXCollections.observableArrayList("Book", "CD"));
        typeCombo.getSelectionModel().selectFirst();
    }

    private void configureMediaRowFactory() {
        searchResultsTable.setRowFactory(tv -> new TableRow<Media>() {
            @Override
            protected void updateItem(Media item, boolean empty) {
                super.updateItem(item, empty);
                styleMediaRow(this, item, empty);
            }
        });
    }

    private void styleMediaRow(TableRow<Media> row, Media item, boolean empty) {
        if (empty || item == null) {
            row.setStyle("");
            return;
        }

        String status = item.getStatus();
        switch (status) {
            case STATUS_AVAILABLE:
                row.setStyle("-fx-background-color: #d0f0c0;");
                break;
            case STATUS_BORROWED:
                row.setStyle("-fx-background-color: #fff9c4;");
                break;
            case STATUS_OVERDUE:
                row.setStyle("-fx-background-color: #ffcdd2;");
                break;
            default:
                row.setStyle("");
                break;
        }
    }

    private void configureUsersTable() {
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colMembership.setCellValueFactory(new PropertyValueFactory<>("membership"));
        usersTable.setItems(usersList);
        loadUsersFromFile();
    }

    /**
     * Sets the current username on the welcome label.
     *
     * @param username The name of the logged-in user.
     */
    public void setCurrentUsername(String username) {
        if (welcomeLabel != null) welcomeLabel.setText("Welcome, " + username + " 👋");
    }

    /**
     * Handles the logout action.
     * Loads the login screen and closes the current dashboard.
     */
    @FXML
    private void handleLogout() {
        try {
            Parent login = FXMLLoader.load(getClass().getResource("login.fxml"));
            Stage stage = (Stage) titleField.getScene().getWindow();
            stage.setScene(new Scene(login));
        } catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * Handles adding a new Book or CD to the library.
     * Validates input fields, checks for duplicate ISBNs, and saves the new item to the file.
     * (Refactored to reduce Cognitive Complexity.)
     */
    @FXML
    void handleAddBook() {
        String type   = typeCombo.getValue();
        String title  = titleField.getText().trim();
        String author = authorField.getText().trim();
        String isbn   = isbnField.getText().trim();

        if (isInvalidBookInput(type, title, author, isbn)) {
            addBookMessage.setText("❗ Please fill all fields.");
            return;
        }

        Media firstWithIsbn = null;
        int sameIsbnCount   = 0;

        for (Media m : mediaList) {
            if (isbn.equals(m.getIsbn())) {
                sameIsbnCount++;
                if (firstWithIsbn == null) {
                    firstWithIsbn = m;
                }
            }
        }

        if (firstWithIsbn != null) {
            handleExistingIsbn(type, title, author, isbn, firstWithIsbn, sameIsbnCount);
        } else {
            addFirstCopy(type, title, author, isbn);
        }

        clearBookForm();
    }

    // ---------- helper methods for handleAddBook ----------

    private boolean isInvalidBookInput(String type, String title, String author, String isbn) {
        return title.isEmpty() || author.isEmpty() || isbn.isEmpty() || type == null;
    }

    private void handleExistingIsbn(String type,
                                    String title,
                                    String author,
                                    String isbn,
                                    Media firstWithIsbn,
                                    int sameIsbnCount) {

        if (!hasSameTitleAndAuthor(firstWithIsbn, title, author)) {
            addBookMessage.setText("❌ ISBN already exists but with different title/author!");
            return;
        }

        int newCopyId = sameIsbnCount + 1;
        Media newCopy = createMediaItem(type, title, author, isbn, newCopyId);

        mediaList.add(newCopy);
        mediaMap.putIfAbsent(isbn, firstWithIsbn);
        saveAllMediaToFile();

        addBookMessage.setText("📚 Added NEW COPY (Copy #" + newCopyId + ") of this book.");
    }

    private boolean hasSameTitleAndAuthor(Media firstWithIsbn, String title, String author) {
        boolean sameTitle  = firstWithIsbn.getTitle()  != null &&
                             firstWithIsbn.getTitle().equals(title);
        boolean sameAuthor = firstWithIsbn.getAuthor() != null &&
                             firstWithIsbn.getAuthor().equals(author);
        return sameTitle && sameAuthor;
    }

    private void addFirstCopy(String type, String title, String author, String isbn) {
        int copyId = 1;
        Media newItem = createMediaItem(type, title, author, isbn, copyId);

        mediaList.add(newItem);
        mediaMap.put(isbn, newItem);
        saveAllMediaToFile();

        addBookMessage.setText("📗 New Book Added (Copy #1).");
    }

    private Media createMediaItem(String type,
                                  String title,
                                  String author,
                                  String isbn,
                                  int copyId) {
        if ("CD".equals(type)) {
            return new CD(title, author, isbn,
                    STATUS_AVAILABLE, "", 0.0, "", 0.0, copyId);
        }
        return new Book(title, author, isbn,
                STATUS_AVAILABLE, "", 0.0, "", 0.0, copyId);
    }

    private void clearBookForm() {
        titleField.clear();
        authorField.clear();
        isbnField.clear();
    }

    /**
     * Handles searching for media items based on criteria.
     * Filters the table view based on Title, Author, or ISBN.
     * (Refactored to reduce Cognitive Complexity.)
     */
    @FXML
    void handleSearch() {
        String keyword = searchField.getText().toLowerCase().trim();
        String mode    = (searchByCombo.getValue() == null) ? "All" : searchByCombo.getValue();

        if (keyword.isEmpty()) {
            searchResultsTable.setItems(mediaList);
            addBookMessage.setText("📚 Showing all items.");
            return;
        }

        ObservableList<Media> filtered = FXCollections.observableArrayList();
        for (Media m : mediaList) {
            if (matchesSearch(m, mode, keyword)) {
                filtered.add(m);
            }
        }
        searchResultsTable.setItems(filtered);
    }

    private boolean matchesSearch(Media m, String mode, String keyword) {
        String t = m.getTitle().toLowerCase();
        String a = m.getAuthor().toLowerCase();
        String i = m.getIsbn().toLowerCase();

        switch (mode) {
            case "Title":  return t.contains(keyword);
            case "Author": return a.contains(keyword);
            case "ISBN":   return i.contains(keyword);
            default:       return t.contains(keyword) || a.contains(keyword) || i.contains(keyword);
        }
    }

    /**
     * Reloads data from the file and refreshes the table view.
     * Useful for updating the view after external changes.
     */
    @FXML
    void handleReload() {
        loadMediaFromFile();
        searchResultsTable.refresh();
        addBookMessage.setText("🔄 Reloaded.");
    }

    /**
     * Loads media items from 'books.txt'.
     * Parses the file and populates the media list. Also calculates fines for overdue items.
     * (Refactored to reduce Cognitive Complexity.)
     */
    private void loadMediaFromFile() {
        mediaList.clear();
        mediaMap.clear();
        File file = new File(FILE_PATH);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Media item = parseMediaLine(line);
                if (item != null) {
                    mediaList.add(item);
                    mediaMap.put(item.getIsbn(), item);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (searchResultsTable != null) searchResultsTable.refresh();
    }

    private Media parseMediaLine(String line) {
        String[] parts = line.split(",", 10);
        if (parts.length < 5) {
            return null;
        }

        String type   = parts[0].trim();
        String title  = parts[1].trim();
        String author = parts[2].trim();
        String isbn   = parts[3].trim();

        int copyId      = parseIntSafe(getPart(parts, 4), 1);
        String status   = parts.length >= 6 ? parts[5].trim() : STATUS_AVAILABLE;
        String dueDate  = parts.length >= 7 ? parts[6].trim() : "";
        double fine     = parts.length >= 8 ? parseDoubleSafe(parts[7], 0.0) : 0.0;
        String borrowed = normalizeBorrowedBy(getPart(parts, 8));
        double amountPaid = parts.length >= 10 ? parseDoubleSafe(parts[9], 0.0) : 0.0;

        Media item;
        if (type.equalsIgnoreCase("CD")) {
            item = new CD(title, author, isbn, status, dueDate, fine, borrowed, amountPaid, copyId);
        } else {
            item = new Book(title, author, isbn, status, dueDate, fine, borrowed, amountPaid, copyId);
        }

        if (item.isOverdue() && !borrowed.isEmpty()) {
            String membership = getUserMembership(borrowed);
            item.calculateFine(membership);
        }

        return item;
    }

    private int parseIntSafe(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private double parseDoubleSafe(String value, double defaultValue) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String getPart(String[] parts, int index) {
        return index < parts.length ? parts[index] : "";
    }

    private String normalizeBorrowedBy(String rawBorrowedBy) {
        String trimmed = rawBorrowedBy == null ? "" : rawBorrowedBy.trim();
        return "0.0".equals(trimmed) ? "" : trimmed;
    }

    /**
     * Saves all current media items to the 'books.txt' file.
     */
    private void saveAllMediaToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (Media m : mediaList) {
                writer.write(m.toFileFormat());
                writer.newLine();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * Retrieves the membership type for a specific user.
     *
     * @param username The username to check.
     * @return The membership type (Gold/Silver), defaults to Silver if not found.
     */
    private String getUserMembership(String username) {
        File file = new File(USERS_FILE_PATH);
        if (!file.exists() || username.isEmpty()) return "Silver";
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4 && parts[0].trim().equals(username.trim()))
                    return parts[3].trim();
            }
        } catch (IOException e) {}
        return "Silver";
    }

    /**
     * Retrieves the email address for a specific user.
     *
     * @param username The username to check.
     * @return The email address, or an empty string if not found.
     */
    private String getUserEmail(String username) {
        File file = new File(USERS_FILE_PATH);
        if (!file.exists() || username == null || username.isEmpty()) return "";
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 5 && parts[0].trim().equals(username.trim())) {
                    return parts[4].trim();
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        return "";
    }

    /**
     * Loads registered users from 'users.txt' into the table view.
     * Excludes Admin accounts from the view.
     */
    private void loadUsersFromFile() {
        usersList.clear();
        File file = new File(USERS_FILE_PATH);
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

    /**
     * Deletes a selected user.
     * Ensures the user has no active loans before deletion.
     */
    @FXML
    void handleDeleteUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Select a user.");
            return;
        }

        String target = selected.getUsername();
        boolean hasLoans = false;

        for (Media m : mediaList) {
            if (m.getBorrowedBy() != null && m.getBorrowedBy().equals(target)) {
                hasLoans = true;
                break;
            }
        }

        if (hasLoans) showAlert("Error", "User has active loans.");
        else {
            usersList.remove(selected);
            saveUsersToFile();
            showAlert("Success", "User deleted.");
        }
    }

    /**
     * Saves the list of users to 'users.txt'.
     */
    private void saveUsersToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE_PATH))) {
            writer.write("m,123,Admin,Gold");
            writer.newLine();
            for (User u : usersList) {
                writer.write(u.toFileFormat());
                writer.newLine();
            }
        } catch (IOException e) {}
    }

    /**
     * Shows an information alert dialog.
     * <p>
     * Includes a check for the JavaFX Application Thread to support unit testing.
     * </p>
     *
     * @param title   The title of the alert.
     * @param content The content message.
     */
    private void showAlert(String title, String content) {
        if (!Platform.isFxApplicationThread()) {
            System.out.println("ALERT (test mode): " + title + " | " + content);
            return;
        }

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Sends an email reminder to a selected user regarding overdue books.
     * Uses the Observer pattern to trigger the notification.
     */
    @FXML
    void handleSendReminder() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Select a user to send reminder.");
            return;
        }

        String username = selected.getUsername();
        String email    = getUserEmail(username);

        if (email == null || email.isEmpty()) {
            showAlert("Error", "This user has no email saved.");
            return;
        }

        List<Media> overdueList = new ArrayList<>();

        for (Media m : mediaList) {
            if (username.equals(m.getBorrowedBy()) && m.isOverdue()) {
                overdueList.add(m);
            }
        }

        if (overdueList.isEmpty()) {
            showAlert("Info", "This user has no overdue books.");
            return;
        }

        overduePublisher.notifySubscribers(username, email, overdueList);

        showAlert("Success", "Reminder email sent.");
    }
}
