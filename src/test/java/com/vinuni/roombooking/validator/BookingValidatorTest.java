package com.vinuni.roombooking.validator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vinuni.roombooking.enums.AccessLevel;
import com.vinuni.roombooking.model.BookingRequest;
import com.vinuni.roombooking.model.Room;
import com.vinuni.roombooking.model.Staff;
import com.vinuni.roombooking.model.Student;
import com.vinuni.roombooking.model.TimeSlot;
import com.vinuni.roombooking.model.User;
import com.vinuni.roombooking.repository.BookingRepository;

public class BookingValidatorTest {

    private BookingValidator validator;

    @BeforeEach
    void setUp() {
        // BookingValidator now takes a repo by constructor injection. A fresh
        // empty in-memory repo is fine for these tests — validateOneBookingPerDay
        // is the only path that touches it, and the tests don't pre-seed it.
        validator = new BookingValidator(new BookingRepository());
    }

    @Test
    void testValidateDuration_Valid() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(2); // 2 hours, within 3
        TimeSlot slot = new TimeSlot(start, end);
        assertTrue(validator.validateDuration(slot));
    }

    @Test
    void testValidateDuration_Invalid() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(4); // 4 hours, exceeds 3
        TimeSlot slot = new TimeSlot(start, end);
        assertFalse(validator.validateDuration(slot));
    }

    @Test
    void testDetectConflict_NoConflict() {
        Room room = new Room(1, "Room A", 10, AccessLevel.ALL_USERS);
        User user = new Student("user1", "Student", "pass", "email", "stu1", "CS", 3);
        TimeSlot reqSlot = new TimeSlot(LocalDateTime.of(2023, 5, 11, 10, 0), LocalDateTime.of(2023, 5, 11, 12, 0));
        BookingRequest req = new BookingRequest("b1", user, room, reqSlot);

        List<BookingRequest> existing = new ArrayList<>();
        BookingRequest existing1 = new BookingRequest("b2", user, room, new TimeSlot(LocalDateTime.of(2023, 5, 11, 13, 0), LocalDateTime.of(2023, 5, 11, 15, 0)));
        existing.add(existing1);

        assertFalse(validator.detectConflict(req, existing));
    }

    @Test
    void testDetectConflict_Conflict() {
        Room room = new Room(1, "Room A", 10, AccessLevel.ALL_USERS);
        User user = new Student("user1", "Student", "pass", "email", "stu1", "CS", 3);
        TimeSlot reqSlot = new TimeSlot(LocalDateTime.of(2023, 5, 11, 10, 0), LocalDateTime.of(2023, 5, 11, 12, 0));
        BookingRequest req = new BookingRequest("b1", user, room, reqSlot);

        List<BookingRequest> existing = new ArrayList<>();
        BookingRequest existing1 = new BookingRequest("b2", user, room, new TimeSlot(LocalDateTime.of(2023, 5, 11, 11, 0), LocalDateTime.of(2023, 5, 11, 13, 0))); // overlaps
        existing.add(existing1);

        assertTrue(validator.detectConflict(req, existing));
    }

    @Test
    void testValidateAccess_AllUsers() {
        Room room = new Room(1, "Room A", 10, AccessLevel.ALL_USERS);
        User student = new Student("user1", "Student", "pass", "email", "stu1", "CS", 3);
        assertTrue(validator.validateAccess(room, student));
    }

    @Test
    void testValidateAccess_StudentOnly_WithStudent() {
        Room room = new Room(1, "Room A", 10, AccessLevel.STUDENT_ONLY);
        User student = new Student("user1", "Student", "pass", "email", "stu1", "CS", 3);
        assertTrue(validator.validateAccess(room, student));
    }

    @Test
    void testValidateAccess_StudentOnly_WithStaff() {
        Room room = new Room(1, "Room A", 10, AccessLevel.STUDENT_ONLY);
        User staff = new Staff("user2", "Staff Member", "pass", "email", "staff1", "IT");
        assertFalse(validator.validateAccess(room, staff));
    }

    @Test
    void testValidateAccess_StaffOnly_WithStaff() {
        Room room = new Room(1, "Room A", 10, AccessLevel.STAFF_ONLY);
        User staff = new Staff("user2", "Staff Member", "pass", "email", "staff1", "IT");
        assertTrue(validator.validateAccess(room, staff));
    }

    @Test
    void testValidateAccess_StaffOnly_WithStudent() {
        Room room = new Room(1, "Room A", 10, AccessLevel.STAFF_ONLY);
        User student = new Student("user1", "Student", "pass", "email", "stu1", "CS", 3);
        assertFalse(validator.validateAccess(room, student));
    }

    @Test
    void testValidateAdvanceWindow_WithinOneWeek() {
        LocalDateTime start = LocalDateTime.now().plusDays(3); // 3 days from now
        LocalDateTime end = start.plusHours(2);
        TimeSlot slot = new TimeSlot(start, end);
        assertTrue(validator.validateAdvanceWindow(slot));
    }

    @Test
    void testValidateAdvanceWindow_ExactlyOneWeek() {
        LocalDateTime start = LocalDateTime.now().plusWeeks(1); // exactly 7 days from now
        LocalDateTime end = start.plusHours(2);
        TimeSlot slot = new TimeSlot(start, end);
        assertTrue(validator.validateAdvanceWindow(slot));
    }

    @Test
    void testValidateAdvanceWindow_BeyondOneWeek() {
        LocalDateTime start = LocalDateTime.now().plusWeeks(1).plusDays(1); // 8 days from now
        LocalDateTime end = start.plusHours(2);
        TimeSlot slot = new TimeSlot(start, end);
        assertFalse(validator.validateAdvanceWindow(slot));
    }

    @Test
    void testValidateOneBookingPerDay_NoExistingBookings() {
        User user = new Student("user1", "Student", "pass", "email", "stu1", "CS", 3);
        // Empty repo means there's no booking today, so the daily-limit check passes.
        assertTrue(validator.validateOneBookingPerDay(user));
    }

    @Test
    void testValidateOneBookingPerDay_SlotAware_BlocksSameDateOnly() {
        BookingRepository repo = new BookingRepository();
        BookingValidator v = new BookingValidator(repo);
        User user = new Student("user1", "Student", "pass", "email", "stu1", "CS", 3);
        Room room = new Room(1, "Room A", 10, AccessLevel.ALL_USERS);

        // Seed an existing booking on May 26 at 10:00.
        TimeSlot existing = new TimeSlot(
                LocalDateTime.of(2026, 5, 26, 10, 0),
                LocalDateTime.of(2026, 5, 26, 11, 0));
        repo.save(new BookingRequest("b-existing", user, room, existing));

        // Another slot on May 26 → blocked.
        TimeSlot sameDay = new TimeSlot(
                LocalDateTime.of(2026, 5, 26, 14, 0),
                LocalDateTime.of(2026, 5, 26, 15, 0));
        assertFalse(v.validateOneBookingPerDay(user, sameDay));

        // A slot on May 27 → allowed.
        TimeSlot nextDay = new TimeSlot(
                LocalDateTime.of(2026, 5, 27, 10, 0),
                LocalDateTime.of(2026, 5, 27, 11, 0));
        assertTrue(v.validateOneBookingPerDay(user, nextDay));
    }
}