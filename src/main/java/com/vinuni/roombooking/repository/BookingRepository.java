package com.vinuni.roombooking.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.vinuni.roombooking.enums.BookingStatus;
import com.vinuni.roombooking.model.BookingRequest;

/**
 * In-memory storage for BookingRequests.
 * Phase 2: Huy Tam wires real DB reads/writes into BookingService on May 15;
 * these in-memory structures serve as fallback / cache layer.
 *
 * Phase 2 owner: Huy Dung
 */
@Repository
public class BookingRepository {

    private ArrayList<BookingRequest>       bookingHistory = new ArrayList<>();
    private HashMap<String, BookingRequest> bookingIndex   = new HashMap<>();
    private Queue<BookingRequest>           requestQueue   = new LinkedList<>();

    /**
     * STUB — Phase 2: add req to history, index, and queue (if PENDING).
     */
    public void save(BookingRequest req) {
        BookingRequest existing = bookingIndex.get(req.getBookingId());
        if (existing != null) {
            bookingHistory.remove(existing);
            if (existing.getStatus() == BookingStatus.PENDING) {
                requestQueue.remove(existing);
            }
        }

        bookingHistory.add(req);
        bookingIndex.put(req.getBookingId(), req);

        if (req.getStatus() == BookingStatus.PENDING) {
            requestQueue.offer(req);
        }
    }

    /**
     * STUB — Phase 2: look up by bookingId in bookingIndex.
     * CONTRACT (frontend must handle null): returns null if not found.
     */
    public BookingRequest findById(String id) {
        return bookingIndex.get(id);
    }

    /**
     * CONTRACT METHOD — frontend depends on this signature from Phase 1.
     * STUB — Phase 2: filter bookingHistory where req.getUser().getUserId().equals(userId).
     */
    public List<BookingRequest> findByUser(String userId) {
        return bookingHistory.stream()
                .filter(r -> r.getUser().getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    /**
     * CONTRACT METHOD — frontend depends on this signature from Phase 1.
     * STUB — Phase 2: filter bookingHistory where req.getRoom().getRoomId() == roomId.
     */
    public List<BookingRequest> findByRoom(int roomId) {
        return bookingHistory.stream()
                .filter(r -> r.getRoom().getRoomId() == roomId)
                .collect(Collectors.toList());
    }

    /**
     * STUB — Phase 2: remove from history + index; return false if not found.
     */
    public boolean delete(String bookingId) {
        BookingRequest req = bookingIndex.remove(bookingId);
        if (req == null) {
            return false;
        }

        bookingHistory.remove(req);
        if (req.getStatus() == BookingStatus.PENDING) {
            requestQueue.remove(req);
        }
        return true;
    }

    /**
     * CONTRACT METHOD — frontend depends on this signature from Phase 1.
     * STUB — Phase 2: return the live requestQueue.
     */
    public Queue<BookingRequest> getPendingQueue() {
        return requestQueue;
    }
}
