package com.vinuni.roombooking.service;

import java.util.List;
import java.util.Queue;

import org.springframework.stereotype.Service;

import com.vinuni.roombooking.enums.BookingStatus;
import com.vinuni.roombooking.model.Admin;
import com.vinuni.roombooking.model.BookingRequest;
import com.vinuni.roombooking.model.User;
import com.vinuni.roombooking.repository.BookingRepository;
import com.vinuni.roombooking.validator.BookingValidator;

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
    private final DatabaseConnector   dbConnector;

    public BookingService(BookingValidator validator,
                          BookingRepository repository, DatabaseConnector dbConnector) { 
        this.validator   = validator;
        this.repository  = repository;
        this.dbConnector = dbConnector;
    }

    // -------------------------------------------------------------------------
    // CONTRACT METHODS — frontend depends on these exact signatures
    // -------------------------------------------------------------------------

    /**
     * CONTRACT — Submit a booking request through validation and persist it.
     *
     * The {@code inviteeCount} arg drives the minimum-participation check
     * (see {@link BookingValidator#validateMinimumParticipation}). FE passes
     * the number of invitees the host picked in the form.
     */
    public BookingStatus submitRequest(BookingRequest req, int inviteeCount) {
        if (!validator.validateAccess(req.getRoom(), req.getUser())) return BookingStatus.REJECTED;
        if (!validator.validateDuration(req.getTimeSlot()))           return BookingStatus.REJECTED;
        if (!validator.validateAdvanceWindow(req.getTimeSlot()))      return BookingStatus.REJECTED;
        if (!validator.validateOneBookingPerDay(req.getUser()))        return BookingStatus.REJECTED;
        if (!validator.validateMinimumParticipation(req.getRoom(), inviteeCount)) return BookingStatus.REJECTED;
        List<BookingRequest> existing = repository.findByRoom(req.getRoom().getRoomId());
        if (validator.detectConflict(req, existing))                  return BookingStatus.REJECTED;
        req.setStatus(BookingStatus.PENDING);
        repository.save(req);
        dbConnector.insertBooking(req); // Insert booking information into MySQL database
        return BookingStatus.APPROVED;
    }

    /**
     * Back-compat wrapper for callers that haven't moved to the 2-arg form.
     * Passes 0 as inviteeCount, which fails the minimum-participation check
     * for any non-trivial room — by design, so silent under-participation
     * doesn't slip through.
     *
     * @deprecated Use {@link #submitRequest(BookingRequest, int)}.
     */
    @Deprecated
    public BookingStatus submitRequest(BookingRequest req) {
        return submitRequest(req, 0);
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
        dbConnector.updateBookingStatus(req);
        return true;
    }

    /**
     * Self-RSVP path against the legacy {@code rsvp} table.
     *
     * @deprecated Bookings are invite-only now (see invitations table).
     *             No FE caller relies on this; kept for back-compat with
     *             existing tests. Will be removed once those are migrated.
     */
    @Deprecated
    public boolean addRsvp(String bookingId, User user) {
        BookingRequest req = repository.findById(bookingId);
        if (req == null) return false;
        if (dbConnector.hasRsvp(bookingId, user.getUserId())) return false;
        dbConnector.insertRsvp(bookingId, user.getUserId());
        return true;
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
                dbConnector.updateBookingStatus(req);
            } else if (!validator.validateDuration(req.getTimeSlot())) {
                req.setStatus(BookingStatus.REJECTED);
                dbConnector.updateBookingStatus(req);
            } else if (!validator.validateAdvanceWindow(req.getTimeSlot())) {
                req.setStatus(BookingStatus.REJECTED);
                dbConnector.updateBookingStatus(req);
            } else if (!validator.validateOneBookingPerDay(req.getUser())) {
                req.setStatus(BookingStatus.REJECTED);
                dbConnector.updateBookingStatus(req);
            } else {
                // Check for conflicts with existing bookings for the room
                List<BookingRequest> existingBookings = repository.findByRoom(req.getRoom().getRoomId());
                if (validator.detectConflict(req, existingBookings)) {
                    req.setStatus(BookingStatus.REJECTED);
                    dbConnector.updateBookingStatus(req);
                } else {
                    req.setStatus(BookingStatus.APPROVED);
                    dbConnector.updateBookingStatus(req);
                }
            }
            
            // Persist the processed request
            repository.save(req);
        }
    }
}
