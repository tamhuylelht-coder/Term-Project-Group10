package com.vinuni.roombooking.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ClientCallable;
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
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vinuni.roombooking.enums.BookingStatus;
import com.vinuni.roombooking.enums.InvitationStatus;
import com.vinuni.roombooking.model.Admin;
import com.vinuni.roombooking.model.BookingRequest;
import com.vinuni.roombooking.model.Invitation;
import com.vinuni.roombooking.model.Room;
import com.vinuni.roombooking.model.TimeSlot;
import com.vinuni.roombooking.model.User;
import com.vinuni.roombooking.service.BookingService;
import com.vinuni.roombooking.service.DatabaseConnector;
import com.vinuni.roombooking.service.RoomApprovalPolicy;
import com.vinuni.roombooking.ui.MainLayout;
import com.vinuni.roombooking.ui.SessionUtil;
import com.vinuni.roombooking.ui.VaadinFrontendUI;

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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Calendar view. Day / Week / Month modes with a left sidebar mini-month
 * and filter checkboxes. Renders the current user's hosted bookings and
 * invited meetings on a time grid, supports inspecting an event and
 * responding to invitations, and provides an in-calendar New booking dialog
 * that mirrors {@link BookingFormView}.
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

    private static final int CAL_START_HOUR = 7;
    private static final int CAL_END_HOUR = 22;
    private static final int PX_PER_HOUR = 60;
    private static final double PX_PER_MIN = PX_PER_HOUR / 60.0;

    private static final DateTimeFormatter FMT_TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FMT_LONG_DATE = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    private static final DateTimeFormatter FMT_MONTH = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final String CALENDAR_BLUE = "#2f6fbd";
    private static final String CALENDAR_BLUE_DARK = "#245aa0";
    private static final String CALENDAR_BLUE_SOFT = "rgba(47, 111, 189, 0.18)";
    private static final String CALENDAR_BLUE_SELECTION = "rgba(47, 111, 189, 0.82)";

    private enum Mode { DAY, WEEK, MONTH }

    private final DatabaseConnector db;
    private final BookingService service;
    private final VaadinFrontendUI frontend;
    private final RoomApprovalPolicy policy;

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
                        RoomApprovalPolicy policy) {
        this.db = db;
        this.service = service;
        this.frontend = frontend;
        this.policy = policy;

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
                .set("margin-left", "0.5rem");

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

        Span spacer = new Span();

        toolbar.add(newBookingBtn, todayBtn, prevBtn, nextBtn, rangeLabel, spacer, viewTabs);
        toolbar.setFlexGrow(1, spacer);
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
                .set("border-right", "1px solid var(--lumo-contrast-10pct)");

        int totalHours = CAL_END_HOUR - CAL_START_HOUR;
        for (int i = 0; i < totalHours; i++) {
            int h = CAL_START_HOUR + i;
            Div label = new Div();
            label.setText(String.format("%02d:00", h));
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
        int totalHours = CAL_END_HOUR - CAL_START_HOUR;
        int totalHeight = totalHours * PX_PER_HOUR;
        col.getStyle()
                .set("flex", "1 1 0")
                .set("position", "relative")
                .set("border-left", "1px solid var(--lumo-contrast-10pct)")
                .set("min-width", "120px")
                .set("height", totalHeight + "px")
                .set("box-sizing", "border-box")
                .set("cursor", "crosshair")
                .set("user-select", "none")
                .set("touch-action", "none");
        if (day.equals(anchorDate)) {
            col.getStyle().set("background", "var(--lumo-primary-color-10pct)");
        }

        // Hour and half-hour grid lines. Half-hour is lighter so the hour band
        // still reads as the dominant rhythm.
        for (int i = 0; i < totalHours; i++) {
            Div hourCell = new Div();
            hourCell.getStyle()
                    .set("position", "absolute")
                    .set("left", "0")
                    .set("right", "0")
                    .set("top", (i * PX_PER_HOUR) + "px")
                    .set("height", PX_PER_HOUR + "px")
                    .set("border-top", i == 0 ? "none" : "1px solid var(--lumo-contrast-10pct)")
                    .set("pointer-events", "none");
            col.add(hourCell);

            Div halfLine = new Div();
            halfLine.getStyle()
                    .set("position", "absolute")
                    .set("left", "0")
                    .set("right", "0")
                    .set("top", (i * PX_PER_HOUR + PX_PER_HOUR / 2) + "px")
                    .set("height", "0")
                    .set("border-top", "1px dashed var(--lumo-contrast-5pct)")
                    .set("pointer-events", "none");
            col.add(halfLine);
        }

        Div selection = makeSelectionBlock();
        applyStoredSelection(selection, day);
        col.add(selection);
        wireDragSelection(col, selection, day, totalHeight);

        List<EventPlacement> placements = layoutDayEvents(day, allEvents);
        for (EventPlacement p : placements) {
            col.add(makeEventBlock(p, day));
        }
        return col;
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
            chip.setText(FMT_TIME.format(e.startTime) + " " + e.booking.getRoom().getRoomName());
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
        LocalDateTime dayEnd = day.atTime(CAL_END_HOUR, 0);

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

        Div titleDiv = new Div();
        titleDiv.setText(p.event.booking.getRoom().getRoomName());
        titleDiv.getStyle()
                .set("font-weight", "600")
                .set("white-space", "nowrap")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis");
        block.add(titleDiv);

        if (height >= 32) {
            Div subDiv = new Div();
            subDiv.setText(FMT_TIME.format(p.event.startTime) + "–" + FMT_TIME.format(p.event.endTime));
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

    private void wireDragSelection(Div col, Div selection, LocalDate day, int totalHeight) {
        col.getElement().executeJs("""
                const col = this;
                const selection = $0;
                const root = $1;
                const date = $2;
                const totalHeight = $3;
                const totalMinutes = $4;
                const pxPerMinute = $5;

                if (col.__calendarDragCleanup) {
                  col.__calendarDragCleanup();
                }

                const clamp = (v, min, max) => Math.max(min, Math.min(max, v));
                const snap = minute => clamp(Math.round(minute / 15) * 15, 0, totalMinutes);
                const minuteFromPointer = e => {
                  const rect = col.getBoundingClientRect();
                  const height = rect.height || totalHeight;
                  return snap(((e.clientY - rect.top) / height) * totalMinutes);
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
                totalHeight,
                (CAL_END_HOUR - CAL_START_HOUR) * 60,
                PX_PER_MIN);
    }

    @ClientCallable
    public void storeDraggedSelection(String date, int startMinute, int endMinute) {
        if (date == null || date.isBlank()) return;
        LocalDate day = LocalDate.parse(date);
        int totalMinutes = (CAL_END_HOUR - CAL_START_HOUR) * 60;
        int start = Math.max(0, Math.min(totalMinutes, Math.min(startMinute, endMinute)));
        int end = Math.max(0, Math.min(totalMinutes, Math.max(startMinute, endMinute)));
        if (end - start < 30) {
            end = Math.min(totalMinutes, start + 30);
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
        int totalMinutes = (CAL_END_HOUR - CAL_START_HOUR) * 60;
        int start = Math.max(0, Math.min(totalMinutes, Math.min(a, b)));
        int end = Math.max(0, Math.min(totalMinutes, Math.max(a, b)));
        if (end == start) end = Math.min(totalMinutes, start + 30);
        selection.getStyle()
                .set("display", "block")
                .set("top", (start * PX_PER_MIN) + "px")
                .set("height", Math.max(30, (end - start) * PX_PER_MIN) + "px");
    }

    private void applyStoredSelection(Div selection, LocalDate day) {
        if (selectedStart == null || selectedEnd == null) return;
        LocalDateTime dayStart = day.atTime(CAL_START_HOUR, 0);
        LocalDateTime dayEnd = day.atTime(CAL_END_HOUR, 0);
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
        LocalDateTime dayEnd = day.atTime(CAL_END_HOUR, 0);

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

        content.add(detailRow("ID", b.getBookingId()));
        content.add(detailRow("Room", b.getRoom().getRoomName()));
        content.add(detailRow("Start", FMT_DATETIME.format(b.getTimeSlot().getStartTime())));
        content.add(detailRow("End", FMT_DATETIME.format(b.getTimeSlot().getEndTime())));
        content.add(detailRow("Host", b.getUser().getUserName()));
        content.add(detailRow("Status", b.getStatus().name()));
        if (event.inviteStatus != null) {
            content.add(detailRow("Your invitation", event.inviteStatus.name()));
        }
        content.add(detailRow("Attending", String.valueOf(db.countAcceptedInvitees(b.getBookingId()))));
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
        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-weight", "600")
                .set("min-width", "130px")
                .set("color", "var(--lumo-secondary-text-color)");
        Span valueSpan = new Span(value);
        HorizontalLayout row = new HorizontalLayout(labelSpan, valueSpan);
        row.setSpacing(true);
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
            frontend.showConfirmation(next == InvitationStatus.ACCEPTED
                    ? "Accepted invitation to " + bookingId
                    : "Declined invitation to " + bookingId);
        } catch (RuntimeException ex) {
            frontend.showError("Could not update invitation: " + ex.getMessage());
        }
        refresh();
    }

    // -------------------------------------------------------------------------
    // New booking dialog — mirrors BookingFormView.submit() logic.
    // -------------------------------------------------------------------------

    private void openNewBookingDialog() {
        User me = SessionUtil.getCurrentUser();
        if (me == null) { frontend.showError("Not logged in"); return; }

        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("New booking");
        dlg.setWidth("520px");

        DateTimePicker startPicker = new DateTimePicker("Start");
        DateTimePicker endPicker = new DateTimePicker("End");

        LocalDateTime defaultStart = selectedStart != null ? selectedStart : defaultStartTime();
        LocalDateTime defaultEnd = selectedEnd != null && selectedEnd.isAfter(defaultStart)
                ? selectedEnd
                : defaultStart.plusHours(1);
        startPicker.setValue(defaultStart);
        endPicker.setValue(defaultEnd);
        startPicker.setStep(Duration.ofMinutes(15));
        endPicker.setStep(Duration.ofMinutes(15));
        startPicker.setWidthFull();
        endPicker.setWidthFull();

        ComboBox<Room> roomBox = new ComboBox<>("Room");
        roomBox.setItems(db.findAllRooms());
        roomBox.setItemLabelGenerator(Room::getRoomName);
        roomBox.setRenderer(new ComponentRenderer<Component, Room>(
                room -> renderRoomOption(room, me, startPicker.getValue(), endPicker.getValue())));
        roomBox.setWidthFull();
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

        VerticalLayout form = new VerticalLayout(roomBox, startPicker, endPicker, inviteePicker);
        form.setPadding(false);
        form.setSpacing(true);
        dlg.add(form);

        Button submit = new Button("Create booking", e -> {
            Room room = roomBox.getValue();
            LocalDateTime s = startPicker.getValue();
            LocalDateTime ed = endPicker.getValue();
            Set<User> invitees = inviteePicker.getSelectedItems();

            if (room == null) { frontend.showError("Pick a room"); return; }
            if (s == null || ed == null) { frontend.showError("Pick start and end time"); return; }
            if (!ed.isAfter(s)) { frontend.showError("End must be after start"); return; }
            if (!roomAvailableFor(room, s, ed)) {
                frontend.showError("Room is occupied or unavailable for the selected time.");
                return;
            }

            int inviteeCount = (int) invitees.stream()
                    .filter(u -> u != null && !u.getUserId().equals(me.getUserId()))
                    .count();
            int required = (int) Math.ceil(room.getCapacity() * 0.5);
            if (inviteeCount < required) {
                frontend.showError("Invite at least " + required + " people for a room of "
                        + room.getCapacity() + ".");
                return;
            }

            TimeSlot slot = new TimeSlot(s, ed);
            BookingRequest req = new BookingRequest(
                    UUID.randomUUID().toString().substring(0, 8), me, room, slot);
            BookingStatus status = service.submitRequest(req, inviteeCount);
            if (status == BookingStatus.REJECTED) {
                frontend.showError("Booking rejected by validator");
                return;
            }

            int invited = 0;
            for (User invitee : invitees) {
                if (invitee == null || invitee.getUserId().equals(me.getUserId())) continue;
                try {
                    db.insertInvitation(req.getBookingId(), invitee.getUserId());
                    invited++;
                } catch (RuntimeException ex) {
                    frontend.showError("Could not invite " + invitee.getUserName()
                            + ": " + ex.getMessage());
                }
            }

            String msg = "Created booking " + req.getBookingId() + " (" + req.getStatus() + ")";
            if (invited > 0) msg += " · " + invited + " invited";
            frontend.showConfirmation(msg);
            clearSelectedSlot();
            dlg.close();
            refresh();
        });
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> dlg.close());
        dlg.getFooter().add(cancel, submit);
        dlg.open();
    }

    private LocalDateTime defaultStartTime() {
        int defaultHour = Math.max(CAL_START_HOUR,
                Math.min(CAL_END_HOUR - 1, LocalTime.now().getHour() + 1));
        return anchorDate.atTime(LocalTime.of(defaultHour, 0));
    }

    private void refreshRoomOptions(ComboBox<Room> roomBox) {
        Room selected = roomBox.getValue();
        List<Room> rooms = db.findAllRooms();
        roomBox.setItems(rooms);
        if (selected != null) {
            rooms.stream()
                    .filter(r -> r.getRoomId() == selected.getRoomId())
                    .findFirst()
                    .ifPresent(roomBox::setValue);
        }
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
        boolean available = roomAvailableFor(room, start, end);
        meta.add(statusBadge(available ? "Available" : "Occupied", available));
        meta.add(policyBadge(policy.canAutoApprove(room, user)));
        meta.getStyle().set("font-size", "0.8rem");

        option.add(name, meta);
        return option;
    }

    private Span statusBadge(String text, boolean good) {
        Span badge = new Span(text);
        badge.getStyle()
                .set("padding", "1px 6px")
                .set("border-radius", "999px")
                .set("font-size", "0.75rem")
                .set("font-weight", "600")
                .set("background", good ? "var(--lumo-success-color-10pct)" : "var(--lumo-error-color-10pct)")
                .set("color", good ? "var(--lumo-success-text-color)" : "var(--lumo-error-text-color)");
        return badge;
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
