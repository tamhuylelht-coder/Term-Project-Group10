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
    private final RoomApprovalPolicy  policy;

    public BookingService(BookingValidator validator,
                          BookingRepository repository,
                          DatabaseConnector dbConnector,
                          RoomApprovalPolicy policy) {
        this.validator   = validator;
        this.repository  = repository;
        this.dbConnector = dbConnector;
        this.policy      = policy;
    }

    // -------------------------------------------------------------------------
    // CONTRACT METHODS — frontend depends on these exact signatures
    // -------------------------------------------------------------------------

    /**
     * CONTRACT — Submit a booking request through validation and persist it.
     *
     * Validators run for access, past-time, advance window, duration, minimum
     * participation, and one-booking-per-day. Conflict is checked against the
     * live bookings table via {@link DatabaseConnector#hasRoomConflict}. If any
     * hard rule fails, returns REJECTED. Otherwise the {@link RoomApprovalPolicy}
     * decides whether the request can skip the admin queue (APPROVED) or must
     * wait (PENDING).
     */
    public BookingStatus submitRequest(BookingRequest req, int inviteeCount) {
        // Fast-fail at submission so a Student trying to book a STAFF_ONLY room
        // doesn't sit in the pending queue waiting to be rejected by processQueue.
        if (!validator.validateAccess(req.getRoom(), req.getUser()))   return BookingStatus.REJECTED;
        if (!validator.validateNotPast(req.getTimeSlot()))             return BookingStatus.REJECTED;
        if (!validator.validateAdvanceWindow(req.getTimeSlot()))       return BookingStatus.REJECTED;
        if (!validator.validateDuration(req.getTimeSlot()))            return BookingStatus.REJECTED;
        if (!validator.validateMinimumParticipation(req.getRoom(), inviteeCount)) {
            return BookingStatus.REJECTED;
        }
        if (!validator.validateOneBookingPerDay(req.getUser()))        return BookingStatus.REJECTED;
        if (dbConnector.hasRoomConflict(req.getRoom().getRoomId(),
                req.getTimeSlot().getStartTime(),
                req.getTimeSlot().getEndTime())) {
            return BookingStatus.REJECTED;
        }

        BookingStatus decided = policy.canAutoApprove(req.getRoom(), req.getUser())
                ? BookingStatus.APPROVED
                : BookingStatus.PENDING;
        req.setStatus(decided);
        repository.save(req);
        dbConnector.insertBooking(req);
        return decided;
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

    // -------------------------------------------------------------------------
    // Internal helpers — not part of the frontend contract
    // -------------------------------------------------------------------------

    /**
     * Drain the pending queue and decide APPROVED / REJECTED for each.
     * Runs the same hard validators {@link #submitRequest} uses, plus an
     * in-memory conflict check across other queued items.
     */
    public void processQueue() {
        Queue<BookingRequest> pendingQueue = repository.getPendingQueue();
        while (!pendingQueue.isEmpty()) {
            BookingRequest req = pendingQueue.poll();
            req.setStatus(decideQueued(req));
            dbConnector.updateBookingStatus(req);
            repository.save(req);
        }
    }

    private BookingStatus decideQueued(BookingRequest req) {
        if (!validator.validateAccess(req.getRoom(), req.getUser()))   return BookingStatus.REJECTED;
        if (!validator.validateNotPast(req.getTimeSlot()))             return BookingStatus.REJECTED;
        if (!validator.validateAdvanceWindow(req.getTimeSlot()))       return BookingStatus.REJECTED;
        if (!validator.validateDuration(req.getTimeSlot()))            return BookingStatus.REJECTED;
        if (!validator.validateOneBookingPerDay(req.getUser()))        return BookingStatus.REJECTED;
        List<BookingRequest> existing = repository.findByRoom(req.getRoom().getRoomId());
        if (validator.detectConflict(req, existing))                   return BookingStatus.REJECTED;
        return BookingStatus.APPROVED;
    }
}
