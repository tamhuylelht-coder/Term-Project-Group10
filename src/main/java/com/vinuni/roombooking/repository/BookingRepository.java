package com.vinuni.roombooking.repository;

import com.vinuni.roombooking.model.BookingRequest;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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
        // TODO (Huy Dung):
        //   bookingHistory.add(req);
        //   bookingIndex.put(req.getBookingId(), req);
        //   if (req.getStatus() == BookingStatus.PENDING) requestQueue.offer(req);
    }

    /**
     * STUB — Phase 2: look up by bookingId in bookingIndex.
     * CONTRACT (frontend must handle null): returns null if not found.
     */
    public BookingRequest findById(String id) {
        // TODO (Huy Dung): return bookingIndex.get(id);
        return null;
    }

    /**
     * CONTRACT METHOD — frontend depends on this signature from Phase 1.
     * STUB — Phase 2: filter bookingHistory where req.getUser().getUserId().equals(userId).
     */
    public List<BookingRequest> findByUser(String userId) {
        // TODO (Huy Dung): return bookingHistory.stream()
        //   .filter(r -> r.getUser().getUserId().equals(userId))
        //   .collect(Collectors.toList());
        return new ArrayList<>();
    }

    /**
     * CONTRACT METHOD — frontend depends on this signature from Phase 1.
     * STUB — Phase 2: filter bookingHistory where req.getRoom().getRoomId() == roomId.
     */
    public List<BookingRequest> findByRoom(int roomId) {
        // TODO (Huy Dung): return bookingHistory.stream()
        //   .filter(r -> r.getRoom().getRoomId() == roomId)
        //   .collect(Collectors.toList());
        return new ArrayList<>();
    }

    /**
     * STUB — Phase 2: remove from history + index; return false if not found.
     */
    public boolean delete(String bookingId) {
        // TODO (Huy Dung): BookingRequest req = bookingIndex.remove(bookingId);
        //   if (req == null) return false;
        //   bookingHistory.remove(req);
        //   return true;
        return true;
    }

    /**
     * CONTRACT METHOD — frontend depends on this signature from Phase 1.
     * STUB — Phase 2: return the live requestQueue.
     */
    public Queue<BookingRequest> getPendingQueue() {
        // TODO (Huy Dung): return requestQueue;
        return new LinkedList<>();
    }
}
