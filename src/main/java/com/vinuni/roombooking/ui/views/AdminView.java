package com.vinuni.roombooking.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vinuni.roombooking.enums.BookingStatus;
import com.vinuni.roombooking.enums.RoomStatus;
import com.vinuni.roombooking.model.Admin;
import com.vinuni.roombooking.model.BookingRequest;
import com.vinuni.roombooking.model.Room;
import com.vinuni.roombooking.repository.BookingRepository;
import com.vinuni.roombooking.ui.DemoData;
import com.vinuni.roombooking.ui.MainLayout;
import com.vinuni.roombooking.ui.SessionUtil;
import com.vinuni.roombooking.ui.VaadinFrontendUI;

import java.util.ArrayList;
import java.util.Queue;

/**
 * View 5 - Admin panel. Restricted to Admin users.
 *   - Room status toggle  -> mutates Room.status (admin.addRoom/removeRoom hooks left for backend)
 *   - Force cancel         -> Admin.forceCancel(bookingId)
 *   - Override pending     -> Admin.overrideRequest(req) + BookingRepository.getPendingQueue()
 */
@Route(value = "admin", layout = MainLayout.class)
@PageTitle("Admin")
public class AdminView extends VerticalLayout implements BeforeEnterObserver {

    private final DemoData demoData;
    private final BookingRepository repository;
    private final VaadinFrontendUI frontend;

    private final Grid<Room> roomGrid = new Grid<>(Room.class, false);
    private final Grid<BookingRequest> pendingGrid = new Grid<>(BookingRequest.class, false);
    private final TextField cancelIdField = new TextField("Booking ID");

    public AdminView(DemoData demoData,
                     BookingRepository repository,
                     VaadinFrontendUI frontend) {
        this.demoData = demoData;
        this.repository = repository;
        this.frontend = frontend;
        setSizeFull();
        // Larger base font for paragraph + grid cells across the page.
        getStyle().set("font-size", "var(--lumo-font-size-l)").set("padding", "var(--lumo-space-l)");

        H2 pageTitle = new H2("Admin panel");
        pageTitle.getStyle().set("font-size", "2.25rem").set("margin-bottom", "0.5em");
        add(pageTitle);

        add(sectionHeading("Room status"));
        Paragraph rh = new Paragraph(
                "Switch a room between AVAILABLE / OCCUPIED / MAINTENANCE.");
        rh.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "1.1rem");
        add(rh);
        buildRoomGrid();
        add(roomGrid);

        add(sectionHeading("Force-cancel booking"));
        cancelIdField.setPlaceholder("Booking ID");
        cancelIdField.setWidth("320px");
        cancelIdField.getStyle().set("--lumo-size-m", "var(--lumo-size-l)");
        Button forceBtn = new Button("Force cancel", e -> forceCancel());
        forceBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        forceBtn.getStyle().set("--lumo-size-m", "var(--lumo-size-l)").set("font-weight", "500");
        HorizontalLayout cancelRow = new HorizontalLayout(cancelIdField, forceBtn);
        cancelRow.setSpacing(true);
        add(cancelRow);

