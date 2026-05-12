package com.vinuni.roombooking.model;

import com.vinuni.roombooking.enums.BookingStatus;
import java.time.LocalDateTime;
import java.util.HashSet;

public class BookingRequest {

    private final String        bookingId;
    private final  User          user;
    private final Room          room;
    private final TimeSlot      timeSlot;
    private final HashSet<String> rsvpList;
    private BookingStatus status;
    private final LocalDateTime createdAt;

    public BookingRequest(String bookingId, User user, Room room, TimeSlot timeSlot) {
        this.bookingId = bookingId;
        this.user      = user;
        this.room      = room;
        this.timeSlot  = timeSlot;
        this.rsvpList  = new HashSet<>();
        this.status    = BookingStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public String        getBookingId() { return bookingId; }
    public User          getUser()      { return user; }
    public Room          getRoom()      { return room; }
    public TimeSlot      getTimeSlot()  { return timeSlot; }
    public BookingStatus getStatus()    { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public int getRsvpCount() {
        return rsvpList.size();
    }

    /**
     * STUB — Phase 2: add userId to rsvpList, return false if already present.
     */
    public boolean addRsvp(String userId) {
        // TODO (Huy Dung): return rsvpList.add(userId);
        return true;
    }
}
