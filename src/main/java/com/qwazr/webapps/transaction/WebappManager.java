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

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.fasterxml.jackson.jaxrs.xml.JacksonXMLProvider;
import com.qwazr.classloader.ClassLoaderManager;
import com.qwazr.library.LibraryManager;
import com.qwazr.utils.ClassLoaderUtils;
import com.qwazr.utils.FunctionUtils;
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.file.TrackedInterface;
import com.qwazr.utils.json.JacksonConfig;
import com.qwazr.utils.server.GenericServer;
import com.qwazr.utils.server.InFileSessionPersistenceManager;
import com.qwazr.utils.server.ServerBuilder;
import com.qwazr.utils.server.ServerException;
import com.qwazr.webapps.WebappManagerServiceImpl;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.ConstructorInstanceFactory;
import org.apache.commons.io.FilenameUtils;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimetypesFileTypeMap;
import javax.management.MBeanPermission;
import javax.management.MBeanServerPermission;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.net.SocketPermission;
import java.net.URISyntaxException;
import java.security.AccessControlContext;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Objects;

public class WebappManager {

	public final static String SERVICE_NAME_WEBAPPS = "webapps";

	public final static String SESSIONS_PERSISTENCE_DIR = "webapp-sessions";

	//TODO Parameters for fileupload limitation
	private final static MultipartConfigElement multipartConfigElement =
			new MultipartConfigElement(System.getProperty("java.io.tmpdir"));

	private static final Logger logger = LoggerFactory.getLogger(WebappManager.class);

	public static volatile WebappManager INSTANCE = null;

	private static final String ACCESS_LOG_LOGGER_NAME = "com.qwazr.webapps.accessLogger";
	private static final Logger accessLogger = LoggerFactory.getLogger(ACCESS_LOG_LOGGER_NAME);

	final static String FAVICON_PATH = "/favicon.ico";

	public synchronized static void load(final ServerBuilder serverBuilder, final TrackedInterface etcTracker,
			final File tempDirectory) throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");

