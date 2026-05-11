package com.vinuni.roombooking.model;

import java.time.LocalDateTime;

public class TimeSlot {

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public TimeSlot(LocalDateTime startTime, LocalDateTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    /**
     * STUB — Phase 2: compute (endTime - startTime) in fractional hours.
     */
    public double getDurationHours() {
        // TODO (Huy Dung): return ChronoUnit.MINUTES.between(startTime, endTime) / 60.0;
        return 1.0;
    }

    /**
     * STUB — Phase 2: return true if this slot overlaps with other.
     * Two slots overlap when one starts before the other ends.
     */
    public boolean overlapsWith(TimeSlot other) {
        // TODO (Huy Dung): return startTime.isBefore(other.endTime) && other.startTime.isBefore(endTime);
        return false;
    }

    /**
     * STUB — Phase 2: return true if startTime is within 7 days from now.
     */
    public boolean isWithinOneWeek() {
        // TODO (Huy Dung): return !startTime.isAfter(LocalDateTime.now().plusWeeks(1));
        return true;
    }

    /**
     * STUB — Phase 2: return true if getDurationHours() <= 3.
     */
    public boolean isWithinMaxDuration() {
        // TODO (Huy Dung): return getDurationHours() <= 3.0;
        return true;
    }
}
