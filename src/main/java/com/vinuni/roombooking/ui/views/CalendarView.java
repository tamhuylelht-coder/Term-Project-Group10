package com.vinuni.roombooking.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vinuni.roombooking.enums.BookingStatus;
import com.vinuni.roombooking.enums.InvitationStatus;
import com.vinuni.roombooking.enums.RoomStatus;
import com.vinuni.roombooking.model.Admin;
import com.vinuni.roombooking.model.BookingRequest;
import com.vinuni.roombooking.model.Invitation;
import com.vinuni.roombooking.model.Room;
import com.vinuni.roombooking.model.TimeSlot;
import com.vinuni.roombooking.model.User;
import com.vinuni.roombooking.service.BookingService;
import com.vinuni.roombooking.service.DatabaseConnector;
import com.vinuni.roombooking.service.RoomApprovalPolicy;
import com.vinuni.roombooking.ui.Badges;
import com.vinuni.roombooking.ui.MainLayout;
import com.vinuni.roombooking.ui.SessionUtil;
import com.vinuni.roombooking.ui.VaadinFrontendUI;
import com.vinuni.roombooking.validator.BookingValidator;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Calendar view. Day / Week / Month modes with a left sidebar mini-month
 * and filter checkboxes. Renders the current user's hosted bookings and
 * invited meetings on a time grid, supports inspecting an event and
 * responding to invitations, and provides an in-calendar New booking dialog.
 * Since the legacy room-list / booking-form views were removed this is the
 * primary (and only) entry point for creating a booking.
 *
 * Visibility rules:
 *   Hosted PENDING/APPROVED  → visible (toggled by "Hosted" checkbox)
 *   Hosted CANCELLED/REJECTED → hidden
 *   Invitation ACCEPTED      → visible solid (toggled by "Accepted" checkbox)
 *   Invitation PENDING       → visible outlined (toggled by "Pending" checkbox)
 *   Invitation DECLINED      → hidden
 */
@Route(value = "calendar", layout = MainLayout.class)
@PageTitle("Calendar")
public class CalendarView extends HorizontalLayout {

    private static final int CAL_START_HOUR = 0;
    private static final int CAL_END_HOUR = 24;
    private static final int PX_PER_HOUR = 60;
    private static final double PX_PER_MIN = PX_PER_HOUR / 60.0;
    private static final int TOTAL_MINUTES = (CAL_END_HOUR - CAL_START_HOUR) * 60;
    private static final int TOTAL_HEIGHT_PX = (CAL_END_HOUR - CAL_START_HOUR) * PX_PER_HOUR;

    private static final DateTimeFormatter FMT_TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter FMT_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter FMT_LONG_DATE = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter FMT_MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final String CALENDAR_BLUE = "#2f6fbd";
    private static final String CALENDAR_BLUE_DARK = "#245aa0";
    private static final String CALENDAR_BLUE_SOFT = "rgba(47, 111, 189, 0.18)";
    private static final String CALENDAR_BLUE_SELECTION = "rgba(47, 111, 189, 0.82)";

    private enum Mode { DAY, WEEK, MONTH }

    private final DatabaseConnector db;
    private final BookingService service;
    private final VaadinFrontendUI frontend;
    private final RoomApprovalPolicy policy;
    private final BookingValidator validator;

    private Mode mode = Mode.WEEK;
    private LocalDate anchorDate = LocalDate.now();
    private YearMonth miniMonth = YearMonth.now();

    /** Set by drag-to-select on a day column; consumed by the New booking dialog. */
    private LocalDateTime selectedStart;
    private LocalDateTime selectedEnd;

    private final Checkbox showHosted = new Checkbox("Hosted", true);
    private final Checkbox showAccepted = new Checkbox("Accepted invites", true);
    private final Checkbox showPending = new Checkbox("Pending invites", true);

    private final VerticalLayout sidebar = new VerticalLayout();
    private final VerticalLayout main = new VerticalLayout();
    private final Span rangeLabel = new Span();
    private final Span miniMonthLabel = new Span();
    private final Div miniGrid = new Div();
    private final Div calendarBody = new Div();
    private final Tabs viewTabs = new Tabs();
    private final Tab dayTab = new Tab("Day");
    private final Tab weekTab = new Tab("Week");
    private final Tab monthTab = new Tab("Month");

    public CalendarView(DatabaseConnector db,
                        BookingService service,
                        VaadinFrontendUI frontend,
                        RoomApprovalPolicy policy,
                        BookingValidator validator) {
        this.db = db;
        this.service = service;
        this.frontend = frontend;
        this.policy = policy;
        this.validator = validator;

        UI ui = UI.getCurrent();
        if (ui != null) ui.setLocale(Locale.ENGLISH);

        setSizeFull();
        setSpacing(false);
        setPadding(false);
        getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("min-width", "0")
                .set("width", "100%")
                .set("height", "100%");

        buildSidebar();
        buildMain();

        add(sidebar, main);
        setFlexGrow(0, sidebar);
        setFlexGrow(1, main);

        refresh();
    }

    // -------------------------------------------------------------------------
    // Layout scaffolding
    // -------------------------------------------------------------------------

    private void buildSidebar() {
        sidebar.setSpacing(false);
        sidebar.setPadding(false);
        sidebar.setWidth("260px");
        sidebar.setHeightFull();
        sidebar.getStyle()
                .set("padding", "1rem")
                .set("border-right", "1px solid var(--lumo-contrast-10pct)")
                .set("flex-shrink", "0")
                .set("box-sizing", "border-box");

        HorizontalLayout miniNav = new HorizontalLayout();
        miniNav.setWidthFull();
        miniNav.setAlignItems(FlexComponent.Alignment.CENTER);
        miniNav.setSpacing(false);

        Button miniPrev = new Button(new Icon(VaadinIcon.CHEVRON_LEFT));
        miniPrev.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        miniPrev.addClickListener(e -> {
            miniMonth = miniMonth.minusMonths(1);
            rebuildMiniMonth();
        });

        Button miniNext = new Button(new Icon(VaadinIcon.CHEVRON_RIGHT));
        miniNext.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        miniNext.addClickListener(e -> {
            miniMonth = miniMonth.plusMonths(1);
            rebuildMiniMonth();
        });

        miniMonthLabel.getStyle()
                .set("flex", "1")
                .set("text-align", "center")
                .set("font-weight", "600");

        miniNav.add(miniPrev, miniMonthLabel, miniNext);
        miniNav.setFlexGrow(1, miniMonthLabel);

        miniGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(7, 1fr)")
                .set("gap", "2px")
                .set("margin-top", "0.5rem");

        Span filterHeader = new Span("Show");
        filterHeader.getStyle()
                .set("font-weight", "600")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("display", "block")
                .set("margin-top", "1.5rem")
                .set("margin-bottom", "0.5rem");

        showHosted.addValueChangeListener(e -> rebuildCalendarBody());
        showAccepted.addValueChangeListener(e -> rebuildCalendarBody());
        showPending.addValueChangeListener(e -> rebuildCalendarBody());

        sidebar.add(miniNav, miniGrid, filterHeader, showHosted, showAccepted, showPending);
    }

    private void buildMain() {
        main.setSpacing(false);
        main.setPadding(false);
        main.setSizeFull();
        main.getStyle()
                .set("padding", "1rem")
                .set("min-width", "0")
                .set("box-sizing", "border-box");

        HorizontalLayout toolbar = buildToolbar();
        calendarBody.setSizeFull();
        calendarBody.getStyle()
                .set("display", "block")
                .set("flex", "1 1 auto")
                .set("align-self", "stretch")
                .set("width", "100%")
                .set("height", "100%")
                .set("overflow", "auto")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("margin-top", "1rem")
                .set("min-height", "0")
                .set("box-sizing", "border-box");
        calendarBody.getElement().setAttribute("data-calendar-body", "");

        main.add(toolbar, calendarBody);
        main.setFlexGrow(0, toolbar);
        main.setFlexGrow(1, calendarBody);
    }

