package com.vinuni.roombooking.ui;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vinuni.roombooking.service.BookingService;
import org.springframework.stereotype.Service;

/**
 * Vaadin UI entry point. Acts as a cross-cutting helper for confirmation and
 * error notifications used by every @Route view in {@code ui.views}.
 *
 * The five Phase-2 views live in {@code com.vinuni.roombooking.ui.views}:
 *   1. LoginView          ("")            - User.authenticate()
 *   2. RoomListView       ("rooms")       - Room.isAvailable(), Room.getAccess()
 *   3. BookingFormView    ("book")        - BookingService.submitRequest()
 *   4. MyBookingsView     ("my-bookings") - BookingRepository.findByUser(), BookingService.cancelBooking()
 *   5. AdminView          ("admin")       - Admin.* + BookingRepository.getPendingQueue()
 */
@Service
public class VaadinFrontendUI {

    private final BookingService service;

    // Phase 2: replace with real Vaadin Calendar and Form components
    private Object calendar;    // com.vaadin.flow.component.datepicker.DatePicker
    private Object bookingForm; // com.vaadin.flow.component.formlayout.FormLayout

    public VaadinFrontendUI(BookingService service) {
        this.service = service;
    }

    /**
     * Replaced by {@code RoomListView} (@Route("rooms")). Kept for UML parity.
     */
    public void renderRoomAvailability() {
        // See ui.views.RoomListView
    }

    /**
     * Replaced by {@code BookingFormView} (@Route("book")). Kept for UML parity.
     */
    public void renderBookingForm() {
        // See ui.views.BookingFormView
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
