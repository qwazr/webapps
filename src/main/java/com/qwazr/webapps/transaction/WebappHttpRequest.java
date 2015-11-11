/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.qwazr.webapps.transaction;

import com.qwazr.webapps.transaction.body.HttpBodyInterface;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

public interface WebappHttpRequest extends HttpServletRequest {

	Map<String, Object> getAttributes();
	
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

	boolean authenticate(HttpServletResponse response) throws IOException, ServletException;

	void login(String username, String password) throws ServletException;

	void logout() throws ServletException;

	Collection<Part> getParts() throws IOException, ServletException;

	Part getPart(String name) throws IOException, ServletException;

	HttpBodyInterface getBody();

}
