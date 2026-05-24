package com.vinuni.roombooking.validator;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vinuni.roombooking.enums.AccessLevel;
import com.vinuni.roombooking.enums.BookingStatus;
import com.vinuni.roombooking.model.Admin;
import com.vinuni.roombooking.model.BookingRequest;
import com.vinuni.roombooking.model.Room;
import com.vinuni.roombooking.model.Staff;
import com.vinuni.roombooking.model.Student;
import com.vinuni.roombooking.model.TimeSlot;
import com.vinuni.roombooking.model.User;
import com.vinuni.roombooking.repository.BookingRepository;

@Component
public class BookingValidator {

    /** Injected so {@link #validateOneBookingPerDay} sees the real shared cache,
     *  not a fresh empty instance. */
    private final BookingRepository repository;

    @Autowired
    public BookingValidator(BookingRepository repository) {
        this.repository = repository;
    }

    /** In-memory overlap predicate. Kept for {@code processQueue}; live conflict
     *  checks at submission time go through {@code DatabaseConnector.hasRoomConflict}. */
    public boolean detectConflict(BookingRequest req, List<BookingRequest> existingBookings) {
        for (BookingRequest b : existingBookings) {
            if (req.getTimeSlot().overlapsWith(b.getTimeSlot())) {
                return true;
            }
        }
        return false;
    }

    /** True if the slot is at most 3 hours long. */
    public boolean validateDuration(TimeSlot slot) {
        return slot.isWithinMaxDuration();
    }

    /** True if the slot's start is strictly in the future. New in this pass — the
     *  old upper-bound check ({@link #validateAdvanceWindow}) didn't stop past
     *  bookings. */
    public boolean validateNotPast(TimeSlot slot) {
        return slot.isInFuture();
    }

    /** Minimum-participation rule: the host must invite at least
     *  ceil(capacity * 0.5) people. Prevents one user from grabbing a large
     *  room for a solo session. */
    public boolean validateMinimumParticipation(Room room, int inviteeCount) {
        int required = (int) Math.ceil(room.getCapacity() * 0.5);
        return inviteeCount >= required;
    }

    /** True if the slot's start is within 7 days from now. Upper bound. */
    public boolean validateAdvanceWindow(TimeSlot slot) {
        return slot.isWithinOneWeek();
    }

    /** True if the user has zero bookings whose start is today.
     *  <p>Previously this created a fresh {@code new BookingRepository()} — meaning
     *  the in-memory cache was always empty and the check always passed. Now uses
     *  the injected repository and filters by today's date. */
    public boolean validateOneBookingPerDay(User user) {
        if (user == null) return false;
        LocalDate today = LocalDate.now();
        return repository.findByUser(user.getUserId()).stream()
                // Don't count cancelled or rejected bookings against the daily limit.
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED
                          && b.getStatus() != BookingStatus.REJECTED)
                .map(b -> b.getTimeSlot().getStartTime().toLocalDate())
                .noneMatch(today::equals);
    }

    /** True if the user's role matches the room's access level. */
    public boolean validateAccess(Room room, User user) {
        if (user == null || room == null) return false;
        AccessLevel access = room.getAccess();
        if (access == AccessLevel.ALL_USERS) return true;
        if (user instanceof Admin) return true;  // admin overrides
        if (access == AccessLevel.STUDENT_ONLY) return user instanceof Student;
        if (access == AccessLevel.STAFF_ONLY)   return user instanceof Staff;
        return false;
    }

    /** Runs every hard rule (the ones that make a booking REJECTED, not PENDING)
     *  in order and returns the first failure's reason, or {@code null} if all
     *  pass. Used by {@code BookingService.submitRequest} so the frontend can
     *  show a specific message instead of "Booking rejected by validator". */
    public String firstFailureReason(BookingRequest req, int inviteeCount) {
        if (!validateAccess(req.getRoom(), req.getUser())) {
            return "Your role can't book a " + req.getRoom().getAccess().name() + " room.";
        }
        if (!validateNotPast(req.getTimeSlot())) {
            return "Start time must be in the future.";
        }
        if (!validateAdvanceWindow(req.getTimeSlot())) {
            return "Bookings must start within the next 7 days.";
        }
        if (!validateDuration(req.getTimeSlot())) {
            return "Bookings can be at most 3 hours long.";
        }
        if (!validateMinimumParticipation(req.getRoom(), inviteeCount)) {
            int required = (int) Math.ceil(req.getRoom().getCapacity() * 0.5);
            return "Invite at least " + required + " people for a room of "
                    + req.getRoom().getCapacity() + ".";
        }
        if (!validateOneBookingPerDay(req.getUser())) {
            return "You already have a booking today.";
        }
        return null;
    }
}
