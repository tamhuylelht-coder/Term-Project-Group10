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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for BookingService.submitRequest() after calendar auto-approval rules.
 */
public class BookingServiceTest {

    @Mock
    private DatabaseConnector mockDatabaseConnector;

    @Mock
    private BookingValidator mockValidator;

    @Mock
    private BookingRepository mockRepository;

    @Mock
    private RoomApprovalPolicy mockPolicy;

    private BookingService bookingService;
    private User testStudent;
    private Room testRoom;
    private TimeSlot validTimeSlot;
    private BookingRequest validBookingRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        bookingService = new BookingService(
                mockValidator, mockRepository, mockDatabaseConnector, mockPolicy);

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
        when(mockDatabaseConnector.hasRoomConflict(anyInt(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(mockPolicy.canAutoApprove(testRoom, testStudent)).thenReturn(true);

        BookingStatus result = bookingService.submitRequest(validBookingRequest, 5);

        assertEquals(BookingStatus.APPROVED, result);
        assertEquals(BookingStatus.APPROVED, validBookingRequest.getStatus());
        verify(mockRepository).save(validBookingRequest);
        verify(mockDatabaseConnector).insertBooking(validBookingRequest);
    }

    @Test
    void submitRequest_NoConflictButNoAutoApproval_ReturnsPendingAndPersistsPending() {
        allowBaseValidation();
        when(mockDatabaseConnector.hasRoomConflict(anyInt(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(mockPolicy.canAutoApprove(testRoom, testStudent)).thenReturn(false);

        BookingStatus result = bookingService.submitRequest(validBookingRequest, 5);

        assertEquals(BookingStatus.PENDING, result);
        assertEquals(BookingStatus.PENDING, validBookingRequest.getStatus());
        verify(mockRepository).save(validBookingRequest);
        verify(mockDatabaseConnector).insertBooking(validBookingRequest);
    }

    @Test
    void submitRequest_ConflictDetected_ReturnsRejectedWithoutPersisting() {
        allowBaseValidation();
        when(mockDatabaseConnector.hasRoomConflict(anyInt(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

        BookingStatus result = bookingService.submitRequest(validBookingRequest, 5);

        assertEquals(BookingStatus.REJECTED, result);
        verify(mockRepository, never()).save(any(BookingRequest.class));
        verify(mockDatabaseConnector, never()).insertBooking(any(BookingRequest.class));
    }

    @Test
    void submitRequest_MinimumParticipationViolated_ReturnsRejectedWithoutPersisting() {
        when(mockValidator.validateDuration(validTimeSlot)).thenReturn(true);
        when(mockValidator.validateAdvanceWindow(validTimeSlot)).thenReturn(true);
        when(mockValidator.validateMinimumParticipation(testRoom, 1)).thenReturn(false);

        BookingStatus result = bookingService.submitRequest(validBookingRequest, 1);

        assertEquals(BookingStatus.REJECTED, result);
        verify(mockDatabaseConnector, never()).hasRoomConflict(
                anyInt(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(mockRepository, never()).save(any(BookingRequest.class));
        verify(mockDatabaseConnector, never()).insertBooking(any(BookingRequest.class));
    }

    @Test
    void submitRequest_DurationViolated_ReturnsRejectedWithoutPersisting() {
        when(mockValidator.validateDuration(validTimeSlot)).thenReturn(false);

        BookingStatus result = bookingService.submitRequest(validBookingRequest, 5);

        assertEquals(BookingStatus.REJECTED, result);
        verify(mockRepository, never()).save(any(BookingRequest.class));
        verify(mockDatabaseConnector, never()).insertBooking(any(BookingRequest.class));
    }

    private void allowBaseValidation() {
        when(mockValidator.validateDuration(validTimeSlot)).thenReturn(true);
        when(mockValidator.validateAdvanceWindow(validTimeSlot)).thenReturn(true);
        when(mockValidator.validateMinimumParticipation(testRoom, 5)).thenReturn(true);
    }
}
