package com.vinuni.roombooking.validator;

import java.util.List;

import org.springframework.stereotype.Component;

import com.vinuni.roombooking.model.BookingRequest;
import com.vinuni.roombooking.model.Room;
import com.vinuni.roombooking.model.TimeSlot;
import com.vinuni.roombooking.model.User;

@Component
public class BookingValidator {

    public boolean detectConflict(BookingRequest req, List<BookingRequest> existingBookings) {
        for (BookingRequest b : existingBookings) {
            if (req.getTimeSlot().overlapsWith(b.getTimeSlot())) {
                return true; // conflict detected
            }
        }
        return false; //no conflict detected
    }

    public boolean validateRsvp(BookingRequest req) {
        return req.getRsvpCount() >= req.getRoom().getCapacity() * 0.5;
    }

    public boolean validateDuration(TimeSlot slot) {
        return slot.isWithinMaxDuration();
    }

    /**
     * STUB — Rule 4: reject if booking is more than 1 week in advance.
     */
    public boolean validateAdvanceWindow(TimeSlot slot) {
        // TODO (Huy Dung): return slot.isWithinOneWeek();
        return true;
    }

    /**
     * STUB — Rule 3: reject if user already has an approved/pending booking on the same calendar day.
     */
    public boolean validateOneBookingPerDay(User user) {
        // TODO (Huy Dung): query BookingRepository.findByUser(user.getUserId()),
        //   filter by same date as requested slot, check count == 0
        return true;
    }

    /**
     * STUB — Rule 6: reject if the user's role is not permitted for this room's access level.
     * Already partially implemented — keep this signature stable.
     */
    public boolean validateAccess(User user, Room room) {
        // TODO (Huy Dung): switch on room.getAccess() and user's concrete type
        return true;
    }
}
