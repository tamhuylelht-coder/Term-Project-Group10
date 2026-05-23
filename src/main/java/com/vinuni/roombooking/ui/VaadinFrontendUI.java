package com.vinuni.roombooking.ui;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vinuni.roombooking.model.Room;
import com.vinuni.roombooking.service.BookingService;
import com.vinuni.roombooking.service.DatabaseConnector;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Vaadin UI entry point per the UML diagram.
 *
 * The five Phase-2 views live in {@code com.vinuni.roombooking.ui.views}:
 *   1. LoginView          ("")            - User.authenticate()
 *   2. RoomListView       ("rooms")       - Room.isAvailable(), Room.getAccess()
 *   3. BookingFormView    ("book")        - BookingService.submitRequest()
 *   4. MyBookingsView     ("my-bookings") - BookingRepository.findByUser(), BookingService.cancelBooking()
 *   5. AdminView          ("admin")       - Admin.* + BookingRepository.getPendingQueue()
 *
 * This class implements {@link ComponentEventListener} per the UML so cross-cutting
 * button events from any view can be routed here, and exposes the UML's
 * renderRoomAvailability / renderBookingForm methods as Dialog-based shortcuts.
 */
@Service
public class VaadinFrontendUI implements ComponentEventListener<ClickEvent<Button>> {

    private final BookingService service;
    private final DatabaseConnector db;

    private final DatePicker calendar = new DatePicker("Pick a date");
    private final FormLayout bookingForm = new FormLayout();

    public VaadinFrontendUI(BookingService service, DatabaseConnector db) {
        this.service = service;
        this.db = db;
        calendar.setValue(LocalDate.now());
    }

    /**
     * Opens a dialog showing today's room availability snapshot.
     */
    public void renderRoomAvailability() {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("Room availability");

        List<Room> rooms = db.findAllRooms();
        dlg.add(new H3("Availability for " + calendar.getValue()));
        dlg.add(calendar);
        if (rooms == null || rooms.isEmpty()) {
            dlg.add(new Paragraph("No rooms configured."));
        } else {
            for (Room r : rooms) {
                String line = r.getRoomName() + " (cap " + r.getCapacity() + ", "
                        + r.getAccess().name() + ") - "
                        + (r.isAvailable() ? "AVAILABLE" : r.getStatus().name());
                Paragraph p = new Paragraph(line);
                p.getStyle().set("margin", "0.25rem 0");
                dlg.add(p);
            }
        }
        Button close = new Button("Close", e -> dlg.close());
        dlg.getFooter().add(close);
        dlg.open();
    }

    /**
     * Opens a dialog with a {@link FormLayout} that hosts the booking form.
     * Used by views that want a modal booking shortcut without navigating to /book.
     */
    public void renderBookingForm() {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("Quick book");
        bookingForm.removeAll();
        bookingForm.add(new Paragraph(
                "Open Calendar and click New booking to start a booking."));
        HorizontalLayout actions = new HorizontalLayout();
        Button goCalendar = new Button("Open calendar", e -> {
            dlg.close();
            UI.getCurrent().navigate("calendar");
        });
        Button close = new Button("Close", e -> dlg.close());
        actions.add(goCalendar, close);
        dlg.add(bookingForm);
        dlg.getFooter().add(actions);
        dlg.open();
    }

    /**
     * UML-mandated cross-cutting handler. Routes a button click to a sensible
     * default: an unmapped button just shows a confirmation. Views typically
     * attach their own ClickListeners instead, so this is a fallback.
     */
    @Override
    public void onComponentEvent(ClickEvent<Button> event) {
        Component src = event.getSource();
        String label = src instanceof Button b && b.getText() != null
                ? b.getText() : src.getClass().getSimpleName();
        showConfirmation("Action: " + label);
    }

    public DatePicker getCalendar() {
        return calendar;
    }

    public FormLayout getBookingForm() {
        return bookingForm;
    }

    public BookingService getBookingService() {
        return service;
    }

    /**
     * Show a green success notification at the top of the page.
     */
    public void showConfirmation(String msg) {
        Notification n = Notification.show(msg, 3000, Notification.Position.TOP_CENTER);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    /**
     * Show a red error notification at the top of the page.
     */
    public void showError(String msg) {
        Notification n = Notification.show(msg, 3000, Notification.Position.TOP_CENTER);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
