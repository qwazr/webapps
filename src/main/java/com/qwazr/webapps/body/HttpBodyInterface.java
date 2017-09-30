/*
 * Copyright 2015-2017 Emmanuel Keller / QWAZR
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.webapps.body;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

public interface HttpBodyInterface {

	static HttpBodyInterface newEntity(HttpServletRequest request) throws IOException, ServletException {
		final String contentTypeString = request.getContentType();
		if (contentTypeString == null)
			return null;
		final MimeType mimeType;
		try {
			mimeType = new MimeType(contentTypeString);
		} catch (MimeTypeParseException e) {
			throw new IOException(e);
		}
		final String type = mimeType.getBaseType();
		if (MediaType.APPLICATION_FORM_URLENCODED.equals(type))
			return new FormHttpBody(request);
		if (MediaType.MULTIPART_FORM_DATA.equals(type))
			return new MultipartHttpBody(request);
		if (MediaType.APPLICATION_XML.equals(type))
			return new XMLHttpBody(request);
		return null;
	}

}
