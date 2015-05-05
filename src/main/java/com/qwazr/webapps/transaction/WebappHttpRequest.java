/**   
 * License Agreement for QWAZR
 *
 * Copyright (C) 2014-2015 OpenSearchServer Inc.
 * 
 * http://www.qwazr.com
 * 
 * This file is part of QWAZR.
 *
 * QWAZR is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * QWAZR is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with QWAZR. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.qwazr.webapps.transaction;

import java.io.IOException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public interface WebappHttpRequest {

	String getCharacterEncoding();

	int getContentLength();

	long getContentLengthLong();

	String getContentType();

	String getParameter(String name);

	Enumeration<String> getParameterNames();

	String[] getParameterValues(String name);

	Map<String, String[]> getParameterMap();

	String getProtocol();

	String getServerName();

	String getScheme();

	int getServerPort();

	String getRemoteAddr();

	String getRemoteHost();

	Locale getLocale();

	Enumeration<Locale> getLocales();

	boolean isSecure();

	int getRemotePort();

	String getAuthType();

	Cookie[] getCookies();

	long getDateHeader(String name);

	String getHeader(String name);

	Enumeration<String> getHeaders(String name);

	Enumeration<String> getHeaderNames();

	int getIntHeader(String name);

	String getMethod();

	String getPathInfo();

	String getContextPath();

	String getQueryString();

	String getRemoteUser();

	boolean isUserInRole(String role);

	Principal getUserPrincipal();

	String getRequestedSessionId();

	String getRequestURI();

	WebappHttpSession getSession(boolean create);

	WebappHttpSession getSession();

	boolean isRequestedSessionIdValid();

	boolean isRequestedSessionIdFromCookie();

	boolean isRequestedSessionIdFromURL();

	boolean authenticate(HttpServletResponse response) throws IOException,
			ServletException;

	void login(String username, String password) throws ServletException;

	void logout() throws ServletException;

}
