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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.PrivilegedActionException;

import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.qwazr.webapps.exception.WebappHtmlException;
import com.qwazr.webapps.transaction.FilePathResolver.FilePath;
import com.qwazr.webapps.transaction.body.HttpBodyInterface;

public class WebappTransaction {

	private final ApplicationContext context;
	private final WebappHttpRequest request;
	private final FilePath filePath;
	private final WebappResponse response;

	public WebappTransaction(HttpServletRequest request,
			HttpServletResponse response, HttpBodyInterface body)
			throws JsonParseException, JsonMappingException, IOException {
		this.filePath = FilePathResolver.INSTANCE.find(request.getPathInfo());
		if (filePath == null)
			throw new FileNotFoundException();
		this.response = new WebappResponse(response, filePath);
		this.context = ApplicationContextManager.INSTANCE.applyConf(filePath,
				this);
		if (context == null)
			throw new FileNotFoundException("No application found");
		this.request = new WebappHttpRequestImpl(context, filePath, request,
				body);
		this.response.variable("request", this.request);
		this.response.variable("response", this.response);
		this.response.variable("session", this.request.getSession());
	}

	WebappHttpRequest getRequest() {
		return request;
	}

	WebappResponse getResponse() {
		return response;
	}

	public void execute() throws IOException, URISyntaxException,
			ScriptException, PrivilegedActionException {
		StaticManager staticManager = StaticManager.INSTANCE;
		File staticFile = staticManager.findStatic(filePath);
		if (staticFile != null) {
			staticManager.handle(response, staticFile);
			return;
		}
		ControllerManager controllerManager = ControllerManager.INSTANCE;
		File controllerFile = controllerManager.findController(context,
				request, filePath);
		if (controllerFile != null) {
			controllerManager.handle(response, controllerFile);
			return;
		}
		throw new WebappHtmlException(Status.NOT_FOUND);
	}
}
