package com.vinuni.roombooking.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vinuni.roombooking.enums.AccessLevel;
import com.vinuni.roombooking.model.Admin;
import com.vinuni.roombooking.model.Room;
import com.vinuni.roombooking.model.Staff;
import com.vinuni.roombooking.model.Student;
import com.vinuni.roombooking.model.User;
import com.vinuni.roombooking.service.DatabaseConnector;
import com.vinuni.roombooking.ui.Badges;
import com.vinuni.roombooking.ui.MainLayout;
import com.vinuni.roombooking.ui.SessionUtil;

import java.util.Collections;
import java.util.List;

/**
 * Legacy room listing. Calendar is now the primary booking surface, so
 * {@code /rooms} forwards to {@code /calendar} on entry — the body below
 * is kept available for internal use but is never rendered for routed
 * visits.
 */
@Route(value = "rooms", layout = MainLayout.class)
@PageTitle("Rooms")
public class RoomListView extends VerticalLayout implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.forwardTo(CalendarView.class);
    }


    private final DatabaseConnector db;
    private final Grid<Room> grid = new Grid<>(Room.class, false);
    private final TextField searchField = new TextField("Search rooms");
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

        searchField.setPlaceholder("Search by name or code, e.g. A102");
        searchField.setClearButtonVisible(true);
        searchField.setWidth("360px");
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setValueChangeTimeout(150);
        searchField.getStyle().set("--lumo-size-m", "var(--lumo-size-l)");
        searchField.addValueChangeListener(e -> refresh());

        accessFilter.setItems(AccessLevel.values());
        accessFilter.setPlaceholder("All access levels");
        accessFilter.setClearButtonVisible(true);
        accessFilter.setWidth("260px");
        accessFilter.getStyle().set("--lumo-size-m", "var(--lumo-size-l)");
        accessFilter.addValueChangeListener(e -> refresh());
        availableOnly.getStyle().set("font-size", "1.05rem");
        availableOnly.addValueChangeListener(e -> refresh());

        HorizontalLayout filters = new HorizontalLayout(searchField, accessFilter, availableOnly);
        filters.setAlignItems(FlexComponent.Alignment.END);
        filters.setSpacing(true);
        filters.getStyle().set("margin-bottom", "1rem");
        add(filters);

        grid.addColumn(Room::getRoomId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(Room::getRoomName).setHeader("Name").setAutoWidth(true);
        grid.addColumn(Room::getCapacity).setHeader("Capacity").setAutoWidth(true);
        grid.addColumn(r -> r.getAccess().name()).setHeader("Access").setAutoWidth(true);
        grid.addComponentColumn(r -> Badges.roomStatus(r.getStatus()))
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
        String q = searchField.getValue();
        String needle = q == null ? "" : q.trim().toLowerCase();

        List<Room> all = db.findAllRooms();
        if (all == null) all = Collections.emptyList();
        List<Room> filtered = all.stream()
                .filter(r -> level == null || r.getAccess() == level)
                .filter(r -> !onlyAvail || r.isAvailable())
                .filter(r -> needle.isEmpty() || matches(r, needle))
                .toList();
        grid.setItems(filtered);
    }

    /** Case-insensitive substring match against name and id. Lets "A102" find
     *  "A102-Group-Discussion-Room" and "10" find rooms with id containing 10. */
    private boolean matches(Room r, String needle) {
        String name = r.getRoomName() == null ? "" : r.getRoomName().toLowerCase();
        return name.contains(needle)
                || String.valueOf(r.getRoomId()).contains(needle);
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
