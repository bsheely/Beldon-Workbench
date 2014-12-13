package com.beldon.apps.BeldonWorkbench;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class LoginScreen extends VerticalLayout {
	public LoginScreen() {
		TextField username = new TextField("Username");
		PasswordField password = new PasswordField("Password");
		Button submit = new Button("Submit");
		username.setRequired(true);
		password.setRequired(true);
		submit.setClickShortcut(KeyCode.ENTER);
		setMargin(true);
		setSizeFull();
		VerticalLayout layout = new VerticalLayout(new FormLayout(username, password), submit);
		layout.setWidth(null);
		addComponent(layout);
		layout.setComponentAlignment(submit, Alignment.BOTTOM_RIGHT);
		setComponentAlignment(layout, Alignment.MIDDLE_CENTER);
		submit.addClickListener(new LoginAuthentication(username, password));
	}
}
