package com.vinuni.roombooking.service;

import com.vinuni.roombooking.enums.AccessLevel;
import com.vinuni.roombooking.enums.BookingStatus;
import com.vinuni.roombooking.model.BookingRequest;
import com.vinuni.roombooking.model.Room;
import com.vinuni.roombooking.model.Student;
import com.vinuni.roombooking.model.TimeSlot;
import com.vinuni.roombooking.model.User;
import com.vinuni.roombooking.repository.BookingRepository;
import com.vinuni.roombooking.validator.BookingValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for BookingService.submitRequest() after calendar auto-approval rules.
 */
public class BookingServiceTest {

    private FakeDatabaseConnector db;
    private FakeBookingValidator validator;
    private FakeBookingRepository repository;
    private FakeRoomApprovalPolicy policy;
    private BookingService bookingService;
    private User testStudent;
    private Room testRoom;
    private TimeSlot validTimeSlot;
    private BookingRequest validBookingRequest;

    @BeforeEach
    void setUp() {
        db = new FakeDatabaseConnector();
        validator = new FakeBookingValidator();
        repository = new FakeBookingRepository();
        policy = new FakeRoomApprovalPolicy();
        bookingService = new BookingService(validator, repository, db, policy);

        testStudent = new Student("user1", "John Doe", "password",
                "john@email.com", "stu001", "CS", 3);
        testRoom = new Room(1, "A102-Group-Discussion-Room", 10, AccessLevel.ALL_USERS);
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        validTimeSlot = new TimeSlot(start, start.plusHours(2));
        validBookingRequest = new BookingRequest("b1", testStudent, testRoom, validTimeSlot);
    }

    @Test
    void submitRequest_NoConflictAndAutoApproval_ReturnsApprovedAndPersistsApproved() {
        allowBaseValidation();
        db.conflict = false;
        policy.autoApprove = true;

        BookingStatus result = bookingService.submitRequest(validBookingRequest, 5);

        assertEquals(BookingStatus.APPROVED, result);
        assertEquals(BookingStatus.APPROVED, validBookingRequest.getStatus());
        assertSame(validBookingRequest, repository.savedRequest);
        assertSame(validBookingRequest, db.insertedRequest);
    }

    @Test
    void submitRequest_NoConflictButNoAutoApproval_ReturnsPendingAndPersistsPending() {
        allowBaseValidation();
        db.conflict = false;
        policy.autoApprove = false;

        BookingStatus result = bookingService.submitRequest(validBookingRequest, 5);

        assertEquals(BookingStatus.PENDING, result);
        assertEquals(BookingStatus.PENDING, validBookingRequest.getStatus());
        assertSame(validBookingRequest, repository.savedRequest);
        assertSame(validBookingRequest, db.insertedRequest);
    }

    @Test
    void submitRequest_ConflictDetected_ReturnsRejectedWithoutPersisting() {
        allowBaseValidation();
        db.conflict = true;

        BookingStatus result = bookingService.submitRequest(validBookingRequest, 5);

        assertEquals(BookingStatus.REJECTED, result);
        assertNull(repository.savedRequest);
        assertNull(db.insertedRequest);
    }

    @Test
    void submitRequest_MinimumParticipationViolated_ReturnsRejectedWithoutPersisting() {
        validator.durationValid = true;
        validator.advanceWindowValid = true;
        validator.minimumParticipationValid = false;

        BookingStatus result = bookingService.submitRequest(validBookingRequest, 1);

        assertEquals(BookingStatus.REJECTED, result);
        assertEquals(0, db.conflictChecks);
        assertNull(repository.savedRequest);
        assertNull(db.insertedRequest);
    }

    @Test
    void submitRequest_DurationViolated_ReturnsRejectedWithoutPersisting() {
        validator.durationValid = false;

        BookingStatus result = bookingService.submitRequest(validBookingRequest, 5);

        assertEquals(BookingStatus.REJECTED, result);
        assertNull(repository.savedRequest);
        assertNull(db.insertedRequest);
    }

    private void allowBaseValidation() {
        validator.durationValid = true;
        validator.advanceWindowValid = true;
        validator.minimumParticipationValid = true;
    }

    private static class FakeDatabaseConnector extends DatabaseConnector {
        boolean conflict;
        int conflictChecks;
        BookingRequest insertedRequest;

        @Override
        public boolean hasRoomConflict(int roomId, LocalDateTime start, LocalDateTime end) {
            conflictChecks++;
            return conflict;
        }

        @Override
        public void insertBooking(BookingRequest req) {
            insertedRequest = req;
        }
    }

    private static class FakeBookingValidator extends BookingValidator {
        boolean durationValid;
        boolean advanceWindowValid;
        boolean minimumParticipationValid;

        @Override
        public boolean validateDuration(TimeSlot slot) {
            return durationValid;
        }

        @Override
        public boolean validateAdvanceWindow(TimeSlot slot) {
            return advanceWindowValid;
        }

        @Override
        public boolean validateMinimumParticipation(Room room, int inviteeCount) {
            return minimumParticipationValid;
        }
    }

    private static class FakeBookingRepository extends BookingRepository {
        BookingRequest savedRequest;

        @Override
        public void save(BookingRequest req) {
            savedRequest = req;
        }
    }

    private static class FakeRoomApprovalPolicy extends RoomApprovalPolicy {
        boolean autoApprove;

        @Override
        public boolean canAutoApprove(Room room, User user) {
            return autoApprove;
        }
    }
}
