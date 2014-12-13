package com.beldon.apps.BeldonWorkbench;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.vaadin.dialogs.ConfirmDialog;

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
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Notification.Type;

@SuppressWarnings("serial")
public class TableContentLayout extends HorizontalLayout implements ItemClickListener {
	private static final transient Logger log = LoggerFactory.getLogger(TableContentLayout.class);
	private Item selectedItem; 
	private SQLContainer sqlContainer;
	private Table table = new Table();
	private VerticalLayout inputFields;
	private final String tableName;
	private Boolean nameIsEdited = false;
	
	public TableContentLayout(VerticalLayout parentLayout, String name, Boolean isAdmin) {	
		tableName = name;
		setMargin(true);
		setSpacing(true);
		setSizeFull();
		Button addButton = new Button("Add");
		Button editButton = new Button("Edit");
		Button deleteButton = new Button("Delete");		
		try {
			String sql = "";
			if (tableName.equals("role")) {
				sql = "select ID, NAME, DESCRIPTION "
					+ "from role "
					+ "where ISACTIVE = true "
					+ "order by ID";		
			} else if (tableName.equals("permission")) {
				sql = "select ID, NAME, DESCRIPTION "
						+ "from permission "
						+ "order by ID";
			} else if (tableName.equals("market")) {
				sql = "select ID, NAME, DESCRIPTION, TIMEZONE "
						+ "from market "
						+ "order by ID";
			} 
			QueryDelegate query = new FreeformQuery(sql, MyVaadinUI.getConnectionPool());
			sqlContainer = new SQLContainer(query);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Exception creating SQLContainer for " + tableName + " table: " + e);
		}		
		table.setContainerDataSource(sqlContainer);
		table.setImmediate(true);
		table.setSelectable(isAdmin);
		if (tableName.equals("role")) {
			table.setVisibleColumns(new Object[] {"NAME", "DESCRIPTION"});
			table.setColumnHeaders(new String[] {"Role", "Description"});
		} else if (tableName.equals("permission")) {
			table.setVisibleColumns(new Object[] {"NAME", "DESCRIPTION"});
			table.setColumnHeaders(new String[] {"Permission", "Description"});
		} else if (tableName.equals("market")) {
			table.setVisibleColumns(new Object[] {"NAME", "DESCRIPTION", "TIMEZONE"});
			table.setColumnHeaders(new String[] {"Market", "Description", "Time Zone"});
		} 
		table.addItemClickListener(this);	
		HorizontalLayout buttons = new HorizontalLayout(addButton, editButton, deleteButton);
		buttons.setSpacing(true);
		VerticalLayout layout = new VerticalLayout(table, buttons);
		layout.setSpacing(true);
		addComponent(layout);
		buttons.setVisible(isAdmin);
		
		addButton.addClickListener(new ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				selectedItem = null;
				table.setValue(null);
				createInputFields();
			}
		});
		editButton.addClickListener(new ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {	
				if (selectedItem == null) {
					Notification.show("Select a row to edit");
					return;
				}	
				createInputFields();
			}
		});
		deleteButton.addClickListener(new ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				if (selectedItem == null) {
					Notification.show("Select a row to delete");
					return;
				}
				final Integer id = ((Long)selectedItem.getItemProperty("ID").getValue()).intValue();
				final String name = (String)selectedItem.getItemProperty("NAME").getValue();				
				ConfirmDialog.show(MyVaadinUI.getCurrent(), "Do you want to delete '" + name + "'?", new ConfirmDialog.Listener() {
					public void onClose(ConfirmDialog dialog) {
						if (dialog.isConfirmed()) {
							String sql = "";
							if (tableName.equals("role")) {
								sql = "update role "
										+ "set ISACTIVE = false, LASTUPDATED = now() "
										+ "where ID = " + id;		
							} else {
								sql = "delete from " + tableName + " where ID = " + id;		
							}
							Connection connection = null;
							try {
								connection = (Connection)MyVaadinUI.getConnectionPool().reserveConnection();
								Statement statement = (Statement)connection.createStatement();
								statement.executeUpdate(sql);
								statement.close();
								connection.commit();
								MyVaadinUI.getConnectionPool().releaseConnection(connection);
								selectedItem = null;
								sqlContainer.refresh();
								log.info(name + " was deleted or set to inactive");
							} catch (SQLException e) {
								String errorMsg = "Unable to update role record with ID " + id + ". Exception: " + e;
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
		parentLayout.removeAllComponents();
		parentLayout.addComponent(this);		
	}

	@Override
	public void itemClick(ItemClickEvent event) {
		selectedItem = event.getItem();
	}

	private void createInputFields() {
		String displayName = "";
		if (tableName.equals("role")) {
			displayName = "Role";
		} else if (tableName.equals("permission")) {
			displayName = "Permission";
		} else if (tableName.equals("market")) {
			displayName = "Market";
		}
		final TextField name = new TextField(displayName);
		final TextField description = new TextField("Description");
		final ComboBox timeZone = new ComboBox("Time Zone");
		if (tableName.equals("market")) {
			SQLContainer timezones = null;
			try {
				TableQuery query = new TableQuery("timezone", MyVaadinUI.getConnectionPool());
				timezones = new SQLContainer(query);	
				for (int i = 0; i < timezones.size(); ++i) {
				    Object id = timezones.getIdByIndex(i);
				    Item item = timezones.getItem(id);
				    timeZone.addItem((String)item.getItemProperty("TIMEZONE").getValue());
				}						
			} catch(SQLException e) {
				log.error("Unable to load Time Zones. Exception - " + e);
			}	
		}
		name.setRequired(true);
		Button saveButton = new Button("Save");
		Button cancelButton = new Button("Cancel");
		HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);
		buttons.setSpacing(true);
		if (tableName.equals("market")) {
			inputFields = new VerticalLayout(new FormLayout(name, description, timeZone), buttons);
		} else {
			inputFields = new VerticalLayout(new FormLayout(name, description), buttons);
		}
		inputFields.setWidth(null);
		inputFields.setComponentAlignment(buttons, Alignment.BOTTOM_RIGHT);
		addComponent(inputFields);
		setExpandRatio(inputFields, 1);
		if (selectedItem != null) {
			name.setValue((String)selectedItem.getItemProperty("NAME").getValue());
			description.setValue((String)selectedItem.getItemProperty("DESCRIPTION").getValue());
			if (tableName.equals("market")) {
				timeZone.setValue((String)selectedItem.getItemProperty("TIMEZONE").getValue());
			}
		}

		saveButton.addClickListener(new ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				if (name.getValue().isEmpty()) {
					Notification.show("Please enter values for all required fields", Type.ERROR_MESSAGE);
					return;
				}
				if ((selectedItem == null || nameIsEdited) && !isNameUnique(name.getValue())) {
					Notification.show("This name is in use. Please enter a different name.", Type.ERROR_MESSAGE);
					return;
				}
				Connection connection = null;
				PreparedStatement  preparedStatement = null;
				String sql = "";
				if (selectedItem == null) {
					if (tableName.equals("role")) {
						sql = "insert into role (BORNON, DESCRIPTION, ISACTIVE, LASTUPDATED, NAME) "
							+ "values (Now(), ?, true, Now(), ?)";
					} else if (tableName.equals("permission")) {
						sql = "insert into permission (BORNON, DESCRIPTION, LASTUPDATED, NAME) "
								+ "values (Now(), ?, Now(), ?)";
					} else if (tableName.equals("market")) {
						sql = "insert into market (BORNON, DESCRIPTION, LASTUPDATED, NAME, TIMEZONE) "
								+ "values (Now(), ?, Now(), ?, ?)";
					} 
				} else {
					int id = ((Long)selectedItem.getItemProperty("ID").getValue()).intValue();
					if (tableName.equals("market")) {
						sql = "update market set DESCRIPTION=?, LASTUPDATED=Now(), NAME=?, TIMEZONE=? where ID = " + id;
					} else {
						sql = "update " + tableName + " set DESCRIPTION=?, LASTUPDATED=Now(), NAME=? where ID = " + id;
					} 
				}
				try {
					connection = (Connection)MyVaadinUI.getConnectionPool().reserveConnection();			
					connection.setAutoCommit(false);
					preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
					preparedStatement.setString(1, description.getValue());
					preparedStatement.setString(2, name.getValue());
					if (tableName.equals("market")) {
						preparedStatement.setString(3, (String)timeZone.getValue());
					}
					if (selectedItem == null) {
						preparedStatement.execute();	
						ResultSet result = preparedStatement.getGeneratedKeys();	
						if (result != null && result.next()) {
							connection.commit();
							log.info(name.getValue() + "' has been added to table " + tableName);
						} else {
							log.error("Unable to add " + name.getValue() + " to table " + tableName);
						}				
					} else { 
						int rows = preparedStatement.executeUpdate();
						if (rows == 1) {
							connection.commit();
							log.info(name.getValue() + " has been updated");
						} else {
							log.error("Unable to edit role " + name.getValue());
						}				
					}
				} catch (SQLException e) {
					log.error("Unable to add role " + name.getValue() + ". Exception: " + e);
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
				removeComponent(inputFields);
				table.setValue(null);  
				selectedItem = null;
				sqlContainer.refresh();
			}
		});
		cancelButton.addClickListener(new ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				removeComponent(inputFields);
				table.setValue(null);
				selectedItem = null;
			}
		});	
		name.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				nameIsEdited = true;
			}
		});
	}

	private Boolean isNameUnique(String name) {
		Boolean isUnique = true;
		Connection connection = null;
		Statement statement = null;
		try {
			connection = (Connection)MyVaadinUI.getConnectionPool().reserveConnection();
			statement = connection.createStatement();
			String sql = "select * from " + tableName + " where NAME = '" + name + "'";	
			ResultSet result = statement.executeQuery(sql);
			isUnique = !result.isBeforeFirst() ? true : false;
		} catch (SQLException e) {
			log.error("Unable to determine if column NAME '" + name + "' is unique. Exception - " + e);
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
