package com.vinuni.roombooking.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLink;
import com.vinuni.roombooking.model.Admin;
import com.vinuni.roombooking.model.User;
import com.vinuni.roombooking.ui.views.AdminView;
import com.vinuni.roombooking.ui.views.LoginView;
import com.vinuni.roombooking.ui.views.MyBookingsView;
import com.vinuni.roombooking.ui.views.RoomListView;

/**
 * Top-bar layout for all authenticated views. Redirects to LoginView when
 * no user is in {@link SessionUtil}.
 */
public class MainLayout extends AppLayout implements BeforeEnterObserver {

    public MainLayout() {
        H2 title = new H2("VinUni Room Booking");
        title.getStyle()
                .set("margin", "0 1.5rem")
                .set("font-size", "1.5rem")
                .set("font-weight", "600");

        HorizontalLayout nav = new HorizontalLayout(
                styledNav(new RouterLink("Rooms", RoomListView.class)),
                styledNav(new RouterLink("My Bookings", MyBookingsView.class))
        );
        nav.setSpacing(true);

        HorizontalLayout header = new HorizontalLayout(title, nav, buildUserMenu());
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.getStyle()
                .set("padding", "0.5rem 1.5rem")
                .set("font-size", "1.1rem");

        addToNavbar(header);
    }

    /** Slightly larger nav links so the top bar reads easily. */
    private RouterLink styledNav(RouterLink link) {
        link.getStyle().set("font-size", "1.1rem").set("font-weight", "500");
        return link;
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
            if (user instanceof Admin) {
                RouterLink adminLink = new RouterLink("Admin", AdminView.class);
                adminLink.getStyle().set("font-size", "1.1rem").set("font-weight", "500");
                menu.add(adminLink);
            }
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
        }
    }
}
