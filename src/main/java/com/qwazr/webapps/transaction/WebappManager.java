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
package com.qwazr.webapps.transaction;

import com.qwazr.classloader.ClassLoaderManager;
import com.qwazr.compiler.CompilerManager;
import com.qwazr.utils.LockUtils;
import com.qwazr.utils.file.TrackedInterface;
import com.qwazr.utils.json.JsonMapper;
import com.qwazr.utils.server.InFileSessionPersistenceManager;
import com.qwazr.utils.server.ServerBuilder;
import com.qwazr.utils.server.ServerException;
import com.qwazr.webapps.WebappHttpServlet;
import com.qwazr.webapps.WebappManagerServiceImpl;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.ListenerInfo;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.MultipartConfigElement;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class WebappManager {

	public final static String SERVICE_NAME_WEBAPPS = "webapps";

	public final static String SESSIONS_PERSISTENCE_DIR = "webapp-sessions";

	//TODO Parameters for fileupload limitation
	private final static MultipartConfigElement multipartConfigElement =
			new MultipartConfigElement(System.getProperty("java.io.tmpdir"));

	private static final Logger logger = LoggerFactory.getLogger(WebappManager.class);

	public static volatile GlobalConfiguration GLOBAL_CONF = null;

	public static volatile WebappManager INSTANCE = null;

	private static final String ACCESS_LOG_LOGGER_NAME = "com.qwazr.webapps.accessLogger";
	private static final Logger accessLogger = LoggerFactory.getLogger(ACCESS_LOG_LOGGER_NAME);

	public synchronized static void load(final ServerBuilder serverBuilder, final TrackedInterface etcTracker,
			final File tempDirectory) throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		ControllerManager.load(serverBuilder.getServerConfiguration().dataDirectory);
		StaticManager.load(serverBuilder.getServerConfiguration().dataDirectory);
		try {

			GLOBAL_CONF = new GlobalConfiguration(etcTracker);

			serverBuilder.registerWebService(WebappManagerServiceImpl.class);

			File sessionPersistenceDir = new File(tempDirectory, SESSIONS_PERSISTENCE_DIR);
			if (!sessionPersistenceDir.exists())
				sessionPersistenceDir.mkdir();
			serverBuilder.setSessionPersistenceManager(new InFileSessionPersistenceManager(sessionPersistenceDir));
			serverBuilder.registerServlet(Servlets.servlet("WebAppServlet", WebappHttpServlet.class).addMapping("/*")
					.setMultipartConfig(multipartConfigElement));

			final WebappDefinition webappDefinition = GLOBAL_CONF.getWebappDefinition();

			if (webappDefinition.listeners != null)
				for (String listenerClass : webappDefinition.listeners)
					serverBuilder.registerListener(new ListenerInfo(ClassLoaderManager.findClass(listenerClass)));
			serverBuilder.setServletAccessLogger(accessLogger);

			INSTANCE = new WebappManager(webappDefinition);

		} catch (ClassNotFoundException e) {
			throw new ServerException(e);
		}
	}

	public static WebappManager getInstance() {
		if (INSTANCE == null)
			throw new RuntimeException("The webapps service is not enabled");
		return INSTANCE;
	}

	private final ApplicationContext applicationContext;

	private WebappManager(final WebappDefinition webappDefinition) throws IOException, ServerException {
		this.applicationContext = new ApplicationContext(webappDefinition);
		if (logger.isInfoEnabled())
			logger.info("Load Web application");
	}

	final ApplicationContext getApplicationContext() throws IOException, URISyntaxException {
		return applicationContext;
	}

	public WebappDefinition getWebAppDefinition() throws IOException, URISyntaxException {
		return getApplicationContext().getWebappDefinition();
	}

	private static class GlobalConfiguration implements TrackedInterface.FileChangeConsumer {

		private final LockUtils.ReadWriteLock mapLock = new LockUtils.ReadWriteLock();

		private final TrackedInterface etcTracker;
		private final LinkedHashMap<File, WebappDefinition> webappFileMap;

		private GlobalConfiguration(final TrackedInterface etcTracker) {
			this.etcTracker = etcTracker;
			webappFileMap = new LinkedHashMap<>();
			etcTracker.register(this);
		}

		private WebappDefinition getWebappDefinition() {
			etcTracker.check();
			return mapLock.read(() -> WebappDefinition.merge(webappFileMap.values()));
		}

		private void loadWebappDefinition(final File jsonFile) {
			try {
				final WebappDefinition webappDefinition = JsonMapper.MAPPER.readValue(jsonFile, WebappDefinition.class);

				if (webappDefinition == null || webappDefinition.isEmpty()) {
					unloadWebappDefinition(jsonFile);
					return;
				}

				if (logger.isInfoEnabled())
					logger.info("Load WebApp configuration file: " + jsonFile.getAbsolutePath());

				mapLock.write(() -> {
					webappFileMap.put(jsonFile, webappDefinition);
				});

			} catch (IOException e) {
				if (logger.isErrorEnabled())
					logger.error(e.getMessage(), e);
			}
		}

		private void unloadWebappDefinition(final File jsonFile) {
			mapLock.write(() -> {
				final WebappDefinition webappDefinition = webappFileMap.remove(jsonFile);
				if (webappDefinition == null)
					return;
				if (logger.isInfoEnabled())
					logger.info("Unload WebApp configuration file: " + jsonFile.getAbsolutePath());
			});
		}

		@Override
		public void accept(final TrackedInterface.ChangeReason changeReason, final File jsonFile) {
			String extension = FilenameUtils.getExtension(jsonFile.getName());
			if (!"json".equals(extension))
				return;
			switch (changeReason) {
				case UPDATED:
					loadWebappDefinition(jsonFile);
					break;
				case DELETED:
					unloadWebappDefinition(jsonFile);
					break;
			}
		}
	}

}
