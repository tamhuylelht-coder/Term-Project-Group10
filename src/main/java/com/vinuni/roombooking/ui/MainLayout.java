package com.vinuni.roombooking.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HighlightConditions;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.RouterLink;
import com.vinuni.roombooking.model.Admin;
import com.vinuni.roombooking.model.User;
import com.vinuni.roombooking.ui.views.AdminView;
import com.vinuni.roombooking.ui.views.InboxView;
import com.vinuni.roombooking.ui.views.CalendarView;
import com.vinuni.roombooking.ui.views.LoginView;
import com.vinuni.roombooking.ui.views.MyBookingsView;

import java.util.Locale;
import java.util.Map;

/**
 * Top-bar layout for all authenticated views. Redirects to LoginView when
 * no user is in {@link SessionUtil}.
 */
public class MainLayout extends AppLayout implements BeforeEnterObserver {

    public MainLayout() {
        UI ui = UI.getCurrent();
        if (ui != null) ui.setLocale(Locale.ENGLISH);

        Image logo = new Image("images/vinuni-logo.png", "Room Booking");
        logo.setHeight("40px");
        logo.getStyle().set("margin-left", "1rem");

        H2 title = new H2("Room Booking");
        title.getStyle()
                .set("margin", "0 1.5rem 0 0.75rem")
                .set("font-size", "1.5rem")
                .set("font-weight", "600");

        HorizontalLayout brand = new HorizontalLayout(logo, title);
        brand.setAlignItems(FlexComponent.Alignment.CENTER);
        brand.setSpacing(false);

        HorizontalLayout nav = new HorizontalLayout(
                styledNav(new RouterLink("Calendar", CalendarView.class)),
                styledNav(new RouterLink("My Bookings", MyBookingsView.class)),
                styledNav(new RouterLink("Inbox", InboxView.class))
        );
        if (SessionUtil.getCurrentUser() instanceof Admin) {
            nav.add(buildAdminMenu());
        }
        nav.setSpacing(true);
        nav.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout header = new HorizontalLayout(brand, nav, buildUserMenu());
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.getStyle()
                .set("padding", "0.5rem 1.5rem")
                .set("font-size", "1.1rem");

        addToNavbar(header);
    }

    /** Nav link with a visible underline when its route is currently active. */
    private RouterLink styledNav(RouterLink link) {
        link.getStyle()
                .set("font-size", "1.1rem")
                .set("font-weight", "500")
                .set("padding", "0.25rem 0.5rem")
                .set("border-radius", "var(--lumo-border-radius-m)");
        link.setHighlightCondition(HighlightConditions.sameLocation());
        link.getElement().addPropertyChangeListener("highlighted", "highlighted-changed", ev -> {
            boolean active = Boolean.TRUE.equals(link.getElement().getProperty("highlighted", false));
            applyActiveStyle(link, active);
        });
        applyActiveStyle(link, false);
        return link;
    }

    private void applyActiveStyle(RouterLink link, boolean active) {
        if (active) {
            link.getStyle()
                    .set("background", "var(--lumo-primary-color-10pct)")
                    .set("color", "var(--lumo-primary-text-color)");
        } else {
            link.getStyle()
                    .set("background", "transparent")
                    .set("color", "var(--lumo-body-text-color)");
        }
    }

    /**
     * Admin entry in the top nav: a MenuBar that opens a dropdown of admin
     * sections rather than navigating on click. Each item routes to
     * /admin?section=… which AdminView reads to switch its body.
     */
    private MenuBar buildAdminMenu() {
        MenuBar bar = new MenuBar();
        bar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        bar.getStyle().set("font-size", "1.1rem");

        MenuItem trigger = bar.addItem("Admin ▾");
        trigger.getElement().getStyle()
                .set("font-size", "1.1rem")
                .set("font-weight", "500")
                .set("padding", "0.25rem 0.5rem")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("color", "var(--lumo-body-text-color)")
                .set("cursor", "pointer");

        SubMenu sub = trigger.getSubMenu();
        sub.addItem("Pending approvals", e -> navigateToAdmin("pending"));
        sub.addItem("All bookings",     e -> navigateToAdmin("all-bookings"));
        sub.addItem("Force cancel",     e -> navigateToAdmin("force-cancel"));
        sub.addItem("Rooms",            e -> navigateToAdmin("rooms"));
        sub.addItem("Users",            e -> navigateToAdmin("users"));
        return bar;
    }

    private void navigateToAdmin(String section) {
        UI ui = UI.getCurrent();
        if (ui == null) return;
        ui.navigate(AdminView.class,
                QueryParameters.simple(Map.of("section", section)));
    }

    private HorizontalLayout buildUserMenu() {
        HorizontalLayout menu = new HorizontalLayout();
        menu.setAlignItems(FlexComponent.Alignment.CENTER);
        menu.setSpacing(true);

        User user = SessionUtil.getCurrentUser();
        if (user != null) {
            Span greeting = new Span("Hello, " + user.getUserName());
            greeting.getStyle().set("font-size", "1.05rem");
            menu.add(greeting);
            Button logout = new Button("Logout", e -> {
                SessionUtil.logout();
                e.getSource().getUI().ifPresent(ui -> ui.navigate(LoginView.class));
            });
            logout.getStyle().set("font-size", "1.05rem");
            logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            menu.add(logout);
        }
        return menu;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SessionUtil.isLoggedIn()) {
            event.forwardTo(LoginView.class);
        } else {
            UI ui = event.getUI();
            if (ui != null) ui.setLocale(Locale.ENGLISH);
        }
    }
}
