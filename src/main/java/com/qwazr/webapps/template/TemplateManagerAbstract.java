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
