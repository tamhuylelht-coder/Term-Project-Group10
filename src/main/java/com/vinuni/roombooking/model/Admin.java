package com.vinuni.roombooking.model;
import com.vinuni.repository.BookingRepository;
import com.vinuni.service.BookingService;
import com.vinuni.service.DatabaseConnector;


import java.util.List;
import java.util.logging.Logger;

import com.vinuni.roombooking.enums.BookingStatus;
import com.vinuni.roombooking.repository.BookingRepository;
import com.vinuni.roombooking.service.DatabaseConnector;

public class Admin extends User {

    private static final Logger logger = Logger.getLogger(Admin.class.getName());
    private final String adminId;
    
    // These will be injected or set by the service layer
    private BookingRepository bookingRepository;
    private DatabaseConnector databaseConnector;

    public Admin(String userId, String userName, String password, String email,
                 String adminId) {
        super(userId, userName, password, email);
        this.adminId = adminId;
    }

    public String getAdminId() { return adminId; }
    
    public void setDatabaseConnector(DatabaseConnector connector) {
        this.databaseConnector = connector;
    }

    @Override
    public String getUserType() {
        return "Admin";
    }

    /**
     * STUB — Phase 2: persist a new Room via BookingRepository / DatabaseConnector.
     * 
     * @param room the Room object to add to the system
     * @throws IllegalArgumentException if room is null or invalid
     * @throws IllegalStateException if database connector is not available
     */
    public void addRoom(Room room) {
        if (room == null) {
            throw new IllegalArgumentException("Room cannot be null");
        }
        
        if (databaseConnector == null) {
            throw new IllegalStateException("DatabaseConnector not initialized. Admin must be instantiated with dependencies.");
        }
        
        try {
            // Validate room has valid ID and other required fields
            if (room.getRoomId() <= 0) {
                throw new IllegalArgumentException("Room ID must be positive");
            }
            if (room.getRoomName() == null || room.getRoomName().isEmpty()) {
                throw new IllegalArgumentException("Room name cannot be empty");
            }
            if (room.getCapacity() <= 0) {
                throw new IllegalArgumentException("Room capacity must be positive");
            }
            
            // Ensure room is in AVAILABLE status
            room.status = com.vinuni.roombooking.enums.RoomStatus.AVAILABLE;
            
            // TODO (Huy Dung): Phase 2
            // Persist room to database - this would require a RoomRepository.save() or DatabaseConnector.insertRoom()
            logger.info("Admin " + this.adminId + " adding room: " + room.getRoomName() + 
                       " (ID: " + room.getRoomId() + ", Capacity: " + room.getCapacity() + ")");
            
            // Placeholder: actual database insertion would happen here
            System.out.println("Room " + room.getRoomName() + " added successfully by admin " + this.adminId);
            
        } catch (Exception e) {
            logger.severe("Error adding room: " + e.getMessage());
            throw new RuntimeException("Failed to add room: " + e.getMessage(), e);
        }
    }

    /**
     * STUB — Phase 2: remove a Room by id from repository and DB.
     * 
     * @param roomId the ID of the room to remove
     * @throws IllegalArgumentException if roomId is invalid
     * @throws IllegalStateException if room has active bookings or dependencies not available
     */
    public void removeRoom(int roomId) {
        if (roomId <= 0) {
            throw new IllegalArgumentException("Room ID must be positive");
        }
        
        if (bookingRepository == null || databaseConnector == null) {
            throw new IllegalStateException("BookingRepository and DatabaseConnector not initialized.");
        }
        
        try {
            // Check if room has active bookings
            List<BookingRequest> activeBookings = bookingRepository.findByRoom(roomId);
            
            if (activeBookings != null && !activeBookings.isEmpty()) {
                // Filter for non-cancelled bookings
                long activeCount = activeBookings.stream()
                    .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                    .count();
                
                if (activeCount > 0) {
                    throw new IllegalStateException(
                        "Cannot remove room " + roomId + ": " + activeCount + " active booking(s) exist");
                }
            }
            
            logger.info("Admin " + this.adminId + " removing room ID: " + roomId);
            
            // TODO (Huy Dung): Phase 2
            // Call database connector to delete room and cascade delete cancelled bookings
            // databaseConnector.deleteRoom(roomId);
            
            System.out.println("Room " + roomId + " removed successfully by admin " + this.adminId);
            
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            logger.severe("Error removing room: " + e.getMessage());
            throw new RuntimeException("Failed to remove room: " + e.getMessage(), e);
        }
    }

