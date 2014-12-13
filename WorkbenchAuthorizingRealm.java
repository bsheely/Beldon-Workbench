package com.beldon.apps.BeldonWorkbench;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import com.vaadin.data.Item;
import com.vaadin.data.util.sqlcontainer.SQLContainer;
import com.vaadin.data.util.sqlcontainer.query.FreeformQuery;
import com.vaadin.data.util.sqlcontainer.query.TableQuery;
import com.vaadin.external.org.slf4j.Logger;
import com.vaadin.external.org.slf4j.LoggerFactory;
import com.beldon.utils.commons.WorkbenchCryptographer;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;


public class WorkbenchAuthorizingRealm extends AuthorizingRealm {
	static final transient Logger log = LoggerFactory.getLogger(WorkbenchAuthorizingRealm.class);
	
	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		if (principals == null) {
			log.error("PrincipalCollection argument is null");
			throw new AuthorizationException("PrincipalCollection argument is null.");
		}
		
		String username = (String)principals.fromRealm(getName()).iterator().next();
		SimpleAuthorizationInfo authInfo = new SimpleAuthorizationInfo();
		
		if (username != null) {
			authInfo.setRoles(getRoles(username));
			authInfo.setStringPermissions(getPermissions(username));
		}
		
		return authInfo;
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
		if (authenticationToken instanceof UsernamePasswordToken) {
			UsernamePasswordToken token = (UsernamePasswordToken)authenticationToken;
			SQLContainer sqlContainer = null;
			
			try {
				sqlContainer = new SQLContainer(new TableQuery("user", MyVaadinUI.getConnectionPool()));
			} catch (SQLException e) {
				log.warn("Database table 'user' appears to be empty");
				e.printStackTrace();
			}
						
			for (int i = 0; i < sqlContainer.size(); ++i) {
				Object id = sqlContainer.getIdByIndex(i);
				Item item = sqlContainer.getItem(id);
				String username = (String)(item.getItemProperty("USERNAME").getValue());
				String password = String.valueOf(item.getItemProperty("ENCRYPTEDPASSWORD").getValue());
				
				if (username.equals(token.getUsername()) && password.equals(WorkbenchCryptographer.encryptPassword(String.valueOf(token.getPassword())))) {
					Boolean active = (Boolean)(item.getItemProperty("ISACTIVE").getValue());
					
					if (!active) {
					    log.info(token.getUsername() + " is inactive");
						break;
					} 
					
				    log.info(token.getUsername() + " has been authenticated by the realm");
					return new SimpleAuthenticationInfo(token.getUsername(), token.getPassword(), this.getName());
				}
			}
			log.info(token.getUsername() + " was NOT authenticated by the realm");
		}
		return null;
	}
	
	private Set<String> getRoles(String username) {
		String query = "SELECT r.NAME as role, r.ISACTIVE as 'active role' "
				+ "FROM user as u, userrole as ur, role as r "
				+ "WHERE u.ID = ur.USER_ID and ur.ROLE_ID = r.ID and u.USERNAME = '" + username + "'";
		SQLContainer sqlContainer = null;
		Set<String> temp = new HashSet<String>();
		
		try {
			sqlContainer = new SQLContainer(new FreeformQuery(query, MyVaadinUI.getConnectionPool()));
		} catch (SQLException e) {
			log.warn("The following SQL query failed: " + query);
			e.printStackTrace();
		}
			
		for (int i = 0; i < sqlContainer.size(); ++i) {
			Object id = sqlContainer.getIdByIndex(i);
			Item item = sqlContainer.getItem(id);
			String role = (String)(item.getItemProperty("role").getValue());
			Boolean isActive = (Boolean)(item.getItemProperty("active role").getValue());
			
			if (isActive) {
				temp.add(role);
			}
		}
			
	    return ImmutableSet.copyOf(temp);
	}

	private Set<String> getPermissions(String username) {
		Set<String> permissions = ImmutableSet.of();
		//TODO: Needs to be implemented for future functionality
		return permissions;
	}
}