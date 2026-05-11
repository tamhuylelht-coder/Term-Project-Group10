package com.vinuni.roombooking.validator;

import com.vinuni.roombooking.model.BookingRequest;
import com.vinuni.roombooking.model.Room;
import com.vinuni.roombooking.model.TimeSlot;
import com.vinuni.roombooking.model.User;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * All six validation rules required by Phase 4 acceptance tests.
 * Each method returns true = valid (allow booking), false = invalid (reject).
 *
 * Phase 2 owner: Huy Dung + Khanh An
 */
@Component
public class BookingValidator {

    /**
     * STUB — Rule 1: reject if req overlaps any slot in existingBookings for the same room.
     */
    public boolean detectConflict(BookingRequest req, List<BookingRequest> existingBookings) {
        // TODO (Huy Dung): iterate existingBookings, check req.getTimeSlot().overlapsWith(b.getTimeSlot())
        return false; // stub: no conflict detected
    }

    /**
     * STUB — Rule 5: reject if rsvpCount < 50% of room capacity at finalization.
     */
    public boolean validateRsvp(BookingRequest req) {
        // TODO (Huy Dung): return req.getRsvpCount() >= req.getRoom().getCapacity() * 0.5;
        return true;
    }

    /**
     * STUB — Rule 2: reject if slot duration > 3 hours.
     */
    public boolean validateDuration(TimeSlot slot) {
        // TODO (Huy Dung): return slot.isWithinMaxDuration();
        return true;
    }

    /**
     * STUB — Rule 4: reject if booking is more than 1 week in advance.
     */
    public boolean validateAdvanceWindow(TimeSlot slot) {
        // TODO (Huy Dung): return slot.isWithinOneWeek();

        
        return slot.isWithinOneWeek();

    }

    /**
     * STUB — Rule 3: reject if user already has an approved/pending booking on the same calendar day.
     */
    public boolean validateOneBookingPerDay(User user) {
        // TODO (Huy Dung): query BookingRepository.findByUser(user.getUserId()),
        //   filter by same date as requested slot, check count == 0


        List<BookingRequest> existingBookings = repository.findByUser(user.getUserId());
        for (BookingRequest existing : existingBookings) {
        // Assuming TimeSlot or BookingRequest has a getDate() method
            if (existing.getSlot().getDate().equals(requestedSlot.getDate())) {
                return false; // User already has a booking on this day [4]

        

        
        return true;
    }

    /**
     * STUB — Rule 6: reject if the user's role is not permitted for this room's access level.
     * Already partially implemented — keep this signature stable.
     */
    public boolean validateAccess(User user, Room room) {
        // TODO (Huy Dung): switch on room.getAccess() and user's concrete type
        
        
        return user.getAccessLevel() >= room.getAccess();
    }
}