    /**
     * STUB — Phase 2: set booking status to CANCELLED regardless of ownership.
     * Admin override capability for urgent cancellations.
     * 
     * @param bookingId the ID of the booking to cancel
     * @throws IllegalArgumentException if bookingId is invalid
     * @throws IllegalStateException if booking not found or dependencies not available
     */
    public void forceCancel(String bookingId) {
        if (bookingId == null || bookingId.isEmpty()) {
            throw new IllegalArgumentException("Booking ID cannot be null or empty");
        }
        
        if (bookingRepository == null || databaseConnector == null) {
            throw new IllegalStateException("BookingRepository and DatabaseConnector not initialized.");
        }
        
        try {
            // Fetch the booking from repository
            BookingRequest booking = bookingRepository.findById(bookingId);
            
            if (booking == null) {
                throw new IllegalStateException("Booking not found: " + bookingId);
            }
            
            // Check if already cancelled
            if (booking.getStatus() == BookingStatus.CANCELLED) {
                logger.warning("Booking " + bookingId + " is already cancelled");
                return;
            }
            
            logger.info("Admin " + this.adminId + " force-cancelling booking: " + bookingId + 
                       " for user: " + booking.getUser().getUserName());
            
            // Set booking status to CANCELLED
            booking.setStatus(BookingStatus.CANCELLED);
            
            // Persist changes to repository and database
            bookingRepository.save(booking);
            databaseConnector.updateBookingStatus(booking);
            
            // TODO (Huy Dung): Phase 2
            // Send email notification to user: booking.getUser().getEmail()
            // Notify with subject: "Your booking has been cancelled by admin"
            // Include reason: "Cancelled by admin " + this.adminId
            
            logger.info("Booking " + bookingId + " cancelled successfully. User " + 
                       booking.getUser().getUserName() + " should be notified.");
            
            System.out.println("Booking " + bookingId + " force-cancelled by admin " + this.adminId);
            
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            logger.severe("Error force-cancelling booking: " + e.getMessage());
            throw new RuntimeException("Failed to force-cancel booking: " + e.getMessage(), e);
        }
    }

    /**
     * STUB — Phase 2: approve or reject a pending BookingRequest.
     * Override admin capability to manage booking requests.
     * 
     * @param req the BookingRequest to override
     * @param approved true to approve, false to reject
     * @throws IllegalArgumentException if request is null or invalid
     * @throws IllegalStateException if request is not pending or dependencies not available
     */
    public void overrideRequest(BookingRequest req, boolean approved) {
        if (req == null) {
            throw new IllegalArgumentException("BookingRequest cannot be null");
        }
        
        if (bookingRepository == null || databaseConnector == null) {
            throw new IllegalStateException("BookingRepository and DatabaseConnector not initialized.");
        }
        
        try {
            // Validate request is in PENDING state
            if (req.getStatus() != BookingStatus.PENDING) {
                throw new IllegalStateException(
                    "Request " + req.getBookingId() + " is not pending. Current status: " + req.getStatus());
            }
            
            // Determine action and set status
            BookingStatus newStatus = approved ? BookingStatus.APPROVED : BookingStatus.REJECTED;
            String action = approved ? "APPROVED" : "REJECTED";
            
            logger.info("Admin " + this.adminId + " " + action + " booking request: " + req.getBookingId() + 
                       " for user: " + req.getUser().getUserName() + 
                       " for room: " + req.getRoom().getRoomName());
            
            // Set request status
            req.setStatus(newStatus);
            
            // Persist changes to repository and database
            bookingRepository.save(req);
            databaseConnector.updateBookingStatus(req);
            
            // TODO (Huy Dung): Phase 2
            // Send email notification to requester
            // If approved: "Your booking request has been approved by admin"
            // If rejected: "Your booking request has been rejected by admin. Reason: [reason]"
            
            logger.info("Booking request " + req.getBookingId() + " " + action + 
                       " successfully. User " + req.getUser().getUserName() + " should be notified.");
            
            System.out.println("Booking request " + req.getBookingId() + " " + action + 
                             " by admin " + this.adminId);
            
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            logger.severe("Error overriding booking request: " + e.getMessage());
            throw new RuntimeException("Failed to override booking request: " + e.getMessage(), e);
        }
    }
    
