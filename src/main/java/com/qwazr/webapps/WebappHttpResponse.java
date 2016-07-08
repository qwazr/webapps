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
package com.qwazr.webapps;

import com.qwazr.utils.IOUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Collection;
import java.util.Locale;

public class WebappHttpResponse implements HttpServletResponse {

	private final HttpServletResponse response;

	WebappHttpResponse(HttpServletResponse response) {
		this.response = response;
	}

	@Override
	public String getCharacterEncoding() {
		return response.getCharacterEncoding();
	}

	@Override
	public String getContentType() {
		return response.getContentType();
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return response.getOutputStream();
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		return response.getWriter();
	}

	public void sendFile(File file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		try {
			BufferedInputStream bis = new BufferedInputStream(fis);
			try {
				IOUtils.copy(bis, response.getOutputStream());
			} finally {
				IOUtils.closeQuietly(bis);
			}
		} finally {
			IOUtils.closeQuietly(fis);
		}
	}

	@Override
	public void setCharacterEncoding(String charset) {
		response.setCharacterEncoding(charset);
	}

	@Override
	public void setContentLength(int len) {
		response.setContentLength(len);
	}

	@Override
	public void setContentLengthLong(long len) {
		response.setContentLengthLong(len);
	}

	@Override
	public void setContentType(String type) {
		response.setContentType(type);
	}

	@Override
	public void setBufferSize(int size) {
		response.setBufferSize(size);
	}

	@Override
	public int getBufferSize() {
		return response.getBufferSize();
	}

	@Override
	public void flushBuffer() throws IOException {
		response.flushBuffer();
	}

	@Override
	public void resetBuffer() {
		response.resetBuffer();
	}

	@Override
	public boolean isCommitted() {
		return response.isCommitted();
	}

	@Override
	public void reset() {
		response.reset();
	}

	@Override
	public void setLocale(Locale loc) {
		response.setLocale(loc);
	}

	@Override
	public Locale getLocale() {
		return response.getLocale();
	}

	@Override
	public void addCookie(Cookie cookie) {
		response.addCookie(cookie);
	}

	@Override
	public boolean containsHeader(String name) {
		return response.containsHeader(name);
	}

	@Override
	public String encodeURL(String url) {
		return response.encodeURL(url);
	}

	@Override
	public String encodeRedirectURL(String url) {
		return response.encodeRedirectURL(url);
	}

	@Override
	@Deprecated
	public String encodeUrl(String url) {
		return response.encodeUrl(url);
	}

	@Override
	@Deprecated
	public String encodeRedirectUrl(String url) {
		return response.encodeRedirectUrl(url);
	}

	@Override
	public void sendError(int sc, String msg) throws IOException {
		response.sendError(sc, msg);
	}

	@Override
	public void sendError(int sc) throws IOException {
		response.sendError(sc);
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		response.sendRedirect(location);
	}

	public void redirect(String location) throws IOException {
		response.sendRedirect(location);
	}

	@Override
	public void setDateHeader(String name, long date) {
		response.setDateHeader(name, date);
	}

	@Override
	public void addDateHeader(String name, long date) {
		response.addDateHeader(name, date);
	}

	@Override
	public void setHeader(String name, String value) {
		response.setHeader(name, value);
	}

	@Override
	public void addHeader(String name, String value) {
		response.addHeader(name, value);
	}

	@Override
	public void setIntHeader(String name, int value) {
		response.setIntHeader(name, value);
	}

	@Override
	public void addIntHeader(String name, int value) {
		response.addIntHeader(name, value);
	}

	@Override
	public void setStatus(int sc) {
		response.setStatus(sc);
	}

	@Override
	@Deprecated
	public void setStatus(int sc, String sm) {
		response.setStatus(sc, sm);

	}

	@Override
	public int getStatus() {
		return response.getStatus();
	}

	@Override
	public String getHeader(String name) {
		return response.getHeader(name);
	}

	@Override
	public Collection<String> getHeaders(String name) {
		return response.getHeaders(name);
	}

	@Override
	public Collection<String> getHeaderNames() {
		return response.getHeaderNames();
	}

}
