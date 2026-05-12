package com.vinuni.roombooking.validator;

import java.util.List;

import org.springframework.stereotype.Component;

import com.vinuni.roombooking.enums.AccessLevel;
import com.vinuni.roombooking.model.BookingRequest;
import com.vinuni.roombooking.model.Room;
import com.vinuni.roombooking.model.TimeSlot;
import com.vinuni.roombooking.model.User;
import com.vinuni.roombooking.repository.BookingRepository;

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

    public boolean validateAdvanceWindow(TimeSlot slot) {
        return slot.isWithinOneWeek();
    }

    public boolean validateOneBookingPerDay(User user) {
        BookingRepository repo = new BookingRepository();
        return repo.findByUser(user.getUserId()).isEmpty();
    }

    public boolean validateAccess(Room room, User user) {
        String userRole = user.getUserType();
        AccessLevel roomAccess = room.getAccess();

        if (roomAccess.equals(AccessLevel.ALL_USERS)) {
            return true;
        } else if (roomAccess.equals(AccessLevel.STUDENT_ONLY) && userRole.equals("Student")) {
            return true;
        } else if (roomAccess.equals(AccessLevel.STAFF_ONLY) && userRole.equals("Staff")) {
            return true;
        }

        return false; // Placeholder return value
    }
}
