/*
 * Copyright 2015-2017 Emmanuel Keller / QWAZR
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.webapps.body;

import com.qwazr.utils.LoggerUtils;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class XMLHttpBody extends InputStreamHttpBody {

	private static final Logger logger = LoggerUtils.getLogger(XMLHttpBody.class);

	XMLHttpBody(HttpServletRequest request) throws IOException, ServletException {
		super(request);
	}

	public Document getDom(Boolean validating, Boolean namespaceAware)
			throws IOException, SAXException, ParserConfigurationException {
		final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		if (namespaceAware)
			documentBuilderFactory.setNamespaceAware(namespaceAware);
		if (validating)
			documentBuilderFactory.setValidating(validating);
		DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
		builder.setErrorHandler(ToolErrorHandler.INSTANCE);
		return builder.parse(inputStream);
	}

	private static class ToolErrorHandler implements ErrorHandler {

		private static final ToolErrorHandler INSTANCE = new ToolErrorHandler();

		@Override
		public void warning(SAXParseException exception) throws SAXException {
			logger.log(Level.WARNING, exception, exception::getMessage);
		}

		@Override
		public void error(SAXParseException exception) throws SAXException {
			logger.log(Level.SEVERE, exception, exception::getMessage);
		}

		@Override
		public void fatalError(SAXParseException exception) throws SAXException {
			throw exception;
		}
	}
}
