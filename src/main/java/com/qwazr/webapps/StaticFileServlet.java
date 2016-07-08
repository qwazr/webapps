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
package com.qwazr.webapps;

import org.apache.commons.io.IOUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class StaticFileServlet extends HttpServlet {

	public final static String STATIC_PATH_PARAM = "com.qwazr.webapps.static.path";

	private volatile File rootFile = null;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init();
		final String path = config.getInitParameter(STATIC_PATH_PARAM);
		if (path == null || path.isEmpty())
			throw new ServletException("The init-param " + STATIC_PATH_PARAM + " is missing.");
		if (path == null || path.isEmpty())
			rootFile = WebappManager.INSTANCE.dataDir;
		else if (path.startsWith("/"))
			rootFile = new File(path);
		else
			rootFile = new File(WebappManager.INSTANCE.dataDir, path);
	}

	private File handleFile(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		final String path = request.getPathInfo();
		if (path == null) {
			response.sendRedirect(request.getContextPath() + request.getServletPath() + "/index.html");
			return null;
		}
		final File staticFile = new File(rootFile, path);
		if (staticFile.isDirectory()) {
			response.sendRedirect(
					request.getContextPath() + request.getServletPath() + request.getPathInfo() + "index.html");
			return null;
		}
		if (!staticFile.exists() || !staticFile.isFile()) {
			response.sendError(404, "File not found: " + path);
			return null;
		}
		return staticFile;
	}

	final static void head(final Long length, final String type, final Long lastModified,
			final HttpServletResponse response) throws IOException {
		if (type != null)
			response.setContentType(type);
		if (length != null)
			response.setContentLengthLong(length);
		if (lastModified != null)
			response.setDateHeader("Last-Modified", lastModified);
		response.setHeader("Cache-Control", "max-age=86400");
		response.setDateHeader("Expires", System.currentTimeMillis() + 86400 * 1000);
	}

	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		final File staticFile = handleFile(request, response);
		if (staticFile == null)
			return;
		final String type = WebappManager.INSTANCE.mimeTypeMap.getContentType(staticFile);
		head(staticFile.length(), type, staticFile.lastModified(), response);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		final File staticFile = handleFile(request, response);
		if (staticFile == null)
			return;
		final String type = WebappManager.INSTANCE.mimeTypeMap.getContentType(staticFile);
		head(staticFile.length(), type, staticFile.lastModified(), response);
		try (final FileInputStream fis = new FileInputStream(staticFile)) {
			IOUtils.copy(fis, response.getOutputStream());
		}
	}
}
