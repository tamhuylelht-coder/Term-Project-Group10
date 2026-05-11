package com.vinuni.roombooking.ui;

import com.vinuni.roombooking.service.BookingService;

/**
 * Vaadin UI entry point. All Vaadin views extend this or wire through BookingService.
 * Frontend team (Quang Dung + Khanh An) owns this class from Phase 2 onward.
 *
 * The five views to build (in order per blueprint):
 *   1. Login / authentication
 *   2. Room listing (filter by AccessLevel)
 *   3. Booking form (TimeSlot picker + submit)
 *   4. My Bookings (list + cancel)
 *   5. Admin panel (room toggle, force cancel, override)
 */
public class VaadinFrontendUI {

    private final BookingService service;

    // Phase 2: replace with real Vaadin Calendar and Form components
    private Object calendar;    // com.vaadin.flow.component.datepicker.DatePicker
    private Object bookingForm; // com.vaadin.flow.component.formlayout.FormLayout

    public VaadinFrontendUI(BookingService service) {
        this.service = service;
    }

    /**
     * STUB — Phase 2 (Quang Dung): render room grid filtered by AccessLevel.
     * Calls room.isAvailable() and room.getAccess() on each Room in the list.
     */
    public void renderRoomAvailability() {
        // TODO (Quang Dung): build Vaadin Grid<Room>, bind isAvailable() and getAccess()
    }

    /**
     * STUB — Phase 2 (Quang Dung): show DateTimePicker + submit button.
     * On submit: call service.submitRequest(req) and route to showConfirmation/showError.
     */
    public void renderBookingForm() {
        // TODO (Quang Dung): build FormLayout, wire submit button to service.submitRequest()
    }

    /**
     * STUB — Phase 2 (Quang Dung): show a Vaadin Notification with msg.
     */
    public void showConfirmation(String msg) {
        // TODO (Quang Dung): Notification.show(msg, 3000, Position.TOP_CENTER);
        System.out.println("[CONFIRM] " + msg);
    }

    /**
     * STUB — Phase 2 (Quang Dung): show a red Vaadin Notification with msg.
     */
    public void showError(String msg) {
        // TODO (Quang Dung): Notification n = Notification.show(msg); n.addThemeVariants(ERROR);
        System.err.println("[ERROR] " + msg);
    }
}
