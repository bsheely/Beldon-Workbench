package com.beldon.apps.BeldonWorkbench;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import com.vaadin.external.org.slf4j.Logger;
import com.vaadin.external.org.slf4j.LoggerFactory;

public class CommonUtil {
	private static final transient Logger log = LoggerFactory.getLogger(CommonUtil.class);
	
	static ArrayList<String> getRolePerissions(String role) {
		ArrayList<String> rolePermissions = new ArrayList<String>();
		Connection connection = null;
		Statement statement = null;	
		String sql = "select p.NAME "
				+ "from role r, rolepermission rp, permission p "
				+ "where r.NAME = '" + role + "' and r.ID = rp.ROLE_ID and p.ID = rp.PERMISSION_ID";
		try {
			connection = (Connection)MyVaadinUI.getConnectionPool().reserveConnection();
			statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				rolePermissions.add(resultSet.getString("NAME"));
			}
		} catch (SQLException e) {
			log.error("Unable to get permissions for role '" + role + "'. Exception - " + e);
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
		return rolePermissions;		
	}
}
