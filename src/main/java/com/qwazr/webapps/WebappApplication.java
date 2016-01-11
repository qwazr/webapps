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

import com.qwazr.utils.server.ServletApplication;
import com.qwazr.webapps.transaction.WebappManager;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionListener;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.ServletInfo;
import org.apache.commons.lang.StringUtils;

import javax.servlet.MultipartConfigElement;
import java.util.ArrayList;
import java.util.List;

public class WebappApplication extends ServletApplication implements SessionListener {

	public WebappApplication() {
	}

	//TODO Parameters for fileupload limitation
	private final static MultipartConfigElement multipartConfigElement = new MultipartConfigElement(
			System.getProperty("java.io.tmpdir"));

	@Override
	protected List<ServletInfo> getServletInfos() {
		List<ServletInfo> servletInfos = new ArrayList<ServletInfo>();
		servletInfos.add(Servlets.servlet("WebAppServlet", WebappHttpServlet.class).addMapping("/*")
				.setMultipartConfig(multipartConfigElement));
		return servletInfos;
	}

	@Override
	protected SessionListener getSessionListener() {
		return this;
	}

	@Override
	public void sessionCreated(Session session, HttpServerExchange exchange) {
	}

	@Override
	public void sessionDestroyed(Session session, HttpServerExchange exchange, SessionDestroyedReason reason) {
		WebappManager.INSTANCE.destroySession(session.getId());
	}

	@Override
	public void attributeAdded(Session session, String name, Object value) {
	}

	@Override
	public void attributeUpdated(Session session, String name, Object newValue, Object oldValue) {
	}

	@Override
	public void attributeRemoved(Session session, String name, Object oldValue) {
	}

	@Override
	public void sessionIdChanged(Session session, String oldSessionId) {
	}

	@Override
	protected String getContextPath() {
		return StringUtils.EMPTY;
	}

}
