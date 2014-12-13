package com.beldon.apps.BeldonWorkbench;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Set;

import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.sqlcontainer.SQLContainer;
import com.vaadin.data.util.sqlcontainer.query.FreeformQuery;
import com.vaadin.data.util.sqlcontainer.query.QueryDelegate;
import com.vaadin.data.util.sqlcontainer.query.TableQuery;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.external.org.slf4j.Logger;
import com.vaadin.external.org.slf4j.LoggerFactory;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickListener;

@SuppressWarnings("serial")
public class RolePermissionContentLayout extends VerticalLayout implements ItemClickListener {
	private static final transient Logger log = LoggerFactory.getLogger(RolePermissionContentLayout.class);
	private Item selectedRole; 
	private OptionGroup permissions = new OptionGroup("Permissions");
	private Boolean permissionsHaveChanged;
	private int roleId;
	private ArrayList<String> rolePermissions;

	public RolePermissionContentLayout(VerticalLayout parentLayout) {
		setMargin(true);
		setSpacing(true);
		Table roles = new Table();
		roles.setImmediate(true);
		roles.setSelectable(true);
		try {
			String sql = "select * "
					+ "from role "
					+ "where ISACTIVE = true "
					+ "order by NAME";
			QueryDelegate query = new FreeformQuery(sql, MyVaadinUI.getConnectionPool());
			SQLContainer roleContainer = new SQLContainer(query);
			roles.setContainerDataSource(roleContainer);
			roles.setVisibleColumns(new Object[] {"NAME"});
			roles.setColumnHeaders(new String[] {"Role"});
			roles.addItemClickListener(this);	
		} catch (SQLException e) {
			log.error("Unable to query 'role' table" + e);
		}	
		permissions.setMultiSelect(true);
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
		HorizontalLayout layout = new HorizontalLayout(roles, permissions);
		layout.setSpacing(true);
		Button saveButton = new Button("Save");
		Button cancelButton = new Button("Cancel");
		HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);
		buttons.setSpacing(true);
		VerticalLayout verticalLayout = new VerticalLayout(layout, buttons);
		verticalLayout.setWidth(null);
		verticalLayout.setComponentAlignment(buttons, Alignment.TOP_RIGHT);
		addComponent(verticalLayout);
		
		saveButton.addClickListener(new ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				if (permissionsHaveChanged) {
					ArrayList<Integer> userIds = getRoleUsers();
					@SuppressWarnings("unchecked")
					Set<String> currentSelections = (Set<String>) permissions.getValue();
					for (String permission : currentSelections) {
						if (!rolePermissions.contains(permission)) {
							insertPermission(permission, userIds);
						}
					}
					for (String permission : rolePermissions) {
						if (!currentSelections.contains(permission)) {
							deletePermission(permission, userIds);
						}
					}
				}
				else {
					Notification.show("No changes to save");
				}
			}
		});	
		cancelButton.addClickListener(new ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				permissions.setValue(null);
				for (String p : rolePermissions) {
					permissions.select(p);
				}			
			}
		});	
		permissions.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				permissionsHaveChanged = true;
			}	
		});
		parentLayout.removeAllComponents();
		parentLayout.addComponent(this);		
	}

	@Override
	public void itemClick(ItemClickEvent event) {
		selectedRole = event.getItem();
		String name = (String)selectedRole.getItemProperty("NAME").getValue();	
		rolePermissions = CommonUtil.getRolePerissions(name);	
		permissions.setValue(null);
		for (String p : rolePermissions) {
			permissions.select(p);
		}	
		roleId = ((Long)selectedRole.getItemProperty("ID").getValue()).intValue();	
		permissionsHaveChanged = false;	
	}
	
	private void deletePermission(String name, ArrayList<Integer> userIds) {
		Connection connection = null;
		Statement statement = null;
		try {
			connection = (Connection)MyVaadinUI.getConnectionPool().reserveConnection();
            statement = (Statement)connection.createStatement();
            String rolePermission = "delete from rolepermission where ROLE_ID = " + roleId + " and PERMISSION_ID = (select ID from permission where NAME = '" + name + "')";
			statement.addBatch(rolePermission);
            for (Integer userId : userIds) {
				String userPermission = "delete from userpermission where PERMISSION_ID = (select ID from permission where NAME = '" + name + "') and USER_ID = " + userId;
				statement.addBatch(userPermission);    	
            }
			statement.executeBatch();		
			statement.close();
			connection.commit();
		} catch (SQLException e) {
			log.error("Unable to delete permission " + name + " from rolepermission and/or userpermission. Exception - " + e);
		} finally {
			if (connection != null) {
				MyVaadinUI.getConnectionPool().releaseConnection(connection);
			}
		    try { 
		    	if (statement != null) 
		    		statement.close(); 
		    	} catch (Exception e) {};
		}
	}
	
	private void insertPermission(String name, ArrayList<Integer> userIds) {
		Connection connection = null;
		Statement statement = null;
		try {
			connection = (Connection)MyVaadinUI.getConnectionPool().reserveConnection();
            statement = (Statement)connection.createStatement();
    		String rolePermission = "insert ignore into rolepermission (PERMISSION_ID, ROLE_ID) values ((select ID from permission where NAME = '" + name + "'), " + roleId + ")";
			statement.addBatch(rolePermission);
            for (Integer userId : userIds) {
				String userPermission = "insert ignore into userpermission (PERMISSION_ID, USER_ID) values ((select ID from permission where NAME = '" + name + "'), " + userId + ")";
				statement.addBatch(userPermission);    	
            }
			statement.executeBatch();		
			statement.close();
			connection.commit();
		} catch (SQLException e) {
			log.error("Unable to insert permission " + name + " into rolepermission and/or userpermission. Exception - " + e);
		} finally {
			if (connection != null) {
				MyVaadinUI.getConnectionPool().releaseConnection(connection);
			}
		    try { 
		    	if (statement != null) 
		    		statement.close(); 
		    	} catch (Exception e) {};
		}
	}
	
	ArrayList<Integer> getRoleUsers() {
		ArrayList<Integer> users = new ArrayList<Integer>();
		Connection connection = null;
		Statement statement = null;
		try {
			connection = (Connection)MyVaadinUI.getConnectionPool().reserveConnection();
			statement = (Statement)connection.createStatement();
			ResultSet result = statement.executeQuery("select USER_ID from userrole where ROLE_ID = " + roleId);
			while (result.next()) {
				users.add(result.getInt("USER_ID"));
			}		
		} catch (SQLException e) {
			log.error("Unable to select USER_ID from userrole where ROLE_ID = " + roleId + ". Exception - " + e);
		} finally {
			if (connection != null) {
				MyVaadinUI.getConnectionPool().releaseConnection(connection);
			}
		    try { 
		    	if (statement != null) 
		    		statement.close(); 
		    	} catch (Exception e) {};
		}
		return users;
	}
}
