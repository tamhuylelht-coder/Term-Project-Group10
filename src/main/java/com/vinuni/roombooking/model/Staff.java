package com.vinuni.roombooking.model;

import java.util.UUID;
import java.util.logging.Logger;

public class Staff extends User implements Bookable {

    private static final Logger logger = Logger.getLogger(Staff.class.getName());
    
    private final String staffId;
    private final String department;
    
    // BookingService will be injected or set by service layer
    private static com.vinuni.roombooking.service.BookingService bookingService;

    public Staff(String userId, String userName, String password, String email,
                 String staffId, String department) {
        super(userId, userName, password, email);
        this.staffId     = staffId;
        this.department  = department;
    }

    @Override
    public String getUserType() {
        return "Staff";
    }

    public String getStaffId()    { return staffId; }
    public String getDepartment() { return department; }
    
    public static void setBookingService(com.vinuni.roombooking.service.BookingService service) {
        Staff.bookingService = service;
    }

    /**
     * STUB — Phase 2: build BookingRequest and delegate to BookingService.submitRequest().
     * Creates a new booking request for a room during a specific time slot.
     * 
     * @param room the Room to book
     * @param slot the TimeSlot for the booking
     * @return BookingRequest with status PENDING (if accepted by validator)
     * @throws IllegalArgumentException if room or slot is null
     * @throws IllegalStateException if BookingService is not initialized
     */
    @Override
    public BookingRequest bookRoom(Room room, TimeSlot slot) {
        // Input validation
        if (room == null) {
            throw new IllegalArgumentException("Room cannot be null");
        }
        
        if (slot == null) {
            throw new IllegalArgumentException("TimeSlot cannot be null");
        }
        
        // Check if BookingService is initialized
        if (bookingService == null) {
            logger.warning("BookingService not initialized for user: " + this.userName);
            throw new IllegalStateException("BookingService not initialized. Cannot submit booking request.");
        }
        
        try {
            // Generate unique booking ID
            String bookingId = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            
            logger.info("Staff " + this.userName + " (ID: " + this.staffId + 
                       ") booking room: " + room.getRoomName() + 
                       " (ID: " + room.getRoomId() + ")");
            
            // Create BookingRequest
            BookingRequest bookingRequest = new BookingRequest(bookingId, this, room, slot);
            
            // Delegate to BookingService to validate and submit
            com.vinuni.roombooking.enums.BookingStatus status = bookingService.submitRequest(bookingRequest);
            
            logger.info("Booking request " + bookingId + " submitted with status: " + status);
            
            return bookingRequest;
            
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.severe("Error booking room for staff " + this.userName + ": " + e.getMessage());
            throw new RuntimeException("Failed to book room: " + e.getMessage(), e);
        }
    }

    /**
     * STUB — Phase 2: delegate to BookingService.cancelBooking(bookingId, this).
     * Cancels an existing booking if the requesting staff owns it.
     * 
     * @param bookingId the ID of the booking to cancel
     * @return true if cancellation succeeded, false otherwise
     * @throws IllegalArgumentException if bookingId is null or empty
     * @throws IllegalStateException if BookingService is not initialized
     */
    @Override
    public boolean cancelBooking(String bookingId) {
        // Input validation
        if (bookingId == null || bookingId.isEmpty()) {
            throw new IllegalArgumentException("Booking ID cannot be null or empty");
        }
        
        // Check if BookingService is initialized
        if (bookingService == null) {
            logger.warning("BookingService not initialized for user: " + this.userName);
            throw new IllegalStateException("BookingService not initialized. Cannot cancel booking.");
        }
        
        try {
            logger.info("Staff " + this.userName + " (ID: " + this.staffId + 
                       ") attempting to cancel booking: " + bookingId);
            
            // Delegate to BookingService to cancel the booking
            // BookingService will verify ownership and admin status
            boolean success = bookingService.cancelBooking(bookingId, this);
            
            if (success) {
                logger.info("Booking " + bookingId + " cancelled successfully by staff: " + this.userName);
            } else {
                logger.warning("Failed to cancel booking " + bookingId + 
                             " for staff: " + this.userName + 
                             " (may not be owner or booking not found)");
            }
            
            return success;
            
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            logger.severe("Error cancelling booking " + bookingId + " for staff " + 
                         this.userName + ": " + e.getMessage());
            throw new RuntimeException("Failed to cancel booking: " + e.getMessage(), e);
        }
    }
}
