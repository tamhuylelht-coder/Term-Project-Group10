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

/**
 * View 1 - Login. Looks up user by name via DemoData and calls
 * {@link User#authenticate(String)}. On success, stashes user in session
 * and routes to /rooms.
 */
@Route("")
@PageTitle("Login - Room Booking")
public class LoginView extends VerticalLayout {

    private final DemoData demoData;
    private final VaadinFrontendUI frontend;

    public LoginView(DemoData demoData, VaadinFrontendUI frontend) {
        this.demoData = demoData;
        this.frontend = frontend;

        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        H1 title = new H1("VinUni Room Booking");
        Paragraph hint = new Paragraph(
                "Demo accounts (password \"pass\"): alice (student), bob (staff), carol (admin).");
        hint.getStyle().set("color", "var(--lumo-secondary-text-color)");

        TextField nameField = new TextField("Username");
        nameField.setWidth("320px");
        nameField.setRequired(true);
        nameField.focus();

        PasswordField pwdField = new PasswordField("Password");
        pwdField.setWidth("320px");
        pwdField.setRequired(true);

        Button loginBtn = new Button("Log in");
        loginBtn.setWidth("320px");
        loginBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginBtn.addClickShortcut(Key.ENTER);
        loginBtn.addClickListener(e ->
                attemptLogin(nameField.getValue(), pwdField.getValue()));

        add(title, hint, nameField, pwdField, loginBtn);
    }

    private void attemptLogin(String name, String pwd) {
        if (name == null || name.isBlank() || pwd == null || pwd.isBlank()) {
            frontend.showError("Enter both username and password");
            return;
        }
        User user = demoData.lookupUser(name.trim());
        if (user == null) {
            frontend.showError("Unknown user");
            return;
        }
        if (!user.authenticate(pwd)) {
            frontend.showError("Wrong password");
            return;
        }
        SessionUtil.setCurrentUser(user);
        frontend.showConfirmation("Welcome, " + user.getUserName());
        getUI().ifPresent(ui -> ui.navigate("rooms"));
    }
}
