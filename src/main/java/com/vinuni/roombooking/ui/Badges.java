package com.vinuni.roombooking.ui;

import com.vaadin.flow.component.html.Span;
import com.vinuni.roombooking.enums.BookingStatus;
import com.vinuni.roombooking.enums.InvitationStatus;
import com.vinuni.roombooking.enums.RoomStatus;

/**
 * Colored status pills used in grid columns across the views.
 *
 * Uses inline styles (rather than Vaadin's {@code theme="badge"} attribute)
 * so the pill renders correctly in every context — including the ComboBox
 * overlay used by the room picker, where Lumo's badge theme styling does
 * not always propagate into the overlay's stamping scope.
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
        return pill(text, "hsl(145, 55%, 88%)", "hsl(145, 65%, 25%)");
    }

    private static Span red(String text) {
        return pill(text, "hsl(0, 75%, 92%)", "hsl(0, 70%, 35%)");
    }

    private static Span orange(String text) {
        return pill(text, "hsl(28, 100%, 92%)", "hsl(28, 80%, 30%)");
    }

    private static Span neutral(String text) {
        return pill(text, "hsl(0, 0%, 92%)", "hsl(0, 0%, 30%)");
    }

    /**
     * Consistent pill shape across colors. Inline so the styles survive
     * shadow-DOM scopes like ComboBox overlays where theme attributes
     * don't always inherit Lumo's badge CSS.
     */
    private static Span pill(String text, String background, String color) {
        Span s = new Span(text);
        s.getStyle()
                .set("display", "inline-flex")
                .set("align-items", "center")
                .set("background", background)
                .set("color", color)
                .set("padding", "2px 8px")
                .set("border-radius", "999px")
                .set("font-size", "0.78rem")
                .set("font-weight", "600")
                .set("line-height", "1.4")
                .set("white-space", "nowrap");
        return s;
    }
}
