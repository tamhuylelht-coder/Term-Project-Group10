package com.vinuni.roombooking.model;

public interface Bookable {

    /**
     * Submit a booking request for the given room and time slot.
     * Returns the created BookingRequest (status will be PENDING).
     */
    BookingRequest bookRoom(Room room, TimeSlot slot);

    /**
     * Cancel the booking identified by bookingId.
     * Returns true if cancellation succeeded.
     */
    boolean cancelBooking(String bookingId);
}
