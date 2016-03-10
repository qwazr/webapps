/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
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
 **/
package com.qwazr.webapps.transaction.body;

import com.qwazr.webapps.exception.WebappException;
import com.qwazr.webapps.exception.WebappException.Title;
import org.apache.http.entity.ContentType;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;

public interface HttpBodyInterface {

	static HttpBodyInterface newEntity(HttpServletRequest request) throws IOException, ServletException {
		final String contentTypeString = request.getContentType();
		if (contentTypeString != null) {
			final ContentType contentType = ContentType.parse(contentTypeString);
			final String mimeType = contentType.getMimeType();
			if ("application/x-www-form-urlencoded".equals(mimeType))
				return new FormHttpBody(request);
			if ("multipart/form-data".equals(mimeType))
				return new MultipartHttpBody(request);
			if ("application/xml".equals(mimeType))
				return new XMLHttpBody(request);
		}
		throw new WebappException(Status.NOT_ACCEPTABLE, Title.BODY_ERROR,
				"Not supported content type: " + contentTypeString);
	}

}
