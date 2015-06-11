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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.io.IOUtils;

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
		mimeTypeMap = new MimetypesFileTypeMap();
	}

	File findStatic(ApplicationContext context, String requestPath)
			throws URISyntaxException, IOException {
		// First we try to find the root directory using configuration mapping
		String staticPath = context.findStatic(requestPath);
		if (staticPath == null)
			return null;
		File staticFile = new File(dataDir, staticPath);
		if (!staticFile.exists())
			throw new FileNotFoundException("File not found");
		if (!staticFile.isFile())
			throw new FileNotFoundException("File not found");
		return staticFile;
	}

	void handle(WebappResponse response, File staticFile) throws IOException {
		String type = mimeTypeMap.getContentType(staticFile);
		if (type != null)
			response.setContentType(type);
		response.setContentLengthLong(staticFile.length());
		response.setDateHeader("Last-Modified", staticFile.lastModified());
		response.setHeader("Cache-Control", "1");
		InputStream inputStream = new FileInputStream(staticFile);
		try {
			IOUtils.copy(inputStream, response.getOutputStream());
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}
}
