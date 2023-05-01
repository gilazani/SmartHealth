package superapp.Boundary.User;

public class NewUserBoundary {

    private String email;
    private String role;
    private String username;
    private String avatar;


    public NewUserBoundary() {
        super();
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public UserBoundary newUserBoundaryToUserBoundary(String springAppName) {
        UserBoundary user = new UserBoundary();

        user.setUserId(new UserId(springAppName, this.email));
        user.setRole(this.role);
        user.setUsername(this.username);
        user.setAvatar(this.avatar);

        return user;
    }

}
