package com.vinuni.roombooking.model;
import com.vinuni.roombooking.repository.BookingRepository;
import com.vinuni.roombooking.service.BookingService;
import com.vinuni.roombooking.service.DatabaseConnector;


public class Admin extends User {

    private String adminId;

    public Admin(String userId, String userName, String password, String email,
                 String adminId) {
        super(userId, userName, password, email);
        this.adminId = adminId;
    }

    public String getAdminId() { return adminId; }
    @Override
    public String getUserType() {
        return "Admin";
    }

    /**
     * STUB — Phase 2: persist a new Room via BookingRepository / DatabaseConnector.
     */
    public void addRoom(Room room) {
        // TODO (Huy Dung): call BookingRepository or DatabaseConnector to persist room
        
    }

    /**
     * STUB — Phase 2: remove a Room by id from repository and DB.
     */
    public void removeRoom(int roomId) {
        // TODO (Huy Dung): call repository.delete / DB DELETE
        
    }

    /**
     * STUB — Phase 2: set booking status to CANCELLED regardless of ownership.
     */
    public void forceCancel(String bookingId) {
        // TODO (Huy Dung): call BookingService or repository directly
        
    }

    /**
     * STUB — Phase 2: approve or reject a pending BookingRequest.
     */
    public void overrideRequest(BookingRequest req) {
        // TODO (Huy Dung): set req.setStatus(APPROVED / REJECTED) and persist
    }

    /**
     * STUB — Phase 2: list, activate, deactivate user accounts.
     */
    public void manageUserAccounts() {
        // TODO (Huy Dung): implement user management logic
    }
}
