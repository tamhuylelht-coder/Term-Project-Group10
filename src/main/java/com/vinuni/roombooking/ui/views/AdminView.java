package com.vinuni.roombooking.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vinuni.roombooking.enums.AccessLevel;
import com.vinuni.roombooking.enums.BookingStatus;
import com.vinuni.roombooking.enums.RoomStatus;
import com.vinuni.roombooking.model.Admin;
import com.vinuni.roombooking.model.BookingRequest;
import com.vinuni.roombooking.model.Room;
import com.vinuni.roombooking.model.User;
import com.vinuni.roombooking.repository.BookingRepository;
import com.vinuni.roombooking.service.BookingService;
import com.vinuni.roombooking.service.DatabaseConnector;
import com.vinuni.roombooking.ui.MainLayout;
import com.vinuni.roombooking.ui.SessionUtil;
import com.vinuni.roombooking.ui.VaadinFrontendUI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * View 5 - Admin panel. Restricted to Admin users.
 *   - Room CRUD: status toggle, add room, remove room (DatabaseConnector)
 *   - Pending queue: approve / reject (Admin.overrideRequest)
 *   - Force cancel (Admin.forceCancel)
 *   - Process queue (BookingService.processQueue)
 *   - Manage users (DatabaseConnector.findAllUsers)
 */
@Route(value = "admin", layout = MainLayout.class)
@PageTitle("Admin")
public class AdminView extends VerticalLayout implements BeforeEnterObserver {

    private final DatabaseConnector db;
    private final BookingRepository repository;
    private final BookingService service;
    private final VaadinFrontendUI frontend;

    private final Grid<Room> roomGrid = new Grid<>(Room.class, false);
    private final Grid<BookingRequest> pendingGrid = new Grid<>(BookingRequest.class, false);
    private final Grid<User> userGrid = new Grid<>(User.class, false);
    private final TextField cancelIdField = new TextField("Booking ID");

    // Add-room form fields
    private final TextField newRoomName = new TextField("Room name");
    private final IntegerField newRoomCapacity = new IntegerField("Capacity");
    private final ComboBox<AccessLevel> newRoomAccess = new ComboBox<>("Access");

