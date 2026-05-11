package com.vinuni.roombooking.model;

public class Staff extends User implements Bookable {

    private String staffId;
    private String department;

    public Staff(String userId, String userName, String password, String email,
                 String staffId, String department) {
        super(userId, userName, password, email);
        this.staffId     = staffId;
        this.department  = department;
    }

    public String getStaffId()    { return staffId; }
    public String getDepartment() { return department; }

    /**
     * STUB — Phase 2: build BookingRequest and delegate to BookingService.submitRequest().
     */
    @Override
    public BookingRequest bookRoom(Room room, TimeSlot slot) {
        // TODO (Khanh An): inject BookingService and call submitRequest(req)
        return new BookingRequest("STUB-ID", this, room, slot);
    }

    /**
     * STUB — Phase 2: delegate to BookingService.cancelBooking(bookingId, this).
     */
    @Override
    public boolean cancelBooking(String bookingId) {
        // TODO (Khanh An): inject BookingService and call cancelBooking(bookingId, this)
        return true;
    }
}
