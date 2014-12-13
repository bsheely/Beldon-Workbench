package com.beldon.apps.BeldonWorkbench;

import com.vaadin.server.ExternalResource;
import com.vaadin.ui.BrowserFrame;
import com.vaadin.ui.VerticalLayout;

public class ReportContentLayout {
	public ReportContentLayout(VerticalLayout parentLayout, String report) {
		String url = "http://199.168.255.212:8080/jasperserver/flow.html?j_username=jasperadmin&j_password=jasperadmin";	
		if (report.equals("Sample Employee List")) {
			url += "&_flowId=viewReportFlow&standAlone=true&ParentFolderUri=%2Freports%2Fsamples&reportUnit=%2Freports%2Fsamples%2FEmployees";
		} else if (report.equals("Sample Permissions")) {
			url += "&_flowId=viewReportFlow&standAlone=true&ParentFolderUri=%2Freports%2Fsamples&reportUnit=%2Freports%2Fsamples%2FPermissionsOfUsersWithARoleSharedByLoggedIn";
		}
		url += "&decorate=no";
		BrowserFrame browser = new BrowserFrame(null, new ExternalResource(url));
		browser.setSizeFull();
		parentLayout.removeAllComponents();
		parentLayout.addComponent(browser);			
	}
}
