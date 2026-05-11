package com.vinuni.roombooking.comparator;

import com.vinuni.roombooking.model.BookingRequest;
import java.util.Comparator;

/**
 * Sorts BookingRequests by creation time (earliest first).
 * Phase 2: update sort key if priority ordering changes.
 */
public class BookingComparator implements Comparator<BookingRequest> {

    /**
     * STUB — Phase 2: compare by createdAt ascending.
     */
    @Override
    public int compare(BookingRequest b1, BookingRequest b2) {
        // TODO (Huy Dung): return b1.getCreatedAt().compareTo(b2.getCreatedAt());
        return 0;
    }
}
