package com.vinuni.roombooking.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
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
import com.vinuni.roombooking.ui.Badges;
import com.vinuni.roombooking.ui.MainLayout;
import com.vinuni.roombooking.ui.SessionUtil;
import com.vinuni.roombooking.ui.VaadinFrontendUI;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * View 4 - My bookings. Reads from BookingRepository.findByUser() and offers
 * per-row Cancel via BookingService.cancelBooking().
 */
@Route(value = "my-bookings", layout = MainLayout.class)
@PageTitle("My Bookings")
public class MyBookingsView extends VerticalLayout {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BookingRepository repository;
    private final BookingService service;
    private final DatabaseConnector db;
    private final VaadinFrontendUI frontend;
    private final Grid<BookingRequest> grid = new Grid<>(BookingRequest.class, false);
    private final Paragraph emptyState = new Paragraph(
            "No bookings yet. Open the Rooms page to book one.");

    public MyBookingsView(BookingRepository repository,
                          BookingService service,
                          DatabaseConnector db,
                          VaadinFrontendUI frontend) {
        this.repository = repository;
        this.service = service;
        this.db = db;
        this.frontend = frontend;
        setSizeFull();
        getStyle().set("font-size", "var(--lumo-font-size-l)").set("padding", "var(--lumo-space-l)");

        H2 heading = new H2("My bookings");
        heading.getStyle().set("font-size", "2.25rem").set("margin-bottom", "0.5em");
        add(heading);

        emptyState.getStyle().set("font-size", "1.1rem");

        grid.addColumn(BookingRequest::getBookingId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(r -> r.getRoom().getRoomName()).setHeader("Room").setAutoWidth(true);
        grid.addColumn(r -> FMT.format(r.getTimeSlot().getStartTime()))
                .setHeader("Start").setAutoWidth(true);
        grid.addColumn(r -> FMT.format(r.getTimeSlot().getEndTime()))
                .setHeader("End").setAutoWidth(true);
        grid.addComponentColumn(r -> Badges.bookingStatus(r.getStatus()))
                .setHeader("Status").setAutoWidth(true);
        grid.addColumn(r -> db.countAcceptedInvitees(r.getBookingId())).setHeader("Attending").setAutoWidth(true);
        grid.addComponentColumn(this::buildCancelButton).setHeader("").setAutoWidth(true);
        grid.setSizeFull();
        grid.getStyle()
                .set("font-size", "1.05rem")
                .set("--vaadin-grid-cell-padding", "1rem");

        add(emptyState, grid);
        refresh();
    }

    private Button buildCancelButton(BookingRequest req) {
        Button cancel = new Button("Cancel", e -> confirmCancel(req));
        cancel.addThemeVariants(ButtonVariant.LUMO_ERROR);
        cancel.getStyle().set("--lumo-size-m", "var(--lumo-size-l)");
        boolean already = req.getStatus() == BookingStatus.CANCELLED
                       || req.getStatus() == BookingStatus.REJECTED;
        cancel.setEnabled(!already);
        return cancel;
    }

    private void refresh() {
        User user = SessionUtil.getCurrentUser();
        if (user == null) {
            grid.setItems(List.of());
            emptyState.setVisible(true);
            grid.setVisible(false);
            return;
        }
        // Hydrate the in-memory repo from DB so cancel/RSVP can find bookings
        // that were persisted in a previous app run or by another session.
        List<BookingRequest> fromDb = db.findBookingsByUser(user.getUserId());
        for (BookingRequest req : fromDb) repository.save(req);
        List<BookingRequest> mine = repository.findByUser(user.getUserId());
        grid.setItems(mine);
        emptyState.setVisible(mine.isEmpty());
        grid.setVisible(!mine.isEmpty());
    }

    private void confirmCancel(BookingRequest req) {
        ConfirmDialog dlg = new ConfirmDialog(
                "Cancel booking?",
                "Cancel booking " + req.getBookingId() + " for "
                        + req.getRoom().getRoomName() + "? This cannot be undone.",
                "Cancel booking", e -> cancel(req),
                "Keep it", e -> {});
        dlg.setConfirmButtonTheme("error primary");
        dlg.open();
    }

    private void cancel(BookingRequest req) {
        User user = SessionUtil.getCurrentUser();
        boolean ok = service.cancelBooking(req.getBookingId(), user);
        if (ok) {
            frontend.showConfirmation("Cancelled " + req.getBookingId());
        } else {
            frontend.showError("Could not cancel " + req.getBookingId());
        }
        refresh();
    }
}
