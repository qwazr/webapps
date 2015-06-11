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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.qwazr.connectors.ConnectorManager;
import com.qwazr.tools.ToolsManager;
import com.qwazr.utils.LockUtils;

public class ApplicationContext {

	private final String contextPath;

	private final Map<String, WebappHttpSessionImpl> sessions;

	private final List<Pair<Matcher, File>> controllerMatchers;

	private final List<Pair<Matcher, File>> staticMatchers;

	private final LockUtils.ReadWriteLock sessionsLock = new LockUtils.ReadWriteLock();

	ApplicationContext(String contextPath, WebappDefinition webappDefinition,
			ApplicationContext oldContext) throws JsonParseException,
			JsonMappingException, IOException {
		this.contextPath = contextPath.intern();

		// Load the resources
		controllerMatchers = loadMatchers(webappDefinition.controllers);
		staticMatchers = loadMatchers(webappDefinition.statics);

		// Prepare the sessions
		this.sessions = oldContext != null ? oldContext.sessions
				: new HashMap<String, WebappHttpSessionImpl>();
	}

	/**
	 * Load the matcher map by reading the configuration file
	 * 
	 * @param configuration
	 * @return the matcher map
	 */
	private static List<Pair<Matcher, File>> loadMatcherConf(
			Map<String, String> patternMap, List<Pair<Matcher, File>> matchers) {
		if (patternMap == null)
			return null;
		if (matchers == null)
			matchers = new ArrayList<Pair<Matcher, File>>(patternMap.size());
		for (Map.Entry<String, String> entry : patternMap.entrySet()) {
			String patternString = entry.getKey();
			File destination = new File(ControllerManager.INSTANCE.dataDir,
					entry.getValue());
			Matcher matcher = Pattern.compile(patternString).matcher(
					StringUtils.EMPTY);
			matchers.add(Pair.of(matcher, destination));
		}
		return matchers;
	}

	private static List<Pair<Matcher, File>> loadMatchers(
			Map<String, String> patternMap) {
		List<Pair<Matcher, File>> matchers = null;
		if (patternMap != null)
			matchers = loadMatcherConf(patternMap, matchers);
		return matchers;
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

	static File findMatchingFile(String requestPath,
			List<Pair<Matcher, File>> matchers) {
		if (matchers == null)
			return null;
		for (Pair<Matcher, File> matcher : matchers)
			if (matcher.getLeft().reset(requestPath).find())
				return matcher.getRight();
		return null;
	}

	File findStatic(String requestPath) {
		return findMatchingFile(requestPath, staticMatchers);
	}

	File findController(String requestPath) {
		return findMatchingFile(requestPath, controllerMatchers);
	}

	public String getContextId() {
		String cpath = contextPath;
		if (cpath.endsWith("/"))
			cpath = cpath.substring(0, cpath.length() - 1);
		return cpath;
	}

}
