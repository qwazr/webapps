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

import com.qwazr.utils.LockUtils;
import com.qwazr.utils.file.TrackedDirectory;
import com.qwazr.utils.file.TrackedInterface;
import com.qwazr.utils.json.JsonMapper;
import com.qwazr.utils.server.InFileSessionPersistenceManager;
import com.qwazr.utils.server.ServerBuilder;
import com.qwazr.utils.server.ServerException;
import com.qwazr.webapps.WebappHttpServlet;
import com.qwazr.webapps.WebappManagerServiceImpl;
import io.undertow.servlet.Servlets;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.MultipartConfigElement;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class WebappManager implements TrackedInterface.FileChangeConsumer {

	public final static String SERVICE_NAME_WEBAPPS = "webapps";

	public final static String SESSIONS_PERSISTENCE_DIR = "webapp-sessions";

	//TODO Parameters for fileupload limitation
	private final static MultipartConfigElement multipartConfigElement =
			new MultipartConfigElement(System.getProperty("java.io.tmpdir"));

	private static final Logger logger = LoggerFactory.getLogger(WebappManager.class);

	public static volatile WebappManager INSTANCE = null;

	public synchronized static void load(final ServerBuilder serverBuilder, final TrackedDirectory etcTracker,
			final File tempDirectory) throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		ControllerManager.load(serverBuilder.getServerConfiguration().dataDirectory);
		StaticManager.load(serverBuilder.getServerConfiguration().dataDirectory);
		try {
			INSTANCE = new WebappManager(etcTracker, tempDirectory);
			etcTracker.register(INSTANCE);
			serverBuilder.registerWebService(WebappManagerServiceImpl.class);

			File sessionPersistenceDir = new File(tempDirectory, SESSIONS_PERSISTENCE_DIR);
			if (!sessionPersistenceDir.exists())
				sessionPersistenceDir.mkdir();
			serverBuilder.setSessionPersistenceManager(new InFileSessionPersistenceManager(sessionPersistenceDir));
			serverBuilder.registerServlet(Servlets.servlet("WebAppServlet", WebappHttpServlet.class).addMapping("/*")
					.setMultipartConfig(multipartConfigElement));

		} catch (ServerException e) {
			throw new RuntimeException(e);
		}
	}

	public static WebappManager getInstance() {
		if (INSTANCE == null)
			throw new RuntimeException("The webapps service is not enabled");
		return INSTANCE;
	}

	private final TrackedDirectory etcTracker;

	private volatile ApplicationContext applicationContext;

	private final LockUtils.ReadWriteLock mapLock = new LockUtils.ReadWriteLock();
	private final Map<File, WebappDefinition> webappFileMap;

	private WebappManager(TrackedDirectory etcTracker, File tempDirectory) throws IOException, ServerException {
		this.webappFileMap = new HashMap<>();
		this.applicationContext = null;
		this.etcTracker = etcTracker;
	}

	private ApplicationContext buildApplicationContext() {
		if (webappFileMap.isEmpty())
			return null;
		WebappDefinition globalWebapp = WebappDefinition.merge((webappFileMap.values()));
		if (logger.isInfoEnabled())
			logger.info("Load Web application");
		return new ApplicationContext(globalWebapp);
	}

	public WebappDefinition getWebAppDefinition() throws IOException {
		etcTracker.check();
		ApplicationContext ac = applicationContext;
		return ac == null ? null : ac.getWebappDefinition();
	}

	ApplicationContext getApplicationContext() throws IOException, URISyntaxException {
		return applicationContext;
	}

	@Override
	public void accept(TrackedInterface.ChangeReason changeReason, File jsonFile) {
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

	private void loadWebappDefinition(File jsonFile) {
		try {
			final WebappDefinition webappDefinition = JsonMapper.MAPPER.readValue(jsonFile, WebappDefinition.class);

			if (webappDefinition == null || webappDefinition.isEmpty()) {
				unloadWebappDefinition(jsonFile);
				return;
			}

			if (logger.isInfoEnabled())
				logger.info("Load WebApp configuration file: " + jsonFile.getAbsolutePath());

			mapLock.w.lock();
			try {
				webappFileMap.put(jsonFile, webappDefinition);
				applicationContext = buildApplicationContext();
			} finally {
				mapLock.w.unlock();
			}

		} catch (IOException e) {
			if (logger.isErrorEnabled())
				logger.error(e.getMessage(), e);
			return;
		}
	}

	private void unloadWebappDefinition(File jsonFile) {
		final WebappDefinition webappDefinition;
		mapLock.w.lock();
		try {
			webappDefinition = webappFileMap.remove(jsonFile);
			if (webappDefinition == null)
				return;
			if (logger.isInfoEnabled())
				logger.info("Unload WebApp configuration file: " + jsonFile.getAbsolutePath());
			applicationContext = buildApplicationContext();
		} finally {
			mapLock.w.unlock();
		}
	}
}
