/*
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
 */
package com.qwazr.webapps;

import com.qwazr.server.ServerException;
import com.qwazr.utils.StringUtils;
import org.apache.commons.io.IOUtils;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;

public class StaticFileServlet extends HttpServlet {

	private final MimetypesFileTypeMap mimeTypeMap;
	private final File rootFile;

	public StaticFileServlet(final MimetypesFileTypeMap mimeTypeMap, final File dataDirectory, final String path) {
		this.mimeTypeMap = mimeTypeMap;
		if (path == null || StringUtils.isBlank(path))
			throw new ServerException("The path is empty");
		if (Paths.get(path).isAbsolute())
			rootFile = new File(path);
		else
			rootFile = new File(dataDirectory, path);
		if (!rootFile.exists())
			throw new ServerException("Cannot initialize the static path: " + path + " - The path does not exists " +
					rootFile.getAbsolutePath());
	}

	private File handleFile(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		final String contextPath = request.getContextPath();
		final String servletPath = request.getServletPath();
		final String pathInfo = request.getPathInfo();
		final File staticFile;
		final String fullPath;
		if (pathInfo == null) {
			staticFile = rootFile;
			fullPath = contextPath + servletPath;
		} else {
			staticFile = new File(rootFile, pathInfo);
			fullPath = contextPath + servletPath + pathInfo;
		}
		if (staticFile.isDirectory()) {
			if (new File(staticFile, "index.html").exists()) {
				final boolean slashEnd = fullPath.endsWith("/");
				response.sendRedirect(fullPath + (slashEnd ? "index.html" : "/index.html"));
			}
			return null;
		}
		if (!staticFile.exists() || !staticFile.isFile()) {
			response.sendError(404, "File not found: " + fullPath);
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
	protected void doHead(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		final File staticFile = handleFile(request, response);
		if (staticFile == null)
			return;
		final String type = mimeTypeMap.getContentType(staticFile);
		head(staticFile.length(), type, staticFile.lastModified(), response);
	}

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		final File staticFile = handleFile(request, response);
		if (staticFile == null)
			return;
		final String type = mimeTypeMap.getContentType(staticFile);
		head(staticFile.length(), type, staticFile.lastModified(), response);
		try (final FileInputStream fis = new FileInputStream(staticFile)) {
			final ServletOutputStream out = response.getOutputStream();
			IOUtils.copy(fis, out);
			out.flush();
		}
	}
}
