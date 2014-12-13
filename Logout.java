package com.beldon.apps.BeldonWorkbench;

import org.apache.shiro.subject.Subject;

import com.vaadin.external.org.slf4j.Logger;
import com.vaadin.external.org.slf4j.LoggerFactory;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;

public class Logout {
	private static final transient Logger log = LoggerFactory.getLogger(Logout.class);
	
	public Logout() {
		Subject currentUser = VaadinSession.getCurrent().getAttribute(Subject.class);
		log.info("User [" + currentUser.getPrincipal() + "] logged out");
		VaadinSession.getCurrent().setAttribute(Subject.class, null);
		UI.getCurrent().setContent(new LoginScreen());
		Notification.show("You have been logged out");
	}
}
