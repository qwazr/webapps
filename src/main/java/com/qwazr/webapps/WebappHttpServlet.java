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
package com.qwazr.webapps;

import com.qwazr.utils.IOUtils;
import com.qwazr.webapps.exception.AbstractWebappException;
import com.qwazr.webapps.transaction.WebappTransaction;
import com.qwazr.webapps.transaction.body.HttpBodyInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.EventLogger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WebappHttpServlet extends HttpServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 4286830782439792427L;

	private static final Logger logger = LoggerFactory.getLogger(WebappHttpServlet.class);

	private void handle(HttpServletRequest request, HttpServletResponse response, boolean body) {
		WebappTransaction transaction = null;
		try {
			final long time = System.currentTimeMillis();
			HttpBodyInterface bodyEntity = null;
			if (body)
				bodyEntity = HttpBodyInterface.newEntity(request);
			transaction = new WebappTransaction(request, response, bodyEntity);
			transaction.execute();
			EventLogger.logEvent(new WebappLogger(request, response, System.currentTimeMillis() - time));
		} catch (Exception e) {
			AbstractWebappException.newInstance(e).sendQuietly(response);
			logger.error(request.getRequestURI() + "\t" + response.getStatus(), e);
		} catch (Error e) {
			AbstractWebappException.newInstance(e).sendQuietly(response);
			logger.error(request.getRequestURI() + "\t" + response.getStatus(), e);
		} finally {
			IOUtils.close(transaction);
		}
	}

	@Override
	public void doDelete(HttpServletRequest request, HttpServletResponse response) {
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
	public void doOptions(HttpServletRequest request, HttpServletResponse response) {
		handle(request, response, false);
	}

	@Override
	public void doTrace(HttpServletRequest request, HttpServletResponse response) {
		handle(request, response, false);
	}

}
