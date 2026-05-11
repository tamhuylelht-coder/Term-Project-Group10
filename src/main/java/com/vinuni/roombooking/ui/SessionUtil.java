package com.vinuni.roombooking.ui;

import com.vaadin.flow.server.VaadinSession;
import com.vinuni.roombooking.model.User;

/**
 * Holds the currently authenticated User in the Vaadin session.
 * Replaces a real Spring Security principal until the backend wires one up.
 */
public final class SessionUtil {

    private SessionUtil() { }

    public static void setCurrentUser(User user) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(User.class, user);
        }
    }

    public static User getCurrentUser() {
        VaadinSession session = VaadinSession.getCurrent();
        return session == null ? null : (User) session.getAttribute(User.class);
    }

    public static void logout() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(User.class, null);
        }
    }

    public static boolean isLoggedIn() {
        return getCurrentUser() != null;
    }
}