    private HorizontalLayout buildToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setSpacing(true);

        Button newBookingBtn = new Button("New booking", new Icon(VaadinIcon.PLUS));
        newBookingBtn.getElement().executeJs("""
                const button = this;
                const root = $0;
                if (button.__calendarNewBookingHandler) {
                  button.removeEventListener('click', button.__calendarNewBookingHandler);
                }
                button.__calendarNewBookingHandler = event => {
                  event.preventDefault();
                  event.stopPropagation();
                  const body = document.querySelector('[data-calendar-body]');
                  const slot = body && body.__selectedSlot;
                  if (slot) {
                    root.$server.openNewBookingFromClient(slot.date, slot.start, slot.end);
                  } else {
                    root.$server.openNewBookingFromClient('', -1, -1);
                  }
                };
                button.addEventListener('click', button.__calendarNewBookingHandler);
                """, getElement());
        newBookingBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button todayBtn = new Button("Today", e -> {
            clearSelectedSlot();
            anchorDate = LocalDate.now();
            miniMonth = YearMonth.now();
            refresh();
        });

        Button prevBtn = new Button(new Icon(VaadinIcon.ANGLE_LEFT));
        prevBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        prevBtn.addClickListener(e -> {
            clearSelectedSlot();
            anchorDate = switch (mode) {
                case DAY -> anchorDate.minusDays(1);
                case WEEK -> anchorDate.minusWeeks(1);
                case MONTH -> anchorDate.minusMonths(1);
            };
            miniMonth = YearMonth.from(anchorDate);
            refresh();
        });

        Button nextBtn = new Button(new Icon(VaadinIcon.ANGLE_RIGHT));
        nextBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        nextBtn.addClickListener(e -> {
            clearSelectedSlot();
            anchorDate = switch (mode) {
                case DAY -> anchorDate.plusDays(1);
                case WEEK -> anchorDate.plusWeeks(1);
                case MONTH -> anchorDate.plusMonths(1);
            };
            miniMonth = YearMonth.from(anchorDate);
            refresh();
        });

        rangeLabel.getStyle()
                .set("font-size", "1.25rem")
                .set("font-weight", "600")
                .set("color", "var(--lumo-header-text-color)")
                .set("white-space", "nowrap");

        viewTabs.add(dayTab, weekTab, monthTab);
        viewTabs.setSelectedTab(weekTab);
        viewTabs.addSelectedChangeListener(e -> {
            clearSelectedSlot();
            Tab sel = viewTabs.getSelectedTab();
            if (sel == dayTab) mode = Mode.DAY;
            else if (sel == weekTab) mode = Mode.WEEK;
            else mode = Mode.MONTH;
            refresh();
        });

        // Toolbar grouping: [primary action] · [nav group] · [centered title] · [view tabs].
        // Wrapping each group in its own HorizontalLayout keeps the spacing
        // intentional instead of one long row of widgets.
        HorizontalLayout navGroup = new HorizontalLayout(todayBtn, prevBtn, nextBtn);
        navGroup.setSpacing(true);
        navGroup.setAlignItems(FlexComponent.Alignment.CENTER);

        Span leftSpacer = new Span();
        Span rightSpacer = new Span();

