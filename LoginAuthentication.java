package com.beldon.apps.BeldonWorkbench;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;

import com.vaadin.external.org.slf4j.Logger;
import com.vaadin.external.org.slf4j.LoggerFactory;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;

@SuppressWarnings("serial")
public class LoginAuthentication implements ClickListener {
	private static final transient Logger log = LoggerFactory.getLogger(LoginAuthentication.class);
	private final TextField login;
	private final PasswordField password;
	
	public LoginAuthentication(TextField login, PasswordField password) {
		this.login = login;
		this.password = password;	
	}

	@Override
	public void buttonClick(ClickEvent event) {
		Subject currentUser = SecurityUtils.getSubject();

		if (!currentUser.isAuthenticated()) {
			UsernamePasswordToken token = new UsernamePasswordToken(login.getValue(), password.getValue(), true);
			try {
                currentUser.login(token);
                log.info("User [" + currentUser.getPrincipal() + "] logged in successfully");
                VaadinSession.getCurrent().setAttribute(Subject.class, currentUser);
				UI.getCurrent().setContent(new MainScreen());
			} catch (UnknownAccountException e) {
                log.info("LOGIN ERROR - Invalid username [" + token.getPrincipal() + "]");
                Notification.show("Invalid username/password", Notification.Type.ERROR_MESSAGE);
            } catch (IncorrectCredentialsException e) {
                log.info("LOGIN ERROR - Invalid password for username [" + token.getPrincipal() + "]");
                Notification.show("Invalid username/password", Notification.Type.ERROR_MESSAGE);
            } catch (LockedAccountException e) {
                log.info("LOGIN ERROR - Account for username [" + token.getPrincipal() + "] is locked");
                Notification.show("The account for " + token.getPrincipal() + " has been locked", Notification.Type.ERROR_MESSAGE);
            } catch (AuthenticationException e) {
                log.info("LOGIN ERROR - Unknown error for username [" + token.getPrincipal() + "] " + e.getMessage());
                Notification.show("Unknown login error", Notification.Type.ERROR_MESSAGE);
            }
		}
	}
}
