
/**
 * Represents a user in the library system.
 * 
 * @author Zainab
 * @version 1.0
 */

public class User {
    private String username;
    private String password;
    private String role;
    private String membership;
    
    /**
     * Constructor to create a new User.
     * 
     * @param username The username.
     * @param password The password.
     * @param role The role (Admin, Librarian, User).
     * @param membership The membership type (Gold, Silver).
     */

    public User(String username, String password, String role, String membership) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.membership = membership;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public String getMembership() { return membership; }

    /**
     * Formats the user data for file storage.
     * @return Comma-separated string.
     */
    
    public String toFileFormat() {
        return username + "," + password + "," + role + "," + membership;
    }
}