/**
 * Copyright 2015-2017 Emmanuel Keller / QWAZR
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

import com.qwazr.library.LibraryManager;
import com.qwazr.server.ApplicationBuilder;
import com.qwazr.server.GenericServer;
import com.qwazr.server.InFileSessionPersistenceManager;
import com.qwazr.server.ServerException;
import com.qwazr.server.ServletInfoBuilder;
import com.qwazr.server.configuration.ServerConfiguration;
import com.qwazr.utils.ClassLoaderUtils;
import com.qwazr.utils.FunctionUtils;
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.SubstitutedVariables;
import io.swagger.jaxrs.config.SwaggerContextService;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.ServletInfo;
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
import java.util.Collection;
import java.util.Objects;

public class WebappManager {

	public final static String SERVICE_NAME_WEBAPPS = "webapps";

	public final static String SESSIONS_PERSISTENCE_DIR = "webapp-sessions";

	//TODO Parameters for fileupload limitation
	private final static MultipartConfigElement multipartConfigElement =
			new MultipartConfigElement(System.getProperty("java.io.tmpdir"));

	private static final Logger logger = LoggerFactory.getLogger(WebappManager.class);

	private static final String ACCESS_LOG_LOGGER_NAME = "com.qwazr.webapps.accessLogger";
	private static final Logger accessLogger = LoggerFactory.getLogger(ACCESS_LOG_LOGGER_NAME);

	private final WebappServiceInterface service;

	final static String FAVICON_PATH = "/favicon.ico";

	final File dataDir;
	final MimetypesFileTypeMap mimeTypeMap;
	final WebappDefinition webappDefinition;

	final LibraryManager libraryManager;

	final GlobalConfiguration globalConfiguration;
	final ScriptEngine scriptEngine;

	public WebappManager(final LibraryManager libraryManager, final GenericServer.Builder builder)
			throws IOException, ServerException, ReflectiveOperationException {
		if (logger.isInfoEnabled())
			logger.info("Loading Web application");

		this.libraryManager = libraryManager;

		final ServerConfiguration configuration = builder.getConfiguration();
		final Collection<File> etcFiles = configuration.getEtcFiles();

		// Load the configuration
		globalConfiguration = new GlobalConfiguration();
		if (etcFiles != null)
			etcFiles.forEach(globalConfiguration::loadWebappDefinition);
		webappDefinition = globalConfiguration.getWebappDefinition();

		dataDir = configuration.dataDirectory;
		mimeTypeMap = new MimetypesFileTypeMap(getClass().getResourceAsStream("/com/qwazr/webapps/mime.types"));

		// Register the webservice
		builder.webService(WebappServiceImpl.class);

		File sessionPersistenceDir = new File(configuration.tempDirectory, SESSIONS_PERSISTENCE_DIR);
		if (!sessionPersistenceDir.exists())
			sessionPersistenceDir.mkdir();
		builder.sessionPersistenceManager(new InFileSessionPersistenceManager(sessionPersistenceDir));

		// Load the static handlers
		if (webappDefinition.statics != null)
			webappDefinition.statics.forEach((urlPath, filePath) -> builder.servlet(
					getStaticServlet(urlPath, SubstitutedVariables.propertyAndEnvironmentSubstitute(filePath))));

		// Prepare the Javascript interpreter
		final ScriptEngineManager manager = new ScriptEngineManager();
		scriptEngine = manager.getEngineByName("nashorn");

		// Load the listeners
		if (webappDefinition.listeners != null)
			for (String listenerClass : webappDefinition.listeners)
				builder.listener(Servlets.listener(ClassLoaderUtils.findClass(listenerClass)));
		builder.servletAccessLogger(accessLogger);

		// Load the controllers
		if (webappDefinition.controllers != null)
			webappDefinition.controllers.forEach((urlPath, filePath) -> registerController(urlPath, filePath, builder));

		// Load the closable filter
		builder.filter("/*", Servlets.filter(CloseableFilter.class));

		// Load the filters
		if (webappDefinition.filters != null)
			FunctionUtils.forEach(webappDefinition.filters, (urlPath, filterClass) -> builder.filter(urlPath,
					Servlets.filter(ClassLoaderUtils.findClass(filterClass))));

		// Load the identityManager provider if any
		if (webappDefinition.identity_manager != null)
			builder.identityManagerProvider((GenericServer.IdentityManagerProvider) ClassLoaderUtils.findClass(
					webappDefinition.identity_manager).newInstance());

		// Set the default favicon
		builder.servlet(getDefaultFaviconServlet());

		builder.contextAttribute(this);

		service = new WebappServiceImpl(this);
	}

	public WebappServiceInterface getService() {
		return service;
	}

	private ServletInfo getStaticServlet(final String urlPath, final String path) {
		if (path.contains(".") && !path.contains("/"))
			return new ServletInfo(StaticResourceServlet.class.getName() + '@' + urlPath,
					StaticResourceServlet.class).addInitParam(StaticResourceServlet.STATIC_RESOURCE_PARAM,
					'/' + StringUtils.replaceChars(path, '.', '/')).addMapping(urlPath);
		else
			return new ServletInfo(StaticFileServlet.class.getName() + '@' + urlPath,
					StaticFileServlet.class).addInitParam(StaticFileServlet.STATIC_PATH_PARAM, path)
					.addMapping(urlPath);
	}

	private ServletInfo getDefaultFaviconServlet() {
		return new ServletInfo(StaticResourceServlet.class.getName() + '@' + FAVICON_PATH,
				StaticResourceServlet.class).addInitParam(StaticResourceServlet.STATIC_RESOURCE_PARAM,
				"/com/qwazr/webapps/favicon.ico").addMapping(FAVICON_PATH);
	}

	private void registerController(final String urlPath, final String filePath, final GenericServer.Builder builder) {
		try {
			String ext = FilenameUtils.getExtension(filePath).toLowerCase();
			if ("js".equals(ext))
				registerJavascriptServlet(urlPath, SubstitutedVariables.propertyAndEnvironmentSubstitute(filePath),
						builder);
			else
				registerJavaController(urlPath, filePath, builder);
		} catch (ReflectiveOperationException e) {
			throw new ServerException("Cannot build an instance of the controller: " + filePath, e);
		}
	}

	private void registerJavascriptServlet(final String urlPath, final String filePath,
			final GenericServer.Builder builder) {
		builder.servlet(new ServletInfo(JavascriptServlet.class.getName() + '@' + urlPath,
				JavascriptServlet.class).addInitParam(JavascriptServlet.JAVASCRIPT_PATH_PARAM, filePath)
				.addMapping(urlPath)
				.setMultipartConfig(multipartConfigElement));
	}

	private void registerJavaController(final String urlPath, final String classDef,
			final GenericServer.Builder builder) throws ReflectiveOperationException {
		if (classDef.contains(" ") || classDef.contains(" ,")) {
			registerJavaJaxRsClassServlet(urlPath, classDef, builder);
			return;
		}
		final Class<?> clazz = ClassLoaderUtils.findClass(classDef);
		Objects.requireNonNull(clazz, "Class not found: " + classDef);
		if (Servlet.class.isAssignableFrom(clazz)) {
			registerJavaServlet(urlPath, (Class<? extends Servlet>) clazz, builder);
			return;
		} else if (Application.class.isAssignableFrom(clazz)) {
			registerJavaJaxRsAppServlet(urlPath, (Class<? extends Application>) clazz, builder);
			return;
		} else if (clazz.isAnnotationPresent(Path.class)) {
			registerJavaJaxRsClassServlet(urlPath, classDef, builder);
			return;
		}
		throw new ServerException("This type of class is not supported: " + classDef + " / " + clazz.getName());
	}

	private <T extends Servlet> void registerJavaServlet(final String urlPath, final Class<T> servletClass,
			final GenericServer.Builder builder) throws NoSuchMethodException {
		final ServletInfo servletInfo = ServletInfoBuilder.servlet(null, servletClass)
				.addMapping(urlPath)
				.setMultipartConfig(multipartConfigElement)
				.setLoadOnStartup(1);
		servletInfo.setInstanceFactory(new ServletLibraryFactory<>(libraryManager, servletClass));
		builder.servlet(servletInfo);
	}

	private ServletInfo addSwaggerContext(String urlPath, final ServletInfo servletInfo) {
		final String contextId = ServletContainer.class.getName() + '@' + urlPath;
		urlPath = StringUtils.removeEnd(urlPath, "*");
		urlPath = StringUtils.removeEnd(urlPath, "/");
		return servletInfo.addInitParam(SwaggerContextService.SCANNER_ID_KEY, contextId)
				.addInitParam(SwaggerContextService.CONFIG_ID_KEY, contextId)
				.addInitParam(SwaggerContextService.CONTEXT_ID_KEY, contextId)
				.addInitParam("swagger.api.basepath", urlPath);
	}

	private void registerJavaJaxRsAppServlet(final String urlPath, final Class<? extends Application> appClass,
			final GenericServer.Builder builder) throws NoSuchMethodException {
		final ServletInfo servletInfo =
				ServletInfoBuilder.jaxrs(ServletContainer.class.getName() + '@' + urlPath, appClass)
						.addMapping(urlPath)
						.setLoadOnStartup(1);
		addSwaggerContext(urlPath, servletInfo);
		builder.servlet(servletInfo);
	}

	private void registerJavaJaxRsClassServlet(final String urlPath, final String classList,
			final GenericServer.Builder builder) throws ReflectiveOperationException {
		final ApplicationBuilder appBuilder = new ApplicationBuilder(urlPath);
		final String[] classes = StringUtils.split(classList, " ,");
		for (String className : classes)
			appBuilder.classes(ClassLoaderUtils.findClass(className.trim()));
		registerJaxRsResources(appBuilder, builder);
	}

	public void registerJaxRsResources(final ApplicationBuilder applicationBuilder, final GenericServer.Builder builder)
			throws ReflectiveOperationException {
		applicationBuilder.classes(BaseRestApplication.PROVIDERS_CLASSES);
		final ServletInfo servletInfo = ServletInfoBuilder.
				jaxrs(null, applicationBuilder);
		final Collection<String> paths = applicationBuilder.getApplicationPaths();
		if (paths != null && !paths.isEmpty())
			addSwaggerContext(paths.iterator().next(), servletInfo);
		builder.servlet(servletInfo);
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
