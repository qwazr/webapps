/*
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
 */
package com.qwazr.webapps;

import com.qwazr.library.LibraryManager;
import com.qwazr.server.ApplicationBuilder;
import com.qwazr.server.GenericFactory;
import com.qwazr.server.GenericServer;
import com.qwazr.server.InFileSessionPersistenceManager;
import com.qwazr.server.ServerException;
import com.qwazr.server.ServletContextBuilder;
import com.qwazr.server.configuration.ServerConfiguration;
import com.qwazr.utils.ClassLoaderUtils;
import com.qwazr.utils.FunctionUtils;
import com.qwazr.utils.LoggerUtils;
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.SubstitutedVariables;
import com.qwazr.utils.reflection.ConstructorParametersImpl;
import io.swagger.jaxrs.config.SwaggerContextService;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.SecurityInfo;
import io.undertow.servlet.api.ServletInfo;
import org.apache.commons.io.FilenameUtils;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.activation.MimetypesFileTypeMap;
import javax.management.MBeanPermission;
import javax.management.MBeanServerPermission;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.net.SocketPermission;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.security.AccessControlContext;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class WebappManager extends ConstructorParametersImpl {

	public final static String SESSIONS_PERSISTENCE_DIR = "webapp-sessions";

	private static final Logger logger = LoggerUtils.getLogger(WebappManager.class);

	private static final String ACCESS_LOG_LOGGER_NAME = "com.qwazr.webapps.accessLogger";
	private static final Logger accessLogger = Logger.getLogger(ACCESS_LOG_LOGGER_NAME);

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
		super(builder.getConstructorParameters() == null ?
				new ConcurrentHashMap<>() :
				builder.getConstructorParameters().getMap());
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

		final java.nio.file.Path sessionPersistenceDir =
				new File(configuration.tempDirectory, SESSIONS_PERSISTENCE_DIR).toPath();
		if (!Files.exists(sessionPersistenceDir))
			Files.createDirectory(sessionPersistenceDir);
		builder.sessionPersistenceManager(new InFileSessionPersistenceManager(sessionPersistenceDir));

		final ServletContextBuilder context = builder.getWebAppContext();

		// Load the static handlers
		if (webappDefinition.statics != null)
			webappDefinition.statics.forEach((urlPath, filePath) -> registerStaticServlet(urlPath,
					SubstitutedVariables.propertyAndEnvironmentSubstitute(filePath), context));

		// Prepare the Javascript interpreter
		final ScriptEngineManager manager = new ScriptEngineManager();
		scriptEngine = manager.getEngineByName("nashorn");

		// Load the listeners
		if (webappDefinition.listeners != null)
			for (String listenerClass : webappDefinition.listeners)
				context.listener(Servlets.listener(ClassLoaderUtils.findClass(listenerClass)));
		builder.webAppAccessLogger(accessLogger);

		// Load the controllers
		if (webappDefinition.controllers != null)
			webappDefinition.controllers.forEach((urlPath, filePath) -> registerController(urlPath, filePath, context));

		// Load the filters
		if (webappDefinition.filters != null)
			FunctionUtils.forEachEx(webappDefinition.filters,
					(urlPath, filterClass) -> registerJavaFilter(urlPath, ClassLoaderUtils.findClass(filterClass),
							context));
		// Load the closeable filter
		registerJavaFilter("/*", CloseableFilter.class, context);

		// Load the filters
		if (webappDefinition.filters != null)
			FunctionUtils.forEachEx(webappDefinition.filters, (urlPath, filterClass) -> {
				final String name = filterClass + "@" + urlPath;
				context.filter(name, ClassLoaderUtils.findClass(filterClass));
				context.urlFilterMapping(name, urlPath, DispatcherType.REQUEST);
			});

		// Load the identityManager provider if any
		if (webappDefinition.identity_manager != null) {
			final Class<? extends GenericServer.IdentityManagerProvider> identityManagerClass =
					ClassLoaderUtils.findClass(webappDefinition.identity_manager);
			final GenericServer.IdentityManagerProvider identityManagerProvider =
					SmartFactory.from(libraryManager, this, identityManagerClass).createInstance().getInstance();
			builder.identityManagerProvider(identityManagerProvider);
		}

		// Load the security constraint
		if (webappDefinition.secure_paths != null)
			registerSecurePaths(webappDefinition.secure_paths, context);

		// Set the default favicon
		context.servlet(getDefaultFaviconServlet());

		// Create the webservice singleton
		service = new WebappServiceImpl(this);
	}

	public WebappManager registerWebService(ApplicationBuilder builder) {
		builder.singletons(service);
		return this;
	}

	public WebappManager registerContextAttribute(final GenericServer.Builder builder) {
		builder.contextAttribute(this);
		return this;
	}

	public WebappServiceInterface getService() {
		return service;
	}

	public void registerStaticServlet(final String urlPath, final String path, final ServletContextBuilder context) {
		final ServletInfo servletInfo;
		if (path.contains(".") && !path.contains("/"))
			servletInfo = new ServletInfo(StaticResourceServlet.class.getName() + '@' + urlPath,
					StaticResourceServlet.class).addInitParam(StaticResourceServlet.STATIC_RESOURCE_PARAM,
					'/' + StringUtils.replaceChars(path, '.', '/')).addMapping(urlPath);
		else
			servletInfo = new ServletInfo(StaticFileServlet.class.getName() + '@' + urlPath,
					StaticFileServlet.class).addInitParam(StaticFileServlet.STATIC_PATH_PARAM, path)
					.addMapping(urlPath);
		context.servlet(servletInfo);
	}

	private ServletInfo getDefaultFaviconServlet() {
		return new ServletInfo(StaticResourceServlet.class.getName() + '@' + FAVICON_PATH,
				StaticResourceServlet.class).addInitParam(StaticResourceServlet.STATIC_RESOURCE_PARAM,
				"/com/qwazr/webapps/favicon.ico").addMapping(FAVICON_PATH);
	}

	private void registerController(final String urlPath, final String filePath, final ServletContextBuilder context) {
		try {
			String ext = FilenameUtils.getExtension(filePath).toLowerCase();
			if ("js".equals(ext))
				registerJavascriptServlet(urlPath, SubstitutedVariables.propertyAndEnvironmentSubstitute(filePath),
						context);
			else
				registerJavaController(urlPath, filePath, context);
		} catch (ReflectiveOperationException e) {
			throw ServerException.of("Cannot build an instance of the controller: " + filePath, e);
		}
	}

	private void registerJavascriptServlet(final String urlPath, final String filePath,
			final ServletContextBuilder context) {
		context.servlet(new ServletInfo(JavascriptServlet.class.getName() + '@' + urlPath,
				JavascriptServlet.class).addInitParam(JavascriptServlet.JAVASCRIPT_PATH_PARAM, filePath)
				.addMapping(urlPath));
	}

	private void registerJavaController(final String urlPath, final String classDef,
			final ServletContextBuilder context) throws ReflectiveOperationException {
		if (classDef.contains(" ") || classDef.contains(" ,")) {
			registerJavaJaxRsClassServlet(urlPath, classDef, context);
			return;
		}
		final Class<?> clazz = ClassLoaderUtils.findClass(classDef);
		Objects.requireNonNull(clazz, "Class not found: " + classDef);
		if (Servlet.class.isAssignableFrom(clazz)) {
			registerJavaServlet(urlPath, (Class<? extends Servlet>) clazz, context);
			return;
		} else if (Application.class.isAssignableFrom(clazz)) {
			registerJavaJaxRsAppServlet(urlPath, (Class<? extends Application>) clazz, context);
			return;
		} else if (clazz.isAnnotationPresent(Path.class)) {
			registerJavaJaxRsClassServlet(urlPath, classDef, context);
			return;
		}
		throw new ServerException("This type of class is not supported: " + classDef + " / " + clazz.getName());
	}

	public <T extends Servlet> void registerJavaServlet(final String urlPath, final Class<T> servletClass,
			final GenericFactory<T> servletFactory, final ServletContextBuilder context) throws NoSuchMethodException {
		context.servlet(servletClass.getName() + '@' + urlPath, servletClass,
				servletFactory == null ? SmartFactory.from(libraryManager, this, servletClass) : servletFactory,
				urlPath == null ? null : StringUtils.split(urlPath));
	}

	public <T extends Servlet> void registerJavaServlet(final String urlPath, final Class<T> servletClass,
			final ServletContextBuilder context) throws NoSuchMethodException {
		registerJavaServlet(urlPath, servletClass, (GenericFactory<T>) null, context);
	}

	public <T extends Servlet> void registerJavaServlet(final Class<T> servletClass,
			final GenericFactory<T> servletFactory, final ServletContextBuilder context) throws NoSuchMethodException {
		registerJavaServlet(null, servletClass, servletFactory, context);
	}

	public <T extends Servlet> void registerJavaServlet(final Class<T> servletClass, final Supplier<T> servletSupplier,
			final ServletContextBuilder context) throws NoSuchMethodException {
		registerJavaServlet(null, servletClass, GenericFactory.fromSupplier(servletSupplier), context);
	}

	public <T extends Servlet> void registerJavaServlet(final Class<T> servletClass,
			final ServletContextBuilder context) throws NoSuchMethodException {
		registerJavaServlet(servletClass, (GenericFactory<T>) null, context);
	}

	public <T extends Filter> void registerJavaFilter(final String urlPathes, final Class<T> filterClass,
			final GenericFactory<T> filterFactory, final ServletContextBuilder context) throws NoSuchMethodException {
		final String filterName = filterClass.getName() + '@' + urlPathes;
		context.filter(filterName, filterClass,
				filterFactory == null ? SmartFactory.from(libraryManager, this, filterClass) : filterFactory);
		if (urlPathes != null) {
			String[] urlPaths = StringUtils.split(urlPathes);
			for (String urlPath : urlPaths)
				context.urlFilterMapping(filterName, urlPath, DispatcherType.REQUEST);
		}
	}

	public <T extends Filter> void registerJavaFilter(final String urlPath, final Class<T> filterClass,
			final ServletContextBuilder context) throws NoSuchMethodException {
		registerJavaFilter(urlPath, filterClass, null, context);
	}

	public void registerSecurePaths(final Collection<String> securePaths, final ServletContextBuilder context) {
		context.addSecurityConstraint(Servlets.securityConstraint()
				.setEmptyRoleSemantic(SecurityInfo.EmptyRoleSemantic.AUTHENTICATE)
				.addWebResourceCollection(Servlets.webResourceCollection().addUrlPatterns(securePaths)));
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
			final ServletContextBuilder context) throws NoSuchMethodException {
		context.jaxrs(ServletContainer.class.getName() + '@' + urlPath, appClass, servletInfo -> {
			servletInfo.addMapping(urlPath).setLoadOnStartup(1);
			addSwaggerContext(urlPath, servletInfo);
		});
	}

	private void registerJavaJaxRsClassServlet(final String urlPath, final String classList,
			final ServletContextBuilder context) throws ReflectiveOperationException {
		final ApplicationBuilder appBuilder = new ApplicationBuilder(urlPath);
		final String[] classes = StringUtils.split(classList, " ,");
		for (String className : classes)
			appBuilder.classes(ClassLoaderUtils.findClass(className.trim()));
		registerJaxRsResources(appBuilder, context);
	}

	public void registerJaxRsResources(final ApplicationBuilder applicationBuilder,
			final ServletContextBuilder context) {
		applicationBuilder.classes(BaseRestApplication.PROVIDERS_CLASSES);
		context.jaxrs(null, applicationBuilder, servletInfo -> {
			final Collection<String> paths = applicationBuilder.getApplicationPaths();
			if (paths != null && !paths.isEmpty())
				addSwaggerContext(paths.iterator().next(), servletInfo);
		});
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
