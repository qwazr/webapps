/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
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
package com.qwazr.webapps.transaction;

import com.qwazr.utils.FileClassCompilerLoader;
import com.qwazr.utils.LockUtils;

import javax.servlet.http.HttpSession;
import java.io.Closeable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationContext implements Closeable, AutoCloseable {

	private final String contextPath;

	private final WebappDefinition webappDefinition;

	private final Map<String, WebappHttpSessionImpl> sessions;

	private final List<PathBind> controllerMatchers;

	private final List<PathBind> staticMatchers;

	private final FileClassCompilerLoader compilerLoader;

	private final LockUtils.ReadWriteLock sessionsLock = new LockUtils.ReadWriteLock();

	ApplicationContext(String contextPath, WebappDefinition webappDefinition, ApplicationContext oldContext)
					throws IOException, URISyntaxException {
		this.contextPath = contextPath.intern();
		this.webappDefinition = webappDefinition;

		// Load the resources
		controllerMatchers = PathBind.loadMatchers(webappDefinition.controllers);
		staticMatchers = PathBind.loadMatchers(webappDefinition.statics);

		if (webappDefinition.javac != null && webappDefinition.javac.source_root != null) {
			compilerLoader = FileClassCompilerLoader.newInstance(webappDefinition.javac);
		} else {
			compilerLoader = null;
		}
		// Prepare the sessions
		this.sessions = oldContext != null ? oldContext.sessions : new HashMap<String, WebappHttpSessionImpl>();
	}

	WebappDefinition getWebappDefinition() {
		return webappDefinition;
	}

	public void close() throws IOException {
		if (controllerMatchers != null)
			controllerMatchers.clear();
		if (compilerLoader != null)
			compilerLoader.close();
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

	FileClassCompilerLoader getCompilerLoader() {
		return compilerLoader;
	}

	void invalidateSession(String sessionId) {
		sessionsLock.w.lock();
		try {
			sessions.remove(sessionId);
		} finally {
			sessionsLock.w.unlock();
		}
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
