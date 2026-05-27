package com.vinuni.roombooking.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.springframework.stereotype.Service;

import com.vinuni.roombooking.enums.BookingStatus;
import com.vinuni.roombooking.model.Admin;
import com.vinuni.roombooking.model.BookingRequest;
import com.vinuni.roombooking.model.User;
import com.vinuni.roombooking.repository.BookingRepository;
import com.vinuni.roombooking.validator.BookingValidator;

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

    /**
     * CONTRACT — Submit a booking request through validation and persist it.
     *
     * Validators run for access, past-time, advance window, duration, minimum
     * participation, and one-booking-per-day. Conflict is checked against the
     * live bookings table via {@link DatabaseConnector#hasRoomConflict}. If any
     * hard rule fails, returns REJECTED. Otherwise the {@link RoomApprovalPolicy}
     * decides whether the request can skip the admin queue (APPROVED) or must
     * wait (PENDING).
     *
     * <p><b>Invitee gating:</b> bookings with invitees always start PENDING,
     * even if the room is normally auto-approved — the booking only promotes
     * to APPROVED once every invitee has accepted, via
     * {@link #tryPromoteAfterInvitationResponse(String)}.
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
        if (!validator.validateOneBookingPerDay(req.getUser(), req.getTimeSlot())) {
            return BookingStatus.REJECTED;
        }
        if (dbConnector.hasRoomConflict(req.getRoom().getRoomId(),
                req.getTimeSlot().getStartTime(),
                req.getTimeSlot().getEndTime())) {
            return BookingStatus.REJECTED;
        }

        boolean autoApprove = policy.canAutoApprove(req.getRoom(), req.getUser());
        // Bookings with invitees must wait for them to respond, regardless of
        // the room policy. The promotion helper flips status to APPROVED once
        // every invitee accepts (auto rooms) or hands it to admin (non-auto).
        BookingStatus decided = (autoApprove && inviteeCount == 0)
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
     * Called after an invitee responds. If the booking is PENDING, the host
     * was waiting on invitees, and every invitee has now accepted, the booking
     * promotes to APPROVED — but only when the room's policy allows auto-approval.
     * Non-auto rooms stay PENDING for admin review.
     *
     * <p>Returns the booking's status after the call so callers can show the
     * appropriate confirmation, or {@code null} if the booking wasn't found.
     */
    public BookingStatus tryPromoteAfterInvitationResponse(String bookingId) {
        if (bookingId == null || bookingId.isBlank()) return null;
        BookingRequest req = repository.findById(bookingId);
        if (req == null) {
            // Fall back to DB so we can promote bookings that were created in
            // a previous JVM run (in-memory repo is cold on restart).
            req = dbConnector.findBookingById(bookingId);
            if (req == null) return null;
            repository.save(req);
        }
        if (req.getStatus() != BookingStatus.PENDING) return req.getStatus();
        if (!dbConnector.allInviteesAccepted(bookingId)) return req.getStatus();
        if (!policy.canAutoApprove(req.getRoom(), req.getUser())) return req.getStatus();
        req.setStatus(BookingStatus.APPROVED);
        repository.save(req);
        dbConnector.updateBookingStatus(req);
        return BookingStatus.APPROVED;
    }

    /**
     * Drain the pending queue and decide APPROVED / REJECTED for each.
     * Runs the same hard validators {@link #submitRequest} uses, plus an
     * in-memory conflict check across other queued items.
     *
     * <p>Bookings that are still waiting on invitees are skipped (left
     * PENDING) — promoting them is the invitation-response code path's job.
     * Skipped items are taken off the queue so we don't loop forever; the
     * promotion helper will re-add or admins can promote manually.
     */
    public void processQueue() {
        Queue<BookingRequest> pendingQueue = repository.getPendingQueue();
        // Snapshot first so we don't modify the queue while iterating.
        List<BookingRequest> snapshot = new ArrayList<>(pendingQueue);
        pendingQueue.clear();
        for (BookingRequest req : snapshot) {
            if (waitingForInvitees(req)) {
                // Re-save keeps the booking PENDING in the cache but doesn't
                // requeue it indefinitely — promotion is event-driven now.
                repository.save(req);
                continue;
            }
            req.setStatus(decideQueued(req));
            dbConnector.updateBookingStatus(req);
            repository.save(req);
        }
    }

    private boolean waitingForInvitees(BookingRequest req) {
        int total = dbConnector.countInvitations(req.getBookingId());
        if (total == 0) return false;
        return !dbConnector.allInviteesAccepted(req.getBookingId());
    }

    private BookingStatus decideQueued(BookingRequest req) {
        // All checks exclude this booking's own id — the row is already
        // persisted as PENDING, so without exclusion the same-day and
        // room-conflict checks would match against itself and falsely reject.
        String self = req.getBookingId();
        if (!validator.validateAccess(req.getRoom(), req.getUser()))   return BookingStatus.REJECTED;
        if (!validator.validateNotPast(req.getTimeSlot()))             return BookingStatus.REJECTED;
        if (!validator.validateAdvanceWindow(req.getTimeSlot()))       return BookingStatus.REJECTED;
        if (!validator.validateDuration(req.getTimeSlot()))            return BookingStatus.REJECTED;
        if (!validator.validateOneBookingPerDay(req.getUser(), req.getTimeSlot(), self)) {
            return BookingStatus.REJECTED;
        }
        if (dbConnector.hasRoomConflict(req.getRoom().getRoomId(),
                req.getTimeSlot().getStartTime(),
                req.getTimeSlot().getEndTime(),
                self)) {
            return BookingStatus.REJECTED;
        }
        return BookingStatus.APPROVED;
    }
}
