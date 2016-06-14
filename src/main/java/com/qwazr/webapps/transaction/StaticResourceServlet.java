/**
 * Copyright 2015-2016 Emmanuel Keller / QWAZR
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

import org.apache.commons.io.IOUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class StaticResourceServlet extends HttpServlet {

	final static String STATIC_RESOURCE_PARAM = "com.qwazr.webapps.static.resource";

	private volatile String resourcePrefix = null;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init();
		resourcePrefix = config.getInitParameter(STATIC_RESOURCE_PARAM);
		if (resourcePrefix == null || resourcePrefix.isEmpty())
			throw new ServletException("The init-param " + STATIC_RESOURCE_PARAM + " is missing.");
	}

	private InputStream findResource(final HttpServletRequest request) throws IOException {
		final String path = request.getServletPath();
		final InputStream input = StaticResourceServlet.class.getResourceAsStream(resourcePrefix + path);
		if (input == null)
			throw new FileNotFoundException("File not found: " + path);
		return input;
	}

	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try (final InputStream input = findResource(request)) {
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try (final InputStream input = findResource(request)) {
			final String type = WebappManager.INSTANCE.mimeTypeMap.getContentType(request.getServletPath());
			StaticFileServlet.head(null, type, null, response);
			IOUtils.copy(input, response.getOutputStream());
		}
	}
}
