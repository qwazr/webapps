/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
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
 **/
package com.qwazr.webapps.transaction.body;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class XMLHttpBody extends InputStreamHttpBody {

    XMLHttpBody(HttpServletRequest request) throws IOException, ServletException {
	super(request);
    }

    public Document getDom(Boolean validating, Boolean namespaceAware) throws IOException, SAXException,
	    ParserConfigurationException {
	final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
	if (namespaceAware)
	    documentBuilderFactory.setNamespaceAware(namespaceAware);
	if (validating)
	    documentBuilderFactory.setValidating(validating);
	DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
	return builder.parse(inputStream);
    }
}
