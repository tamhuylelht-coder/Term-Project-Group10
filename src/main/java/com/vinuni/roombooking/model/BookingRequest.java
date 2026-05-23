package com.vinuni.roombooking.model;

import java.time.LocalDateTime;

import com.vinuni.roombooking.enums.BookingStatus;

/**
 * Identifies a booking. RSVPs and invitations are tracked in the rsvp and
 * invitations tables respectively and are never cached on this object — every
 * read goes through DatabaseConnector so the data stays correct across restarts
 * and across sessions.
 */
public class BookingRequest {

    private final String        bookingId;
    private final  User          user;
    private final Room          room;
    private final TimeSlot      timeSlot;
    private BookingStatus status;
    private final LocalDateTime createdAt;

    public BookingRequest(String bookingId, User user, Room room, TimeSlot timeSlot) {
        this.bookingId = bookingId;
        this.user      = user;
        this.room      = room;
        this.timeSlot  = timeSlot;
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
}
