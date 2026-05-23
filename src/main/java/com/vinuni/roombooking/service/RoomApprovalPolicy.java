package com.vinuni.roombooking.service;

import com.vinuni.roombooking.model.Admin;
import com.vinuni.roombooking.model.Room;
import com.vinuni.roombooking.model.Staff;
import com.vinuni.roombooking.model.User;

import org.springframework.stereotype.Component;

/**
 * Classifies rooms by name into approval categories and decides whether a
 * given user can have a booking auto-APPROVED versus left PENDING for admin
 * review.
 *
 * Booleans here intentionally do not reject — a user outside a category's
 * auto-approval set may still submit; their booking is just kept PENDING.
 * Rejection is reserved for hard conflicts (overlapping bookings, validator
 * failures).
 */
@Component
public class RoomApprovalPolicy {

    public enum Category { OPEN, MEETING_ROOM, CLASSROOM, OTHER }

    /** Specific A12x rooms that override the Meeting-Room rule and become open. */
    private static final String[] OPEN_PREFIXES = {"A122", "A123", "A124", "A128"};

    /** Substrings that mark a room as open / auto-approved for everyone. */
    private static final String[] OPEN_SUBSTRINGS = {
            "Group-Discussion-Room", "Prayer-Room", "One-Button-Studio"
    };

    public Category classify(Room room) {
        if (room == null || room.getRoomName() == null) return Category.OTHER;
        String name = room.getRoomName();

        for (String prefix : OPEN_PREFIXES) {
            if (name.startsWith(prefix)) return Category.OPEN;
        }
        for (String sub : OPEN_SUBSTRINGS) {
            if (name.contains(sub)) return Category.OPEN;
        }
        if (name.contains("Meeting-Room")) return Category.MEETING_ROOM;
        if (name.contains("Classroom")) return Category.CLASSROOM;
        return Category.OTHER;
    }

    /**
     * @return true if a booking for this room by this user should skip the
     *         PENDING queue and be APPROVED immediately. Admin clears the
     *         controlled-room gates for meeting rooms and classrooms, but
     *         truly uncategorized rooms still require approval.
     */
    public boolean canAutoApprove(Room room, User user) {
        if (user == null) return false;
        Category cat = classify(room);
        return switch (cat) {
            case OPEN         -> true;
            case MEETING_ROOM -> user instanceof Staff || user instanceof Admin;
            case CLASSROOM    -> user instanceof Admin;
            case OTHER        -> false;
        };
    }

    public String displayName(Category cat) {
        return switch (cat) {
            case OPEN         -> "Open";
            case MEETING_ROOM -> "Meeting room";
            case CLASSROOM    -> "Classroom";
            case OTHER        -> "Other";
        };
    }
}
