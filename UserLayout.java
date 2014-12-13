package com.beldon.apps.BeldonWorkbench;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.beldon.utils.commons.WorkbenchCryptographer;
import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.sqlcontainer.SQLContainer;
import com.vaadin.data.util.sqlcontainer.query.TableQuery;
import com.vaadin.external.org.slf4j.Logger;
import com.vaadin.external.org.slf4j.LoggerFactory;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;

@SuppressWarnings("serial")
public class UserLayout extends VerticalLayout {
	private static final transient Logger log = LoggerFactory.getLogger(UserLayout.class);
	private VerticalLayout mainLayout;
	protected TextField firstname = new TextField("First Name");
	protected TextField lastname = new TextField("Last Name");
	protected TextField email = new TextField("E-Mail"); 
	protected TextField username = new TextField("Username");
	protected PasswordField password = new PasswordField("Password");
	protected TextField hostname = new TextField("Host Name");
	protected TextField ipAddress = new TextField("IP Address");
	protected ComboBox primaryMarket = new ComboBox("Primary Market");
	protected ComboBox timeZone = new ComboBox("Time Zone");
	protected OptionGroup roles = new OptionGroup("Roles");
	protected OptionGroup permissions = new OptionGroup("Permissions");
	protected OptionGroup markets = new OptionGroup("Markets");
	protected int userId = 0;
	private Set<String> selectedRoles = new HashSet<String>();
	private Boolean selectionsHaveChanged = false;

	UserLayout(VerticalLayout main) {
		mainLayout = main;
		setMargin(true);
		firstname.setRequired(true);
		lastname.setRequired(true);
		email.setRequired(true);
		username.setRequired(true);
		password.setRequired(true);
		roles.setMultiSelect(true);
		permissions.setMultiSelect(true);
		markets.setMultiSelect(true);
		roles.setImmediate(true);
		
		try {
			TableQuery query = new TableQuery("role", MyVaadinUI.getConnectionPool());
			SQLContainer roleContainer = new SQLContainer(query);
			for (int i = 0; i < roleContainer.size(); ++i) {
			    Object id = roleContainer.getIdByIndex(i);
			    Item item = roleContainer.getItem(id);
			    if ((Boolean)item.getItemProperty("ISACTIVE").getValue()) {
				    roles.addItem((String)item.getItemProperty("NAME").getValue());   	
			    }
			}		
		} catch (SQLException e) {
			log.error("Unable to query 'role' table" + e);
		}
		try {
			TableQuery query = new TableQuery("permission", MyVaadinUI.getConnectionPool());
			SQLContainer permissionContainer = new SQLContainer(query);
			for (int i = 0; i < permissionContainer.size(); ++i) {
			    Object id = permissionContainer.getIdByIndex(i);
			    Item item = permissionContainer.getItem(id);
			    permissions.addItem((String)item.getItemProperty("NAME").getValue());
			}		
		} catch (SQLException e) {
			log.error("Unable to query 'permission' table" + e);
		}
		try {
			TableQuery query = new TableQuery("market", MyVaadinUI.getConnectionPool());
			SQLContainer marketContainer = new SQLContainer(query);		
			for (int i = 0; i < marketContainer.size(); ++i) {
			    Object id = marketContainer.getIdByIndex(i);
			    Item item = marketContainer.getItem(id);
			    String market = (String)item.getItemProperty("NAME").getValue();
			    primaryMarket.addItem(market);
			    markets.addItem(market);
			}		
		} catch (SQLException e) {
			log.error("Unable to query 'market' table" + e);
		}	
		try {
			TableQuery query = new TableQuery("timezone", MyVaadinUI.getConnectionPool());
			SQLContainer timezoneContainer = new SQLContainer(query);
			for (int i = 0; i < timezoneContainer.size(); ++i) {
			    Object id = timezoneContainer.getIdByIndex(i);
			    Item item = timezoneContainer.getItem(id);
			    timeZone.addItem((String)item.getItemProperty("TIMEZONE").getValue());
			}		
		} catch (SQLException e) {
			log.error("Unable to query 'timezone' table" + e);
		}
		
		FormLayout userData = new FormLayout(firstname, lastname, email, username, password, hostname, ipAddress, primaryMarket, timeZone);
		HorizontalLayout data = new HorizontalLayout(userData, roles, permissions, markets);
		data.setSpacing(true);
		Button saveButton = new Button("Save");
		Button cancelButton = new Button("Cancel");
		HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);
		buttons.setSpacing(true);
		VerticalLayout layout = new VerticalLayout(data, buttons); 
		layout.setWidth(null);
		layout.setComponentAlignment(buttons, Alignment.BOTTOM_RIGHT);
		addComponent(layout);
		mainLayout.removeAllComponents();
		mainLayout.addComponent(this);
		
