package com.beldon.apps.BeldonWorkbench;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import com.vaadin.data.Item;
import com.vaadin.data.util.sqlcontainer.SQLContainer;
import com.vaadin.external.org.slf4j.Logger;
import com.vaadin.external.org.slf4j.LoggerFactory;
import com.vaadin.ui.Notification;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Notification.Type;

@SuppressWarnings("serial")
public class EditUserLayout extends UserLayout {
	private static final transient Logger log = LoggerFactory.getLogger(EditUserLayout.class);
	protected Item selectedUser; 

	EditUserLayout(VerticalLayout main, Item selected, SQLContainer roles, SQLContainer permissions, SQLContainer markets) {
		super(main);
		selectedUser = selected;
		password.setVisible(false);
		firstname.setValue((String)selectedUser.getItemProperty("FIRSTNAME").getValue());
		lastname.setValue((String)selectedUser.getItemProperty("LASTNAME").getValue());
		email.setValue((String)selectedUser.getItemProperty("EMAIL").getValue());
		username.setValue((String)selectedUser.getItemProperty("USERNAME").getValue());
		hostname.setValue((String)selectedUser.getItemProperty("HOSTNAME").getValue());
		ipAddress.setValue((String)selectedUser.getItemProperty("IPADDRESS").getValue());
		primaryMarket.setValue((String)selectedUser.getItemProperty("NAME").getValue());
		timeZone.setValue((String)selectedUser.getItemProperty("TIMEZONE").getValue());
		hostname.setNullRepresentation("");
		ipAddress.setNullRepresentation("");
		populateRoles(roles);
		populatePermissions(permissions);
		populateMarkets(markets);
	}
	
	protected Boolean editUser() {
		if (firstname.getValue().isEmpty() || lastname.getValue().isEmpty() || email.getValue().isEmpty() || username.getValue().isEmpty()) {
			Notification.show("Please enter values for all required fields", Type.ERROR_MESSAGE);
			return false;
		}	
		if (!username.getValue().equals((String)selectedUser.getItemProperty("USERNAME").getValue()) && !isUsernameUnique(username.getValue())) {
			Notification.show("This username is in use. Please enter a different username.", Type.ERROR_MESSAGE);
			return false;
		}
		userId = ((Long)selectedUser.getItemProperty("ID").getValue()).intValue();
		Connection connection = null;
		PreparedStatement preparedStatement = null;	
		try {
			connection = (Connection)MyVaadinUI.getConnectionPool().reserveConnection();			
			connection.setAutoCommit(false);
			preparedStatement = connection.prepareStatement("update user set EMAIL=?, FIRSTNAME=?, HOSTNAME=?, IPADDRESS=?, LASTNAME=?, LASTUPDATED=Now(), PRIMARYMARKETID=?, TIMEZONE=?, USERNAME=? where ID = " + userId);
			preparedStatement.setString(1, email.getValue());
			preparedStatement.setString(2, firstname.getValue());
			if (hostname.getValue() != null && !hostname.getValue().isEmpty()) {
				preparedStatement.setString(3, hostname.getValue());
			} else {
				preparedStatement.setNull(3, Types.VARCHAR);
			}
			if (ipAddress.getValue() != null && !ipAddress.getValue().isEmpty()) {
				preparedStatement.setString(4, ipAddress.getValue());
			} else {
				preparedStatement.setNull(4, Types.VARCHAR);
			}
			preparedStatement.setString(5, lastname.getValue());
			if (primaryMarket.getValue() != null && !((String)primaryMarket.getValue()).isEmpty()) {
				int primaryMarketId = 0;
				Statement statement = null;
				try {
					statement = connection.createStatement();
					ResultSet result = statement.executeQuery("select ID from market where NAME = '" + (String)primaryMarket.getValue() + "'");
				    while (result.next()) {
						primaryMarketId = result.getInt("ID");
				    }
				} catch (SQLException e) {
					log.error("Unable to set Primary Market ID. Exception - " + e);
				} finally {
					if (statement != null) {
						statement.close();
					}
				}			
				preparedStatement.setInt(6, primaryMarketId);
			} else {
				preparedStatement.setNull(6, Types.BIGINT);
			}
			if (timeZone.getValue() != null && !((String)timeZone.getValue()).isEmpty()) {
				preparedStatement.setString(7, (String)timeZone.getValue());
			} else {
				preparedStatement.setNull(7, Types.VARCHAR);
			}
			preparedStatement.setString(8, username.getValue());
			int rows = preparedStatement.executeUpdate();
			if (rows == 1) {
				connection.commit();
				log.info("User '" + username.getValue() + "' has been updated");
			}
			else {
				log.error("Unable to edit user " + username.getValue());
			}
		} catch (SQLException e) {
			log.error("Unable to edit user " + username.getValue() + ". Exception: " + e);
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
	
	protected void deletePreviousSelections() {
		Connection connection = null;
		Statement statement = null;
		try {
			connection = (Connection)MyVaadinUI.getConnectionPool().reserveConnection();
			statement = connection.createStatement();
			String deleteRoles = "delete from userrole where USER_ID = " + userId;
			String deletePermissions = "delete from userpermission where USER_ID = " + userId;
			String deleteMarkets = "delete from usermarket where USER_ID = " + userId;
			statement.addBatch(deleteRoles);
			statement.addBatch(deletePermissions);
			statement.addBatch(deleteMarkets);
			int[] rows = statement.executeBatch();
			if (rows[0] < 1 && rows[1] < 1 && rows[2] < 1) {
				log.error("No current selections were deleted from database (Duplication is now possible)");
			}
			connection.commit();
		} catch (SQLException e) {
			log.error("Unable to delete current selections from database. Duplication is possible. Exception - " + e);
		} finally {
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					log.error("Unable to close JDBC Statement. Exception - " + e);
				}
			}
			if (connection != null) {
				MyVaadinUI.getConnectionPool().releaseConnection(connection);
			}
		}
	}
	
	private void populateRoles(SQLContainer rolesContainer) {
		for (int i = 0; i < rolesContainer.size(); ++i) {
			Object id = rolesContainer.getIdByIndex(i);
		    Item item = rolesContainer.getItem(id);
		    roles.select((String)item.getItemProperty("NAME").getValue());
		}				
	}
	
	private void populatePermissions(SQLContainer permissionsContainer) {
		for (int i = 0; i < permissionsContainer.size(); ++i) {
			Object id = permissionsContainer.getIdByIndex(i);
		    Item item = permissionsContainer.getItem(id);
		    permissions.select((String)item.getItemProperty("NAME").getValue());
		}				
	}

	private void populateMarkets(SQLContainer marketsContainer) {
		for (int i = 0; i < marketsContainer.size(); ++i) {
			Object id = marketsContainer.getIdByIndex(i);
		    Item item = marketsContainer.getItem(id);
		    markets.select((String)item.getItemProperty("NAME").getValue());
		}		
	}
}
