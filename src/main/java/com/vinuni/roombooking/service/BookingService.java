package com.vinuni.roombooking.service;

import com.vinuni.roombooking.enums.BookingStatus;
import com.vinuni.roombooking.model.BookingRequest;
import com.vinuni.roombooking.model.User;
import com.vinuni.roombooking.repository.BookingRepository;
import com.vinuni.roombooking.validator.BookingValidator;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Queue;

/**
 * PRIMARY CONTRACT between frontend and backend.
 * These three method signatures are FROZEN from Phase 1 — do not change them.
 *
 * Phase 2 owner: Huy Dung (logic) + Huy Tam (DB integration, May 15)
 */
@Service
public class BookingService {

    private final BookingValidator    validator;
    private final BookingRepository   repository;
    // private final DatabaseConnector   dbConnector;

    public BookingService(BookingValidator validator,
                          BookingRepository repository) { //DatabaseConnector dbConnector
        this.validator   = validator;
        this.repository  = repository;
        //this.dbConnector = dbConnector;
    }

    // -------------------------------------------------------------------------
    // CONTRACT METHODS — frontend depends on these exact signatures
    // -------------------------------------------------------------------------

    /**
     * CONTRACT — Submit a booking request through validation and persist it.
     *
     * STUB returns APPROVED unconditionally.
     * Phase 2: run all six validator rules; set status to PENDING, then
     *          call repository.save(req) and dbConnector.insertBooking(req).
     */
    public BookingStatus submitRequest(BookingRequest req) {
        if (!validator.validateAccess(req.getRoom(), req.getUser())) return BookingStatus.REJECTED;
        if (!validator.validateDuration(req.getTimeSlot()))           return BookingStatus.REJECTED;
        if (!validator.validateAdvanceWindow(req.getTimeSlot()))      return BookingStatus.REJECTED;
        if (!validator.validateOneBookingPerDay(req.getUser()))        return BookingStatus.REJECTED;
        List<BookingRequest> existing = repository.findByRoom(req.getRoom().getRoomId());
        if (validator.detectConflict(req, existing))                  return BookingStatus.REJECTED;
        req.setStatus(BookingStatus.PENDING);
        repository.save(req);
        //dbConnector.insertBooking(req);
        return BookingStatus.APPROVED;
    }

    /**
     * CONTRACT — Cancel a booking if the requesting user owns it (or is Admin).
     *
     * STUB returns true unconditionally.
     * Phase 2: verify ownership, set CANCELLED, persist.
     */
    public boolean cancelBooking(String bookingId, User user) {
        BookingRequest req = repository.findById(bookingId);
        if (req == null) return false;
        if (!req.getUser().getUserId().equals(user.getUserId()) && !(user instanceof Admin)) return false;
        req.setStatus(BookingStatus.CANCELLED);
        repository.save(req);
        return true;
    }

    /**
     * CONTRACT — Add an RSVP entry for the given user on a booking.
     *
     * STUB returns true unconditionally.
     * Phase 2: look up booking, delegate to req.addRsvp(user.getUserId()).
     */
    public boolean addRsvp(String bookingId, User user) {
        BookingRequest req = repository.findById(bookingId);
        if (req == null) return false;
        return req.addRsvp(user.getUserId());
    }

    // -------------------------------------------------------------------------
    // Internal helpers — not part of the frontend contract
    // -------------------------------------------------------------------------

    /**
     * STUB — Phase 2: drain requestQueue and approve/reject each in order.
     */
    public void processQueue() {
        Queue<BookingRequest> pendingQueue = repository.getPendingQueue();
        while (!pendingQueue.isEmpty()) {
            BookingRequest req = pendingQueue.poll();
            
            // Run all validators
            if (!validator.validateAccess(req.getRoom(), req.getUser())) {
                req.setStatus(BookingStatus.REJECTED);
            } else if (!validator.validateDuration(req.getTimeSlot())) {
                req.setStatus(BookingStatus.REJECTED);
            } else if (!validator.validateAdvanceWindow(req.getTimeSlot())) {
                req.setStatus(BookingStatus.REJECTED);
            } else if (!validator.validateOneBookingPerDay(req.getUser())) {
                req.setStatus(BookingStatus.REJECTED);
            } else {
                // Check for conflicts with existing bookings for the room
                List<BookingRequest> existingBookings = repository.findByRoom(req.getRoom().getRoomId());
                if (validator.detectConflict(req, existingBookings)) {
                    req.setStatus(BookingStatus.REJECTED);
                } else {
                    req.setStatus(BookingStatus.APPROVED);
                }
            }
            
            // Persist the processed request
            repository.save(req);
        }
    }
}
