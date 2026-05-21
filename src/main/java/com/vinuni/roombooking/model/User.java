package com.vinuni.roombooking.model;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public abstract class User {

    private static final Logger logger = Logger.getLogger(User.class.getName());
    
    protected String userId;
    protected String userName;
    private   String password;
    protected String email;
    
    // Repository will be set by service layer if needed
    private static com.vinuni.roombooking.repository.BookingRepository bookingRepository;

    public User(String userId, String userName, String password, String email) {
        this.userId   = userId;
        this.userName = userName;
        this.password = password;
        this.email    = email;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword(){
        return password;
    }

    public abstract String getUserType();

    /**
     * STUB — Phase 2: hash pwd and compare against stored hash using BCrypt.
     * Validates user credentials for authentication.
     * 
     * @param pwd the password to authenticate
     * @return true if password matches stored hash, false otherwise
     * @throws IllegalArgumentException if password is null or empty
     */
    public boolean authenticate(String pwd) {
        // Input validation
        if (pwd == null || pwd.isEmpty()) {
            logger.warning("Authentication attempt with null/empty password for user: " + this.userName);
            return false;
        }
        
        // Check if password field is set
        if (this.password == null || this.password.isEmpty()) {
            logger.warning("No password stored for user: " + this.userName);
            return false;
        }
        
        try {
            // TODO (Huy Dung): Phase 2 - Implement BCrypt comparison
            // Use: org.springframework.security.crypto.bcrypt.BCrypt.checkpw(pwd, this.password)
            // For now, doing simple equality check (NOT SECURE - REMOVE IN PRODUCTION)
            boolean isAuthenticated = this.password.equals(pwd);
            
            if (isAuthenticated) {
                logger.info("User " + this.userName + " authenticated successfully");
            } else {
                logger.warning("Authentication failed for user: " + this.userName);
            }
            
            return isAuthenticated;
            
        } catch (Exception e) {
            logger.severe("Error during authentication for user " + this.userName + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * STUB — Phase 2: delegate to BookingRepository.findByUser(userId).
     * Retrieves all booking requests associated with this user.
     * 
     * @return List of BookingRequest objects for this user, empty list if none found
     * @throws IllegalStateException if BookingRepository is not initialized
     */
    public List<BookingRequest> viewMyBookings() {
        // Check if repository is initialized
        if (bookingRepository == null) {
            logger.warning("BookingRepository not initialized for user: " + this.userName);
            // Return empty list as fallback (Phase 2: should throw exception or wire to DB)
            return new ArrayList<>();
        }
        
        try {
            logger.info("User " + this.userName + " retrieving their bookings");
            
            // Delegate to BookingRepository to find all bookings for this user
            List<BookingRequest> myBookings = bookingRepository.findByUser(this.userId);
            
            if (myBookings == null) {
                logger.info("No bookings found for user: " + this.userName);
                return new ArrayList<>();
            }
            
            logger.info("Found " + myBookings.size() + " booking(s) for user: " + this.userName);
            return myBookings;
            
        } catch (Exception e) {
            logger.severe("Error retrieving bookings for user " + this.userName + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