    public AdminView(DatabaseConnector db,
                     BookingRepository repository,
                     BookingService service,
                     VaadinFrontendUI frontend) {
        this.db = db;
        this.repository = repository;
        this.service = service;
        this.frontend = frontend;
        setSizeFull();
        getStyle().set("font-size", "var(--lumo-font-size-l)").set("padding", "var(--lumo-space-l)");

        H2 pageTitle = new H2("Admin panel");
        pageTitle.getStyle().set("font-size", "2.25rem").set("margin-bottom", "0.5em");
        add(pageTitle);

        add(sectionHeading("Rooms"));
        Paragraph rh = new Paragraph(
                "Toggle status, add a new room, or remove an existing one.");
        rh.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "1.1rem");
        add(rh);
        buildRoomGrid();
        add(roomGrid);
        add(buildAddRoomRow());

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
                "Approve or reject items individually, or process the entire queue.");
        ph.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "1.1rem");
        add(ph);
        buildPendingGrid();
        add(pendingGrid);
        Button processAll = new Button("Process queue (auto-validate all)", e -> processQueue());
        processAll.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        processAll.getStyle().set("--lumo-size-m", "var(--lumo-size-l)").set("margin-top", "0.5rem");
        add(processAll);

        add(sectionHeading("Manage user accounts"));
        Paragraph uh = new Paragraph("All registered users.");
        uh.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "1.1rem");
        add(uh);
        buildUserGrid();
        add(userGrid);

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
        roomGrid.addColumn(Room::getCapacity).setHeader("Capacity").setAutoWidth(true);
        roomGrid.addColumn(r -> r.getAccess().name()).setHeader("Access").setAutoWidth(true);
        roomGrid.addColumn(r -> r.getStatus().name()).setHeader("Status").setAutoWidth(true);
        roomGrid.addComponentColumn(this::buildStatusPicker)
                .setHeader("Change to").setAutoWidth(true);
        roomGrid.addComponentColumn(this::buildRemoveButton)
                .setHeader("Remove").setAutoWidth(true);
        roomGrid.getStyle()
                .set("font-size", "1.05rem")
                .set("--vaadin-grid-cell-padding", "1rem");
    }

    private ComboBox<RoomStatus> buildStatusPicker(Room r) {
        ComboBox<RoomStatus> picker = new ComboBox<>();
        picker.setItems(RoomStatus.values());
        picker.setValue(r.getStatus());
        picker.getStyle().set("--lumo-size-m", "var(--lumo-size-l)");
        picker.addValueChangeListener(e -> {
            RoomStatus next = e.getValue();
            if (next == null || next == r.getStatus()) return;
            try {
                db.updateRoomStatus(r.getRoomId(), next);
                r.setStatus(next);
                frontend.showConfirmation("Room " + r.getRoomId() + " -> " + next.name());
                roomGrid.getDataProvider().refreshItem(r);
            } catch (IllegalStateException ex) {
                frontend.showError(ex.getMessage());
                picker.setValue(r.getStatus());
            }
        });
        return picker;
    }

    private Button buildRemoveButton(Room r) {
        Button rm = new Button("Remove");
        rm.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        rm.getStyle().set("--lumo-size-m", "var(--lumo-size-l)");
        rm.addClickListener(e -> {
            ConfirmDialog dlg = new ConfirmDialog(
                    "Remove room?",
                    "Delete \"" + r.getRoomName() + "\" (id " + r.getRoomId() + ")? "
                            + "This will fail if active bookings reference it.",
                    "Remove", ev -> removeRoom(r),
                    "Keep it", ev -> {});
            dlg.setConfirmButtonTheme("error primary");
            dlg.open();
        });
        return rm;
    }

    private HorizontalLayout buildAddRoomRow() {
        newRoomName.setWidth("260px");
        newRoomName.getStyle().set("--lumo-size-m", "var(--lumo-size-l)");
        newRoomCapacity.setWidth("140px");
        newRoomCapacity.setMin(1);
        newRoomCapacity.setValue(10);
        newRoomCapacity.getStyle().set("--lumo-size-m", "var(--lumo-size-l)");
        newRoomAccess.setItems(AccessLevel.values());
        newRoomAccess.setValue(AccessLevel.ALL_USERS);
        newRoomAccess.setWidth("200px");
        newRoomAccess.getStyle().set("--lumo-size-m", "var(--lumo-size-l)");
        Button addBtn = new Button("Add room", e -> addRoom());
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addBtn.getStyle().set("--lumo-size-m", "var(--lumo-size-l)");
        HorizontalLayout row = new HorizontalLayout(
                newRoomName, newRoomCapacity, newRoomAccess, addBtn);
        row.setSpacing(true);
        row.getStyle().set("margin-top", "0.75rem");
        return row;
    }

    private void addRoom() {
        String name = newRoomName.getValue();
        Integer cap = newRoomCapacity.getValue();
        AccessLevel acc = newRoomAccess.getValue();
        if (name == null || name.isBlank()) {
            frontend.showError("Enter a room name"); return;
        }
        if (cap == null || cap <= 0) {
            frontend.showError("Capacity must be a positive number"); return;
        }
        if (acc == null) { frontend.showError("Pick an access level"); return; }
        // Find the next id by scanning current rooms (simple, demo-safe).
        List<Room> all = db.findAllRooms();
        int nextId = 1;
        if (all != null) {
            for (Room r : all) if (r.getRoomId() >= nextId) nextId = r.getRoomId() + 1;
        }
        Room room = new Room(nextId, name.trim(), cap, acc);
        try {
            db.insertRoom(room);
            frontend.showConfirmation("Added " + room.getRoomName() + " (id " + room.getRoomId() + ")");
            newRoomName.clear();
            refresh();
        } catch (IllegalStateException ex) {
            frontend.showError(ex.getMessage());
        }
    }

    private void removeRoom(Room r) {
        try {
            db.deleteRoom(r.getRoomId());
            frontend.showConfirmation("Removed room " + r.getRoomId());
            refresh();
        } catch (IllegalStateException ex) {
            frontend.showError("Could not remove: " + ex.getMessage());
        }
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

    private void buildUserGrid() {
        userGrid.addColumn(User::getUserId).setHeader("User ID").setAutoWidth(true);
        userGrid.addColumn(User::getUserName).setHeader("Name").setAutoWidth(true);
        userGrid.addColumn(User::getEmail).setHeader("Email").setAutoWidth(true);
        userGrid.addColumn(User::getUserType).setHeader("Role").setAutoWidth(true);
        userGrid.getStyle()
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
        List<Room> rooms = db.findAllRooms();
        roomGrid.setItems(rooms == null ? Collections.emptyList() : rooms);
        // Hydrate the in-memory queue from DB so pending items survive restarts.
        List<BookingRequest> allFromDb = db.findAllBookings();
        for (BookingRequest req : allFromDb) repository.save(req);
        List<BookingRequest> pending = new ArrayList<>();
        for (BookingRequest req : repository.getPendingQueue()) pending.add(req);
        pendingGrid.setItems(pending);
        List<User> users = db.findAllUsers();
        userGrid.setItems(users == null ? Collections.emptyList() : users);
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
            frontend.showError(ex.getMessage());
        }
        refresh();
    }

    private void override(BookingRequest req, BookingStatus desired) {
        Admin admin = currentAdmin();
        if (admin == null) return;
        boolean approved = desired == BookingStatus.APPROVED;
        try {
            admin.overrideRequest(req, approved);
            frontend.showConfirmation("Booking " + req.getBookingId() + " -> " + desired.name());
        } catch (IllegalStateException | IllegalArgumentException ex) {
            frontend.showError(ex.getMessage());
        }
        refresh();
    }

    private void processQueue() {
        try {
            int before = repository.getPendingQueue().size();
            service.processQueue();
            frontend.showConfirmation("Processed " + before + " pending request(s)");
        } catch (RuntimeException ex) {
            frontend.showError("Process queue failed: " + ex.getMessage());
        }
        refresh();
    }

    private Admin currentAdmin() {
        return SessionUtil.getCurrentUser() instanceof Admin a ? a : null;
    }
}
