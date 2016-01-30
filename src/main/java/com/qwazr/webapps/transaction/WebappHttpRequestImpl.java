/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
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

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

public class WebappHttpRequestImpl implements WebappHttpRequest {

	private final String contextPath;
	private final String pathInfo;
	private final HttpServletRequest request;
	private final WebappRequestMaps.WebappAttributes attributes;
	private final WebappRequestMaps.WebappHeaders headers;
	private final HttpBodyInterface body;

	WebappHttpRequestImpl(HttpServletRequest request, HttpBodyInterface body) {
		this.contextPath = request.getContextPath();
		this.pathInfo = request.getPathInfo();
		this.request = request;
		this.attributes = new WebappRequestMaps.WebappAttributes(request);
		this.headers = new WebappRequestMaps.WebappHeaders(request);
		this.body = body;
	}

	public HttpBodyInterface getBody() {
		return body;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public Map<String, String[]> getParameters() {
		return getParameterMap();
	}

	@Override
	public Map<String, String> getHeadersMap() {
		return headers;
	}

	@Override
	public Object getAttribute(String name) {
		return request.getAttribute(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return request.getAttributeNames();
	}

	@Override
	public String getCharacterEncoding() {
		return request.getCharacterEncoding();
	}

	@Override
	public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
	}

	@Override
	public int getContentLength() {
		return request.getContentLength();
	}

	@Override
	public long getContentLengthLong() {
		return request.getContentLengthLong();
	}

	@Override
	public String getContentType() {
		return request.getContentType();
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		return request.getInputStream();
	}

	@Override
	public String getParameter(String name) {
		return request.getParameter(name);
	}

	@Override
	public Enumeration<String> getParameterNames() {
		return request.getParameterNames();
	}

	@Override
	public String[] getParameterValues(String name) {
		return request.getParameterValues(name);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		return request.getParameterMap();
	}

	@Override
	public String getProtocol() {
		return request.getProtocol();
	}

	@Override
	public String getScheme() {
		return request.getScheme();
	}

	@Override
	public String getServerName() {
		return request.getServerName();
	}

	@Override
	public int getServerPort() {
		return request.getServerPort();
	}

	@Override
	public BufferedReader getReader() throws IOException {
		return request.getReader();
	}

	@Override
	public String getRemoteAddr() {
		return request.getRemoteAddr();
	}

	@Override
	public String getRemoteHost() {
		return request.getRemoteHost();
	}

	@Override
	public void setAttribute(String name, Object o) {
		request.setAttribute(name, o);
	}

	@Override
	public void removeAttribute(String name) {
		request.removeAttribute(name);
	}

	@Override
	public Locale getLocale() {
		return request.getLocale();
	}

	@Override
	public Enumeration<Locale> getLocales() {
		return request.getLocales();
	}

	@Override
	public boolean isSecure() {
		return request.isSecure();
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		return null;
	}

	@Override
	@Deprecated
	public String getRealPath(String path) {
		return null;
	}

	@Override
	public int getRemotePort() {
		return request.getRemotePort();
	}

	@Override
	public String getLocalName() {
		return null;
	}

	@Override
	public String getLocalAddr() {
		return null;
	}

	@Override
	public int getLocalPort() {
		return 0;
	}

	@Override
	public ServletContext getServletContext() {
		return null;
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		return null;
	}

	@Override
	public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
			throws IllegalStateException {
		return null;
	}

	@Override
	public boolean isAsyncStarted() {
		return false;
	}

	@Override
	public boolean isAsyncSupported() {
		return false;
	}

	@Override
	public AsyncContext getAsyncContext() {
		return null;
	}

	@Override
	public DispatcherType getDispatcherType() {
		return null;
	}

	@Override
	public String getAuthType() {
		return request.getAuthType();
	}

	@Override
	public Cookie[] getCookies() {
		return request.getCookies();
	}

	@Override
	public long getDateHeader(String name) {
		return request.getDateHeader(name);
	}

	@Override
	public String getHeader(String name) {
		return request.getHeader(name);
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		return request.getHeaders(name);
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return request.getHeaderNames();
	}

	@Override
	public int getIntHeader(String name) {
		return request.getIntHeader(name);
	}

	@Override
	public String getMethod() {
		return request.getMethod();
	}

	@Override
	public String getPathInfo() {
		return pathInfo;
	}

	@Override
	public String getPathTranslated() {
		return null;
	}

	@Override
	public String getContextPath() {
		return contextPath;
	}

	@Override
	public String getQueryString() {
		return request.getQueryString();
	}

	@Override
	public String getRemoteUser() {
		return request.getRemoteUser();
	}

	@Override
	public boolean isUserInRole(String role) {
		return request.isUserInRole(role);
	}

	@Override
	public Principal getUserPrincipal() {
		return request.getUserPrincipal();
	}

	@Override
	public String getRequestedSessionId() {
		return request.getRequestedSessionId();
	}

	@Override
	public String getRequestURI() {
		return request.getRequestURI();
	}

	@Override
	public StringBuffer getRequestURL() {
		return null;
	}

	@Override
	public String getServletPath() {
		return null;
	}

	@Override
	public WebappHttpSession getSession(boolean create) {
		HttpSession session = request.getSession(create);
		if (session == null)
			return null;
		return new WebappHttpSessionImpl(session);
	}

	@Override
	public WebappHttpSession getSession() {
		return getSession(true);
	}

	@Override
	public String changeSessionId() {
		return null;
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		return request.isRequestedSessionIdValid();
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		return request.isRequestedSessionIdFromCookie();
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		return request.isRequestedSessionIdFromURL();
	}

	@Override
	@Deprecated
	public boolean isRequestedSessionIdFromUrl() {
		return false;
	}

	@Override
	public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
		return request.authenticate(response);
	}

	@Override
	public void login(String username, String password) throws ServletException {
		request.login(username, password);
	}

	@Override
	public void logout() throws ServletException {
		request.logout();
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		return request.getParts();
	}

	@Override
	public Part getPart(String name) throws IOException, ServletException {
		return request.getPart(name);
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
		return null;
	}

}
