package com.vinuni.roombooking.ui;

import com.vaadin.flow.component.html.Span;
import com.vinuni.roombooking.enums.BookingStatus;
import com.vinuni.roombooking.enums.RoomStatus;

/**
 * Colored status pills used in grid columns across the views.
 *
 * Uses Vaadin Lumo's built-in {@code badge success / badge error} theme variants
 * for green/red. Orange (used for transitional states like OCCUPIED / PENDING)
 * isn't a stock Lumo variant, so it's styled inline against the Lumo warning
 * tokens that ship with the theme.
 */
public final class Badges {

    private Badges() {}

    public static Span roomStatus(RoomStatus status) {
        switch (status) {
            case AVAILABLE:   return green(status.name());
            case OCCUPIED:    return orange(status.name());
            case MAINTENANCE: return red(status.name());
            default:          return neutral(status.name());
        }
    }

    public static Span bookingStatus(BookingStatus status) {
        switch (status) {
            case APPROVED:  return green(status.name());
            case PENDING:   return orange(status.name());
            case REJECTED:  return red(status.name());
            case CANCELLED: return red(status.name());
            default:        return neutral(status.name());
        }
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
