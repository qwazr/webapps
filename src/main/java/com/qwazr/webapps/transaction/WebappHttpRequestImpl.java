/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.qwazr.webapps.transaction;

import com.qwazr.webapps.transaction.body.HttpBodyInterface;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

public class WebappHttpRequestImpl implements WebappHttpRequest {

    private final String contextPath;
    private final String pathInfo;
    private final ApplicationContext context;
    private final HttpServletRequest request;
    private final HttpBodyInterface body;

    WebappHttpRequestImpl(ApplicationContext context, HttpServletRequest request, HttpBodyInterface body) {
	this.context = context;
	this.contextPath = context.getContextPath();
	this.pathInfo = request.getPathInfo();
	this.request = request;
	this.body = body;
    }

    public HttpBodyInterface getBody() {
	return body;
    }

    @Override
    public String getCharacterEncoding() {
	return request.getCharacterEncoding();
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

    ServletInputStream getInputStream() throws IOException {
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

    BufferedReader getReader() throws IOException {
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
    public int getRemotePort() {
	return request.getRemotePort();
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
    public WebappHttpSession getSession(boolean create) {
	HttpSession session = request.getSession(create);
	if (session == null)
	    return null;
	return context.getSessionOrCreate(session);
    }

    @Override
    public WebappHttpSession getSession() {
	return getSession(true);
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

}
