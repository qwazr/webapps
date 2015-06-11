/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
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
package com.qwazr.webapps.transaction;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.qwazr.connectors.ConnectorManager;
import com.qwazr.tools.ToolsManager;
import com.qwazr.utils.LockUtils;

public class ApplicationContext {

	private final String contextPath;

	private final Map<String, WebappHttpSessionImpl> sessions;

	private final List<PathBind> controllerMatchers;

	private final List<PathBind> staticMatchers;

	private final LockUtils.ReadWriteLock sessionsLock = new LockUtils.ReadWriteLock();

	ApplicationContext(String contextPath, WebappDefinition webappDefinition,
			ApplicationContext oldContext) throws JsonParseException,
			JsonMappingException, IOException {
		this.contextPath = contextPath.intern();

		// Load the resources
		controllerMatchers = PathBind
				.loadMatchers(webappDefinition.controllers);
		staticMatchers = PathBind.loadMatchers(webappDefinition.statics);

		// Prepare the sessions
		this.sessions = oldContext != null ? oldContext.sessions
				: new HashMap<String, WebappHttpSessionImpl>();
	}

	void close() {
		if (controllerMatchers != null)
			controllerMatchers.clear();
	}

	WebappHttpSession getSessionOrCreate(HttpSession session) {
		if (session == null)
			return null;
		String id = session.getId().intern();
		sessionsLock.r.lock();
		try {
			WebappHttpSession webappSession = sessions.get(id);
			if (webappSession != null)
				return webappSession;
		} finally {
			sessionsLock.r.unlock();
		}
		sessionsLock.w.lock();
		try {
			WebappHttpSessionImpl webappSession = sessions.get(id);
			if (webappSession != null)
				return webappSession;
			webappSession = new WebappHttpSessionImpl(this, id);
			sessions.put(id, webappSession);
			return webappSession;
		} finally {
			sessionsLock.w.unlock();
		}
	}

	void invalidateSession(String sessionId) {
		sessionsLock.w.lock();
		try {
			sessions.remove(sessionId);
		} finally {
			sessionsLock.w.unlock();
		}
	}

	final public void apply(WebappResponse response) {
		response.variable("connectors",
				ConnectorManager.INSTANCE.getReadOnlyMap());
		response.variable("tools", ToolsManager.INSTANCE.getReadOnlyMap());
	}

	public String getContextPath() {
		return contextPath;
	}

	String findStatic(String requestPath) {
		return PathBind.findMatchingPath(requestPath, staticMatchers);
	}

	String findController(String requestPath) {
		return PathBind.findMatchingPath(requestPath, controllerMatchers);
	}

	public String getContextId() {
		String cpath = contextPath;
		if (cpath.endsWith("/"))
			cpath = cpath.substring(0, cpath.length() - 1);
		return cpath;
	}

}
