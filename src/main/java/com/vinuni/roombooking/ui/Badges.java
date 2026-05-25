package com.vinuni.roombooking.ui;

import com.vaadin.flow.component.html.Span;
import com.vinuni.roombooking.enums.BookingStatus;
import com.vinuni.roombooking.enums.InvitationStatus;
import com.vinuni.roombooking.enums.RoomStatus;

/**
 * Colored status pills used in grid columns across the views.
 *
 * Uses Vaadin Lumo's built-in {@code badge success / badge error} theme variants
 * for green/red. Orange (used for transitional states like OCCUPIED / PENDING)
 * isn't a stock Lumo variant, so it's styled inline against the Lumo warning
 * tokens that ship with the theme.
 *
 * <p>Color contract:
 * <ul>
 *   <li>Room AVAILABLE / Booking APPROVED / "Available now" → green</li>
 *   <li>Room OCCUPIED / Booking REJECTED / Booking CANCELLED → red</li>
 *   <li>Room MAINTENANCE / Booking PENDING → orange</li>
 *   <li>Invitation ACCEPTED → green, PENDING → orange, DECLINED → red</li>
 * </ul>
 * Every view that shows a status should route through one of these helpers
 * so the color rules stay consistent without copy-paste drift.
 */
public final class Badges {

    private Badges() {}

    public static Span roomStatus(RoomStatus status) {
        if (status == null) return neutral("UNKNOWN");
        switch (status) {
            case AVAILABLE:   return green(status.name());
            case OCCUPIED:    return red(status.name());
            case MAINTENANCE: return orange(status.name());
            default:          return neutral(status.name());
        }
    }

    public static Span bookingStatus(BookingStatus status) {
        if (status == null) return neutral("UNKNOWN");
        switch (status) {
            case APPROVED:  return green(status.name());
            case PENDING:   return orange(status.name());
            case REJECTED:  return red(status.name());
            case CANCELLED: return red(status.name());
            default:        return neutral(status.name());
        }
    }

    public static Span invitationStatus(InvitationStatus status) {
        if (status == null) return neutral("UNKNOWN");
        switch (status) {
            case ACCEPTED: return green(status.name());
            case PENDING:  return orange(status.name());
            case DECLINED: return red(status.name());
            default:       return neutral(status.name());
        }
    }

    /** Green "Available" / red "Occupied" pill for the room picker. */
    public static Span bookingAvailability(boolean available) {
        return available ? green("Available") : red("Occupied");
    }

    private static Span green(String text) {
        Span s = new Span(text);
        s.getElement().getThemeList().add("badge success");
        return s;
    }

    private static Span red(String text) {
        Span s = new Span(text);
        s.getElement().getThemeList().add("badge error");
        return s;
    }

    /** Orange — Vaadin Lumo doesn't ship a warning badge, so we hand-roll one. */
    private static Span orange(String text) {
        Span s = new Span(text);
        s.getElement().getThemeList().add("badge");
        s.getStyle()
                .set("background-color", "hsl(28, 100%, 92%)")
                .set("color", "hsl(28, 80%, 30%)");
        return s;
    }

    private static Span neutral(String text) {
        Span s = new Span(text);
        s.getElement().getThemeList().add("badge contrast");
        return s;
    }
}
