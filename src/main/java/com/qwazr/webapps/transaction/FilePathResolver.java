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
