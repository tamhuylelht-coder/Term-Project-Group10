package com.vinuni.roombooking.model;

import com.vinuni.roombooking.enums.BookingStatus;
import java.time.LocalDateTime;

public class Student extends User implements Bookable {

    private String studentId;
    private String major;
    private int    yearOfStudy;

    public Student(String userId, String userName, String password, String email,
                   String studentId, String major, int yearOfStudy) {
        super(userId, userName, password, email);
        this.studentId   = studentId;
        this.major       = major;
        this.yearOfStudy = yearOfStudy;
    }

    public String getStudentId() { return studentId; }
    public String getMajor()     { return major; }
    public int    getYearOfStudy() { return yearOfStudy; }

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