        add(sectionHeading("Pending requests"));
        Paragraph ph = new Paragraph(
                "Approve or reject items from BookingRepository.getPendingQueue().");
        ph.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "1.1rem");
        add(ph);
        buildPendingGrid();
        add(pendingGrid);

        refresh();
    }

    private H3 sectionHeading(String text) {
        H3 h = new H3(text);
        h.getStyle().set("font-size", "1.5rem").set("margin-top", "1.5rem");
        return h;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!(SessionUtil.getCurrentUser() instanceof Admin)) {
            event.forwardTo("rooms");
        }
    }

    private void buildRoomGrid() {
        roomGrid.addColumn(Room::getRoomId).setHeader("ID").setAutoWidth(true);
        roomGrid.addColumn(Room::getRoomName).setHeader("Name").setAutoWidth(true);
        roomGrid.addColumn(r -> r.getAccess().name()).setHeader("Access").setAutoWidth(true);
        roomGrid.addColumn(r -> r.status.name()).setHeader("Status").setAutoWidth(true);
        roomGrid.addComponentColumn(this::buildStatusPicker)
                .setHeader("Change to").setAutoWidth(true);
        roomGrid.getStyle()
                .set("font-size", "1.05rem")
                .set("--vaadin-grid-cell-padding", "1rem");
    }

    private ComboBox<RoomStatus> buildStatusPicker(Room r) {
        ComboBox<RoomStatus> picker = new ComboBox<>();
        picker.setItems(RoomStatus.values());
        picker.setValue(r.status);
        picker.getStyle().set("--lumo-size-m", "var(--lumo-size-l)");
        picker.addValueChangeListener(e -> {
            RoomStatus next = e.getValue();
            if (next == null || next == r.status) return;
            r.status = next;
            frontend.showConfirmation("Room " + r.getRoomId() + " -> " + next.name());
            roomGrid.getDataProvider().refreshItem(r);
        });
        return picker;
    }

    private void buildPendingGrid() {
        pendingGrid.addColumn(BookingRequest::getBookingId).setHeader("ID").setAutoWidth(true);
        pendingGrid.addColumn(r -> r.getUser().getUserName()).setHeader("User").setAutoWidth(true);
        pendingGrid.addColumn(r -> r.getRoom().getRoomName()).setHeader("Room").setAutoWidth(true);
        pendingGrid.addColumn(r -> r.getTimeSlot().getStartTime().toString())
                .setHeader("Start").setAutoWidth(true);
        pendingGrid.addColumn(r -> r.getStatus().name()).setHeader("Status").setAutoWidth(true);
        pendingGrid.addComponentColumn(this::buildOverrideButtons)
                .setHeader("Override").setAutoWidth(true);
        pendingGrid.getStyle()
                .set("font-size", "1.05rem")
                .set("--vaadin-grid-cell-padding", "1rem");
    }

    private HorizontalLayout buildOverrideButtons(BookingRequest r) {
        Button approve = new Button("Approve", e -> override(r, BookingStatus.APPROVED));
        approve.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        approve.getStyle().set("--lumo-size-m", "var(--lumo-size-l)").set("font-weight", "500");
        Button reject = new Button("Reject", e -> override(r, BookingStatus.REJECTED));
        reject.addThemeVariants(ButtonVariant.LUMO_ERROR);
        reject.getStyle().set("--lumo-size-m", "var(--lumo-size-l)").set("font-weight", "500");
        HorizontalLayout row = new HorizontalLayout(approve, reject);
        row.setSpacing(true);
        return row;
    }

    private void refresh() {
        roomGrid.setItems(demoData.getRooms());
        Queue<BookingRequest> q = repository.getPendingQueue();
        pendingGrid.setItems(q == null ? new ArrayList<>() : new ArrayList<>(q));
    }

    private void forceCancel() {
        Admin admin = currentAdmin();
        if (admin == null) return;
        String id = cancelIdField.getValue();
        if (id == null || id.isBlank()) {
            frontend.showError("Enter a booking ID");
            return;
        }
        try {
            admin.forceCancel(id);
            frontend.showConfirmation("Force-cancelled " + id);
            cancelIdField.clear();
        } catch (IllegalStateException | IllegalArgumentException ex) {
            // Most common cause today: Admin.bookingRepository not wired yet (backend TODO).
            frontend.showError(ex.getMessage());
        }
        refresh();
    }

    private void override(BookingRequest req, BookingStatus desired) {
        Admin admin = currentAdmin();
        if (admin == null) return;
        // Use the 2-arg overload. The deprecated single-arg version always rejects
        // regardless of req.getStatus(), which silently broke the Approve button.
        // The 2-arg method also sets the status itself, so we don't pre-set it.
        boolean approved = desired == BookingStatus.APPROVED;
        try {
            admin.overrideRequest(req, approved);
            frontend.showConfirmation("Booking " + req.getBookingId() + " -> " + desired.name());
        } catch (IllegalStateException | IllegalArgumentException ex) {
            frontend.showError(ex.getMessage());
        }
        refresh();
    }

    private Admin currentAdmin() {
        return SessionUtil.getCurrentUser() instanceof Admin a ? a : null;
    }
}
