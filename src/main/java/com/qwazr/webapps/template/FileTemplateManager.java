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
