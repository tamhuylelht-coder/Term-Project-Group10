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
import com.vinuni.roombooking.ui.DemoData;
import com.vinuni.roombooking.ui.MainLayout;
import com.vinuni.roombooking.ui.SessionUtil;

import java.util.List;

/**
 * View 2 - Room listing. Filters by AccessLevel and Room.isAvailable().
 * "Book" button routes to BookingFormView with the chosen roomId.
 */
@Route(value = "rooms", layout = MainLayout.class)
@PageTitle("Rooms")
public class RoomListView extends VerticalLayout {

    private final DemoData demoData;
    private final Grid<Room> grid = new Grid<>(Room.class, false);
    private final ComboBox<AccessLevel> accessFilter = new ComboBox<>("Access");
    private final Checkbox availableOnly = new Checkbox("Available only", true);

    public RoomListView(DemoData demoData) {
        this.demoData = demoData;
        setSizeFull();

        add(new H2("Rooms"));

        accessFilter.setItems(AccessLevel.values());
        accessFilter.setPlaceholder("All access levels");
        accessFilter.setClearButtonVisible(true);
        accessFilter.addValueChangeListener(e -> refresh());
        availableOnly.addValueChangeListener(e -> refresh());

        HorizontalLayout filters = new HorizontalLayout(accessFilter, availableOnly);
        filters.setAlignItems(FlexComponent.Alignment.END);
        add(filters);

        grid.addColumn(Room::getRoomId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(Room::getRoomName).setHeader("Name").setAutoWidth(true);
        grid.addColumn(Room::getCapacity).setHeader("Capacity").setAutoWidth(true);
        grid.addColumn(r -> r.getAccess().name()).setHeader("Access").setAutoWidth(true);
        grid.addComponentColumn(r -> new Span(r.isAvailable() ? "Available" : r.status.name()))
                .setHeader("Status").setAutoWidth(true);
        grid.addComponentColumn(this::buildBookButton).setHeader("").setAutoWidth(true);
        grid.setSizeFull();
        add(grid);

        refresh();
    }

    private Button buildBookButton(Room room) {
        Button book = new Button("Book", e ->
                getUI().ifPresent(ui -> ui.navigate(BookingFormView.class, room.getRoomId())));
        book.setEnabled(canBook(room));
        return book;
    }

    private void refresh() {
        AccessLevel level = accessFilter.getValue();
        boolean onlyAvail = Boolean.TRUE.equals(availableOnly.getValue());
        List<Room> filtered = demoData.getRooms().stream()
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
