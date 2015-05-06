/**
 * Copyright 2014-2015 OpenSearchServer Inc.
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
import java.io.IOException;
import java.io.InputStream;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.io.IOUtils;

import com.qwazr.webapps.transaction.FilePathResolver.FilePath;

public class StaticManager {

	static volatile StaticManager INSTANCE = null;

	public static void load() throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		INSTANCE = new StaticManager();
	}

	private final MimetypesFileTypeMap mimeTypeMap;

	private StaticManager() {
		mimeTypeMap = new MimetypesFileTypeMap();
	}

	File findStatic(FilePath filePath) {
		if (filePath == null)
			return null;
		File file = filePath.buildFile("static");
		if (file == null)
			return null;
		if (!file.exists())
			return null;
		if (!file.isFile())
			return null;
		return file;
	}

	void handle(WebappResponse response, File staticFile) throws IOException {
		String type = mimeTypeMap.getContentType(staticFile);
		if (type != null)
			response.setContentType(type);
		response.setContentLengthLong(staticFile.length());
		response.setDateHeader("Last-Modified", staticFile.lastModified());
		InputStream inputStream = new FileInputStream(staticFile);
		try {
			IOUtils.copy(inputStream, response.getOutputStream());
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

}
