package com.vinuni.roombooking.repository;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.vinuni.roombooking.enums.BookingStatus;
import com.vinuni.roombooking.model.BookingRequest;

/**
 * In-memory cache of BookingRequests. Persistence lives in MySQL via
 * {@link com.vinuni.roombooking.service.DatabaseConnector}; this repo is just
 * a fast in-memory mirror that views hydrate from DB on every refresh.
 *
 * <p>Backed by an LRU-evicting {@link LinkedHashMap} so a long-running JVM
 * doesn't accumulate every booking ever made. Eviction order is insertion
 * order — when the cap is reached, the oldest entry is dropped from both the
 * index/history map and the pending queue.
 */
@Repository
public class BookingRepository {

    /** Cap chosen large enough to cover all reasonable demo / single-session
     *  workloads; if a real production deployment ever ships, this should
     *  shrink and the repo should pull from DB on every read instead of
     *  serving from the cache. */
    private static final int MAX_CACHED = 5000;

    /** LinkedHashMap in access-order so we evict the genuinely-least-recently-used. */
    private final Map<String, BookingRequest> cache =
            new LinkedHashMap<String, BookingRequest>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, BookingRequest> eldest) {
                    if (size() > MAX_CACHED) {
                        // Mirror eviction into the pending queue so it doesn't
                        // hold stale references.
                        if (eldest.getValue().getStatus() == BookingStatus.PENDING) {
                            requestQueue.remove(eldest.getValue());
                        }
                        return true;
                    }
                    return false;
                }
            };
    private final Queue<BookingRequest> requestQueue = new LinkedList<>();

    public void save(BookingRequest req) {
        BookingRequest existing = cache.get(req.getBookingId());
        if (existing != null && existing.getStatus() == BookingStatus.PENDING) {
            requestQueue.remove(existing);
        }
        cache.put(req.getBookingId(), req);
        if (req.getStatus() == BookingStatus.PENDING) {
            requestQueue.offer(req);
        }
    }

    /** CONTRACT: returns null if not found. */
    public BookingRequest findById(String id) {
        return cache.get(id);
    }

    public List<BookingRequest> findByUser(String userId) {
        return cache.values().stream()
                .filter(r -> r.getUser().getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    public List<BookingRequest> findByRoom(int roomId) {
        return cache.values().stream()
                .filter(r -> r.getRoom().getRoomId() == roomId)
                .collect(Collectors.toList());
    }

    public boolean delete(String bookingId) {
        BookingRequest req = cache.remove(bookingId);
        if (req == null) return false;
        if (req.getStatus() == BookingStatus.PENDING) {
            requestQueue.remove(req);
        }
        return true;
    }

    public Queue<BookingRequest> getPendingQueue() {
        return requestQueue;
    }
}
