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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for BookingService.submitRequest() with dummy data.
 * Uses Mockito to mock the BookingValidator and BookingRepository.
 */
public class BookingServiceTest {

    private BookingService bookingService;

    @Mock
    private BookingValidator mockValidator;

    @Mock
    private BookingRepository mockRepository;

    // Test data
    private User testStudent;
    private Room testRoom;
    private TimeSlot validTimeSlot;
    private BookingRequest validBookingRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        bookingService = new BookingService(mockValidator, mockRepository);

        // Set up test data
        testStudent = new Student("user1", "John Doe", "password", "john@email.com", "stu001", "CS", 3);
        testRoom = new Room(1, "Conference Room A", 10, AccessLevel.ALL_USERS);
        validTimeSlot = new TimeSlot(LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(2));
        validBookingRequest = new BookingRequest("b1", testStudent, testRoom, validTimeSlot);
    }

    @Test
    void testSubmitRequest_AllValidationsPass_ReturnsApproved() {
        // Arrange
        when(mockValidator.validateAccess(testRoom, testStudent)).thenReturn(true);
        when(mockValidator.validateDuration(validTimeSlot)).thenReturn(true);
        when(mockValidator.validateAdvanceWindow(validTimeSlot)).thenReturn(true);
        when(mockValidator.validateOneBookingPerDay(testStudent)).thenReturn(true);
        when(mockRepository.findByRoom(testRoom.getRoomId())).thenReturn(new ArrayList<>());
        when(mockValidator.detectConflict(validBookingRequest, new ArrayList<>())).thenReturn(false);

        // Act
        BookingStatus result = bookingService.submitRequest(validBookingRequest);

        // Assert
        assertEquals(BookingStatus.APPROVED, result);
        assertEquals(BookingStatus.PENDING, validBookingRequest.getStatus());
        verify(mockRepository).save(validBookingRequest);
    }

    @Test
    void testSubmitRequest_AccessDenied_ReturnsRejected() {
        // Arrange
        when(mockValidator.validateAccess(testRoom, testStudent)).thenReturn(false);

        // Act
        BookingStatus result = bookingService.submitRequest(validBookingRequest);

        // Assert
        assertEquals(BookingStatus.REJECTED, result);
    }

    @Test
    void testSubmitRequest_DurationExceeded_ReturnsRejected() {
        // Arrange
        when(mockValidator.validateAccess(testRoom, testStudent)).thenReturn(true);
        when(mockValidator.validateDuration(validTimeSlot)).thenReturn(false);

        // Act
        BookingStatus result = bookingService.submitRequest(validBookingRequest);

        // Assert
        assertEquals(BookingStatus.REJECTED, result);
    }

    @Test
    void testSubmitRequest_AdvanceWindowViolated_ReturnsRejected() {
        // Arrange
        when(mockValidator.validateAccess(testRoom, testStudent)).thenReturn(true);
        when(mockValidator.validateDuration(validTimeSlot)).thenReturn(true);
        when(mockValidator.validateAdvanceWindow(validTimeSlot)).thenReturn(false);

        // Act
        BookingStatus result = bookingService.submitRequest(validBookingRequest);

        // Assert
        assertEquals(BookingStatus.REJECTED, result);
    }

    @Test
    void testSubmitRequest_OneBookingPerDayViolated_ReturnsRejected() {
        // Arrange
        when(mockValidator.validateAccess(testRoom, testStudent)).thenReturn(true);
        when(mockValidator.validateDuration(validTimeSlot)).thenReturn(true);
        when(mockValidator.validateAdvanceWindow(validTimeSlot)).thenReturn(true);
        when(mockValidator.validateOneBookingPerDay(testStudent)).thenReturn(false);

        // Act
        BookingStatus result = bookingService.submitRequest(validBookingRequest);

        // Assert
        assertEquals(BookingStatus.REJECTED, result);
    }

    @Test
    void testSubmitRequest_ConflictDetected_ReturnsRejected() {
        // Arrange
        List<BookingRequest> existingBookings = new ArrayList<>();
        existingBookings.add(new BookingRequest("b2", testStudent, testRoom, validTimeSlot));

        when(mockValidator.validateAccess(testRoom, testStudent)).thenReturn(true);
        when(mockValidator.validateDuration(validTimeSlot)).thenReturn(true);
        when(mockValidator.validateAdvanceWindow(validTimeSlot)).thenReturn(true);
        when(mockValidator.validateOneBookingPerDay(testStudent)).thenReturn(true);
        when(mockRepository.findByRoom(testRoom.getRoomId())).thenReturn(existingBookings);
        when(mockValidator.detectConflict(validBookingRequest, existingBookings)).thenReturn(true);

        // Act
        BookingStatus result = bookingService.submitRequest(validBookingRequest);

        // Assert
        assertEquals(BookingStatus.REJECTED, result);
    }

    @Test
    void testSubmitRequest_MultipleValidationsPass_RepositorySaveCalled() {
        // Arrange
        when(mockValidator.validateAccess(testRoom, testStudent)).thenReturn(true);
        when(mockValidator.validateDuration(validTimeSlot)).thenReturn(true);
        when(mockValidator.validateAdvanceWindow(validTimeSlot)).thenReturn(true);
        when(mockValidator.validateOneBookingPerDay(testStudent)).thenReturn(true);
        when(mockRepository.findByRoom(testRoom.getRoomId())).thenReturn(new ArrayList<>());
        when(mockValidator.detectConflict(validBookingRequest, new ArrayList<>())).thenReturn(false);

        // Act
        bookingService.submitRequest(validBookingRequest);

        // Assert - verify that repository.save was called exactly once
        verify(mockRepository).save(validBookingRequest);
    }

    @Test
    void testSubmitRequest_StatusSetToPending() {
        // Arrange
        when(mockValidator.validateAccess(testRoom, testStudent)).thenReturn(true);
        when(mockValidator.validateDuration(validTimeSlot)).thenReturn(true);
        when(mockValidator.validateAdvanceWindow(validTimeSlot)).thenReturn(true);
        when(mockValidator.validateOneBookingPerDay(testStudent)).thenReturn(true);
        when(mockRepository.findByRoom(testRoom.getRoomId())).thenReturn(new ArrayList<>());
        when(mockValidator.detectConflict(validBookingRequest, new ArrayList<>())).thenReturn(false);

        // Act
        bookingService.submitRequest(validBookingRequest);

        // Assert
        assertEquals(BookingStatus.PENDING, validBookingRequest.getStatus());
    }
}
