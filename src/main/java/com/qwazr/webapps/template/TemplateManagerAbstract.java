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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.qwazr.webapps.exception.WebappHtmlException;
import com.qwazr.webapps.exception.WebappHtmlException.Title;

import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

public abstract class TemplateManagerAbstract implements TemplateLoader {

	protected final Configuration cfg;

	private final static String DEFAULT_CHARSET = "UTF-8";
	private final static String DEFAULT_TYPE = "text/html";

	public TemplateManagerAbstract() {
		cfg = new Configuration(Configuration.VERSION_2_3_21);
		cfg.setTemplateLoader(this);
		cfg.setOutputEncoding(DEFAULT_CHARSET);
		cfg.setDefaultEncoding(DEFAULT_CHARSET);
		cfg.setLocalizedLookup(false);
		cfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
	}

	final public void template(String templatePath, Map<?, ?> dataModel,
			HttpServletResponse response) throws TemplateException {
		try {
			String result = template(templatePath, dataModel);
			if (response.getContentType() == null)
				response.setContentType(DEFAULT_TYPE);
			response.setCharacterEncoding(DEFAULT_CHARSET);
			response.getWriter().print(result);
		} catch (FileNotFoundException e) {
			throw new WebappHtmlException(Title.VIEW_ERROR, e);
		} catch (IOException e) {
			throw new WebappHtmlException(Title.VIEW_ERROR, e);
		}
	}

	final private String template(String templatePath, Map<?, ?> dataModel)
			throws TemplateException, IOException {
		Template template = cfg.getTemplate(templatePath);
		StringWriter stringWriter = new StringWriter();
		try {
			template.process(dataModel, stringWriter);
			return stringWriter.toString();
		} finally {
			IOUtils.closeQuietly(stringWriter);
		}
	}
}
