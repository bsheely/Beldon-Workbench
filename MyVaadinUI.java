package com.beldon.apps.BeldonWorkbench;

import java.sql.SQLException;

import javax.servlet.annotation.WebServlet;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.util.Factory;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.data.util.sqlcontainer.connection.JDBCConnectionPool;
import com.vaadin.data.util.sqlcontainer.connection.SimpleJDBCConnectionPool;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.UI;

@Theme("mytheme")
@SuppressWarnings("serial")
public class MyVaadinUI extends UI {
	static JDBCConnectionPool connectionPool; 
    @WebServlet(value = "/*", asyncSupported = true)
    @VaadinServletConfiguration(productionMode = false, ui = MyVaadinUI.class, widgetset = "com.beldon.apps.BeldonWorkbench.AppWidgetSet")
    public static class Servlet extends VaadinServlet { }

    @Override
    protected void init(VaadinRequest request) {
    	try {
    		connectionPool = new SimpleJDBCConnectionPool(DatabaseConstants.DRIVER, DatabaseConstants.URL, DatabaseConstants.USER, DatabaseConstants.PASSWORD);
    	} catch(SQLException e) {
    		throw new RuntimeException(e);
    	}

		Factory<SecurityManager> factory = new IniSecurityManagerFactory("classpath:shiro.ini");
		SecurityManager securityManager = factory.getInstance();
		SecurityUtils.setSecurityManager(securityManager);    
    	setContent(new LoginScreen());
    }
    
    static JDBCConnectionPool getConnectionPool() {
    	return connectionPool;
    }
}
