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
