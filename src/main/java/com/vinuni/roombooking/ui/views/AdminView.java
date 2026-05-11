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

        add(new H2("Admin panel"));

        add(new H3("Room status"));
        Paragraph rh = new Paragraph(
                "Switch a room between AVAILABLE / OCCUPIED / MAINTENANCE.");
        rh.getStyle().set("color", "var(--lumo-secondary-text-color)");
        add(rh);
        buildRoomGrid();
        add(roomGrid);

        add(new H3("Force-cancel booking"));
        cancelIdField.setPlaceholder("Booking ID");
        Button forceBtn = new Button("Force cancel", e -> forceCancel());
        forceBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        add(new HorizontalLayout(cancelIdField, forceBtn));

        add(new H3("Pending requests"));
        Paragraph ph = new Paragraph(
                "Approve or reject items from BookingRepository.getPendingQueue().");
        ph.getStyle().set("color", "var(--lumo-secondary-text-color)");
        add(ph);
        buildPendingGrid();
        add(pendingGrid);

        refresh();
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
    }

    private ComboBox<RoomStatus> buildStatusPicker(Room r) {
        ComboBox<RoomStatus> picker = new ComboBox<>();
        picker.setItems(RoomStatus.values());
        picker.setValue(r.status);
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
    }

    private HorizontalLayout buildOverrideButtons(BookingRequest r) {
        Button approve = new Button("Approve", e -> override(r, BookingStatus.APPROVED));
        approve.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        Button reject = new Button("Reject", e -> override(r, BookingStatus.REJECTED));
        reject.addThemeVariants(ButtonVariant.LUMO_ERROR);
        return new HorizontalLayout(approve, reject);
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
        admin.forceCancel(id);
        frontend.showConfirmation("Force-cancelled " + id);
        cancelIdField.clear();
        refresh();
    }

    private void override(BookingRequest req, BookingStatus desired) {
        Admin admin = currentAdmin();
        if (admin == null) return;
        req.setStatus(desired);
        admin.overrideRequest(req);
        frontend.showConfirmation("Booking " + req.getBookingId() + " -> " + desired.name());
        refresh();
    }

    private Admin currentAdmin() {
        return SessionUtil.getCurrentUser() instanceof Admin a ? a : null;
    }
}
