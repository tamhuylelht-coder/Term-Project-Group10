package com.vinuni.roombooking.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vinuni.roombooking.enums.AccessLevel;
import com.vinuni.roombooking.model.Admin;
import com.vinuni.roombooking.model.Room;
import com.vinuni.roombooking.model.Staff;
import com.vinuni.roombooking.model.Student;
import com.vinuni.roombooking.model.User;
import com.vinuni.roombooking.service.DatabaseConnector;
import com.vinuni.roombooking.ui.MainLayout;
import com.vinuni.roombooking.ui.SessionUtil;

import java.util.Collections;
import java.util.List;

/**
 * View 2 - Room listing. Filters by AccessLevel and Room.isAvailable().
 * "Book" button routes to BookingFormView with the chosen roomId.
 */
@Route(value = "rooms", layout = MainLayout.class)
@PageTitle("Rooms")
public class RoomListView extends VerticalLayout {

    private final DatabaseConnector db;
    private final Grid<Room> grid = new Grid<>(Room.class, false);
    private final ComboBox<AccessLevel> accessFilter = new ComboBox<>("Access");
    private final Checkbox availableOnly = new Checkbox("Available only", true);

    public RoomListView(DatabaseConnector db) {
        this.db = db;
        setSizeFull();
        // Larger base font for filters, grid cells, etc.
        getStyle().set("font-size", "var(--lumo-font-size-l)").set("padding", "var(--lumo-space-l)");

        H2 heading = new H2("Rooms");
        heading.getStyle().set("font-size", "2.25rem").set("margin-bottom", "0.5em");
        add(heading);

        accessFilter.setItems(AccessLevel.values());
        accessFilter.setPlaceholder("All access levels");
        accessFilter.setClearButtonVisible(true);
        accessFilter.setWidth("260px");
        accessFilter.getStyle().set("--lumo-size-m", "var(--lumo-size-l)");
        accessFilter.addValueChangeListener(e -> refresh());
        availableOnly.getStyle().set("font-size", "1.05rem");
        availableOnly.addValueChangeListener(e -> refresh());

        HorizontalLayout filters = new HorizontalLayout(accessFilter, availableOnly);
        filters.setAlignItems(FlexComponent.Alignment.END);
        filters.setSpacing(true);
        filters.getStyle().set("margin-bottom", "1rem");
        add(filters);

        grid.addColumn(Room::getRoomId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(Room::getRoomName).setHeader("Name").setAutoWidth(true);
        grid.addColumn(Room::getCapacity).setHeader("Capacity").setAutoWidth(true);
        grid.addColumn(r -> r.getAccess().name()).setHeader("Access").setAutoWidth(true);
        grid.addComponentColumn(r -> new Span(r.isAvailable() ? "Available" : r.getStatus().name()))
                .setHeader("Status").setAutoWidth(true);
        grid.addComponentColumn(this::buildBookButton).setHeader("").setAutoWidth(true);
        grid.setSizeFull();
        // Taller rows + larger cell font for easier reading.
        grid.getStyle()
                .set("font-size", "1.05rem")
                .set("--vaadin-grid-cell-padding", "1rem");
        add(grid);

        refresh();
    }

    private Button buildBookButton(Room room) {
        Button book = new Button("Book", e ->
                getUI().ifPresent(ui -> ui.navigate(BookingFormView.class, room.getRoomId())));
        book.getStyle().set("--lumo-size-m", "var(--lumo-size-l)").set("font-weight", "500");
        boolean allowed = canBook(room);
        boolean available = room.isAvailable();
        book.setEnabled(allowed && available);
        if (!allowed) {
            book.getElement().setAttribute("title",
                    "Your role can't book a " + room.getAccess().name() + " room.");
        } else if (!available) {
            book.getElement().setAttribute("title",
                    "Room is currently " + room.getStatus().name() + ".");
        }
        return book;
    }

    private void refresh() {
        AccessLevel level = accessFilter.getValue();
        boolean onlyAvail = Boolean.TRUE.equals(availableOnly.getValue());
        List<Room> all = db.findAllRooms();
        if (all == null) all = Collections.emptyList();
        List<Room> filtered = all.stream()
                .filter(r -> level == null || r.getAccess() == level)
                .filter(r -> !onlyAvail || r.isAvailable())
                .toList();
        grid.setItems(filtered);
    }

    private boolean canBook(Room r) {
        User u = SessionUtil.getCurrentUser();
        if (u == null) return false;
        if (u instanceof Admin) return true;
        AccessLevel a = r.getAccess();
        if (a == AccessLevel.ALL_USERS) return true;
        if (a == AccessLevel.STUDENT_ONLY) return u instanceof Student;
        if (a == AccessLevel.STAFF_ONLY) return u instanceof Staff;
        return false;
    }
}