		roles.addValueChangeListener(new ValueChangeListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void valueChange(com.vaadin.data.Property.ValueChangeEvent event) {
				selectionsHaveChanged = true;
				Set<String> newRoleSelections = (Set<String>)roles.getValue();
				String role = null;
				Boolean newRoleWasAdded = false;			
				for (String s : newRoleSelections) {
					if (!selectedRoles.contains(s)) {
						role = s;
						newRoleWasAdded = true;
						break;
					}
				}
				if (!newRoleWasAdded) {
					for (String s : selectedRoles) {
						if (!newRoleSelections.contains(s)) {
							role = s;
							break;
						}
					}
				}				
				if (role == null) {
					log.error("Error in processing selected roles");
					return;
				}		
				selectedRoles = new HashSet<String>(newRoleSelections);
				ArrayList<String> rolePermissions = CommonUtil.getRolePerissions(role);				
				if (newRoleWasAdded) {
					for (String p : rolePermissions) {
						permissions.select(p);
						permissions.setItemEnabled(p, false);
					}					
				}
				else {
					for (String p : rolePermissions) {
						Boolean selectedRoleHasPermission = false;
						for (String r : selectedRoles) {
							ArrayList<String> permissions = CommonUtil.getRolePerissions(r);
							for (String permission : permissions) {
								if (permission.equals(p)) {
									selectedRoleHasPermission = true;
									break;								
								}
							}
						}
						if (!selectedRoleHasPermission) {
							permissions.unselect(p);
							permissions.setItemEnabled(p, true);
						}
					}										
				}	
			}
        });	
		permissions.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				selectionsHaveChanged = true;
			}	
		});
		markets.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				selectionsHaveChanged = true;
			}	
		});
		saveButton.addClickListener(new ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {		
				processSaveButtonEvent();
			}
		});	
		cancelButton.addClickListener(new ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				mainLayout.removeAllComponents();
				mainLayout.addComponent(new UserContentLayout(mainLayout, true));
			}
		});	
	}

	private void processSaveButtonEvent() {
		if (this instanceof EditUserLayout) {
			if (!((EditUserLayout)this).editUser()) {
				return;
			}
			if (selectionsHaveChanged) {
				/* If any roles, permissions, or markets have changed, all previous roles, permissions, and markets are deleted
				 * and all currently selected roles, permissions, and markets are saved */
				((EditUserLayout)this).deletePreviousSelections();
			}
		} else {
			if (!saveNewUser()) {
				return;
			}
		}
		if (selectionsHaveChanged) {
			if (selectedRoles != null && !selectedRoles.isEmpty()) {
				saveSelections(selectedRoles, "insert ignore into userrole (ROLE_ID, USER_ID) values (?, ?)", "select ID from role where ISACTIVE = true and NAME = '");
			}
			@SuppressWarnings("unchecked")
			Set<String> selectedPermissions = (Set<String>) permissions.getValue();
			if (!selectedPermissions.isEmpty()) {
				saveSelections(selectedPermissions, "insert ignore into userpermission (PERMISSION_ID, USER_ID) values (?, ?)", "select ID from permission where NAME = '");
			}
			@SuppressWarnings("unchecked")
			Set<String> selectedMarkets = (Set<String>) markets.getValue();
			if (!selectedMarkets.isEmpty()) {
				saveSelections(selectedMarkets, "insert ignore into usermarket (MARKET_ID, USER_ID) values (?, ?)", "select ID from market where NAME = '");
			}
		}
		UserContentLayout contentLayout = new UserContentLayout(mainLayout, true);
		contentLayout.selectRow(userId);
		mainLayout.removeAllComponents();
		mainLayout.addComponent(contentLayout);
	}

	private Boolean saveNewUser() {
		if (firstname.getValue().isEmpty() || lastname.getValue().isEmpty() || email.getValue().isEmpty() || username.getValue().isEmpty() || password.getValue().isEmpty()) {
			Notification.show("Please enter values for all required fields", Type.ERROR_MESSAGE);
			return false;
		}
		if (password.getValue().length() < 8 || password.getValue().length() > 20) {
			Notification.show("Passwords must be at least 8 and no more than 20 characters", Type.ERROR_MESSAGE);
			return false;
		}
		if (!isUsernameUnique(username.getValue())) {
			Notification.show("This username is in use. Please enter a different username.", Type.ERROR_MESSAGE);
			return false;
		}
		Connection connection = null;
		PreparedStatement  preparedStatement = null;	
		try {
			connection = (Connection)MyVaadinUI.getConnectionPool().reserveConnection();			
			connection.setAutoCommit(false);
			preparedStatement = connection.prepareStatement("insert into user (BORNON, EMAIL, ENCRYPTEDPASSWORD, FIRSTNAME, HOSTNAME, IPADDRESS, ISACTIVE, LASTNAME, LASTUPDATED, PRIMARYMARKETID, TIMEZONE, USERNAME) "
					+ "values (Now(), ?, ?, ?, ?, ?, true, ?, Now(), ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			preparedStatement.setString(1, email.getValue());
			preparedStatement.setString(2, WorkbenchCryptographer.encryptPassword(password.getValue()));
			preparedStatement.setString(3, firstname.getValue());
			if (hostname.getValue().isEmpty()) {
				preparedStatement.setNull(4, Types.VARCHAR);
			} else {
				preparedStatement.setString(4, hostname.getValue());
			}
			if (ipAddress.getValue().isEmpty()) {
				preparedStatement.setNull(5, Types.VARCHAR);
			} else {
				preparedStatement.setString(5, ipAddress.getValue());
			}
			preparedStatement.setString(6, lastname.getValue());
			if (primaryMarket.getValue() == null || ((String)primaryMarket.getValue()).isEmpty()) {
				preparedStatement.setNull(7, Types.BIGINT);
			} else {
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
				preparedStatement.setInt(7, primaryMarketId);
			}
			if (timeZone.getValue() == null || ((String)timeZone.getValue()).isEmpty()) {
				preparedStatement.setNull(8, Types.VARCHAR);
			} else {
				preparedStatement.setString(8, (String)timeZone.getValue());
			}
			preparedStatement.setString(9, username.getValue());
			preparedStatement.execute();		
			ResultSet result = preparedStatement.getGeneratedKeys();
			if (result != null && result.next()) {
				userId = result.getInt(1);
				connection.commit();
				log.info("New user '" + username.getValue() + "' has been added");
			} else {
				log.error("Unable to add user " + username.getValue());
			}
		} catch (SQLException e) {
			log.error("Unable to add user " + username.getValue() + ". Exception: " + e);
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
	
	private void saveSelections(Set<String> selections, String sql, String partialSql) {
		Connection connection = null;
		Statement statement = null;
		PreparedStatement  preparedStatement = null;
		try {
			connection = (Connection)MyVaadinUI.getConnectionPool().reserveConnection();			
			connection.setAutoCommit(false);
			statement = connection.createStatement();
			preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);		
			for (String str : selections) {
				int id = 0;
				try {
					ResultSet result = statement.executeQuery(partialSql + str + "'");
					while (result.next()) {
						id = result.getInt("ID");
					}
					preparedStatement.setInt(1, id);
					preparedStatement.setInt(2, userId);
					preparedStatement.execute();
					result = preparedStatement.getGeneratedKeys();
					if (result != null && result.next()) {
						log.info(str + " has been added to user '" + username.getValue() + "'");		
					}
					connection.commit();	
				} catch (SQLException e) {
					log.error("Unable to add selection '" + str + "' to user '" + username.getValue() + "'. Exception - " + e);
				} 
			}			
		} catch (SQLException e) {
			log.error("Unable to add selections to user '" + username.getValue() + "'. Exception: " + e);
		} finally {
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					log.error("Unable to close JDBC Statement. Exception - " + e);
				}
			}
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
	}	
	
	protected Boolean isUsernameUnique(String username) {
		Boolean isUnique = true;
		Connection connection = null;
		Statement statement = null;

		try {
			connection = (Connection) MyVaadinUI.getConnectionPool().reserveConnection();
			statement = connection.createStatement();
			ResultSet result = statement.executeQuery("select * from user where USERNAME = '" + username + "'");
			isUnique = !result.isBeforeFirst() ? true : false;
		} catch (SQLException e) {
			log.error("Unable to determine if username '" + username + "' is unique. Exception - " + e);
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
		return isUnique;
	}
}
