package com.vinuni.roombooking.model;

/**
 * Abstract domain user. Subclasses ({@link Student}, {@link Staff},
 * {@link Admin}) carry role-specific fields. Authentication itself goes
 * through Spring Security's {@code AuthenticationManager} in
 * {@code config.SecurityConfig}, not through this class — the
 * {@link #authenticate(String)} method is kept for UML compliance but is
 * not called by the live auth flow.
 */
public abstract class User {

    protected String userId;
    protected String userName;
    private   String password;
    protected String email;

    public User(String userId, String userName, String password, String email) {
        this.userId   = userId;
        this.userName = userName;
        this.password = password;
        this.email    = email;
    }

    public String getUserId()   { return userId; }
    public String getUserName() { return userName; }
    public String getEmail()    { return email; }
    public String getPassword() { return password; }

    public abstract String getUserType();

    public boolean authenticate(String pwd) {
        if (pwd == null || pwd.isEmpty()) return false;
        if (password == null || password.isEmpty()) return false;
        return password.equals(pwd);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return userId != null && userId.equals(other.userId);
    }

    @Override
    public int hashCode() {
        return userId == null ? 0 : userId.hashCode();
    }
}