        toolbar.removeAll();
        toolbar.add(newBookingBtn, navGroup, leftSpacer, rangeLabel, rightSpacer, viewTabs);
        toolbar.setFlexGrow(1, leftSpacer);
        toolbar.setFlexGrow(1, rightSpacer);
        toolbar.getStyle().set("gap", "1rem").set("padding-bottom", "0.25rem");
        return toolbar;
    }

    private void refresh() {
        rebuildMiniMonth();
        rebuildCalendarBody();
    }

    private void clearSelectedSlot() {
        selectedStart = null;
        selectedEnd = null;
        calendarBody.getElement().executeJs("""
                this.__selectedSlot = null;
                this.querySelectorAll('.calendar-selection')
                  .forEach(el => { el.style.display = 'none'; });
                """);
    }

    // -------------------------------------------------------------------------
    // Sidebar mini-month
    // -------------------------------------------------------------------------

    private void rebuildMiniMonth() {
        miniMonthLabel.setText(FMT_MONTH.format(miniMonth));
        miniGrid.removeAll();

        for (String label : new String[] {"M", "T", "W", "T", "F", "S", "S"}) {
            Span head = new Span(label);
            head.getStyle()
                    .set("text-align", "center")
                    .set("font-size", "0.75rem")
                    .set("font-weight", "600")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("padding", "0.25rem 0");
            miniGrid.add(head);
        }

        LocalDate first = miniMonth.atDay(1);
        LocalDate gridStart = first.with(DayOfWeek.MONDAY);
        if (gridStart.isAfter(first)) gridStart = gridStart.minusWeeks(1);

        for (int i = 0; i < 42; i++) {
            final LocalDate cellDate = gridStart.plusDays(i);
            Button cell = new Button(String.valueOf(cellDate.getDayOfMonth()));
            cell.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            cell.getStyle()
                    .set("min-width", "0")
                    .set("width", "100%")
                    .set("padding", "0")
                    .set("font-size", "0.8rem");

            boolean inMonth = YearMonth.from(cellDate).equals(miniMonth);
            boolean isToday = cellDate.equals(LocalDate.now());
            boolean isAnchor = cellDate.equals(anchorDate);

            if (!inMonth) {
                cell.getStyle().set("color", "var(--lumo-disabled-text-color)");
            }
            if (isAnchor) {
                cell.getStyle()
                        .set("background", "var(--lumo-primary-color)")
                        .set("color", "var(--lumo-primary-contrast-color)")
                        .set("font-weight", "700");
            } else if (isToday) {
                cell.getStyle()
                        .set("background", "var(--lumo-primary-color-10pct)")
                        .set("color", "var(--lumo-primary-text-color)")
                        .set("font-weight", "700");
            }

            cell.addClickListener(ev -> {
                clearSelectedSlot();
                anchorDate = cellDate;
                if (!YearMonth.from(cellDate).equals(miniMonth)) {
                    miniMonth = YearMonth.from(cellDate);
                }
                refresh();
            });
            miniGrid.add(cell);
        }
    }

    // -------------------------------------------------------------------------
    // Main calendar body
    // -------------------------------------------------------------------------

    private void rebuildCalendarBody() {
        calendarBody.removeAll();
        rangeLabel.setText(formatRangeLabel());
        switch (mode) {
            case DAY -> buildDayBody();
            case WEEK -> buildWeekBody();
            case MONTH -> buildMonthBody();
        }
    }

    private String formatRangeLabel() {
        return switch (mode) {
            case DAY -> FMT_LONG_DATE.format(anchorDate);
            case WEEK -> {
                LocalDate ws = anchorDate.with(DayOfWeek.MONDAY);
                LocalDate we = ws.plusDays(6);
                yield FMT_MONTH.format(ws) + " — week of " + ws.getDayOfMonth() + "–" + we.getDayOfMonth();
            }
            case MONTH -> FMT_MONTH.format(anchorDate);
        };
    }

    private void buildDayBody() {
        LocalDate day = anchorDate;
        List<CalendarEvent> events = visibleEventsForRange(day, day.plusDays(1));

        Div container = new Div();
        container.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("box-sizing", "border-box")
                .set("width", "100%")
                .set("min-width", "720px")
                .set("min-height", "100%");

        container.add(makeDayHeader(List.of(day)));

        Div body = new Div();
        body.getStyle()
                .set("display", "flex")
                .set("flex", "1 1 auto")
                .set("align-items", "stretch")
                .set("width", "100%")
                .set("min-width", "0");
        body.add(makeTimeAxis());
        body.add(makeDayColumn(day, events));

        container.add(body);
        calendarBody.add(container);
        scrollToCurrentHour();
    }

    private void buildWeekBody() {
        LocalDate weekStart = anchorDate.with(DayOfWeek.MONDAY);
        List<LocalDate> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) days.add(weekStart.plusDays(i));

        List<CalendarEvent> events = visibleEventsForRange(weekStart, weekStart.plusDays(7));

        Div container = new Div();
        container.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("box-sizing", "border-box")
                .set("width", "100%")
                .set("min-width", "1120px")
                .set("min-height", "100%");

        container.add(makeDayHeader(days));

        Div body = new Div();
        body.getStyle()
                .set("display", "flex")
                .set("flex", "1 1 auto")
                .set("align-items", "stretch")
                .set("width", "100%")
                .set("min-width", "0");
        body.add(makeTimeAxis());
        for (LocalDate d : days) body.add(makeDayColumn(d, events));

        container.add(body);
        calendarBody.add(container);
        scrollToCurrentHour();
    }

    /**
     * After re-render, nudge the scrollable calendar so "now" is roughly
     * a third of the way down. The 24-hour grid otherwise opens at midnight
     * and forces the user to scroll through eight hours of empty space.
     */
    private void scrollToCurrentHour() {
        calendarBody.getElement().executeJs("""
                const body = this;
                const minutes = $0;
                requestAnimationFrame(() => {
                  const target = Math.max(0, minutes - 180);
                  body.scrollTop = (target / 60) * $1;
                });
                """,
                LocalTime.now().getHour() * 60 + LocalTime.now().getMinute(),
                PX_PER_HOUR);
    }

    private void buildMonthBody() {
        YearMonth month = YearMonth.from(anchorDate);
        LocalDate first = month.atDay(1);
        LocalDate gridStart = first.with(DayOfWeek.MONDAY);
        if (gridStart.isAfter(first)) gridStart = gridStart.minusWeeks(1);

        LocalDate gridEnd = gridStart.plusWeeks(6);
        List<CalendarEvent> events = visibleEventsForRange(gridStart, gridEnd);
        Map<LocalDate, List<CalendarEvent>> byDay = new HashMap<>();
        for (CalendarEvent e : events) {
            LocalDate d = e.startTime.toLocalDate();
            byDay.computeIfAbsent(d, k -> new ArrayList<>()).add(e);
        }

        Div container = new Div();
        container.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(7, 1fr)")
                .set("grid-template-rows", "auto repeat(6, 1fr)")
                .set("box-sizing", "border-box")
                .set("width", "100%")
                .set("min-width", "840px")
                .set("min-height", "100%")
                .set("gap", "1px")
                .set("background", "var(--lumo-contrast-10pct)");

        DayOfWeek[] order = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY};
        for (DayOfWeek dow : order) {
            Div h = new Div();
            h.setText(dow.getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            h.getStyle()
                    .set("background", "var(--lumo-base-color)")
                    .set("padding", "0.5rem")
                    .set("font-weight", "600")
                    .set("text-align", "center")
                    .set("font-size", "0.85rem")
                    .set("color", "var(--lumo-secondary-text-color)");
            container.add(h);
        }

        for (int w = 0; w < 6; w++) {
            for (int d = 0; d < 7; d++) {
                LocalDate cellDate = gridStart.plusDays(w * 7L + d);
                container.add(makeMonthCell(cellDate, month, byDay.getOrDefault(cellDate, List.of())));
            }
        }
        calendarBody.add(container);
    }

    // -------------------------------------------------------------------------
    // Header / axis / column primitives
    // -------------------------------------------------------------------------

    private Div makeDayHeader(List<LocalDate> days) {
        Div row = new Div();
        row.getStyle()
                .set("display", "flex")
                .set("width", "100%")
                .set("border-bottom", "2px solid var(--lumo-contrast-20pct)")
                .set("position", "sticky")
                .set("top", "0")
                .set("background", "var(--lumo-base-color)")
                .set("z-index", "2");

        Div spacer = new Div();
        spacer.getStyle().set("width", "84px").set("flex-shrink", "0");
        row.add(spacer);

        for (LocalDate d : days) {
            boolean isToday = d.equals(LocalDate.now());
            boolean isSelected = d.equals(anchorDate);
            Div cell = new Div();
            cell.getStyle()
                    .set("flex", "1 1 0")
                    .set("padding", "0.5rem")
                    .set("text-align", "center")
                    .set("border-left", "1px solid var(--lumo-contrast-10pct)")
                    .set("min-width", "0")
                    .set("cursor", "pointer");
            if (isSelected) {
                cell.getStyle().set("background", "var(--lumo-primary-color-10pct)");
            }

            Span name = new Span(d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            name.getStyle()
                    .set("display", "block")
                    .set("font-size", "0.85rem")
                    .set("color", isSelected
                            ? "var(--lumo-primary-text-color)"
                            : "var(--lumo-secondary-text-color)");

            Span num = new Span(String.valueOf(d.getDayOfMonth()));
            num.getStyle()
                    .set("display", "inline-flex")
                    .set("align-items", "center")
                    .set("justify-content", "center")
                    .set("min-width", "2.1rem")
                    .set("height", "2.1rem")
                    .set("font-size", "1.5rem")
                    .set("font-weight", isToday ? "700" : "500")
                    .set("color", isToday ? "var(--lumo-primary-text-color)" : "var(--lumo-body-text-color)");
            if (isToday) {
                num.getStyle()
                        .set("background", "var(--lumo-primary-color)")
                        .set("color", "var(--lumo-primary-contrast-color)")
                        .set("border-radius", "999px");
            }

            cell.add(name, num);
            cell.addClickListener(e -> {
                clearSelectedSlot();
                anchorDate = d;
                miniMonth = YearMonth.from(d);
                if (mode != Mode.DAY) {
                    mode = Mode.DAY;
                    viewTabs.setSelectedTab(dayTab);
                }
                refresh();
            });
            row.add(cell);
        }
        return row;
    }

    private Div makeTimeAxis() {
        Div axis = new Div();
        axis.getStyle()
                .set("width", "84px")
                .set("flex-shrink", "0")
                .set("box-sizing", "border-box")
                // Match the horizontal hour-line weight so the time-axis
                // border reads as part of the same grid rather than fainter.
                .set("border-right", "1px solid rgba(0,0,0,0.18)");

        int totalHours = CAL_END_HOUR - CAL_START_HOUR;
        for (int i = 0; i < totalHours; i++) {
            int h = CAL_START_HOUR + i;
            Div label = new Div();
            label.setText(String.format(Locale.ENGLISH, "%02d:00", h));
            label.getStyle()
                    .set("height", PX_PER_HOUR + "px")
                    .set("font-size", "0.95rem")
                    .set("font-weight", "500")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("padding", "2px 0.5rem 0 0")
                    .set("text-align", "right")
                    .set("box-sizing", "border-box")
                    .set("border-top", i == 0 ? "none" : "1px solid var(--lumo-contrast-5pct)");
            axis.add(label);
        }
        return axis;
    }

    private Div makeDayColumn(LocalDate day, List<CalendarEvent> allEvents) {
        Div col = new Div();
        // Grid lines via repeating-linear-gradient — one layer for darker hour
        // lines (every PX_PER_HOUR), one for lighter half-hour lines. Background
        // is set as a separate background-color so we don't blow away the
        // gradient when highlighting the anchor day.
        String gridImage =
                "repeating-linear-gradient(to bottom,"
                + " rgba(0,0,0,0.18) 0,"
                + " rgba(0,0,0,0.18) 1px,"
                + " transparent 1px,"
                + " transparent " + PX_PER_HOUR + "px),"
                + "repeating-linear-gradient(to bottom,"
                + " rgba(0,0,0,0.08) 0,"
                + " rgba(0,0,0,0.08) 1px,"
                + " transparent 1px,"
                + " transparent " + (PX_PER_HOUR / 2) + "px)";
        col.getStyle()
                .set("flex", "1 1 0")
                .set("position", "relative")
                // Same color as the horizontal hour lines so each day cell
                // reads as a clean rectangle.
                .set("border-left", "1px solid rgba(0,0,0,0.18)")
                .set("min-width", "120px")
                .set("height", TOTAL_HEIGHT_PX + "px")
                .set("box-sizing", "border-box")
                .set("cursor", "crosshair")
                .set("user-select", "none")
                .set("touch-action", "none")
                .set("background-image", gridImage)
                .set("background-repeat", "repeat")
                .set("background-position", "0 0");
        // Use background-color (not background:) so the grid background-image stays.
        if (day.equals(anchorDate)) {
            col.getStyle().set("background-color", "var(--lumo-primary-color-10pct)");
        } else {
            col.getStyle().set("background-color", "var(--lumo-base-color)");
        }

        if (day.equals(LocalDate.now())) {
            col.add(makeNowLine());
        }

        Div selection = makeSelectionBlock();
        applyStoredSelection(selection, day);
        col.add(selection);
        wireDragSelection(col, selection, day);

        List<EventPlacement> placements = layoutDayEvents(day, allEvents);
        for (EventPlacement p : placements) {
            col.add(makeEventBlock(p, day));
        }
        return col;
    }

    /** Red horizontal line at "now" on today's column — current-time indicator. */
    private Div makeNowLine() {
        LocalTime now = LocalTime.now();
        int minutes = now.getHour() * 60 + now.getMinute();
        double top = (minutes - CAL_START_HOUR * 60) * PX_PER_MIN;
        Div line = new Div();
        line.getStyle()
                .set("position", "absolute")
                .set("left", "0")
                .set("right", "0")
                .set("top", top + "px")
                .set("height", "0")
                .set("border-top", "2px solid #d93025")
                .set("z-index", "4")
                .set("pointer-events", "none");
        Div dot = new Div();
        dot.getStyle()
                .set("position", "absolute")
                .set("left", "-5px")
                .set("top", "-5px")
                .set("width", "10px")
                .set("height", "10px")
                .set("border-radius", "999px")
                .set("background", "#d93025");
        line.add(dot);
        return line;
    }

    private Div makeMonthCell(LocalDate date, YearMonth month, List<CalendarEvent> events) {
        Div cell = new Div();
        cell.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("padding", "0.25rem")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("overflow", "hidden")
                .set("min-height", "90px");

        boolean isThisMonth = YearMonth.from(date).equals(month);
        boolean isToday = date.equals(LocalDate.now());

        Span num = new Span(String.valueOf(date.getDayOfMonth()));
        num.getStyle()
                .set("font-size", "0.85rem")
                .set("align-self", "flex-start")
                .set("padding", "1px 6px");
        if (isToday) {
            num.getStyle()
                    .set("background", "var(--lumo-primary-color)")
                    .set("color", "var(--lumo-primary-contrast-color)")
                    .set("border-radius", "50%")
                    .set("font-weight", "700");
        } else {
            num.getStyle()
                    .set("font-weight", isThisMonth ? "600" : "400")
                    .set("color", isThisMonth ? "var(--lumo-body-text-color)"
                            : "var(--lumo-disabled-text-color)");
        }
        cell.add(num);
        if (date.equals(anchorDate)) {
            cell.getStyle().set("background", "var(--lumo-primary-color-10pct)");
        }
        cell.getStyle().set("cursor", "pointer");
        cell.addClickListener(e -> {
            clearSelectedSlot();
            anchorDate = date;
            miniMonth = YearMonth.from(date);
            mode = Mode.DAY;
            viewTabs.setSelectedTab(dayTab);
            refresh();
        });

        List<CalendarEvent> sorted = new ArrayList<>(events);
        sorted.sort(Comparator.comparing(e -> e.startTime));
        int show = Math.min(3, sorted.size());
        for (int i = 0; i < show; i++) {
            CalendarEvent e = sorted.get(i);
            Div chip = new Div();
            chip.setText(FMT_TIME.format(e.startTime) + " " + eventTitle(e));
            chip.getStyle()
                    .set("font-size", "0.72rem")
                    .set("padding", "1px 4px")
                    .set("margin-top", "1px")
                    .set("border-radius", "3px")
                    .set("white-space", "nowrap")
                    .set("overflow", "hidden")
                    .set("text-overflow", "ellipsis")
                    .set("cursor", "pointer")
                    .set("box-sizing", "border-box");
            applyEventStyle(chip, e);
            chip.getElement().executeJs("""
                    this.addEventListener('pointerdown', e => e.stopPropagation());
                    this.addEventListener('click', e => e.stopPropagation());
                    """);
            chip.addClickListener(ev -> openEventDialog(e));
            cell.add(chip);
        }
        if (sorted.size() > show) {
            Span more = new Span("+" + (sorted.size() - show) + " more");
            more.getStyle()
                    .set("font-size", "0.7rem")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("padding", "1px 4px")
                    .set("margin-top", "2px");
            cell.add(more);
        }

        return cell;
    }

    // -------------------------------------------------------------------------
    // Event blocks + lane layout
    // -------------------------------------------------------------------------

    private Div makeEventBlock(EventPlacement p, LocalDate day) {
        LocalDateTime dayStart = day.atTime(CAL_START_HOUR, 0);
        LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();

        LocalDateTime evtStart = p.event.startTime.isBefore(dayStart) ? dayStart : p.event.startTime;
        LocalDateTime evtEnd = p.event.endTime.isAfter(dayEnd) ? dayEnd : p.event.endTime;

        long topMinutes = ChronoUnit.MINUTES.between(dayStart, evtStart);
        long durationMinutes = Math.max(15, ChronoUnit.MINUTES.between(evtStart, evtEnd));

        double top = topMinutes * PX_PER_MIN;
        double height = durationMinutes * PX_PER_MIN;
        double laneWidth = 100.0 / p.laneCount;
        double laneLeft = p.lane * laneWidth;

        Div block = new Div();
        block.getStyle()
                .set("position", "absolute")
                .set("top", top + "px")
                .set("height", height + "px")
                .set("left", "calc(" + laneLeft + "% + 2px)")
                .set("width", "calc(" + laneWidth + "% - 4px)")
                .set("border-radius", "4px")
                .set("padding", "3px 6px")
                .set("font-size", "0.78rem")
                .set("overflow", "hidden")
                .set("cursor", "pointer")
                .set("box-sizing", "border-box")
                .set("box-shadow", "0 1px 2px rgba(0,0,0,0.1)")
                .set("z-index", "3");

        applyEventStyle(block, p.event);

        // Title is the dominant line; the room/time read as supporting metadata
        // underneath.
        Div titleDiv = new Div();
        titleDiv.setText(eventTitle(p.event));
        titleDiv.getStyle()
                .set("font-weight", "600")
                .set("white-space", "nowrap")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis");
        block.add(titleDiv);

        if (height >= 32) {
            Div subDiv = new Div();
            subDiv.setText(p.event.booking.getRoom().getRoomName() + " · "
                    + FMT_TIME.format(p.event.startTime) + "–" + FMT_TIME.format(p.event.endTime));
            subDiv.getStyle()
                    .set("font-size", "0.7rem")
                    .set("opacity", "0.9")
                    .set("white-space", "nowrap")
                    .set("overflow", "hidden")
                    .set("text-overflow", "ellipsis");
            block.add(subDiv);
        }

        block.getElement().executeJs("""
                this.addEventListener('pointerdown', e => e.stopPropagation());
                this.addEventListener('click', e => e.stopPropagation());
                """);
        block.addClickListener(e -> openEventDialog(p.event));
        return block;
    }

    private String eventTitle(CalendarEvent e) {
        String t = e.booking.getTitle();
        if (t != null && !t.isBlank()) return t;
        return e.booking.getRoom().getRoomName();
    }

    private Div makeSelectionBlock() {
        Div selection = new Div();
        selection.addClassName("calendar-selection");
        selection.getStyle()
                .set("position", "absolute")
                .set("left", "2px")
                .set("right", "2px")
                .set("display", "none")
                .set("background", CALENDAR_BLUE_SELECTION)
                .set("border", "2px solid " + CALENDAR_BLUE)
                .set("border-radius", "4px")
                .set("box-sizing", "border-box")
                .set("pointer-events", "none")
                .set("box-shadow", "inset 0 0 0 1px rgba(255,255,255,0.28)")
                .set("z-index", "2");
        return selection;
    }

    private void wireDragSelection(Div col, Div selection, LocalDate day) {
        // Snap on 15 minutes, fixed pixel math so the blue selection's
        // top/bottom in pixels matches the minute values we hand back to Java.
        // Previously we divided by rect.height, which drifted from pxPerMinute
        // whenever the layout was slightly off — the visible box and the
        // dialog's Start/End would then disagree.
        col.getElement().executeJs("""
                const col = this;
                const selection = $0;
                const root = $1;
                const date = $2;
                const totalMinutes = $3;
                const pxPerMinute = $4;
                const totalHeight = totalMinutes * pxPerMinute;

                if (col.__calendarDragCleanup) {
                  col.__calendarDragCleanup();
                }

                const clamp = (v, min, max) => Math.max(min, Math.min(max, v));
                const snap = minute => clamp(Math.round(minute / 15) * 15, 0, totalMinutes);
                // Pure pixel-to-minute conversion: y = clientY - top, then y / pxPerMinute.
                // Uses the SAME pxPerMinute as draw() and the server-side TimeSlot math
                // so the box and the dialog can never diverge.
                const minuteFromPointer = e => {
                  const rect = col.getBoundingClientRect();
                  const y = clamp(e.clientY - rect.top, 0, totalHeight);
                  return snap(y / pxPerMinute);
                };
                const draw = (a, b) => {
                  let start = clamp(Math.min(a, b), 0, totalMinutes);
                  let end = clamp(Math.max(a, b), 0, totalMinutes);
                  if (end === start) end = clamp(start + 30, 0, totalMinutes);
                  selection.style.display = 'block';
                  selection.style.top = (start * pxPerMinute) + 'px';
                  selection.style.height = Math.max(30, (end - start) * pxPerMinute) + 'px';
                };

                let dragging = false;
                let startMinute = 0;

                const onDown = e => {
                  if (e.button !== 0) return;
                  e.preventDefault();
                  e.stopPropagation();
                  dragging = true;
                  startMinute = minuteFromPointer(e);
                  const body = col.closest('[data-calendar-body]');
                  if (body) {
                    body.querySelectorAll('.calendar-selection')
                      .forEach(el => { if (el !== selection) el.style.display = 'none'; });
                  }
                  if (col.setPointerCapture && e.pointerId !== undefined) {
                    try { col.setPointerCapture(e.pointerId); } catch (_) {}
                  }
                  draw(startMinute, startMinute + 30);
                  window.addEventListener('pointermove', onMove);
                  window.addEventListener('pointerup', onUp, { once: true });
                };
                const onMove = e => {
                  if (!dragging) return;
                  e.preventDefault();
                  draw(startMinute, minuteFromPointer(e));
                };
                const onUp = e => {
                  if (!dragging) return;
                  dragging = false;
                  const endMinute = minuteFromPointer(e);
                  let start = Math.min(startMinute, endMinute);
                  let end = Math.max(startMinute, endMinute);
                  if (end - start < 30) {
                    end = clamp(start + 30, 0, totalMinutes);
                    if (end - start < 30) start = clamp(end - 30, 0, totalMinutes);
                  }
                  draw(start, end);
                  if (col.releasePointerCapture && e.pointerId !== undefined) {
                    try { col.releasePointerCapture(e.pointerId); } catch (_) {}
                  }
                  window.removeEventListener('pointermove', onMove);
                  const selectedDetail = { date, start, end };
                  const selectedBody = col.closest('[data-calendar-body]');
                  if (selectedBody) {
                    selectedBody.__selectedSlot = selectedDetail;
                    selectedBody.dispatchEvent(new CustomEvent('calendar-slot-selected', {
                      bubbles: true,
                      detail: selectedDetail
                    }));
                  }
                  if (root && root.$server && root.$server.storeDraggedSelection) {
                    root.$server.storeDraggedSelection(date, start, end);
                  }
                };

                col.addEventListener('pointerdown', onDown);
                col.__calendarDragCleanup = () => {
                  col.removeEventListener('pointerdown', onDown);
                  window.removeEventListener('pointermove', onMove);
                  window.removeEventListener('pointerup', onUp);
                };
                """,
                selection.getElement(),
                getElement(),
                day.toString(),
                TOTAL_MINUTES,
                PX_PER_MIN);
    }

    @ClientCallable
    public void storeDraggedSelection(String date, int startMinute, int endMinute) {
        if (date == null || date.isBlank()) return;
        LocalDate day = LocalDate.parse(date);
        int start = Math.max(0, Math.min(TOTAL_MINUTES, Math.min(startMinute, endMinute)));
        int end = Math.max(0, Math.min(TOTAL_MINUTES, Math.max(startMinute, endMinute)));
        if (end - start < 30) {
            end = Math.min(TOTAL_MINUTES, start + 30);
            if (end - start < 30) start = Math.max(0, end - 30);
        }
        selectedStart = day.atTime(CAL_START_HOUR, 0).plusMinutes(start);
        selectedEnd = day.atTime(CAL_START_HOUR, 0).plusMinutes(end);
        anchorDate = day;
        miniMonth = YearMonth.from(day);
        rebuildMiniMonth();
    }

    @ClientCallable
    public void openNewBookingFromClient(String date, int startMinute, int endMinute) {
        if (date != null && !date.isBlank() && startMinute >= 0 && endMinute >= 0) {
            storeDraggedSelection(date, startMinute, endMinute);
        }
        openNewBookingDialog();
    }

    private void updateSelectionBlock(Div selection, int a, int b) {
        int start = Math.max(0, Math.min(TOTAL_MINUTES, Math.min(a, b)));
        int end = Math.max(0, Math.min(TOTAL_MINUTES, Math.max(a, b)));
        if (end == start) end = Math.min(TOTAL_MINUTES, start + 30);
        selection.getStyle()
                .set("display", "block")
                .set("top", (start * PX_PER_MIN) + "px")
                .set("height", Math.max(30, (end - start) * PX_PER_MIN) + "px");
    }

    private void applyStoredSelection(Div selection, LocalDate day) {
        if (selectedStart == null || selectedEnd == null) return;
        LocalDateTime dayStart = day.atTime(CAL_START_HOUR, 0);
        LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();
        if (!selectedEnd.isAfter(dayStart) || !selectedStart.isBefore(dayEnd)) return;
        int start = (int) ChronoUnit.MINUTES.between(dayStart,
                selectedStart.isBefore(dayStart) ? dayStart : selectedStart);
        int end = (int) ChronoUnit.MINUTES.between(dayStart,
                selectedEnd.isAfter(dayEnd) ? dayEnd : selectedEnd);
        updateSelectionBlock(selection, start, end);
    }

    private void applyEventStyle(Div block, CalendarEvent event) {
        if (event.isHost) {
            if (event.booking.getStatus() == BookingStatus.PENDING) {
                // Hosted, awaiting admin approval — orange to match Badges convention.
                block.getStyle()
                        .set("background", "hsl(28, 88%, 55%)")
                        .set("color", "white");
            } else {
                block.getStyle()
                        .set("background", CALENDAR_BLUE)
                        .set("border", "1px solid " + CALENDAR_BLUE_DARK)
                        .set("color", "white");
            }
        } else {
            if (event.inviteStatus == InvitationStatus.ACCEPTED) {
                block.getStyle()
                        .set("background", CALENDAR_BLUE)
                        .set("border", "1px solid " + CALENDAR_BLUE_DARK)
                        .set("color", "white");
            } else {
                // PENDING — outlined / dashed so it reads as "needs attention".
                block.getStyle()
                        .set("background", CALENDAR_BLUE_SOFT)
                        .set("color", CALENDAR_BLUE_DARK)
                        .set("border", "2px dashed " + CALENDAR_BLUE);
            }
        }
    }

    private List<CalendarEvent> visibleEventsForRange(LocalDate from, LocalDate to) {
        User me = SessionUtil.getCurrentUser();
        if (me == null) return List.of();

        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atStartOfDay();

        List<CalendarEvent> out = new ArrayList<>();

        if (showHosted.getValue()) {
            for (BookingRequest b : db.findBookingsByUser(me.getUserId())) {
                BookingStatus s = b.getStatus();
                if (s == BookingStatus.CANCELLED || s == BookingStatus.REJECTED) continue;
                LocalDateTime st = b.getTimeSlot().getStartTime();
                LocalDateTime et = b.getTimeSlot().getEndTime();
                if (!et.isAfter(fromDt) || !st.isBefore(toDt)) continue;
                out.add(new CalendarEvent(b, true, null));
            }
        }

        for (Invitation inv : db.findInvitationsByUser(me.getUserId())) {
            InvitationStatus is = inv.getStatus();
            if (is == InvitationStatus.DECLINED) continue;
            if (is == InvitationStatus.ACCEPTED && !showAccepted.getValue()) continue;
            if (is == InvitationStatus.PENDING && !showPending.getValue()) continue;
            BookingRequest b = inv.getBooking();
            BookingStatus bs = b.getStatus();
            if (bs == BookingStatus.CANCELLED || bs == BookingStatus.REJECTED) continue;
            LocalDateTime st = b.getTimeSlot().getStartTime();
            LocalDateTime et = b.getTimeSlot().getEndTime();
            if (!et.isAfter(fromDt) || !st.isBefore(toDt)) continue;
            out.add(new CalendarEvent(b, false, is));
        }

        return out;
    }

    /**
     * Assigns each event a lane index per-day. Events that overlap share the
     * column width; each gets a 1/laneCount slice. Lane count is computed per
     * cluster of mutually-overlapping events so non-overlapping events still
     * use the full width.
     */
    private List<EventPlacement> layoutDayEvents(LocalDate day, List<CalendarEvent> events) {
        LocalDateTime dayStart = day.atTime(CAL_START_HOUR, 0);
        LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();

        List<CalendarEvent> sorted = new ArrayList<>();
        for (CalendarEvent e : events) {
            if (e.endTime.isAfter(dayStart) && e.startTime.isBefore(dayEnd)) sorted.add(e);
        }
        sorted.sort(Comparator.comparing((CalendarEvent e) -> e.startTime).thenComparing(e -> e.endTime));
        if (sorted.isEmpty()) return List.of();

        List<EventPlacement> placements = new ArrayList<>();
        List<CalendarEvent> cluster = new ArrayList<>();
        LocalDateTime clusterEnd = null;

        for (CalendarEvent e : sorted) {
            if (cluster.isEmpty() || e.startTime.isBefore(clusterEnd)) {
                cluster.add(e);
                if (clusterEnd == null || e.endTime.isAfter(clusterEnd)) clusterEnd = e.endTime;
            } else {
                placements.addAll(layoutCluster(cluster));
                cluster = new ArrayList<>();
                cluster.add(e);
                clusterEnd = e.endTime;
            }
        }
        if (!cluster.isEmpty()) placements.addAll(layoutCluster(cluster));
        return placements;
    }

    private List<EventPlacement> layoutCluster(List<CalendarEvent> cluster) {
        List<LocalDateTime> laneEnds = new ArrayList<>();
        int[] lanes = new int[cluster.size()];
        for (int idx = 0; idx < cluster.size(); idx++) {
            CalendarEvent e = cluster.get(idx);
            int lane = -1;
            for (int i = 0; i < laneEnds.size(); i++) {
                if (!e.startTime.isBefore(laneEnds.get(i))) {
                    lane = i;
                    laneEnds.set(i, e.endTime);
                    break;
                }
            }
            if (lane == -1) {
                lane = laneEnds.size();
                laneEnds.add(e.endTime);
            }
            lanes[idx] = lane;
        }
        int laneCount = laneEnds.size();
        List<EventPlacement> out = new ArrayList<>();
        for (int i = 0; i < cluster.size(); i++) {
            out.add(new EventPlacement(cluster.get(i), lanes[i], laneCount));
        }
        return out;
    }

    // -------------------------------------------------------------------------
    // Event details dialog
    // -------------------------------------------------------------------------

    private void openEventDialog(CalendarEvent event) {
        User me = SessionUtil.getCurrentUser();
        if (me == null) { frontend.showError("Not logged in"); return; }

        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("Booking details");
        dlg.setWidth("520px");

        BookingRequest b = event.booking;
        boolean isHost = b.getUser().getUserId().equals(me.getUserId());

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(false);
        content.setPadding(false);

        String title = b.getTitle() == null || b.getTitle().isBlank()
                ? "(Untitled booking)" : b.getTitle();
        Span titleHeading = new Span(title);
        titleHeading.getStyle()
                .set("font-size", "1.2rem")
                .set("font-weight", "700")
                .set("display", "block")
                .set("margin-bottom", "0.25rem");
        content.add(titleHeading);
        if (b.getDescription() != null && !b.getDescription().isBlank()) {
            Div desc = new Div();
            desc.setText(b.getDescription());
            desc.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("margin-bottom", "0.75rem")
                    .set("white-space", "pre-wrap");
            content.add(desc);
        }

        content.add(detailRow("ID", b.getBookingId()));
        content.add(detailRow("Room", b.getRoom().getRoomName()));
        content.add(detailRow("Start", FMT_DATETIME.format(b.getTimeSlot().getStartTime())));
        content.add(detailRow("End", FMT_DATETIME.format(b.getTimeSlot().getEndTime())));
        content.add(detailRow("Host", b.getUser().getUserName()));
        content.add(detailComponentRow("Status", Badges.bookingStatus(b.getStatus())));
        if (event.inviteStatus != null) {
            content.add(detailRow("Your invitation", event.inviteStatus.name()));
        }
        int accepted = db.countAcceptedInvitees(b.getBookingId());
        int total = db.countInvitations(b.getBookingId());
        if (total > 0) {
            content.add(detailRow("Invitees accepted", accepted + " / " + total));
        } else {
            content.add(detailRow("Invitees accepted", String.valueOf(accepted)));
        }
        if (isHost) content.add(buildInviteeList(b.getBookingId()));

        dlg.add(content);

        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);

        if (!isHost && event.inviteStatus != null) {
            Button accept = new Button("Accept", e -> {
                respondToInvitation(b.getBookingId(), me.getUserId(), InvitationStatus.ACCEPTED);
                dlg.close();
            });
            accept.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            accept.setEnabled(event.inviteStatus == InvitationStatus.PENDING
                    || event.inviteStatus == InvitationStatus.DECLINED);

            Button decline = new Button("Decline", e -> {
                respondToInvitation(b.getBookingId(), me.getUserId(), InvitationStatus.DECLINED);
                dlg.close();
            });
            decline.addThemeVariants(ButtonVariant.LUMO_ERROR);
            decline.setEnabled(event.inviteStatus == InvitationStatus.PENDING
                    || event.inviteStatus == InvitationStatus.ACCEPTED);

            actions.add(accept, decline);
        }

        boolean canCancel = (isHost || me instanceof Admin)
                && b.getStatus() != BookingStatus.CANCELLED
                && b.getStatus() != BookingStatus.REJECTED;
        if (canCancel) {
            Button cancel = new Button("Cancel booking", e -> {
                boolean ok = service.cancelBooking(b.getBookingId(), me);
                if (ok) frontend.showConfirmation("Cancelled " + b.getBookingId());
                else frontend.showError("Could not cancel " + b.getBookingId());
                dlg.close();
                refresh();
            });
            cancel.addThemeVariants(ButtonVariant.LUMO_ERROR);
            actions.add(cancel);
        }

        Button close = new Button("Close", e -> dlg.close());
        actions.add(close);

        dlg.getFooter().add(actions);
        dlg.open();
    }

    private HorizontalLayout detailRow(String label, String value) {
        Span valueSpan = new Span(value == null ? "" : value);
        return detailComponentRow(label, valueSpan);
    }

    private HorizontalLayout detailComponentRow(String label, Component value) {
        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-weight", "600")
                .set("min-width", "130px")
                .set("color", "var(--lumo-secondary-text-color)");
        HorizontalLayout row = new HorizontalLayout(labelSpan, value);
        row.setSpacing(true);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.getStyle().set("padding", "0.25rem 0");
        return row;
    }

    private Div buildInviteeList(String bookingId) {
        Div container = new Div();
        container.getStyle().set("margin-top", "0.75rem");

        Span heading = new Span("Invitees");
        heading.getStyle()
                .set("font-weight", "600")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("display", "block")
                .set("margin-bottom", "0.25rem");
        container.add(heading);

        List<Invitation> invs = db.findInvitationsByBooking(bookingId);
        if (invs.isEmpty()) {
            Span empty = new Span("No invitees yet");
            empty.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("font-style", "italic");
            container.add(empty);
            return container;
        }
        for (Invitation i : invs) {
            Div row = new Div();
            row.setText(i.getInvitee().getUserName() + " — " + i.getStatus().name());
            row.getStyle()
                    .set("font-size", "0.9rem")
                    .set("padding", "0.125rem 0");
            container.add(row);
        }
        return container;
    }

    private void respondToInvitation(String bookingId, String userId, InvitationStatus next) {
        try {
            db.updateInvitationStatus(bookingId, userId, next);
            BookingStatus afterPromote = service.tryPromoteAfterInvitationResponse(bookingId);
            String msg = next == InvitationStatus.ACCEPTED
                    ? "Accepted invitation to " + bookingId
                    : "Declined invitation to " + bookingId;
            if (afterPromote == BookingStatus.APPROVED) {
                msg += " · booking is now approved";
            }
            frontend.showConfirmation(msg);
        } catch (RuntimeException ex) {
            frontend.showError("Could not update invitation: " + ex.getMessage());
        }
        refresh();
    }

    // -------------------------------------------------------------------------
    // New booking dialog — the only booking-creation surface in the app.
    // -------------------------------------------------------------------------

    private void openNewBookingDialog() {
        User me = SessionUtil.getCurrentUser();
        if (me == null) { frontend.showError("Not logged in"); return; }

        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("New booking");
        dlg.setWidth("620px");

        TextField titleField = new TextField("Title");
        titleField.setPlaceholder("Optional — e.g. Algorithms study session");
        titleField.setWidthFull();

        TextArea descField = new TextArea("Description");
        descField.setPlaceholder("Optional details — agenda, attendees, etc.");
        descField.setMaxLength(2000);
        descField.setMinHeight("72px");
        descField.setWidthFull();

        DateTimePicker startPicker = new DateTimePicker("Start");
        DateTimePicker endPicker = new DateTimePicker("End");

        // Honor the drag selection verbatim. The previous version replaced the
        // dragged time with defaultStartTime() if it wasn't strictly after now —
        // so a drag at 01:00–03:00 on today (current time 09:00) would silently
        // jump to 10:00–11:00. Now we trust whatever the user dragged; the
        // submit-time validator (and "Start must be in the future" check)
        // surfaces past-time selections instead of hiding them.
        LocalDateTime defaultStart = selectedStart != null
                ? selectedStart
                : defaultStartTime();
        LocalDateTime defaultEnd = (selectedEnd != null && selectedEnd.isAfter(defaultStart))
                ? selectedEnd
                : defaultStart.plusHours(1);
        startPicker.setValue(defaultStart);
        endPicker.setValue(defaultEnd);
        startPicker.setStep(Duration.ofMinutes(15));
        endPicker.setStep(Duration.ofMinutes(15));
        // Intentionally no setMin(now): DateTimePicker coerces the displayed
        // value silently if it's below the min, which would make the dialog
        // show a different time than the blue selection box.
        startPicker.setLocale(Locale.ENGLISH);
        endPicker.setLocale(Locale.ENGLISH);
        startPicker.setWidthFull();
        endPicker.setWidthFull();

        ComboBox<Room> roomBox = new ComboBox<>("Room");
        List<Room> roomList = db.findAllRooms();
        roomBox.setItems(query -> {
            String filter = query.getFilter().orElse("");
            return roomList.stream()
                    .filter(r -> matchesRoomSearch(r, filter))
                    .skip(query.getOffset())
                    .limit(query.getLimit());
        });
        roomBox.setItemLabelGenerator(Room::getRoomName);
        roomBox.setRenderer(new ComponentRenderer<Component, Room>(
                room -> renderRoomOption(room, me, startPicker.getValue(), endPicker.getValue())));
        roomBox.setWidthFull();
        roomBox.setHelperText("Search by name, id, access, or status");
        startPicker.addValueChangeListener(e -> refreshRoomOptions(roomBox));
        endPicker.addValueChangeListener(e -> refreshRoomOptions(roomBox));

        MultiSelectComboBox<User> inviteePicker = new MultiSelectComboBox<>("Invite people");
        inviteePicker.setItemLabelGenerator(u -> u.getUserName() + " · " + u.getUserType());
        inviteePicker.setItems(query -> {
            String filter = query.getFilter().orElse("");
            return db.searchUsers(filter, me.getUserId(), 25)
                    .stream()
                    .skip(query.getOffset())
                    .limit(query.getLimit());
        });
        inviteePicker.setWidthFull();

        // Two-column responsive form: Start/End share a row on wide dialogs;
        // everything else spans the full width. Cleaner than the previous
        // stacked VerticalLayout.
        com.vaadin.flow.component.formlayout.FormLayout form =
                new com.vaadin.flow.component.formlayout.FormLayout();
        form.setResponsiveSteps(
                new com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep("0", 1),
                new com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep("420px", 2));
        form.add(titleField, 2);
        form.add(descField, 2);
        form.add(roomBox, 2);
        form.add(startPicker, 1);
        form.add(endPicker, 1);
        form.add(inviteePicker, 2);
        form.getStyle().set("padding", "0.25rem 0").set("row-gap", "0.75rem");
        dlg.add(form);

        Button submit = new Button("Create booking", e -> {
            String titleVal = titleField.getValue();
            if (titleVal != null) titleVal = titleVal.trim();
            // Title is optional — DatabaseConnector.insertBooking defaults a
            // blank value to "Untitled booking" so the NOT NULL column is fine.
            if (titleVal != null && titleVal.isEmpty()) titleVal = null;
            String descVal = descField.getValue();
            if (descVal != null) descVal = descVal.trim();

            Room room = roomBox.getValue();
            LocalDateTime s = startPicker.getValue();
            LocalDateTime ed = endPicker.getValue();
            Set<User> selected = inviteePicker.getSelectedItems();

            if (room == null) { frontend.showError("Pick a room"); return; }
            if (s == null || ed == null) { frontend.showError("Pick start and end time"); return; }
            if (!ed.isAfter(s)) { frontend.showError("End must be after start"); return; }
            if (!s.isAfter(LocalDateTime.now())) {
                frontend.showError("Start time must be in the future.");
                return;
            }
            if (!roomAvailableFor(room, s, ed)) {
                frontend.showError("Room is occupied or unavailable for the selected time.");
                return;
            }

            // Dedup invitees by userId so the host can't accidentally invite
            // the same person twice. equals/hashCode on User makes the picker's
            // own dedup more reliable, but we belt-and-brace here.
            LinkedHashMap<String, User> unique = new LinkedHashMap<>();
            for (User u : selected) {
                if (u == null) continue;
                if (u.getUserId().equals(me.getUserId())) continue;
                unique.putIfAbsent(u.getUserId(), u);
            }
            int inviteeCount = unique.size();

            TimeSlot slot = new TimeSlot(s, ed);
            BookingRequest req = new BookingRequest(
                    UUID.randomUUID().toString().substring(0, 8), me, room, slot,
                    titleVal, descVal == null || descVal.isEmpty() ? null : descVal);

            // Run all hard validators client-side so the user gets a specific reason
            // instead of "Booking rejected by validator". Service still runs the
            // same checks as a defense-in-depth backstop.
            String reason = validator.firstFailureReason(req, inviteeCount);
            if (reason != null) { frontend.showError(reason); return; }

            BookingStatus status = service.submitRequest(req, inviteeCount);
            if (status == BookingStatus.REJECTED) {
                // Service rejected after we passed the pre-check — most likely a
                // race (someone else's conflicting booking landed first).
                frontend.showError("Booking rejected — the room may have been booked while you were filling the form.");
                return;
            }

            int invited = 0;
            for (User invitee : unique.values()) {
                try {
                    if (db.insertInvitation(req.getBookingId(), invitee.getUserId())) {
                        invited++;
                    }
                } catch (RuntimeException ex) {
                    frontend.showError("Could not invite " + invitee.getUserName()
                            + ": " + ex.getMessage());
                }
            }

            String displayTitle = (titleVal != null && !titleVal.isEmpty()) ? titleVal : "booking";
            String msg = "Created \"" + displayTitle + "\" (" + req.getStatus() + ")";
            if (invited > 0) msg += " · " + invited + " invited";
            frontend.showConfirmation(msg);
            clearSelectedSlot();
            dlg.close();
            refresh();
        });
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        // Enter inside the title field submits, Escape closes — standard dialog
        // keyboard semantics. We bind to the title field rather than the whole
        // dialog so Enter inside the description TextArea adds a newline as
        // users expect.
        submit.addClickShortcut(com.vaadin.flow.component.Key.ENTER)
                .listenOn(titleField);

        Button cancel = new Button("Cancel", e -> dlg.close());
        dlg.getFooter().add(cancel, submit);
        dlg.open();
    }

    private boolean matchesRoomSearch(Room r, String filter) {
        if (filter == null || filter.isBlank()) return true;
        String f = filter.trim().toLowerCase(Locale.ENGLISH);
        if (r.getRoomName() != null && r.getRoomName().toLowerCase(Locale.ENGLISH).contains(f)) return true;
        if (String.valueOf(r.getRoomId()).contains(f)) return true;
        if (r.getAccess() != null && r.getAccess().name().toLowerCase(Locale.ENGLISH).contains(f)) return true;
        if (r.getStatus() != null && r.getStatus().name().toLowerCase(Locale.ENGLISH).contains(f)) return true;
        return false;
    }

    private LocalDateTime defaultStartTime() {
        int defaultHour = Math.max(CAL_START_HOUR,
                Math.min(CAL_END_HOUR - 1, LocalTime.now().getHour() + 1));
        return anchorDate.atTime(LocalTime.of(defaultHour, 0));
    }

    private void refreshRoomOptions(ComboBox<Room> roomBox) {
        // Trigger the lazy callback to re-evaluate availability badges.
        Room selected = roomBox.getValue();
        roomBox.getDataProvider().refreshAll();
        if (selected != null) roomBox.setValue(selected);
    }

    private Component renderRoomOption(Room room, User user, LocalDateTime start, LocalDateTime end) {
        VerticalLayout option = new VerticalLayout();
        option.setPadding(false);
        option.setSpacing(false);
        option.getStyle().set("line-height", "1.2").set("padding", "0.25rem 0");

        Span name = new Span(room.getRoomName());
        name.getStyle().set("font-weight", "600");

        HorizontalLayout meta = new HorizontalLayout();
        meta.setSpacing(true);
        meta.setAlignItems(FlexComponent.Alignment.CENTER);
        meta.add(new Span("Cap " + room.getCapacity()));
        meta.add(new Span(policy.displayName(policy.classify(room))));
        meta.add(Badges.roomStatus(room.getStatus()));
        boolean available = roomAvailableFor(room, start, end);
        if (room.getStatus() == RoomStatus.AVAILABLE) {
            meta.add(Badges.bookingAvailability(available));
        }
        meta.add(policyBadge(policy.canAutoApprove(room, user)));
        meta.getStyle().set("font-size", "0.8rem");

        option.add(name, meta);
        return option;
    }

    private Span policyBadge(boolean autoApproved) {
        Span badge = new Span(autoApproved ? "Auto approved" : "Needs approval");
        badge.getStyle()
                .set("padding", "1px 6px")
                .set("border-radius", "999px")
                .set("font-size", "0.75rem")
                .set("font-weight", "600")
                .set("background", autoApproved ? "var(--lumo-primary-color-10pct)" : "var(--lumo-contrast-10pct)")
                .set("color", autoApproved ? "var(--lumo-primary-text-color)" : "var(--lumo-secondary-text-color)");
        return badge;
    }

    private boolean roomAvailableFor(Room room, LocalDateTime start, LocalDateTime end) {
        if (room == null || start == null || end == null || !end.isAfter(start)) return false;
        if (!room.isAvailable()) return false;
        return !db.hasRoomConflict(room.getRoomId(), start, end);
    }

    // -------------------------------------------------------------------------
    // Local value types
    // -------------------------------------------------------------------------

    private static class CalendarEvent {
        final BookingRequest booking;
        final LocalDateTime startTime;
        final LocalDateTime endTime;
        final boolean isHost;
        final InvitationStatus inviteStatus;

        CalendarEvent(BookingRequest booking, boolean isHost, InvitationStatus inviteStatus) {
            this.booking = booking;
            this.startTime = booking.getTimeSlot().getStartTime();
            this.endTime = booking.getTimeSlot().getEndTime();
            this.isHost = isHost;
            this.inviteStatus = inviteStatus;
        }
    }

    private static class EventPlacement {
        final CalendarEvent event;
        final int lane;
        final int laneCount;

        EventPlacement(CalendarEvent event, int lane, int laneCount) {
            this.event = event;
            this.lane = lane;
            this.laneCount = laneCount;
        }
    }
}
