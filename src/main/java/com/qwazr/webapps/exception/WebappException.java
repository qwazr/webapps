/*
 * Copyright 2015-2020 Emmanuel Keller / QWAZR
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
 */
package com.qwazr.webapps.exception;

import com.jamesmurty.utils.XMLBuilder2;
import com.qwazr.utils.LoggerUtils;
import com.qwazr.utils.ObjectMappers;
import org.apache.commons.text.StringEscapeUtils;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebappException extends AbstractWebappException {

	/**
	 *
	 */
	private static final long serialVersionUID = -1221758741584043195L;

	private static final Logger logger = LoggerUtils.getLogger(WebappException.class);

	public enum Title {

		ERROR, SCRIPT_ERROR, QUERY_ERROR, VIEW_ERROR, BODY_ERROR, NOT_FOUND_ERROR;

		final private String title = name().replace('_', ' ');

	}

	public static class Error {

		public final int status;
		public final Title title;
		public final String message;

		private Error(Status status, Title title, String message) {
			this.status = status.getStatusCode();
			this.title = title == null ? Title.ERROR : title;
			this.message = message == null ? status.getReasonPhrase() : message;
		}
	}

	private final Error error;

	public WebappException(Status status, Title title, String message) {
		error = new Error(status, title, message);
	}

	public WebappException(Status status) {
		this(status, null, (String) null);
	}

	public WebappException(Status status, Title title, Throwable e) {
		this(status, title, e == null ? null : e.getMessage());
	}

	private void sendQuietlyHTML(final HttpServletResponse response) throws IOException {
		PrintWriter printWriter = null;
		try {
			try {
				printWriter = response.getWriter();
			} catch (IllegalStateException e) {
				printWriter = new PrintWriter(response.getOutputStream());
			}
			String message = StringEscapeUtils.escapeHtml4(error.message);
			response.setStatus(error.status);
			response.setContentType("text/html");
			printWriter.print("<html><head><title>");
			printWriter.print(error.title.title);
			printWriter.println("</title></head>");
			printWriter.print("<body><h3>");
			printWriter.print(error.title.title);
			printWriter.println("</h3>");
			printWriter.print("<pre>");
			printWriter.print(message);
			printWriter.println("</pre></body></html>");
		} finally {
			if (printWriter != null)
				printWriter.close();
		}
	}

	private void sendQuietlyXML(final HttpServletResponse response) throws IOException {
		XMLBuilder2 xml = XMLBuilder2.create("error").a("code", Integer.toString(error.status));
		if (error.title != null)
			xml.e("title").t(error.title.title).up();
		if (error.message != null)
			xml.e("message").t(error.message).up();
		xml.toWriter(true, response.getWriter(), null);
	}

	private void sendQuietlyJSON(final HttpServletResponse response) throws IOException {
		ObjectMappers.JSON.writeValue(response.getWriter(), error);
	}

	@Override
	public void sendQuietly(final HttpServletResponse response) {
		try {
			String contentType = response.getContentType();
			response.setStatus(error.status);
			if (contentType != null) {
				if (contentType.startsWith("application/xml")) {
					sendQuietlyXML(response);
					return;
				} else if (contentType.startsWith("application/json")) {
					sendQuietlyJSON(response);
					return;
				}
			}
			sendQuietlyHTML(response);
		} catch (IOException e) {
			logger.log(Level.WARNING, e, e::getMessage);
		}
	}

}
