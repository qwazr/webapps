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

import com.qwazr.webapps.exception.WebappHtmlException;
import com.qwazr.webapps.exception.WebappHtmlException.Title;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;

public interface HttpBodyInterface {

    public static HttpBodyInterface newEntity(HttpServletRequest request) throws IOException, ServletException {
	String contentType = request.getContentType();
	if (contentType != null) {
	    if ("application/x-www-form-urlencoded".equals(contentType))
		return new FormHttpBody(request);
	    if (contentType.startsWith("multipart/form-data"))
		return new MultipartHttpBody(request);
	    if (contentType.startsWith("application/xml"))
		return new XMLHttpBody(request);
	}
	throw new WebappHtmlException(Status.NOT_ACCEPTABLE, Title.BODY_ERROR, "Not supported content type: "
		+ contentType);
    }

}
