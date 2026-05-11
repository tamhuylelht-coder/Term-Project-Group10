package com.vinuni.roombooking.service;

import com.vinuni.roombooking.enums.BookingStatus;
import com.vinuni.roombooking.model.BookingRequest;
import com.vinuni.roombooking.model.User;
import com.vinuni.roombooking.repository.BookingRepository;
import com.vinuni.roombooking.validator.BookingValidator;
import org.springframework.stereotype.Service;

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
        // TODO (Huy Dung):
        //   if (!validator.validateAccess(req.getUser(), req.getRoom())) return BookingStatus.REJECTED;
        //   if (!validator.validateDuration(req.getTimeSlot()))           return BookingStatus.REJECTED;
        //   if (!validator.validateAdvanceWindow(req.getTimeSlot()))      return BookingStatus.REJECTED;
        //   if (!validator.validateOneBookingPerDay(req.getUser()))        return BookingStatus.REJECTED;
        //   List<BookingRequest> existing = repository.findByRoom(req.getRoom().getRoomId());
        //   if (validator.detectConflict(req, existing))                  return BookingStatus.REJECTED;
        //   req.setStatus(BookingStatus.PENDING);
        //   repository.save(req);
        //   dbConnector.insertBooking(req);
        //   return BookingStatus.PENDING;
        return BookingStatus.APPROVED; // stub: always succeeds for frontend dev
    }

    /**
     * CONTRACT — Cancel a booking if the requesting user owns it (or is Admin).
     *
     * STUB returns true unconditionally.
     * Phase 2: verify ownership, set CANCELLED, persist.
     */
    public boolean cancelBooking(String bookingId, User user) {
        // TODO (Huy Dung):
        //   BookingRequest req = repository.findById(bookingId);
        //   if (req == null) return false;
        //   if (!req.getUser().getUserId().equals(user.getUserId()) && !(user instanceof Admin)) return false;
        //   req.setStatus(BookingStatus.CANCELLED);
        //   repository.save(req);
        //   return true;
        return true; // stub
    }

    /**
     * CONTRACT — Add an RSVP entry for the given user on a booking.
     *
     * STUB returns true unconditionally.
     * Phase 2: look up booking, delegate to req.addRsvp(user.getUserId()).
     */
    public boolean addRsvp(String bookingId, User user) {
        // TODO (Huy Dung):
        //   BookingRequest req = repository.findById(bookingId);
        //   if (req == null) return false;
        //   return req.addRsvp(user.getUserId());
        return true; // stub
    }

    // -------------------------------------------------------------------------
    // Internal helpers — not part of the frontend contract
    // -------------------------------------------------------------------------

    /**
     * STUB — Phase 2: drain requestQueue and approve/reject each in order.
     */
    public void processQueue() {
        // TODO (Huy Dung): poll requestQueue, run validators, update status, persist
    }
}