		try {
			INSTANCE = new WebappManager(etcTracker, serverBuilder, tempDirectory);
		} catch (ReflectiveOperationException e) {
			throw new ServerException(e);
		}
	}

	public static WebappManager getInstance() {
		if (INSTANCE == null)
			throw new RuntimeException("The webapps service is not enabled");
		return INSTANCE;
	}

	final File dataDir;
	final MimetypesFileTypeMap mimeTypeMap;
	final WebappDefinition webappDefinition;

	final GlobalConfiguration globalConfiguration;
	final ScriptEngine scriptEngine;

	private WebappManager(final TrackedInterface etcTracker, final ServerBuilder serverBuilder,
			final File tempDirectory) throws IOException, ServerException, ReflectiveOperationException {
		if (logger.isInfoEnabled())
			logger.info("Loading Web application");

		// Load the configuration
		globalConfiguration = new GlobalConfiguration(etcTracker);
		webappDefinition = globalConfiguration.getWebappDefinition();

		dataDir = serverBuilder.getServerConfiguration().dataDirectory;
		mimeTypeMap = new MimetypesFileTypeMap(getClass().getResourceAsStream("/com/qwazr/webapps/mime.types"));

		// Register the webservice
		serverBuilder.registerWebService(WebappManagerServiceImpl.class);

		File sessionPersistenceDir = new File(tempDirectory, SESSIONS_PERSISTENCE_DIR);
		if (!sessionPersistenceDir.exists())
			sessionPersistenceDir.mkdir();
		serverBuilder.setSessionPersistenceManager(new InFileSessionPersistenceManager(sessionPersistenceDir));

		// Load the static handlers
		if (webappDefinition.statics != null)
			webappDefinition.statics
					.forEach((urlPath, filePath) -> serverBuilder.registerServlet(getStaticServlet(urlPath, filePath)));

		// Prepare the Javascript interpreter
		final ScriptEngineManager manager = new ScriptEngineManager();
		scriptEngine = manager.getEngineByName("nashorn");

		// Load the listeners
		if (webappDefinition.listeners != null)
			for (String listenerClass : webappDefinition.listeners)
				serverBuilder.registerListener(Servlets.listener(ClassLoaderManager.findClass(listenerClass)));
		serverBuilder.setServletAccessLogger(accessLogger);

		// Load the controllers
		if (webappDefinition.controllers != null)
			webappDefinition.controllers
					.forEach((urlPath, filePath) -> serverBuilder.registerServlet(getController(urlPath, filePath)));

		// Load the closable filter
		serverBuilder.registerFilter("/*", Servlets.filter(CloseableFilter.class));

		// Load the filters
		if (webappDefinition.filters != null)
			FunctionUtils.forEach(webappDefinition.filters, (urlPath, filterClass) -> serverBuilder
					.registerFilter(urlPath, Servlets.filter(ClassLoaderManager.findClass(filterClass))));

		// Load the identityManager provider if any
		if (webappDefinition.identity_manager != null)
			serverBuilder.setIdentityManagerProvider((GenericServer.IdentityManagerProvider) ClassLoaderManager
					.findClass(webappDefinition.identity_manager).newInstance());

		// Set the default favicon
		serverBuilder.registerServlet(getDefaultFaviconServlet());

	}

	private ServletInfo getStaticServlet(final String urlPath, final String path) {
		if (path.contains(".") && !path.contains("/"))
			return Servlets.servlet(StaticResourceServlet.class.getName() + '@' + urlPath, StaticResourceServlet.class)
					.addInitParam(StaticResourceServlet.STATIC_RESOURCE_PARAM,
							'/' + StringUtils.replaceChars(path, '.', '/')).addMapping(urlPath);
		else
			return Servlets.servlet(StaticFileServlet.class.getName() + '@' + urlPath, StaticFileServlet.class)
					.addInitParam(StaticFileServlet.STATIC_PATH_PARAM, path).addMapping(urlPath);
	}

	private ServletInfo getDefaultFaviconServlet() {
		return Servlets.servlet(StaticResourceServlet.class.getName() + '@' + FAVICON_PATH, StaticResourceServlet.class)
				.addInitParam(StaticResourceServlet.STATIC_RESOURCE_PARAM, "/com/qwazr/webapps/favicon.ico")
				.addMapping(FAVICON_PATH);
	}

	private ServletInfo getController(final String urlPath, final String filePath) {
		try {
			String ext = FilenameUtils.getExtension(filePath).toLowerCase();
			if ("js".equals(ext))
				return getJavascriptServlet(urlPath, filePath);
			else
				return getJavaController(urlPath, filePath);
		} catch (ReflectiveOperationException e) {
			throw new ServerException("Cannot build an instance of the controller: " + filePath, e);
		}
	}

	private ServletInfo getJavascriptServlet(final String urlPath, final String filePath) {
		return Servlets.servlet(JavascriptServlet.class.getName() + '@' + urlPath, JavascriptServlet.class)
				.addInitParam(JavascriptServlet.JAVASCRIPT_PATH_PARAM, filePath).addMapping(urlPath)
				.setMultipartConfig(multipartConfigElement);
	}

	private ServletInfo getJavaController(final String urlPath, final String classDef)
			throws ReflectiveOperationException {
		if (classDef.contains(" "))
			return getJavaJaxRsClassServlet(urlPath, classDef);
		final Class<?> clazz = ClassLoaderUtils.findClass(ClassLoaderManager.classLoader, classDef);
		Objects.requireNonNull(clazz, "Class not found: " + classDef);
		if (Servlet.class.isAssignableFrom(clazz))
			return getJavaServlet(urlPath, (Class<? extends Servlet>) clazz);
		else if (Application.class.isAssignableFrom(clazz))
			return getJavaJaxRsAppServlet(urlPath, clazz);
		else if (clazz.isAnnotationPresent(Path.class))
			return getJavaJaxRsClassServlet(urlPath, classDef);
		throw new ServerException("This type of class is not supported: " + classDef + " / " + clazz.getName());
	}

	private ServletInfo getJavaServlet(final String urlPath, final Class<? extends Servlet> servletClass)
			throws NoSuchMethodException {
		return Servlets.servlet(servletClass.getName() + '@' + urlPath, servletClass, new ServletFactory(servletClass))
				.addMapping(urlPath).setMultipartConfig(multipartConfigElement);
	}

	private ServletInfo getJavaJaxRsAppServlet(final String urlPath, final Class<?> clazz)
			throws NoSuchMethodException {
		return Servlets.servlet(ServletContainer.class.getName() + '@' + urlPath, ServletContainer.class,
				new ServletFactory(ServletContainer.class)).addInitParam("javax.ws.rs.Application", clazz.getName())
				.setAsyncSupported(true).addMapping(urlPath);
	}

	private String JACKSON_DEFAULT_PROVIDERS =
			JacksonConfig.class.getName() + ' ' + JacksonXMLProvider.class.getName() + ' ' + JacksonJsonProvider.class
					.getName();

	private ServletInfo getJavaJaxRsClassServlet(final String urlPath, final String classList)
			throws NoSuchMethodException {
		return Servlets.servlet(ServletContainer.class.getName() + '@' + urlPath, ServletContainer.class,
				new ServletFactory(ServletContainer.class))
				.addInitParam("jersey.config.server.provider.classnames", classList + ' ' + JACKSON_DEFAULT_PROVIDERS)
				.setAsyncSupported(true).addMapping(urlPath);
	}

	class ServletFactory<T extends Servlet> extends ConstructorInstanceFactory<T> {

		ServletFactory(final Class<T> clazz) throws NoSuchMethodException {
			super(clazz.getDeclaredConstructor());
		}

		@Override
		public InstanceHandle<T> createInstance() throws InstantiationException {
			final InstanceHandle<T> instance = super.createInstance();
			LibraryManager.inject(instance.getInstance());
			return instance;
		}
	}

	public WebappDefinition getWebAppDefinition() throws IOException, URISyntaxException {
		return webappDefinition;
	}

	static class RestrictedAccessControlContext {

		public static final AccessControlContext INSTANCE;

		static {
			Permissions pm = new Permissions();
			// Required by Cassandra
			pm.add(new RuntimePermission("modifyThread"));
			pm.add(new MBeanServerPermission("createMBeanServer"));
			pm.add(new MBeanPermission("com.codahale.metrics.JmxReporter$JmxCounter#-[*:*]", "registerMBean"));
			pm.add(new MBeanPermission("com.codahale.metrics.JmxReporter$JmxCounter#-[*:*]", "unregisterMBean"));
			pm.add(new MBeanPermission("com.codahale.metrics.JmxReporter$JmxGauge#-[*:*]", "registerMBean"));
			pm.add(new MBeanPermission("com.codahale.metrics.JmxReporter$JmxGauge#-[*:*]", "unregisterMBean"));
			pm.add(new MBeanPermission("com.codahale.metrics.JmxReporter$JmxTimer#-[*:*]", "registerMBean"));
			pm.add(new MBeanPermission("com.codahale.metrics.JmxReporter$JmxTimer#-[*:*]", "unregisterMBean"));
			pm.add(new SocketPermission("*.qwazr.net:9042", "connect,accept,resolve"));

			// Required for templates
			pm.add(new FilePermission("<<ALL FILES>>", "read"));

			INSTANCE = new AccessControlContext(
					new ProtectionDomain[] { new ProtectionDomain(new CodeSource(null, (Certificate[]) null), pm) });
		}
	}
}
