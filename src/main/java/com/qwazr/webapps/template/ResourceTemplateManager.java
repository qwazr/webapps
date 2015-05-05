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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.servlet.ServletContext;

import freemarker.cache.TemplateLoader;

public class ResourceTemplateManager extends TemplateManagerAbstract {

	private final String template_prefix;
	private final ServletContext context;
	private final long lastModified;

	public ResourceTemplateManager(ServletContext context,
			String template_prefix) {
		this.context = context;
		this.template_prefix = template_prefix;
		this.lastModified = System.currentTimeMillis();
	}

	@Override
	final public Object findTemplateSource(String name) throws IOException {
		Object stream = context.getResourceAsStream(template_prefix + name);
		if (stream == null)
			stream = TemplateLoader.class.getResourceAsStream(template_prefix
					+ name);
		return stream;
	}

	@Override
	final public long getLastModified(Object templateSource) {
		return lastModified;
	}

	@Override
	final public Reader getReader(Object templateSource, String encoding)
			throws IOException {
		return new InputStreamReader((InputStream) templateSource, encoding);
	}

	@Override
	final public void closeTemplateSource(Object templateSource)
			throws IOException {
		((InputStream) templateSource).close();
	}

}
