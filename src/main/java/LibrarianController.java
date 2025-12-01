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

/**
 * Controller class for the Librarian Dashboard.
 * Handles borrowing and returning books/CDs and viewing library items.
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

    private ObservableList<Media> mediaList = FXCollections.observableArrayList();
    private static final String FILE_PATH = "books.txt";
    private String accountUsername;

    /**
     * Initializes the controller class.
     * Sets up table columns, loads data, and configures row coloring based on status.
     */
    
    @FXML
    public void initialize() {
        
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("mediaType"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        borrowedByColumn.setCellValueFactory(new PropertyValueFactory<>("borrowedBy"));

        bookTable.setItems(mediaList);
        loadMediaFromFile();

        
        bookTable.setRowFactory(tv -> new TableRow<Media>() {
            @Override
            protected void updateItem(Media item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    switch (item.getStatus()) {
                        case "Available": setStyle("-fx-background-color: #d4edda;"); break; 
                        case "Borrowed": setStyle("-fx-background-color: #fff3cd;"); break; 
                        case "Overdue": setStyle("-fx-background-color: #f8d7da;"); break; 
                        default: setStyle(""); break;
                    }
                } else { setStyle(""); }
            }
        });
    }

    /**
     * Handles the logout action.
     * Redirects to the login screen.
     */
    
    @FXML
    private void handleLogout() {
        try {
            Parent login = FXMLLoader.load(getClass().getResource("login.fxml"));
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(new Scene(login));
        } catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * Handles the borrowing process for a selected item.
     * Checks if the item is available and updates its status.
     */
    
    @FXML
    private void handleBorrowBook() {
        Media selected = bookTable.getSelectionModel().getSelectedItem();
        
        if (selected == null) { 
            infoLabel.setText("⚠️ Select an item to borrow."); 
            return; 
        }
        
        if (selected.getStatus().equals("Borrowed")) { 
            infoLabel.setText("❌ Item already borrowed."); 
            return; 
        }

        
        selected.borrow(accountUsername); 
        
        saveAllMediaToFile();
        reloadBooks();
        infoLabel.setText("✅ Borrowed successfully. Due date: " + selected.getDueDate());
    }

    /**
     * Handles the return process for a selected item.
     * Updates the status to Available.
     */
    
    @FXML
    private void handleReturnBook() {
        Media selected = bookTable.getSelectionModel().getSelectedItem();
        
        if (selected == null) { 
            infoLabel.setText("⚠️ Select an item to return."); 
            return; 
        }
        
        if (!selected.getStatus().equals("Borrowed") && !selected.getStatus().equals("Overdue")) {
             infoLabel.setText("ℹ️ This item is not borrowed."); 
             return; 
        }

        
        selected.returnMedia();
        
        saveAllMediaToFile();
        reloadBooks();
        infoLabel.setText("✅ Item returned successfully.");
    }

    /**
     * Filters the table based on the search keyword.
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
     * Reloads data from the file and refreshes the table.
     */
    
    @FXML
     void handleReload() { 
        reloadBooks(); 
        infoLabel.setText("🔄 Data reloaded."); 
    }

    /**
     * Loads media items from 'books.txt'.
     * Parses lines to create Book or CD objects.
     */
    
    private void loadMediaFromFile() {
        mediaList.clear();
        File file = new File(FILE_PATH);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                
                String[] parts = line.split(",", 9);
                if (parts.length >= 4) {
                    String type = parts[0].trim(); 
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
                        if(borrowedBy.equals("0.0")) borrowedBy="";
                    }
                    
                    double amountPaid = 0.0;
                    if (parts.length == 9) {
                        try { amountPaid = Double.parseDouble(parts[8]); } catch (Exception e) {}
                    }

                    
                    Media item;
                    if (type.equalsIgnoreCase("CD")) {
                        item = new CD(title, author, isbn, status, dueDate, fine, borrowedBy, amountPaid);
                    } else {
                        item = new Book(title, author, isbn, status, dueDate, fine, borrowedBy, amountPaid);
                    }

                    
                    if (item.isOverdue() && !borrowedBy.isEmpty()) {
                        String membership = getUserMembership(borrowedBy);
                        item.calculateFine(membership);
                    }
                    
                    mediaList.add(item);
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        
        if (bookTable != null) bookTable.refresh();
    }

    /**
     * Saves all media items to 'books.txt'.
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
     * Helper method to reload books.
     */
    
    private void reloadBooks() { 
        loadMediaFromFile(); 
        bookTable.refresh(); 
    }
    
    /**
     * Sets the current username.
     * @param username The username of the librarian.
     */

    public void setCurrentUsername(String username) {
        this.accountUsername = username;
        if (welcomeLabel != null) welcomeLabel.setText("Welcome, " + username + " 👋");
    }
    
    /**
     * Helper to get user membership for fine calculation.
     * @param username The username to check.
     * @return Membership type (Silver/Gold).
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
        } catch (IOException e) {}
        return "Silver";
    }
}