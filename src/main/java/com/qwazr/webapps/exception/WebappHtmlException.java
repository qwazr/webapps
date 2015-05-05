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
package com.qwazr.webapps.exception;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import javax.script.ScriptException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import com.datastax.driver.core.exceptions.QueryValidationException;

import freemarker.template.TemplateException;

public class WebappHtmlException extends AbstractWebappException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1221758741584043195L;

	public static enum Title {

		ERROR, SCRIPT_ERROR, QUERY_ERROR, VIEW_ERROR, BODY_ERROR, NOT_FOUND_ERROR;

		private String title = name().replace('_', ' ');

	}

	private final int status;
	private final Title title;
	private final String message;

	public WebappHtmlException(Status status, Title title, String message) {
		this.status = status.getStatusCode();
		this.title = title == null ? Title.ERROR : title;
		this.message = message == null ? status.getReasonPhrase() : message;
	}

	public WebappHtmlException(Status status) {
		this(status, (Title) null, (String) null);
	}

	public WebappHtmlException(Status status, Title title, Throwable e) {
		this(status, title, e == null ? null : e.getMessage());
	}

	public WebappHtmlException(Title title, Throwable e) {
		this(Status.INTERNAL_SERVER_ERROR, title, e);
	}

	public WebappHtmlException(FileNotFoundException e) {
		this(Status.NOT_FOUND, Title.NOT_FOUND_ERROR, e);
	}

	public WebappHtmlException(ScriptException e) {
		this(Status.INTERNAL_SERVER_ERROR, Title.SCRIPT_ERROR, e);
	}

	public WebappHtmlException(QueryValidationException e) {
		this(Status.INTERNAL_SERVER_ERROR, Title.QUERY_ERROR, e);
	}

	public WebappHtmlException(TemplateException e) {
		this(Status.INTERNAL_SERVER_ERROR, Title.VIEW_ERROR, e);
	}

	@Override
	public void sendQuietly(HttpServletResponse response) {
		PrintWriter printWriter = null;
		try {
			String message = StringEscapeUtils.escapeHtml4(this.message);
			response.setStatus(status);
			response.setContentType("text/html");
			printWriter = new PrintWriter(response.getWriter());
			printWriter.print("<html><head><title>");
			printWriter.print(title.title);
			printWriter.println("</title></head>");
			printWriter.print("<body><h3>");
			printWriter.print(title.title);
			printWriter.println("</h3>");
			printWriter.print("<div>");
			printWriter.print(message);
			printWriter.println("</div></body></html>");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (printWriter != null)
				IOUtils.closeQuietly(printWriter);
		}
	}

}
