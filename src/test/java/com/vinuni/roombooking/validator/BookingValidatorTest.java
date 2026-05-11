package com.vinuni.roombooking.validator;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
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

public class BookingValidatorTest {

    private BookingValidator validator;

    @BeforeEach
    void setUp() {
        validator = new BookingValidator();
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
    void testValidateRsvp_Valid() throws Exception {
        Room room = new Room(1, "Room A", 10, AccessLevel.ALL_USERS);
        User user = new Student("user1", "Student", "pass", "email", "stu1", "CS", 3);
        TimeSlot slot = new TimeSlot(LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        BookingRequest req = new BookingRequest("b1", user, room, slot);

        // Set rsvpList to have 5 RSVPs (50% of 10)
        Field rsvpField = BookingRequest.class.getDeclaredField("rsvpList");
        rsvpField.setAccessible(true);
        HashSet<String> rsvpList = new HashSet<>();
        rsvpList.add("u1");
        rsvpList.add("u2");
        rsvpList.add("u3");
        rsvpList.add("u4");
        rsvpList.add("u5");
        rsvpField.set(req, rsvpList);

        assertTrue(validator.validateRsvp(req));
    }

    @Test
    void testValidateRsvp_Invalid() throws Exception {
        Room room = new Room(1, "Room A", 10, AccessLevel.ALL_USERS);
        User user = new Student("user1", "Student", "pass", "email", "stu1", "CS", 3);
        TimeSlot slot = new TimeSlot(LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        BookingRequest req = new BookingRequest("b1", user, room, slot);

        // Set rsvpList to have 4 RSVPs (40% of 10, less than 50%)
        Field rsvpField = BookingRequest.class.getDeclaredField("rsvpList");
        rsvpField.setAccessible(true);
        HashSet<String> rsvpList = new HashSet<>();
        rsvpList.add("u1");
        rsvpList.add("u2");
        rsvpList.add("u3");
        rsvpList.add("u4");
        rsvpField.set(req, rsvpList);

        assertFalse(validator.validateRsvp(req));
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
        // Since repository is created within the method, the stub will return true for empty list
        assertTrue(validator.validateOneBookingPerDay(user));
    }
}