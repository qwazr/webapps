/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import com.qwazr.webapps.transaction.body.HttpBodyInterface;

public class WebappTransaction {

	private final ApplicationContext context;
	private final WebappHttpRequest request;
	private final WebappResponse response;

	public WebappTransaction(HttpServletRequest request,
			HttpServletResponse response, HttpBodyInterface body)
			throws JsonParseException, JsonMappingException, IOException {
		this.context = WebappManager.INSTANCE.findApplicationContext(
				request.getPathInfo(), this);
		this.response = new WebappResponse(response);
		if (context == null)
			throw new FileNotFoundException("No application found");
		this.request = new WebappHttpRequestImpl(context, request, body);
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
		String pathInfo = request.getPathInfo();
		StaticManager staticManager = StaticManager.INSTANCE;
		File staticFile = staticManager.findStatic(context, pathInfo);
		if (staticFile != null) {
			staticManager.handle(response, staticFile);
			return;
		}
		ControllerManager controllerManager = ControllerManager.INSTANCE;
		File controllerFile = controllerManager.findController(context,
				pathInfo);
		if (controllerFile != null) {
			controllerManager.handle(response, controllerFile);
			return;
		}
		throw new WebappHtmlException(Status.NOT_FOUND);
	}
}
