package com.vinuni.roombooking.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vinuni.roombooking.enums.BookingStatus;
import com.vinuni.roombooking.enums.InvitationStatus;
import com.vinuni.roombooking.model.Invitation;
import com.vinuni.roombooking.model.User;
import com.vinuni.roombooking.service.DatabaseConnector;
import com.vinuni.roombooking.ui.Badges;
import com.vinuni.roombooking.ui.MainLayout;
import com.vinuni.roombooking.ui.SessionUtil;
import com.vinuni.roombooking.ui.VaadinFrontendUI;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * View 6 - Inbox (invite-only).
 *
 *   <b>Your invitations</b> — bookings the host invited you to.
 *   Accept / Decline buttons write to the invitations table; the booking
 *   no longer has a public self-RSVP path. PENDING sort to the top so the
 *   action items are obvious.
 *
 * Filters applied:
 *   - Past invitations (end time &lt; now) are hidden.
 *   - Invitations to CANCELLED / REJECTED bookings are hidden so the
 *     invitee can't Accept a dead meeting.
 */
@Route(value = "browse", layout = MainLayout.class)
@PageTitle("Inbox")
public class BrowseBookingsView extends VerticalLayout {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final DatabaseConnector db;
    private final VaadinFrontendUI frontend;

    private final Grid<Invitation> invitesGrid = new Grid<>(Invitation.class, false);
    private final Paragraph invitesEmpty = new Paragraph(
            "No invitations waiting. When someone invites you to a booking, you'll see it here.");

    public BrowseBookingsView(DatabaseConnector db, VaadinFrontendUI frontend) {
        this.db = db;
        this.frontend = frontend;
        setSizeFull();
        getStyle().set("font-size", "var(--lumo-font-size-l)").set("padding", "var(--lumo-space-l)");

        H2 heading = new H2("Your invitations");
        heading.getStyle().set("font-size", "2.25rem").set("margin-bottom", "0.5em");
        add(heading);

        Paragraph hint = new Paragraph(
                "Bookings someone invited you to. Click Accept to attend, or Decline to skip.");
        hint.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "1.1rem");
        add(hint);

        invitesEmpty.getStyle().set("font-size", "1.1rem");
        buildInvitesGrid();
        add(invitesEmpty, invitesGrid);

        refresh();
    }

    private void buildInvitesGrid() {
        invitesGrid.addColumn(i -> i.getBooking().getBookingId()).setHeader("Booking").setAutoWidth(true);
        invitesGrid.addColumn(i -> i.getBooking().getUser().getUserName()).setHeader("Host").setAutoWidth(true);
        invitesGrid.addColumn(i -> i.getBooking().getRoom().getRoomName()).setHeader("Room").setAutoWidth(true);
        invitesGrid.addColumn(i -> FMT.format(i.getBooking().getTimeSlot().getStartTime()))
                .setHeader("Start").setAutoWidth(true);
        invitesGrid.addColumn(i -> FMT.format(i.getBooking().getTimeSlot().getEndTime()))
                .setHeader("End").setAutoWidth(true);
        // Show the booking-level status (PENDING / APPROVED) so the invitee can tell
        // whether the meeting is confirmed before responding.
        invitesGrid.addComponentColumn(i -> Badges.bookingStatus(i.getBooking().getStatus()))
                .setHeader("Booking").setAutoWidth(true);
        invitesGrid.addColumn(i -> i.getStatus().name()).setHeader("Your status").setAutoWidth(true);
        invitesGrid.addComponentColumn(this::buildInviteActions).setHeader("").setAutoWidth(true);
        invitesGrid.setAllRowsVisible(true);
        invitesGrid.getStyle()
                .set("font-size", "1.05rem")
                .set("--vaadin-grid-cell-padding", "1rem");
    }

    private HorizontalLayout buildInviteActions(Invitation inv) {
        Button accept = new Button("Accept", e -> respond(inv, InvitationStatus.ACCEPTED));
        accept.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        accept.getStyle().set("--lumo-size-m", "var(--lumo-size-l)");

        Button decline = new Button("Decline", e -> respond(inv, InvitationStatus.DECLINED));
        decline.addThemeVariants(ButtonVariant.LUMO_ERROR);
        decline.getStyle().set("--lumo-size-m", "var(--lumo-size-l)");

        // Decline is reversible: keep both enabled so a user who declined can
        // still change their mind, and vice versa. We just gray out the
        // current state so the active choice is obvious.
        accept.setEnabled(inv.getStatus() != InvitationStatus.ACCEPTED);
        decline.setEnabled(inv.getStatus() != InvitationStatus.DECLINED);

        HorizontalLayout row = new HorizontalLayout(accept, decline);
        row.setSpacing(true);
        return row;
    }

    private void respond(Invitation inv, InvitationStatus next) {
        User user = SessionUtil.getCurrentUser();
        if (user == null) { frontend.showError("Not logged in"); return; }
        try {
            db.updateInvitationStatus(
                    inv.getBooking().getBookingId(), user.getUserId(), next);
            frontend.showConfirmation(next == InvitationStatus.ACCEPTED
                    ? "Accepted invitation to " + inv.getBooking().getBookingId()
                    : "Declined invitation to " + inv.getBooking().getBookingId());
        } catch (IllegalStateException ex) {
            frontend.showError("Could not update invitation: " + ex.getMessage());
        }
        refresh();
    }

    private void refresh() {
        User user = SessionUtil.getCurrentUser();
        if (user == null) {
            invitesGrid.setItems(List.of());
            invitesEmpty.setVisible(true);
            invitesGrid.setVisible(false);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        // PENDING first, then by start time — surfaces the action items.
        // Hide invites for bookings that already ended or that the host cancelled/rejected.
        List<Invitation> invites = db.findInvitationsByUser(user.getUserId())
                .stream()
                .filter(i -> i.getBooking().getTimeSlot().getEndTime().isAfter(now))
                .filter(i -> {
                    BookingStatus bs = i.getBooking().getStatus();
                    return bs != BookingStatus.CANCELLED && bs != BookingStatus.REJECTED;
                })
                .sorted((a, b) -> {
                    int byStatus = Integer.compare(rank(a.getStatus()), rank(b.getStatus()));
                    if (byStatus != 0) return byStatus;
                    return a.getBooking().getTimeSlot().getStartTime()
                            .compareTo(b.getBooking().getTimeSlot().getStartTime());
                })
                .collect(Collectors.toList());
        invitesGrid.setItems(invites);
        invitesEmpty.setVisible(invites.isEmpty());
        invitesGrid.setVisible(!invites.isEmpty());
    }

    private int rank(InvitationStatus s) {
        return switch (s) {
            case PENDING  -> 0;
            case ACCEPTED -> 1;
            case DECLINED -> 2;
        };
    }
}
