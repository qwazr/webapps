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
package com.qwazr.webapps;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qwazr.webapps.exception.AbstractWebappException;
import com.qwazr.webapps.transaction.WebappTransaction;
import com.qwazr.webapps.transaction.body.HttpBodyInterface;

public class WebappHttpServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4286830782439792427L;

	private static final Logger logger = LoggerFactory
			.getLogger(WebappHttpServlet.class);

	private void handle(HttpServletRequest request,
			HttpServletResponse response, boolean body) {
		try {
			HttpBodyInterface bodyEntity = null;
			if (body)
				bodyEntity = HttpBodyInterface.newEntity(request);
			new WebappTransaction(request, response, bodyEntity).execute();
			logger.info(request.getRequestURI() + "\t" + response.getStatus());
		} catch (Exception e) {
			AbstractWebappException.newInstance(e).sendQuietly(response);
			logger.error(request.getRequestURI() + "\t" + response.getStatus());
		} catch (Error e) {
			AbstractWebappException.newInstance(e).sendQuietly(response);
			logger.error(request.getRequestURI() + "\t" + response.getStatus());
		}
	}

	@Override
	public void doDelete(HttpServletRequest request,
			HttpServletResponse response) {
		handle(request, response, false);
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		handle(request, response, false);
	}

	@Override
	public void doHead(HttpServletRequest request, HttpServletResponse response) {
		handle(request, response, false);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		handle(request, response, true);
	}

	@Override
	public void doPut(HttpServletRequest request, HttpServletResponse response) {
		handle(request, response, true);
	}

	@Override
	public void doOptions(HttpServletRequest request,
			HttpServletResponse response) {
		handle(request, response, false);
	}

	@Override
	public void doTrace(HttpServletRequest request, HttpServletResponse response) {
		handle(request, response, false);
	}

}
