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
import java.io.IOException;

import com.qwazr.utils.StringUtils;

public class FilePathResolver {

	public static volatile FilePathResolver INSTANCE = null;

	public static void load(File rootDirectory, int depth) throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		INSTANCE = new FilePathResolver(rootDirectory, depth);
	}

	final File rootDirectory;

	private final int depth;

	FilePathResolver(File rootDirectory, int depth) {
		this.rootDirectory = rootDirectory;
		this.depth = depth;
	}

	FilePath find(String path) {
		if (StringUtils.isEmpty(path))
			return null;
		String[] paths = StringUtils.split(path, '/');
		if (paths.length < depth)
			return null;
		StringBuilder sbPath = new StringBuilder();
		File file = rootDirectory;
		int i = 0;
		while (i < depth) {
			if (i > 0)
				sbPath.append('/');
			sbPath.append(paths[i]);
			file = new File(file, paths[i++]);
			if (!file.exists())
				return null;
		}
		return new FilePath(paths, sbPath.toString(), file,
				path.substring(sbPath.length() + 1));
	}

	class FilePath {

		public final String[] paths;
		public final String contextPath;
		public final String requestPath;
		public final File fileDepth;

		private FilePath(String[] paths, String contextPath, File fileDepth,
				String requestPath) {
			this.paths = paths;
			this.contextPath = contextPath.intern();
			this.fileDepth = fileDepth;
			this.requestPath = requestPath;
		}

		File buildFile(String internalDirName) {
			File file = new File(fileDepth, internalDirName);
			int i = depth;
			while (i < paths.length)
				file = new File(file, paths[i++]);
			return file;
		}

		File buildFile(String internalDirName, String newPath) {
			File file = new File(fileDepth, internalDirName);
			return new File(file, newPath);
		}
	}
}
