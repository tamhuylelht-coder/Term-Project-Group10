package com.vinuni.roombooking.model;

import java.time.LocalDateTime;
import java.util.HashSet;

import com.vinuni.roombooking.enums.BookingStatus;

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

    public BookingRequest(String bookingId, User user, Room room, TimeSlot timeSlot, HashSet<String> rsvpList, BookingStatus status, LocalDateTime createdAt){
        this.bookingId = bookingId;
        this.user = user;
        this.room = room;
        this.timeSlot = timeSlot;
        this.rsvpList = rsvpList;
        this.status = status;
        this.createdAt = createdAt;
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
        return rsvpList.add(userId);
    }
}