    /**
     * Overload for backward compatibility - defaults to rejecting the request.
     * 
     * @param req the BookingRequest to override (will be rejected)
     * @deprecated Use {@link #overrideRequest(BookingRequest, boolean)} instead
     */
    @Deprecated
    public void overrideRequest(BookingRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("BookingRequest cannot be null");
        }
        overrideRequest(req, false); // Default to rejection for backward compatibility
    }

    /**
     * STUB — Phase 2: list, activate, deactivate user accounts.
     * Admin utility for user account management.
     * Returns a list of all users in the system.
     * 
     * @return List of all User objects
     * @throws IllegalStateException if user repository is not available
     */
    public List<User> manageUserAccounts() {
        // TODO (Huy Dung): Phase 2
        // 1. Implement UserRepository for database persistence
        // 2. Retrieve all users from repository
        // 3. In a future service/controller, provide UI/CLI options to:
        //    a. List all user accounts with status (active/inactive)
        //    b. Activate inactive accounts
        //    c. Deactivate active accounts
        //    d. Reset user password
        // 4. Validate admin permissions for each action
        // 5. Persist changes to database
        // 6. Log all account management operations for audit
        
        logger.info("Admin " + this.adminId + " accessing user accounts management");
        
        try {
            // TODO: Get users from UserRepository when available
            // List<User> allUsers = userRepository.findAll();
            // return allUsers;
            
            System.out.println("User account management interface (requires UserRepository implementation)");
            return java.util.Collections.emptyList();
            
        } catch (Exception e) {
            logger.severe("Error accessing user accounts: " + e.getMessage());
            throw new RuntimeException("Failed to access user accounts: " + e.getMessage(), e);
        }
    }
    
    /**
     * Helper method to activate a user account (for future use).
     * 
     * @param userId the ID of the user to activate
     * @throws IllegalArgumentException if userId is invalid
     * @throws IllegalStateException if user not found
     */
    public void activateUser(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        logger.info("Admin " + this.adminId + " activating user: " + userId);
        
        // TODO (Huy Dung): Phase 2
        // User user = userRepository.findById(userId);
        // if (user == null) throw new IllegalStateException("User not found: " + userId);
        // user.setActive(true);
        // userRepository.save(user);
        // logger.info("User " + userId + " activated successfully");
    }
    
    /**
     * Helper method to deactivate a user account (for future use).
     * 
     * @param userId the ID of the user to deactivate
     * @throws IllegalArgumentException if userId is invalid
     * @throws IllegalStateException if user not found
     */
    public void deactivateUser(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        logger.info("Admin " + this.adminId + " deactivating user: " + userId);
        
        // TODO (Huy Dung): Phase 2
        // User user = userRepository.findById(userId);
        // if (user == null) throw new IllegalStateException("User not found: " + userId);
        // user.setActive(false);
        // userRepository.save(user);
        // logger.info("User " + userId + " deactivated successfully");
    }
    
    /**
     * Helper method to reset a user's password (for future use).
     * 
     * @param userId the ID of the user
     * @param newPassword the new password
     * @throws IllegalArgumentException if inputs are invalid
     * @throws IllegalStateException if user not found
     */
    public void resetUserPassword(String userId, String newPassword) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        
        logger.info("Admin " + this.adminId + " resetting password for user: " + userId);
        
        // TODO (Huy Dung): Phase 2
        // User user = userRepository.findById(userId);
        // if (user == null) throw new IllegalStateException("User not found: " + userId);
        // user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        // userRepository.save(user);
        // logger.info("Password reset for user " + userId + " successfully");
        // Send notification email to user with temporary password
    }
}
