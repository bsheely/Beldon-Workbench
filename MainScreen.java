package com.beldon.apps.BeldonWorkbench;

import org.apache.shiro.subject.Subject;

import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class MainScreen extends HorizontalLayout {	
	public MainScreen() {
		setSizeFull();
		Subject currentUser = VaadinSession.getCurrent().getAttribute(Subject.class);
		final Boolean isAdmin = currentUser.hasRole("Admin") ? true : false;
		final Tree menuTree = new Tree();
		menuTree.setImmediate(true);
		HorizontalLayout menuLayout = new HorizontalLayout();
		menuLayout.addComponent(menuTree);
		menuLayout.setMargin(true);
		menuLayout.setSpacing(true);	
		addComponent(menuLayout);
		final VerticalLayout contentLayout = new VerticalLayout();
		contentLayout.setMargin(true);
		contentLayout.setSizeFull();
		addComponent(contentLayout);
		setExpandRatio(contentLayout, 1);
		menuTree.addItem("Database Tables");				
		if (isAdmin) {
			menuTree.addItem("Markets");
			menuTree.addItem("Permissions");
			menuTree.addItem("Roles");
			menuTree.addItem("Role Permissions");
			menuTree.addItem("Users");
		} else {
			menuTree.addItem("Markets");
			menuTree.addItem("Users");	
		}
		menuTree.addItem("Reports");	
		menuTree.addItem("Sample Employee List");		
		menuTree.addItem("Sample Permissions");			
		menuTree.setParent("Users", "Database Tables");
		menuTree.setParent("Roles", "Database Tables");
		menuTree.setParent("Permissions", "Database Tables");
		menuTree.setParent("Role Permissions", "Database Tables");
		menuTree.setParent("Markets", "Database Tables");
		menuTree.setParent("Sample Employee List", "Reports");	
		menuTree.setParent("Sample Permissions", "Reports");							
		menuTree.setChildrenAllowed("Users", false);
		menuTree.setChildrenAllowed("Roles", false);
		menuTree.setChildrenAllowed("Permissions", false);
		menuTree.setChildrenAllowed("Role Permissions", false);
		menuTree.setChildrenAllowed("Markets", false);	
		menuTree.setChildrenAllowed("Sample Employee List", false);	
		menuTree.setChildrenAllowed("Sample Permissions", false);	
		menuTree.addItem("Logout");	
		menuTree.setChildrenAllowed("Logout", false);	
		menuTree.addItemClickListener(new ItemClickListener() {
			@Override
			public void itemClick(ItemClickEvent event) {
				if (event.getItemId().equals("Users")) {
					new UserContentLayout(contentLayout, isAdmin);
				} else if (event.getItemId().equals("Roles")) {	
					new TableContentLayout(contentLayout, "role", isAdmin);
				} else if (event.getItemId().equals("Permissions")) {
					new TableContentLayout(contentLayout, "permission", isAdmin);
				} else if (event.getItemId().equals("Role Permissions")) {
					new RolePermissionContentLayout(contentLayout);
				} else if (event.getItemId().equals("Markets")) {	
					new TableContentLayout(contentLayout, "market", isAdmin);
				} else if (event.getItemId().equals("Sample Employee List")) {	
					new ReportContentLayout(contentLayout, "Sample Employee List");
				} else if (event.getItemId().equals("Sample Permissions")) {	
					new ReportContentLayout(contentLayout, "Sample Permissions");
				} else if (event.getItemId().equals("Logout")) {
					new Logout();
				}
			}} );
	}
}
