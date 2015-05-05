/**   
 * License Agreement for QWAZR
 *
 * Copyright (C) 2014-2015 OpenSearchServer Inc.
 * 
 * http://www.qwazr.com
 * 
 * This file is part of QWAZR.
 *
 * QWAZR is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * QWAZR is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with QWAZR. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.qwazr.webapps.transaction.body;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response.Status;

import com.qwazr.webapps.exception.WebappHtmlException;
import com.qwazr.webapps.exception.WebappHtmlException.Title;

public interface HttpBodyInterface {

	public static HttpBodyInterface newEntity(HttpServletRequest request) {
		String contentType = request.getContentType();
		if ("application/x-www-form-urlencoded".equals(contentType))
			return new FormHttpBody(request);
		throw new WebappHtmlException(Status.NOT_ACCEPTABLE, Title.BODY_ERROR,
				"Not supported content type: " + contentType);
	}

}
