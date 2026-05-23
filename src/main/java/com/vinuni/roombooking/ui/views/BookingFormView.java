package com.vinuni.roombooking.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vinuni.roombooking.enums.BookingStatus;
import com.vinuni.roombooking.model.BookingRequest;
import com.vinuni.roombooking.model.Room;
import com.vinuni.roombooking.model.TimeSlot;
import com.vinuni.roombooking.model.User;
import com.vinuni.roombooking.service.BookingService;
import com.vinuni.roombooking.service.DatabaseConnector;
import com.vinuni.roombooking.ui.MainLayout;
import com.vinuni.roombooking.ui.SessionUtil;
import com.vinuni.roombooking.ui.VaadinFrontendUI;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

/**
 * View 3 - Booking form. Time slot picker, invitee picker (Outlook-style),
 * and submit. Calls BookingService.submitRequest() and routes to My Bookings
 * on success. Invitees the host picked get rows written to the invitations
 * table with status PENDING; they'll see an Accept / Decline pair in their
 * Browse Bookings view.
 *
 * URL: /book/{roomId}
 */
@Route(value = "book", layout = MainLayout.class)
@PageTitle("Book a room")
public class BookingFormView extends VerticalLayout implements HasUrlParameter<Integer> {

    /** Max suggestions surfaced by the invitee autocomplete. */
    private static final int INVITEE_PICKER_PAGE = 25;

    private final DatabaseConnector db;
    private final BookingService service;
    private final VaadinFrontendUI frontend;

    private final H2 heading = new H2();
    private final Paragraph subtitle = new Paragraph();
    private final DateTimePicker startPicker = new DateTimePicker("Start");
    private final DateTimePicker endPicker = new DateTimePicker("End");
    private final MultiSelectComboBox<User> inviteePicker =
            new MultiSelectComboBox<>("Invite people");
    private final Button submitBtn = new Button("Submit booking");

    private Room room;

    public BookingFormView(DatabaseConnector db, BookingService service, VaadinFrontendUI frontend) {
        this.db = db;
        this.service = service;
        this.frontend = frontend;

        // Larger base font for labels / picker text / paragraph.
        getStyle().set("font-size", "var(--lumo-font-size-l)").set("padding", "var(--lumo-space-l)");

        heading.getStyle().set("font-size", "2.25rem").set("margin-bottom", "0.25em");
        subtitle.getStyle().set("font-size", "1.1rem").set("margin-bottom", "1.5rem");

        LocalDateTime baseline = LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.HOURS);
        // Anchor min to today's midnight so the 30-minute step lands on clean :00 / :30 slots
        // (anchoring to LocalDateTime.now() would offset the dropdown by the current minute).
        LocalDateTime dayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        startPicker.setValue(baseline);
        endPicker.setValue(baseline.plusHours(1));
        startPicker.setMin(dayStart);
        endPicker.setMin(dayStart);
        startPicker.setStep(Duration.ofMinutes(15));
        endPicker.setStep(Duration.ofMinutes(15));
        // Wider pickers + taller controls so date/time read clearly.
        startPicker.setWidth("320px");
        endPicker.setWidth("320px");
        startPicker.getStyle().set("--lumo-size-m", "var(--lumo-size-l)");
        endPicker.getStyle().set("--lumo-size-m", "var(--lumo-size-l)");

        configureInviteePicker();

        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitBtn.getStyle()
                .set("--lumo-size-m", "var(--lumo-size-l)")
                .set("font-size", "1.1rem")
                .set("font-weight", "600");
        submitBtn.addClickListener(e -> submit());

        Button back = new Button("Back to rooms",
                e -> getUI().ifPresent(ui -> ui.navigate("rooms")));
        back.getStyle().set("--lumo-size-m", "var(--lumo-size-l)");

        HorizontalLayout pickerRow = new HorizontalLayout(startPicker, endPicker);
        pickerRow.setSpacing(true);
        HorizontalLayout buttonRow = new HorizontalLayout(submitBtn, back);
        buttonRow.setSpacing(true);
        buttonRow.getStyle().set("margin-top", "1rem");

