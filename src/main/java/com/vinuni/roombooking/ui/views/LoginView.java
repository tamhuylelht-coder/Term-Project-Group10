package com.vinuni.roombooking.ui.views;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vinuni.roombooking.model.User;
import com.vinuni.roombooking.ui.DemoData;
import com.vinuni.roombooking.ui.SessionUtil;
import com.vinuni.roombooking.ui.VaadinFrontendUI;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;

/**
 * View 1 - Login.
 *
 * Validates the typed credentials via Spring Security's {@link AuthenticationManager}
 * (which delegates to the InMemoryUserDetailsManager + BCryptPasswordEncoder declared
 * in {@code config.SecurityConfig}). On success, looks up the matching domain
 * {@link User} (Student / Staff / Admin) from {@code DemoData} and stashes it in
 * {@code SessionUtil} for the rest of the views to read.
 *
 * SecurityContextHolder bookkeeping is intentionally NOT done here because the Spring
 * Security filter chain is permit-all and never reads the SecurityContext at the HTTP
 * layer; view-level access control is enforced by Vaadin BeforeEnterObservers in
 * MainLayout and AdminView reading SessionUtil.
 */
@Route("")
@PageTitle("Login - Room Booking")
public class LoginView extends VerticalLayout {

    private final DemoData demoData;
    private final VaadinFrontendUI frontend;
    private final AuthenticationManager authenticationManager;

    public LoginView(DemoData demoData,
                     VaadinFrontendUI frontend,
                     AuthenticationManager authenticationManager) {
        this.demoData = demoData;
        this.frontend = frontend;
        this.authenticationManager = authenticationManager;

        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        // Larger base font for everything in this view (labels, inputs, hint).
        getStyle().set("font-size", "var(--lumo-font-size-l)");

        H1 title = new H1("VinUni Room Booking");
        title.getStyle().set("font-size", "3rem").set("margin-bottom", "0.25em");
        Paragraph hint = new Paragraph(
                "Demo accounts (password \"pass\"): alice (student), bob (staff), carol (admin).");
        hint.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "1.05rem")
                .set("margin-bottom", "1.5rem");

        // Field width bumped 320 -> 440. Taller inputs via --lumo-size-l.
        String fieldWidth = "440px";

        TextField nameField = new TextField("Username");
        nameField.setWidth(fieldWidth);
        nameField.getStyle().set("--lumo-size-m", "var(--lumo-size-l)");
        nameField.setRequired(true);
        nameField.focus();

        PasswordField pwdField = new PasswordField("Password");
        pwdField.setWidth(fieldWidth);
        pwdField.getStyle().set("--lumo-size-m", "var(--lumo-size-l)");
        pwdField.setRequired(true);

        Button loginBtn = new Button("Log in");
        loginBtn.setWidth(fieldWidth);
        loginBtn.getStyle()
                .set("--lumo-size-m", "var(--lumo-size-l)")
                .set("font-size", "1.1rem")
                .set("font-weight", "600");
        loginBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginBtn.addClickShortcut(Key.ENTER);
        loginBtn.addClickListener(e ->
                attemptLogin(nameField.getValue(), pwdField.getValue()));

        add(title, hint, nameField, pwdField, loginBtn);
    }

    private void attemptLogin(String rawName, String pwd) {
        if (rawName == null || rawName.isBlank() || pwd == null || pwd.isBlank()) {
            frontend.showError("Enter both username and password");
            return;
        }
        // Normalise like DemoData.lookupUser (case-insensitive).
        String username = rawName.trim().toLowerCase();

        try {
            // Validates the password against UserDetailsService + BCryptPasswordEncoder.
            // Throws BadCredentialsException on wrong password, UsernameNotFoundException
            // on unknown user. We don't need the returned Authentication for anything
            // else here.
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, pwd));

            User user = demoData.lookupUser(username);
            if (user == null) {
                // Authenticated against Spring Security but no DemoData profile —
                // means the two stores have drifted. Defensive guard.
                frontend.showError("No profile found for " + username);
                return;
            }
            SessionUtil.setCurrentUser(user);

            frontend.showConfirmation("Welcome, " + user.getUserName());
            getUI().ifPresent(ui -> ui.navigate("rooms"));
        } catch (BadCredentialsException ex) {
            frontend.showError("Wrong username or password");
        } catch (AuthenticationException ex) {
            frontend.showError("Could not log in: " + ex.getMessage());
        }
    }
}
