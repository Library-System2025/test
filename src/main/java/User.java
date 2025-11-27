public class User {
    private String username;
    private String password;
    private String role;
    private String membership;

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

    // هاي عشان لما نحفظ الملف، يرجع يكتبهم بنفس التنسيق
    public String toFileFormat() {
        return username + "," + password + "," + role + "," + membership;
    }
}