        add(heading, subtitle, pickerRow, inviteePicker, buttonRow);
    }

    /**
     * Lazy-loads users from DB as the host types in the invitee picker.
     * MultiSelectComboBox's fetch callback runs once per keystroke (after the
     * built-in debounce) so the dropdown reflects whatever's in the users
     * table without bulk-loading everyone up front.
     */
    private void configureInviteePicker() {
        inviteePicker.setPlaceholder("Type a name…");
        inviteePicker.setWidthFull();
        inviteePicker.getStyle().set("--lumo-size-m", "var(--lumo-size-l)");
        inviteePicker.setItemLabelGenerator(u ->
                u.getUserName() + "  ·  " + u.getUserType());
        inviteePicker.setItems(query -> {
            String filter = query.getFilter().orElse("");
            String selfId = currentUserId();
            return db.searchUsers(filter, selfId, INVITEE_PICKER_PAGE)
                    .stream()
                    .skip(query.getOffset())
                    .limit(query.getLimit());
        });
    }

    private String currentUserId() {
        User u = SessionUtil.getCurrentUser();
        return u == null ? "" : u.getUserId();
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter Integer roomId) {
        if (roomId == null) {
            heading.setText("No room selected");
            subtitle.setText("Open the Rooms page and click Book on a room.");
            submitBtn.setEnabled(false);
            return;
        }
        this.room = db.findRoomById(roomId);
        if (room == null) {
            heading.setText("Room " + roomId + " not found");
            submitBtn.setEnabled(false);
            return;
        }
        heading.setText("Book - " + room.getRoomName());
        subtitle.setText("Access: " + room.getAccess()
                + "   Capacity: " + room.getCapacity());
        submitBtn.setEnabled(true);
    }

    private void submit() {
        User user = SessionUtil.getCurrentUser();
        if (user == null) { frontend.showError("Not logged in"); return; }
        if (room == null) { frontend.showError("No room selected"); return; }
        if (!room.isAvailable()) {
            frontend.showError("Room is " + room.getStatus().name() + " - cannot be booked.");
            return;
        }

        LocalDateTime s = startPicker.getValue();
        LocalDateTime e = endPicker.getValue();
        if (s == null || e == null) { frontend.showError("Pick start and end time"); return; }
        if (!e.isAfter(s))           { frontend.showError("End must be after start"); return; }

        Set<User> invitees = inviteePicker.getSelectedItems();
        int inviteeCount = (int) invitees.stream()
                .filter(u -> u != null && !u.getUserId().equals(user.getUserId()))
                .count();

        // Minimum-participation pre-check (same rule the validator enforces in
        // the service). Done here for a clearer message than "REJECTED".
        int required = (int) Math.ceil(room.getCapacity() * 0.5);
        if (inviteeCount < required) {
            frontend.showError("Invite at least " + required + " people for a room of "
                    + room.getCapacity() + ".");
            return;
        }

        TimeSlot slot = new TimeSlot(s, e);
        BookingRequest req = new BookingRequest(
                UUID.randomUUID().toString().substring(0, 8), user, room, slot);

        BookingStatus status = service.submitRequest(req, inviteeCount);

        if (status == BookingStatus.REJECTED) {
            frontend.showError("Booking rejected by validator");
            return;
        }

        int invited = 0;
        for (User invitee : invitees) {
            // Defensive: searchUsers already filters out self, but the picker may
            // hold cached selections if the host edits this form across sessions.
            if (invitee == null || invitee.getUserId().equals(user.getUserId())) continue;
            try {
                db.insertInvitation(req.getBookingId(), invitee.getUserId());
                invited++;
            } catch (RuntimeException ex) {
                // One bad invite shouldn't roll back the whole booking. Surface
                // the failure but keep going for the rest.
                frontend.showError("Could not invite " + invitee.getUserName()
                        + ": " + ex.getMessage());
            }
        }

        String msg = "Submitted - booking " + req.getBookingId()
                + " (" + req.getStatus() + ")";
        if (invited > 0) msg += " · " + invited + " invited";
        frontend.showConfirmation(msg);
        getUI().ifPresent(ui -> ui.navigate("my-bookings"));
    }
}
