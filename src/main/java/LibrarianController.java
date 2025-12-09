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

/**
 * Controller class for the Librarian Dashboard.
 * Manages core library operations including borrowing, returning, 
 * searching, and viewing library media items.
 * 
 * @author Zainab
 * @version 1.0
 */
public class LibrarianController {

    @FXML private Label welcomeLabel;
    @FXML private TextField searchField;
    @FXML private Label infoLabel;

    @FXML private TableView<Media> bookTable; 
    @FXML private TableColumn<Media, String> typeColumn; 
    @FXML private TableColumn<Media, String> titleColumn;
    @FXML private TableColumn<Media, String> authorColumn;
    @FXML private TableColumn<Media, String> isbnColumn;
    @FXML private TableColumn<Media, String> statusColumn;
    @FXML private TableColumn<Media, String> dueDateColumn;
    @FXML private TableColumn<Media, String> borrowedByColumn;
    @FXML private TableColumn<Media, Integer> copyIdColumn;

    private ObservableList<Media> mediaList = FXCollections.observableArrayList();

    private static final String FILE_PATH          = "books.txt";
    private static final String STATUS_AVAILABLE   = "Available";
    private static final String STATUS_BORROWED    = "Borrowed";
    private static final String STATUS_OVERDUE     = "Overdue";

    private String accountUsername;

    /**
     * Initializes the controller class.
     * Configures the table columns, loads data from file, and sets up row styling.
     */
    @FXML
    public void initialize() {
        setupTableColumns();
        loadMediaFromFile();
        setupRowColoring();
    }

    /**
     * Configures the TableView columns to bind with Media properties.
     */
    private void setupTableColumns() {
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("mediaType"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        borrowedByColumn.setCellValueFactory(new PropertyValueFactory<>("borrowedBy"));
        copyIdColumn.setCellValueFactory(new PropertyValueFactory<>("copyId"));
        
        bookTable.setItems(mediaList);
    }

    /**
     * Configures the TableRow factory to change background colors based on item status.
     */
    private void setupRowColoring() {
        bookTable.setRowFactory(tv -> new TableRow<Media>() {
            @Override
            protected void updateItem(Media item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    switch (item.getStatus()) {
                        case STATUS_AVAILABLE: setStyle("-fx-background-color: #d4edda;"); break;
                        case STATUS_BORROWED:  setStyle("-fx-background-color: #fff3cd;"); break;
                        case STATUS_OVERDUE:   setStyle("-fx-background-color: #f8d7da;"); break;
                        default:               setStyle(""); break;
                    }
                }
            }
        });
    }

    /**
     * Handles the borrowing transaction for the selected media item.
     * Validates availability and updates the status.
     */
    @FXML
    private void handleBorrowBook() {
        Media selected = getSelectedMedia();
        if (selected == null) return;

        if (STATUS_BORROWED.equals(selected.getStatus())) {
            showInfo("❌ Item already borrowed.");
            return;
        }

        selected.borrow(accountUsername);
        finalizeTransaction("✅ Borrowed successfully. Due date: " + selected.getDueDate());
    }

    /**
     * Handles the return transaction for the selected media item.
     * Validates current status and processes the return.
     */
    @FXML
    private void handleReturnBook() {
        Media selected = getSelectedMedia();
        if (selected == null) return;

        if (!STATUS_BORROWED.equals(selected.getStatus()) && !STATUS_OVERDUE.equals(selected.getStatus())) {
            showInfo("ℹ️ This item is not borrowed.");
            return;
        }

        selected.returnMedia();
        finalizeTransaction("✅ Item returned successfully.");
    }

