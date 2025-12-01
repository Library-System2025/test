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

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Controller class for the main dashboard (Home Page).
 * Handles adding books, searching, user management, and email notifications.
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

    
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colMembership;
    private ObservableList<User> usersList = FXCollections.observableArrayList();

    
    private Map<String, Media> mediaMap = new HashMap<>();
    private ObservableList<Media> mediaList = FXCollections.observableArrayList();
    private static final String FILE_PATH = "books.txt";

    
    private static final OverduePublisher overduePublisher = new OverduePublisher();
    private static EmailService emailService;

    /**
     * Static block to initialize the Email Service.
     * Loads credentials from the .env file.
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
     * Sets up table columns, loads data, and configures UI factories.
     */

    @FXML
    public void initialize() {
        
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("mediaType"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        fineColumn.setCellValueFactory(new PropertyValueFactory<>("fineAmount"));
        borrowedByColumn.setCellValueFactory(new PropertyValueFactory<>("borrowedBy"));

        searchResultsTable.setItems(mediaList);

        
        searchByCombo.setItems(FXCollections.observableArrayList("All", "Title", "Author", "ISBN"));
        searchByCombo.getSelectionModel().select("All");

        typeCombo.setItems(FXCollections.observableArrayList("Book", "CD"));
        typeCombo.getSelectionModel().selectFirst();

       
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

        
        loadMediaFromFile();

        
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
     * Redirects to the login screen.
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
     * Handles adding a new Book or CD.
     * Validates inputs and saves to file.
     */
    
    @FXML
    void handleAddBook() {
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

        Media newItem;
        if ("CD".equals(type)) {
            newItem = new CD(title, author, isbn);
        } else {
            newItem = new Book(title, author, isbn);
        }

        mediaMap.put(isbn, newItem);
        mediaList.add(newItem);
        saveAllMediaToFile();

        addBookMessage.setText("✅ " + type + " added successfully.");
        titleField.clear();
        authorField.clear();
        isbnField.clear();
    }

    /**
     * Handles searching for media items.
     * Filters the list based on the search keyword.
     */
    
    @FXML
     void handleSearch() {
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
                case "Title":  match = t.contains(keyword); break;
                case "Author": match = a.contains(keyword); break;
                case "ISBN":   match = i.contains(keyword); break;
                default:       match = t.contains(keyword) || a.contains(keyword) || i.contains(keyword); break;
            }
            if (match) filtered.add(m);
        }
        searchResultsTable.setItems(filtered);
    }

    /**
     * Reloads data from the file and refreshes the view.
     */
    
    @FXML
     void handleReload() {
        loadMediaFromFile();
        searchResultsTable.refresh();
        addBookMessage.setText("🔄 Reloaded.");
    }

    /**
     * Loads media items from 'books.txt'.
     */
     
    private void loadMediaFromFile() {
        mediaList.clear();
        mediaMap.clear();
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
                    try { if (parts.length >= 7) fine = Double.parseDouble(parts[6]); } catch (Exception e) {}

                    String borrowedBy = "";
                    if (parts.length >= 8) {
                        borrowedBy = parts[7].trim();
                        if ("0.0".equals(borrowedBy)) borrowedBy = "";
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
                    mediaMap.put(isbn, item);
                }
            }
        } catch (IOException e) { e.printStackTrace(); }

        if (searchResultsTable != null) searchResultsTable.refresh();
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
     * Gets the membership type for a user.
     * @param username The username.
     * @return The membership (Silver/Gold).
     */
    
    private String getUserMembership(String username) {
        File file = new File("users.txt");
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
     * Gets the email for a user.
     * @param username The username.
     * @return The email address.
     */
    
    private String getUserEmail(String username) {
        File file = new File("users.txt");
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
     * Loads users from 'users.txt'.
     */

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
    
    /**
     * Deletes a selected user.
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
     * Saves user list to file.
     */

    private void saveUsersToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("users.txt"))) {
            writer.write("m,123,Admin,Gold");
            writer.newLine();
            for (User u : usersList) {
                writer.write(u.toFileFormat());
                writer.newLine();
            }
        } catch (IOException e) {}
    }
    
    /**
     * Shows an alert dialog.
     * @param title Title of the alert.
     * @param content Message content.
     */

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        if (title.equals("Error") || title.equals("Warning"))
            alert.setAlertType(Alert.AlertType.ERROR);
        alert.showAndWait();
    }

    /**
     * Sends email reminder to a user with overdue books.
     */
    
    @FXML
     void handleSendReminder() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Select a user to send reminder.");
            return;
        }

        String username = selected.getUsername();
        String email = getUserEmail(username);

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
