package com.beldon.apps.BeldonWorkbench;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.vaadin.dialogs.ConfirmDialog;

import com.vaadin.data.Item;
import com.vaadin.data.util.sqlcontainer.SQLContainer;
import com.vaadin.data.util.sqlcontainer.query.FreeformQuery;
import com.vaadin.data.util.sqlcontainer.query.QueryDelegate;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.external.org.slf4j.Logger;
import com.vaadin.external.org.slf4j.LoggerFactory;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class UserContentLayout extends VerticalLayout implements ItemClickListener {
	private static final transient Logger log = LoggerFactory.getLogger(UserContentLayout.class);
	private Table usersTable = new Table();
	private Table rolesTable = new Table();
	private Table permissionsTable = new Table();
	private Table marketsTable = new Table();
	private Item selectedUser = null; 
	private SQLContainer users = null;
	private SQLContainer roles = null;
	private SQLContainer permissions = null;
	private SQLContainer markets = null;
	
	public UserContentLayout(final VerticalLayout parentLayout, Boolean isAdmin) {
		setSizeFull();
		Button addButton = new Button("Add New User");
		Button editButton = new Button("Edit User");
		Button deleteButton = new Button("Delete User");
		Button changePassword = new Button("Reset Password");
			
		try {
			String sql = "select u.ID, FIRSTNAME, LASTNAME, EMAIL, USERNAME, HOSTNAME, IPADDRESS, NAME, u.TIMEZONE "
					+ "from user u left join market m on PRIMARYMARKETID = m.ID "
					+ "where ISACTIVE = true "
					+ "order by u.ID";
			QueryDelegate query = new FreeformQuery(sql, MyVaadinUI.getConnectionPool());
			users = new SQLContainer(query);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Exception creating SQLContainer for users table: " + e);
		}
			
		usersTable.setContainerDataSource(users);
		usersTable.setImmediate(true);
		usersTable.setSelectable(true);
		usersTable.setVisibleColumns(new Object[] {"FIRSTNAME", "LASTNAME", "EMAIL", "USERNAME", "HOSTNAME", "IPADDRESS", "NAME", "TIMEZONE"});
		usersTable.setColumnHeaders(new String[] {"First Name", "Last Name", "E-Mail", "Username", "Hostname", "IP Address", "Primary Market", "Time Zone"});
		usersTable.addItemClickListener(this);

		populateRoles(0);
		rolesTable.setImmediate(true);
		rolesTable.setColumnHeaders(new String[] {"Roles"});

		populatePermissions(0);
		permissionsTable.setImmediate(true);
		permissionsTable.setColumnHeaders(new String[] {"Permissions"});
		
		populateMarkets(0);
		marketsTable.setImmediate(true);
		marketsTable.setColumnHeaders(new String[] {"Markets", "Time Zone"});
		
		HorizontalLayout nonuserTables = new HorizontalLayout(rolesTable, permissionsTable, marketsTable);
		nonuserTables.setSpacing(true);
		HorizontalLayout tables = new HorizontalLayout(usersTable, nonuserTables);
		tables.setSpacing(true);
		HorizontalLayout buttons = new HorizontalLayout(addButton, editButton, deleteButton, changePassword);
		buttons.setSpacing(true);
		VerticalLayout vaadinBug = new VerticalLayout(tables, buttons);
		vaadinBug.setSpacing(true);
		addComponent(vaadinBug);
		buttons.setVisible(isAdmin);
		
		addButton.addClickListener(new ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				parentLayout.removeAllComponents();
				parentLayout.addComponent(new UserLayout(parentLayout));	
			}
		});
		editButton.addClickListener(new ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				if (selectedUser == null) {
					Notification.show("Select a user to edit");
					return;
				}
				
				parentLayout.removeAllComponents();
				parentLayout.addComponent(new EditUserLayout(parentLayout, selectedUser, roles, permissions, markets));	
			}
		});
		deleteButton.addClickListener(new ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				if (selectedUser == null) {
					Notification.show("Select a user to delete");
					return;
				}

				final Integer userId = ((Long)selectedUser.getItemProperty("ID").getValue()).intValue();
				final String username = (String)selectedUser.getItemProperty("USERNAME").getValue();
				
				ConfirmDialog.show(MyVaadinUI.getCurrent(), "Do you want to delete user '" + username + "'?", new ConfirmDialog.Listener() {
					public void onClose(ConfirmDialog dialog) {
						if (dialog.isConfirmed()) {
							String sql = "update user "
									+ "set ISACTIVE = false, LASTUPDATED = now() "
									+ "where ID = " + userId;
							Connection connection = null;
							try {
								connection = (Connection)MyVaadinUI.getConnectionPool().reserveConnection();
								Statement statement = (Statement)connection.createStatement();
								statement.executeUpdate(sql);
								statement.close();
								connection.commit();
								MyVaadinUI.getConnectionPool().releaseConnection(connection);
								selectedUser = null;
								users.refresh();
								populateRoles(0);
								populatePermissions(0);
								populateMarkets(0);
								log.info("User '" + username + "' was set to inactive");
							} catch (SQLException e) {
								String errorMsg = "Unable to update user record with ID " + userId + ". Exception: " + e;
								try {
									connection.rollback();
								} catch (SQLException e2) {
									errorMsg += " and unable to rollback. Exception: " + e2;
									log.error(errorMsg);
								}
							}
						}
					}
				});		
			}
		});
		changePassword.addClickListener(new ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				if (selectedUser == null) {
					Notification.show("Select a user whose password is to be reset");
					return;
				}
				
				parentLayout.removeAllComponents();
				parentLayout.addComponent(new ChangePasswordLayout(parentLayout, selectedUser));	
			}
		});
		parentLayout.removeAllComponents();
		parentLayout.addComponent(this);		
	}

	@Override
	public void itemClick(ItemClickEvent event) {
		selectedUser = event.getItem();
		Integer userId = ((Long)selectedUser.getItemProperty("ID").getValue()).intValue();
		populateRoles(userId);
		populatePermissions(userId);
		populateMarkets(userId);
	}
	
	void selectRow(int userId) {
		if (userId > users.size()) {
			log.error("User primary key values are larger than the row count");
			return;
		}
		usersTable.setValue(users.getIdByIndex(userId - 1));
		selectedUser = usersTable.getItem(users.getIdByIndex(userId - 1));
		populateRoles(userId);
		populatePermissions(userId);
		populateMarkets(userId);
	}
	
	private void populateRoles(int userId) {
		try {
			String sql = "select NAME "
					+ "from role r, userrole u "
					+ "where u.USER_ID = " + userId + " and r.ISACTIVE = true and r.ID = u.ROLE_ID "
							+ "order by NAME";
			QueryDelegate query = new FreeformQuery(sql, MyVaadinUI.getConnectionPool());
			roles = new SQLContainer(query);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Exception creating SQLContainer for roles table: " + e);
		}
		rolesTable.setContainerDataSource(roles);
		if (roles.size() > 0) {
			rolesTable.setVisibleColumns(new Object[] { "NAME" });
		}
	}

	private void populatePermissions(int userId) {
		try {
			String sql = "select NAME "
					+ "from permission p, userpermission "
					+ "where USER_ID = " + userId + " and p.ID = PERMISSION_ID "
							+ "union "
							+ "select p.NAME "
							+ "from permission p, userrole ur, rolepermission rp, role r "
							+ "where ur.USER_ID = " + userId + " and r.ISACTIVE = true and ur.ROLE_ID = r.ID and r.ID = rp.ROLE_ID and rp.PERMISSION_ID = p.ID "
							+ "order by NAME";
			QueryDelegate query = new FreeformQuery(sql, MyVaadinUI.getConnectionPool());
			permissions = new SQLContainer(query);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Exception creating SQLContainer for permissions table: " + e);
		}
		permissionsTable.setContainerDataSource(permissions);	
		if (permissions.size() > 0) {
			permissionsTable.setVisibleColumns(new Object[] { "NAME" });
		}
	}

	private void populateMarkets(int userId) {
		try {
			String sql = "select NAME, TIMEZONE "
					+ "from market m, usermarket u "
					+ "where u.USER_ID = " + userId + " and m.ID = u.MARKET_ID "
							+ "order by NAME";
			QueryDelegate query = new FreeformQuery(sql, MyVaadinUI.getConnectionPool());
			markets = new SQLContainer(query);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Exception creating SQLContainer for markets table: " + e);
		}
		marketsTable.setContainerDataSource(markets);
		if (markets.size() > 0) {
			marketsTable.setVisibleColumns(new Object[] { "NAME", "TIMEZONE" });
		}
	}
}
