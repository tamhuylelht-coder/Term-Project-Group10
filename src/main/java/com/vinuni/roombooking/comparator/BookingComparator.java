package com.vinuni.roombooking.comparator;

import java.util.Comparator;

import com.vinuni.roombooking.model.BookingRequest;

// Sorts BookingRequests by creation time (earliest first).

public class BookingComparator implements Comparator<BookingRequest> {
    @Override
    public int compare(BookingRequest b1, BookingRequest b2) {
        return b1.getCreatedAt().compareTo(b2.getCreatedAt());
    }
}
