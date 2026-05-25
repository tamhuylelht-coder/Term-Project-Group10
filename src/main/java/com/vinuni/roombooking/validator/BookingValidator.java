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
import com.vinuni.roombooking.service.DatabaseConnector;

@Component
public class BookingValidator {

    /** Injected so {@link #validateOneBookingPerDay} sees the real shared cache,
     *  not a fresh empty instance. */
    private final BookingRepository repository;

    /** Optional — when set, the same-day check prefers the DB so the rule
     *  survives JVM restarts and concurrent sessions. Unit tests can wire a
     *  validator without it and the in-memory repo is used as a fallback. */
    private final DatabaseConnector dbConnector;

    @Autowired
    public BookingValidator(BookingRepository repository, DatabaseConnector dbConnector) {
        this.repository = repository;
        this.dbConnector = dbConnector;
    }

    /** Constructor kept for tests that don't need the DB. */
    public BookingValidator(BookingRepository repository) {
        this(repository, null);
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

    /**
     * Slot-aware "one active booking per host per date" check. Compares
     * against the slot's start date — not today — so a user can still book
     * future days even if they already have a meeting today.
     *
     * <p>Prefers the DB when available so the rule survives JVM restarts
     * and other users' sessions; otherwise falls back to the in-memory
     * repository for unit tests.
     */
    public boolean validateOneBookingPerDay(User user, TimeSlot slot) {
        return validateOneBookingPerDay(user, slot, null);
    }

    /**
     * Variant that skips the row whose {@code booking_id == excludeBookingId}.
     * Pass the booking's own id when re-validating a row that's already
     * persisted (queue processing / admin approval), so it doesn't trip the
     * daily-limit check against itself.
     */
    public boolean validateOneBookingPerDay(User user, TimeSlot slot, String excludeBookingId) {
        if (user == null || slot == null) return false;
        LocalDate target = slot.getStartTime().toLocalDate();
        if (dbConnector != null) {
            try {
                return !dbConnector.hasActiveBookingOnDate(user.getUserId(), target, excludeBookingId);
            } catch (RuntimeException ignored) {
                // Fall through to in-memory check if the DB lookup blows up
                // (e.g. unit-test scaffolding without a real connection).
            }
        }
        return repository.findByUser(user.getUserId()).stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED
                          && b.getStatus() != BookingStatus.REJECTED)
                .filter(b -> excludeBookingId == null
                          || !excludeBookingId.equals(b.getBookingId()))
                .map(b -> b.getTimeSlot().getStartTime().toLocalDate())
                .noneMatch(target::equals);
    }

    /**
     * Back-compat overload: checks against "today" when the caller doesn't
     * pass a slot. Kept so existing callers and tests keep compiling.
     *
     * @deprecated Use {@link #validateOneBookingPerDay(User, TimeSlot)} so
     *             the check applies to the booking's actual date.
     */
    @Deprecated
    public boolean validateOneBookingPerDay(User user) {
        if (user == null) return false;
        LocalDate today = LocalDate.now();
        return repository.findByUser(user.getUserId()).stream()
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
        if (!validateOneBookingPerDay(req.getUser(), req.getTimeSlot())) {
            return "You already have an active booking on "
                    + req.getTimeSlot().getStartTime().toLocalDate() + ".";
        }
        return null;
    }
}
