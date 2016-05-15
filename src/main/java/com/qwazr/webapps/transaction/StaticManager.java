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

import org.apache.commons.io.IOUtils;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;
import java.net.URISyntaxException;

public class StaticManager {

	static volatile StaticManager INSTANCE = null;

	public static void load(File dataDir) throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		INSTANCE = new StaticManager(dataDir);
	}

	private final File dataDir;

	private final MimetypesFileTypeMap mimeTypeMap;

	private StaticManager(File dataDir) {
		this.dataDir = dataDir;
		mimeTypeMap = new MimetypesFileTypeMap(getClass().getResourceAsStream("/com/qwazr/webapps/mime.types"));
	}

	final File findStatic(final ApplicationContext context, final String requestPath)
			throws URISyntaxException, IOException {
		// First we try to find the root directory using configuration mapping
		final String staticPath = context.findStatic(requestPath);
		if (staticPath == null)
			return null;
		final File staticFile = staticPath.startsWith("/") ? new File(staticPath) : new File(dataDir, staticPath);
		if (!staticFile.exists())
			throw new FileNotFoundException("File not found");
		if (!staticFile.isFile())
			throw new FileNotFoundException("File not found");
		return staticFile;
	}

	final void handle(final WebappHttpResponse response, final Long length, final String type, final Long lastModified,
			final InputStream inputStream) throws IOException {
		if (type != null)
			response.setContentType(type);
		if (length != null)
			response.setContentLengthLong(length);
		if (lastModified != null)
			response.setDateHeader("Last-Modified", lastModified);
		response.setHeader("Cache-Control", "max-age=86400");
		response.setDateHeader("Expires", System.currentTimeMillis() + 86400 * 1000);
		IOUtils.copy(inputStream, response.getOutputStream());

	}

	final void handle(final WebappHttpResponse response, final File staticFile) throws IOException {
		try (final InputStream inputStream = new FileInputStream(staticFile)) {
			final String type = mimeTypeMap.getContentType(staticFile);
			handle(response, staticFile.length(), type, staticFile.lastModified(), inputStream);
		}
	}

	final static String FAVICON = "/favicon.ico";

	final boolean handleDefaultResource(final WebappHttpResponse response, final String requestPath)
			throws IOException {
		if (requestPath == null)
			return false;
		if (!FAVICON.equals(requestPath))
			return false;
		try (final InputStream inputStream = getClass().getResourceAsStream("/com/qwazr/webapps" + FAVICON)) {
			if (inputStream == null)
				return false;
			final String type = mimeTypeMap.getContentType(FAVICON);
			handle(response, null, type, null, inputStream);
			return true;
		}
	}
}
