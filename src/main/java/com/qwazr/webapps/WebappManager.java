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

import com.qwazr.classloader.ClassLoaderManager;
import com.qwazr.library.LibraryManager;
import com.qwazr.utils.AnnotationsUtils;
import com.qwazr.utils.ClassLoaderUtils;
import com.qwazr.utils.FunctionUtils;
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.file.TrackedInterface;
import com.qwazr.utils.server.*;
import io.swagger.jersey.config.JerseyJaxrsConfig;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.ConstructorInstanceFactory;
import org.apache.commons.io.FilenameUtils;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimetypesFileTypeMap;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
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
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

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
			webappDefinition.statics.forEach(
					(urlPath, filePath) -> serverBuilder.registerServlet(getStaticServlet(urlPath, filePath)));

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
			webappDefinition.controllers.forEach(
					(urlPath, filePath) -> registerController(urlPath, filePath, serverBuilder));

		// Load the closable filter
		serverBuilder.registerFilter("/*", Servlets.filter(CloseableFilter.class));

		// Load the filters
		if (webappDefinition.filters != null)
			FunctionUtils.forEach(webappDefinition.filters,
					(urlPath, filterClass) -> serverBuilder.registerFilter(urlPath,
							Servlets.filter(ClassLoaderManager.findClass(filterClass))));

		// Load the identityManager provider if any
		if (webappDefinition.identity_manager != null)
			serverBuilder.setIdentityManagerProvider(
					(GenericServer.IdentityManagerProvider) ClassLoaderManager.findClass(
							webappDefinition.identity_manager).newInstance());

		// Set the default favicon
		serverBuilder.registerServlet(getDefaultFaviconServlet());
	}

	private SecurableServletInfo getStaticServlet(final String urlPath, final String path) {
		if (path.contains(".") && !path.contains("/"))
			return (SecurableServletInfo) SecurableServletInfo.servlet(
					StaticResourceServlet.class.getName() + '@' + urlPath, StaticResourceServlet.class)
					.addInitParam(StaticResourceServlet.STATIC_RESOURCE_PARAM,
							'/' + StringUtils.replaceChars(path, '.', '/'))
					.addMapping(urlPath);
		else
			return (SecurableServletInfo) SecurableServletInfo.servlet(
					StaticFileServlet.class.getName() + '@' + urlPath, StaticFileServlet.class)
					.addInitParam(StaticFileServlet.STATIC_PATH_PARAM, path)
					.addMapping(urlPath);
	}

	private SecurableServletInfo getDefaultFaviconServlet() {
		return (SecurableServletInfo) SecurableServletInfo.servlet(
				StaticResourceServlet.class.getName() + '@' + FAVICON_PATH, StaticResourceServlet.class)
				.addInitParam(StaticResourceServlet.STATIC_RESOURCE_PARAM, "/com/qwazr/webapps/favicon.ico")
				.addMapping(FAVICON_PATH);
	}

	private void registerController(final String urlPath, final String filePath, final ServerBuilder serverBuilder) {
		try {
			String ext = FilenameUtils.getExtension(filePath).toLowerCase();
			if ("js".equals(ext))
				registerJavascriptServlet(urlPath, filePath, serverBuilder);
			else
				registerJavaController(urlPath, filePath, serverBuilder);
		} catch (ReflectiveOperationException e) {
			throw new ServerException("Cannot build an instance of the controller: " + filePath, e);
		}
	}

	private void registerJavascriptServlet(final String urlPath, final String filePath,
			final ServerBuilder serverBuilder) {
		serverBuilder.registerServlet(
				(SecurableServletInfo) SecurableServletInfo.servlet(JavascriptServlet.class.getName() + '@' + urlPath,
						JavascriptServlet.class)
						.addInitParam(JavascriptServlet.JAVASCRIPT_PATH_PARAM, filePath)
						.addMapping(urlPath)
						.setMultipartConfig(multipartConfigElement));
	}

	private void registerJavaController(final String urlPath, final String classDef, final ServerBuilder serverBuilder)
			throws ReflectiveOperationException {
		if (classDef.contains(" ") || classDef.contains(" ,")) {
			registerJavaJaxRsClassServlet(urlPath, classDef, serverBuilder);
			return;
		}
		final Class<?> clazz = ClassLoaderUtils.findClass(ClassLoaderManager.classLoader, classDef);
		Objects.requireNonNull(clazz, "Class not found: " + classDef);
		if (Servlet.class.isAssignableFrom(clazz)) {
			registerJavaServlet(urlPath, (Class<? extends Servlet>) clazz, serverBuilder);
			return;
		} else if (Application.class.isAssignableFrom(clazz)) {
			registerJavaJaxRsAppServlet(urlPath, (Class<? extends Application>) clazz, serverBuilder);
			return;
		} else if (clazz.isAnnotationPresent(Path.class)) {
			registerJavaJaxRsClassServlet(urlPath, classDef, serverBuilder);
			return;
		}
		throw new ServerException("This type of class is not supported: " + classDef + " / " + clazz.getName());
	}

	private boolean isSecurable(Class<?> clazz) {
		return AnnotationsUtils.getFirstAnnotation(clazz, RolesAllowed.class) != null
				|| AnnotationsUtils.getFirstAnnotation(clazz, PermitAll.class) != null
				|| AnnotationsUtils.getFirstAnnotation(clazz, DenyAll.class) != null;
	}

	private void registerJavaServlet(final String urlPath, final Class<? extends Servlet> servletClass,
			final ServerBuilder serverBuilder) throws NoSuchMethodException {
		serverBuilder.registerServlet(
				(SecurableServletInfo) SecurableServletInfo.servlet(servletClass.getName() + '@' + urlPath,
						servletClass, new ServletFactory(servletClass))
						.setSecure(isSecurable(servletClass))
						.addMapping(urlPath)
						.setMultipartConfig(multipartConfigElement)
						.setLoadOnStartup(1));
	}

	private ServletInfo addSwaggerContext(final String basePath, final String contextId,
			final ServletInfo servletInfo) {
		return servletInfo.addInitParam("swagger.scanner.id", contextId)
				.addInitParam("swagger.config.id", contextId).addInitParam("swagger.context.id", contextId)
				.addInitParam("swagger.api.basepath", basePath);
	}

	private void registerWithSwagger(String urlPath, final SecurableServletInfo servletInfo,
			final ServerBuilder serverBuilder)
			throws NoSuchMethodException {

		urlPath = StringUtils.removeEnd(urlPath, "*");
		urlPath = StringUtils.removeEnd(urlPath, "/");
		final String contextId = ServletContainer.class.getName() + '@' + urlPath;

		addSwaggerContext(urlPath, contextId, servletInfo);
		serverBuilder.registerServlet(servletInfo);

		SecurableServletInfo swaggerServletInfo = (SecurableServletInfo) SecurableServletInfo
				.servlet(JerseyJaxrsConfig.class.getName(), JerseyJaxrsConfig.class,
						new ServletFactory(JerseyJaxrsConfig.class))
				.setSecure(isSecurable(JerseyJaxrsConfig.class))
				.setLoadOnStartup(2);
		addSwaggerContext(urlPath, contextId, swaggerServletInfo);
		serverBuilder.registerServlet(swaggerServletInfo);
	}

	private void registerJavaJaxRsAppServlet(final String urlPath, final Class<? extends Application> appClass,
			final ServerBuilder serverBuilder) throws NoSuchMethodException {
		final SecurableServletInfo servletInfo = (SecurableServletInfo) SecurableServletInfo.servlet(
				ServletContainer.class.getName() + '@' + urlPath, ServletContainer.class,
				new ServletFactory(ServletContainer.class))
				.setSecure(isSecurable(appClass))
				.addInitParam("javax.ws.rs.Application", appClass.getName())
				.setAsyncSupported(true)
				.addMapping(urlPath).setLoadOnStartup(1);
		registerWithSwagger(urlPath, servletInfo, serverBuilder);
	}

	private void registerJavaJaxRsClassServlet(final String urlPath, final String classList,
			final ServerBuilder serverBuilder) throws NoSuchMethodException, ClassNotFoundException {
		final String[] classes = StringUtils.split(classList, " ,");
		final String resources = BaseRestApplication.joinResources(classes);
		final SecurableServletInfo servletInfo = (SecurableServletInfo) SecurableServletInfo.servlet(
				ServletContainer.class.getName() + '@' + urlPath, ServletContainer.class,
				new ServletFactory(ServletContainer.class))
				.addInitParam("jersey.config.server.provider.classnames", resources)
				.setAsyncSupported(true)
				.addMapping(urlPath).setLoadOnStartup(1);
		final Set<Class<?>> classSet = new LinkedHashSet<>();
		for (String clazz : classes) {
			Class<?> cl = ClassLoaderManager.findClass(clazz);
			classSet.add(cl);
			if (isSecurable(cl))
				servletInfo.setSecure(true);
		}
		registerWithSwagger(urlPath, servletInfo, serverBuilder);
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
					new ProtectionDomain[]{new ProtectionDomain(new CodeSource(null, (Certificate[]) null), pm)});
		}
	}
}
