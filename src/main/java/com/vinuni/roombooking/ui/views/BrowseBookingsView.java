package com.vinuni.roombooking.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vinuni.roombooking.enums.BookingStatus;
import com.vinuni.roombooking.model.BookingRequest;
import com.vinuni.roombooking.model.User;
import com.vinuni.roombooking.repository.BookingRepository;
import com.vinuni.roombooking.service.BookingService;
import com.vinuni.roombooking.service.DatabaseConnector;
import com.vinuni.roombooking.ui.MainLayout;
import com.vinuni.roombooking.ui.SessionUtil;
import com.vinuni.roombooking.ui.VaadinFrontendUI;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * View 6 - Browse approved bookings hosted by other users and RSVP to attend.
 * Calls BookingService.addRsvp() and DatabaseConnector.insertRsvp() so RSVP
 * counts survive a restart.
 */
@Route(value = "browse", layout = MainLayout.class)
@PageTitle("Browse bookings")
public class BrowseBookingsView extends VerticalLayout {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BookingRepository repository;
    private final BookingService service;
    private final DatabaseConnector db;
    private final VaadinFrontendUI frontend;
    private final Grid<BookingRequest> grid = new Grid<>(BookingRequest.class, false);
    private final Paragraph emptyState = new Paragraph(
            "Nothing to RSVP to yet. Approved bookings from other users will show up here.");

    public BrowseBookingsView(BookingRepository repository,
                              BookingService service,
                              DatabaseConnector db,
                              VaadinFrontendUI frontend) {
        this.repository = repository;
        this.service = service;
        this.db = db;
        this.frontend = frontend;
        setSizeFull();
        getStyle().set("font-size", "var(--lumo-font-size-l)").set("padding", "var(--lumo-space-l)");

        H2 heading = new H2("Browse bookings");
        heading.getStyle().set("font-size", "2.25rem").set("margin-bottom", "0.5em");
        add(heading);

        Paragraph hint = new Paragraph(
                "RSVP to approved bookings from other users to mark yourself as attending.");
        hint.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "1.1rem");
        add(hint);

        emptyState.getStyle().set("font-size", "1.1rem");

        grid.addColumn(BookingRequest::getBookingId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(r -> r.getUser().getUserName()).setHeader("Host").setAutoWidth(true);
        grid.addColumn(r -> r.getRoom().getRoomName()).setHeader("Room").setAutoWidth(true);
        grid.addColumn(r -> FMT.format(r.getTimeSlot().getStartTime()))
                .setHeader("Start").setAutoWidth(true);
        grid.addColumn(r -> FMT.format(r.getTimeSlot().getEndTime()))
                .setHeader("End").setAutoWidth(true);
        grid.addColumn(r -> db.countRsvps(r.getBookingId())).setHeader("RSVPs").setAutoWidth(true);
        grid.addComponentColumn(this::buildRsvpButton).setHeader("").setAutoWidth(true);
        grid.setSizeFull();
        grid.getStyle()
                .set("font-size", "1.05rem")
                .set("--vaadin-grid-cell-padding", "1rem");

        add(emptyState, grid);
        refresh();
    }

    private Button buildRsvpButton(BookingRequest req) {
        Button b = new Button("RSVP", e -> rsvp(req));
        b.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        b.getStyle().set("--lumo-size-m", "var(--lumo-size-l)");
        return b;
    }

    private void refresh() {
        User user = SessionUtil.getCurrentUser();
        if (user == null) {
            grid.setItems(List.of());
            emptyState.setVisible(true);
            grid.setVisible(false);
            return;
        }
        // Hydrate from DB so addRsvp can find the booking in the in-memory repo.
        List<BookingRequest> all = db.findAllBookings();
        for (BookingRequest req : all) repository.save(req);

        List<BookingRequest> browsable = all.stream()
                .filter(r -> r.getStatus() == BookingStatus.APPROVED)
                .filter(r -> !r.getUser().getUserId().equals(user.getUserId()))
                .toList();
        grid.setItems(browsable);
        emptyState.setVisible(browsable.isEmpty());
        grid.setVisible(!browsable.isEmpty());
    }

    private void rsvp(BookingRequest req) {
        User user = SessionUtil.getCurrentUser();
        if (user == null) { frontend.showError("Not logged in"); return; }
        try {
            // Guard against duplicates that the in-memory rsvpList misses after
            // a restart (it always starts empty, so service.addRsvp would let a
            // re-RSVP through even though the DB already has the row).
            if (db.hasRsvp(req.getBookingId(), user.getUserId())) {
                frontend.showError("You've already RSVPed to " + req.getBookingId());
                return;
            }
            boolean added = service.addRsvp(req.getBookingId(), user);
            if (!added) {
                frontend.showError("You've already RSVPed to " + req.getBookingId());
                return;
            }
            db.insertRsvp(req.getBookingId(), user.getUserId());
            frontend.showConfirmation("RSVPed to " + req.getBookingId());
        } catch (IllegalStateException ex) {
            // Defensive fallback: if a race let two writers through, the second
            // insert may still fail (no unique constraint today, but possible
            // future migration). Surface it as a normal user-facing error.
            frontend.showError("RSVP not recorded: " + ex.getMessage());
        }
        refresh();
    }
}
