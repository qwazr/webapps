/**
 * Copyright 2014-2015 OpenSearchServer Inc.
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
import com.qwazr.utils.json.JsonMapper;
import com.qwazr.webapps.WebappConfigurationFile;
import com.qwazr.webapps.transaction.FilePathResolver.FilePath;

public class ApplicationContext {

	private final static String CONF_FILE = "configuration.json";

	private final Long lastModified;

	private final String rootPath;

	private final String contextPath;

	public final File contextDirectory;

	private final File configurationFile;

	private final Map<String, WebappHttpSessionImpl> sessions;

	private final List<Pair<Matcher, String>> controllerMatchers;

	private final LockUtils.ReadWriteLock sessionsLock = new LockUtils.ReadWriteLock();

	ApplicationContext(String rootPath, String contextPath,
			WebappConfigurationFile globalConfiguration, File contextDirectory,
			ApplicationContext oldContext) throws JsonParseException,
			JsonMappingException, IOException {
		this.contextDirectory = contextDirectory;
		this.rootPath = rootPath;
		this.contextPath = contextPath.intern();

		// Check the configuration file
		WebappConfigurationFile configuration = null;
		configurationFile = new File(contextDirectory, CONF_FILE);
		if (configurationFile != null && configurationFile.exists()
				&& configurationFile.isFile()) {
			lastModified = configurationFile.lastModified();
			configuration = JsonMapper.MAPPER.readValue(configurationFile,
					WebappConfigurationFile.class);

		} else
			lastModified = null;

		// Load the resources
		controllerMatchers = loadControllers(globalConfiguration, configuration);

		// Prepare the sessions
		this.sessions = oldContext != null ? oldContext.sessions
				: new HashMap<String, WebappHttpSessionImpl>();
	}

	/**
	 * Load the controller map by reading the configuration file
	 * 
	 * @param configuration
	 * @return the controller map
	 */
	private static List<Pair<Matcher, String>> loadControllersConf(
			WebappConfigurationFile configuration,
			List<Pair<Matcher, String>> controllerMatchers) {
		if (configuration == null || configuration.controllers == null)
			return null;
		if (controllerMatchers == null)
			controllerMatchers = new ArrayList<Pair<Matcher, String>>(
					configuration.controllers.size());
		for (Map.Entry<String, List<String>> entry : configuration.controllers
				.entrySet()) {
			String controller = entry.getKey().intern();
			for (String patternString : entry.getValue()) {
				Matcher matcher = Pattern.compile(patternString).matcher(
						StringUtils.EMPTY);
				controllerMatchers.add(Pair.of(matcher, controller));
			}
		}
		return controllerMatchers;
	}

	private static List<Pair<Matcher, String>> loadControllers(
			WebappConfigurationFile globalConf,
			WebappConfigurationFile contextConf) {
		List<Pair<Matcher, String>> controllerMatchers = null;
		if (globalConf != null)
			controllerMatchers = loadControllersConf(globalConf,
					controllerMatchers);
		if (contextConf != null)
			controllerMatchers = loadControllersConf(contextConf,
					controllerMatchers);
		return controllerMatchers;
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

	public String getRootPath() {
		return rootPath;
	}

	public String getContextPath() {
		return contextPath;
	}

	String findController(FilePath filePath) {
		if (controllerMatchers == null)
			return null;
		for (Pair<Matcher, String> controllerMatcher : controllerMatchers)
			if (controllerMatcher.getLeft().reset(filePath.requestPath).find())
				return controllerMatcher.getRight();
		return null;
	}

	boolean mustBeReloaded() {
		boolean confFileExists = configurationFile != null
				&& configurationFile.exists() && configurationFile.isFile();
		if (lastModified == null)
			return confFileExists;
		return lastModified != configurationFile.lastModified();
	}

	public String getContextId() {
		String cpath = contextPath;
		if (cpath.endsWith("/"))
			cpath = cpath.substring(0, cpath.length() - 1);
		return cpath;
	}

	public File getContextDirectory() {
		return contextDirectory;
	}

}