    /**
     * Retrieves the currently selected item from the table.
     * Displays a warning if no item is selected.
     * 
     * @return The selected Media object, or null if nothing is selected.
     */
    private Media getSelectedMedia() {
        Media selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("⚠️ Select an item first.");
        }
        return selected;
    }

    /**
     * Completes a transaction by saving data, refreshing the view, and showing a message.
     * 
     * @param successMessage The message to display to the user.
     */
    private void finalizeTransaction(String successMessage) {
        saveAllMediaToFile();
        reloadBooks();
        showInfo(successMessage);
    }

    /**
     * Updates the information label with a specific message.
     * 
     * @param message The text to display.
     */
    private void showInfo(String message) {
        infoLabel.setText(message);
    }

    /**
     * Handles the logout process and redirects to the login view.
     */
    @FXML
    private void handleLogout() {
        try {
            Parent login = FXMLLoader.load(getClass().getResource("login.fxml"));
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(new Scene(login));
        } catch (IOException e) {
            System.err.println("Error loading login screen: " + e.getMessage());
        }
    }

    /**
     * Filters the displayed media list based on the search input.
     */
    @FXML
    void handleSearch() {
        String keyword = searchField.getText().toLowerCase().trim();
        if (keyword.isEmpty()) {
            bookTable.setItems(mediaList);
            return;
        }

        ObservableList<Media> filtered = FXCollections.observableArrayList();
        for (Media m : mediaList) {
            if (m.getTitle().toLowerCase().contains(keyword) || m.getIsbn().toLowerCase().contains(keyword)) {
                filtered.add(m);
            }
        }
        bookTable.setItems(filtered);
    }

    /**
     * Reloads the data from the source file and refreshes the table view.
     */
    @FXML
    void handleReload() {
        reloadBooks();
        showInfo("🔄 Data reloaded.");
    }

    /**
     * Helper method that combines loading from file and refreshing the table.
     */
    private void reloadBooks() {
        loadMediaFromFile();
        bookTable.refresh();
    }

    /**
     * Reads media data from the text file and populates the list.
     */
    private void loadMediaFromFile() {
        mediaList.clear();
        File file = new File(FILE_PATH);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Media item = parseMediaLine(line);
                if (item != null) mediaList.add(item);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        
        if (bookTable != null) bookTable.refresh();
    }

    /**
     * Parses a single line of text into a Media object.
     * 
     * @param line The comma-separated string from the file.
     * @return A Media object (Book or CD), or null if parsing fails.
     */
    private Media parseMediaLine(String line) {
        String[] parts = line.split(",", 10);
        if (parts.length < 5) return null;

        String type   = parts[0].trim();
        String title  = parts[1].trim();
        String author = parts[2].trim();
        String isbn   = parts[3].trim();
        int copyId    = parseIntSafe(getPart(parts, 4), 1);
        String status = parts.length >= 6 ? parts[5].trim() : STATUS_AVAILABLE;
        String dueDate= parts.length >= 7 ? parts[6].trim() : "";
        double fine   = parts.length >= 8 ? parseDoubleSafe(parts[7], 0.0) : 0.0;
        String borrowedBy = extractBorrowedBy(parts);
        double amountPaid = parts.length >= 10 ? parseDoubleSafe(parts[9], 0.0) : 0.0;

        Media item = createMediaItem(type, title, author, isbn, status, dueDate, fine, borrowedBy, amountPaid, copyId);
        applyOverdueFineIfNeeded(item, borrowedBy);
        return item;
    }

    /**
     * Factory method to create specific Media instances.
     */
    private Media createMediaItem(String type, String t, String a, String i, String s, String d, double f, String b, double p, int c) {
        if ("CD".equalsIgnoreCase(type)) {
            return new CD(t, a, i, s, d, f, b, p, c);
        }
        return new Book(t, a, i, s, d, f, b, p, c);
    }

    /**
     * Calculates and updates fines if the item is overdue.
     */
    private void applyOverdueFineIfNeeded(Media item, String borrowedBy) {
        if (item.isOverdue() && borrowedBy != null && !borrowedBy.isEmpty()) {
            item.calculateFine(getUserMembership(borrowedBy));
        }
    }

    private int parseIntSafe(String value, int defaultValue) {
        try { return Integer.parseInt(value.trim()); } catch (Exception e) { return defaultValue; }
    }

    private double parseDoubleSafe(String value, double defaultValue) {
        try { return Double.parseDouble(value.trim()); } catch (Exception e) { return defaultValue; }
    }

    private String getPart(String[] parts, int index) {
        return index < parts.length ? parts[index] : "";
    }

    private String extractBorrowedBy(String[] parts) {
        if (parts.length < 9) return "";
        String val = parts[8].trim();
        return "0.0".equals(val) ? "" : val;
    }

    /**
     * Writes all current media items back to the file.
     */
    private void saveAllMediaToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (Media m : mediaList) {
                writer.write(m.toFileFormat());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the username for the current session.
     * 
     * @param username The username of the logged-in librarian.
     */
    public void setCurrentUsername(String username) {
        this.accountUsername = username;
        if (welcomeLabel != null) welcomeLabel.setText("Welcome, " + username + " 👋");
    }

    /**
     * Retrieves the membership type for a given user.
     * 
     * @param username The username to look up.
     * @return The membership type (e.g., "Gold", "Silver").
     */
    private String getUserMembership(String username) {
        File file = new File("users.txt");
        if (!file.exists() || username.isEmpty()) return "Silver";
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4 && parts[0].trim().equals(username.trim())) {
                    return parts[3].trim();
                }
            }
        } catch (IOException e) {
            
        }
        return "Silver";
    }
}