package com.beldon.apps.BeldonWorkbench;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.beldon.utils.commons.WorkbenchCryptographer;
import com.vaadin.data.Item;
import com.vaadin.external.org.slf4j.Logger;
import com.vaadin.external.org.slf4j.LoggerFactory;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Notification.Type;

@SuppressWarnings("serial")
public class ChangePasswordLayout extends VerticalLayout {
	private static final transient Logger log = LoggerFactory.getLogger(ChangePasswordLayout.class);
	private VerticalLayout mainLayout;
	private Item selectedUser;
	private PasswordField password = new PasswordField("New Password");
	private PasswordField password2 = new PasswordField("Confirm New Password");
	private int userId;

	ChangePasswordLayout(VerticalLayout main, Item selectedUser) {
		mainLayout = main;
		this.selectedUser = selectedUser;
		userId = ((Long)selectedUser.getItemProperty("ID").getValue()).intValue();
		Button save = new Button("Save");
		Button cancel = new Button("Cancel");
		password.setRequired(true);
		password2.setRequired(true);
		setMargin(true);
		setSizeFull();
		HorizontalLayout buttons = new HorizontalLayout(save, cancel);
		buttons.setSpacing(true);
		VerticalLayout layout = new VerticalLayout(new FormLayout(password, password2), buttons);
		layout.setWidth(null);
		addComponent(layout);
		layout.setComponentAlignment(buttons, Alignment.BOTTOM_RIGHT);
		
		save.addClickListener(new ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				if (!changePassword()) {
					return;
				}
				UserContentLayout contentLayout = new UserContentLayout(mainLayout, true);
				contentLayout.selectRow(userId);
				mainLayout.removeAllComponents();
				mainLayout.addComponent(contentLayout);
			}
		});
		cancel.addClickListener(new ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				mainLayout.removeAllComponents();
				mainLayout.addComponent(new UserContentLayout(mainLayout, true));
			}
		});	
	}
	
	private Boolean changePassword() {
		if (password.getValue().isEmpty() || password2.getValue().isEmpty()) {
			Notification.show("Please enter values for both password fields", Type.ERROR_MESSAGE);
			return false;
		}
		if (password.getValue().length() < 8 || password.getValue().length() > 20) {
			Notification.show("Passwords must be at least 8 and no more than 20 characters", Type.ERROR_MESSAGE);
			return false;
		}
		if (!password.getValue().equals(password2.getValue())) {
			Notification.show("The passwords do not match", Type.ERROR_MESSAGE);
			return false;
		}
		String username = ((String)selectedUser.getItemProperty("USERNAME").getValue());
		Connection connection = null;
		PreparedStatement  preparedStatement = null;	
		try {
			connection = (Connection)MyVaadinUI.getConnectionPool().reserveConnection();			
			connection.setAutoCommit(false);		
			preparedStatement = connection.prepareStatement("update user set ENCRYPTEDPASSWORD=?, LASTUPDATED=Now() where ID = " + userId);
			preparedStatement.setString(1, WorkbenchCryptographer.encryptPassword(password.getValue()));		
			int rows = preparedStatement.executeUpdate();
			if (rows == 1) {
				connection.commit();
				log.info("Password for user '" + username + "' has been changed");
			}
			else {
				log.error("Unable to change password for user " + username);
			}
		} catch (SQLException e) {
			log.error("Unable to change password for user '" + username + "'. Exception: " + e);
		} finally {
			if (preparedStatement != null) {
				try {
					preparedStatement.close();
				} catch (SQLException e) {
					log.error("Unable to close JDBC PreparedStatement. Exception - " + e);
				}
			}
			if (connection != null) {
				MyVaadinUI.getConnectionPool().releaseConnection(connection);
			}
		}
		return true;
	}
	
}
