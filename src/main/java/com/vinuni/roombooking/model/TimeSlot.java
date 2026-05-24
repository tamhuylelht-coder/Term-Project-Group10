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

    /** Length of the slot in fractional hours. */
    public double getDurationHours() {
        return java.time.Duration.between(startTime, endTime).toMinutes() / 60.0;
    }

    /** True if this slot overlaps with {@code other}. */
    public boolean overlapsWith(TimeSlot other) {
        return startTime.isBefore(other.endTime) && other.startTime.isBefore(endTime);
    }

    /** True if the start is within 7 days from now. Upper bound. */
    public boolean isWithinOneWeek() {
        return !startTime.isAfter(LocalDateTime.now().plusWeeks(1));
    }

    /** True if the start is strictly in the future. Lower bound. */
    public boolean isInFuture() {
        return startTime.isAfter(LocalDateTime.now());
    }

    /** True if the duration is at most 3 hours. */
    public boolean isWithinMaxDuration() {
        return getDurationHours() <= 3.0;
    }
}
