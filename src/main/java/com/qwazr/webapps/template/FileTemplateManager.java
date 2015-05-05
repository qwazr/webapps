/**   
 * License Agreement for OpenSearchServer
 *
 * Copyright (C) 2015 OpenSearchServer Inc.
 * 
 * http://www.opensearchserver.com
 * 
 * This file is part of OpenSearchServer.
 *
 * OpenSearchServer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * OpenSearchServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with OpenSearchServer. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.qwazr.webapps.template;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

public class FileTemplateManager extends TemplateManagerAbstract {

	public static volatile FileTemplateManager INSTANCE = null;

	public static void load(File rootDirectory) throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		INSTANCE = new FileTemplateManager(rootDirectory);
	}

	private final File rootDirectory;

	public FileTemplateManager(File rootDirectory) {
		this.rootDirectory = rootDirectory;
	}

	@Override
	final public Object findTemplateSource(String name) {
		File file = new File(rootDirectory, name);
		return file.exists() && file.isFile() ? file : null;
	}

	@Override
	final public long getLastModified(Object templateSource) {
		return ((File) templateSource).lastModified();
	}

	@Override
	final public Reader getReader(Object templateSource, String encoding)
			throws IOException {
		return new FileReader((File) templateSource);
	}

	@Override
	final public void closeTemplateSource(Object templateSource)
			throws IOException {
	}

}
