/*
 * Copyright 2015-2020 Emmanuel Keller / QWAZR
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

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.fasterxml.jackson.jaxrs.xml.JacksonXMLProvider;
import com.qwazr.library.LibraryServiceInterface;
import com.qwazr.server.ApplicationBuilder;
import com.qwazr.server.GenericFactory;
import com.qwazr.server.GenericServer;
import com.qwazr.server.GenericServerBuilder;
import com.qwazr.server.InFileSessionPersistenceManager;
import com.qwazr.server.ServerException;
import com.qwazr.server.ServletContextBuilder;
import com.qwazr.utils.ClassLoaderUtils;
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.SubstitutedVariables;
import com.qwazr.utils.concurrent.ConcurrentUtils;
import com.qwazr.utils.json.JacksonConfig;
import com.qwazr.utils.reflection.ConstructorParametersImpl;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.SecurityInfo;
import io.undertow.servlet.api.ServletInfo;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.servlet.ServletContainer;
import org.webjars.servlet.WebjarsServlet;

import javax.activation.MimetypesFileTypeMap;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class WebappManager {

    public final static int DEFAULT_EXPIRATION_TIME = 86400;

    public final static List<Class<?>> SWAGGER_CLASSES = List.of(OpenApiResource.class);

    public final static List<Class<?>> JACKSON_CLASSES =
            List.of(JacksonConfig.class, JacksonXMLProvider.class, JacksonJsonProvider.class);

    public final static String SESSIONS_PERSISTENCE_DIR = "webapp-sessions";

    private final WebappServiceInterface service;

    public final static String DEFAULT_FAVICON_RESOURCE_PATH = "/com/qwazr/webapps/favicon.ico";
    public final static String DEFAULT_FAVICON_PATH = "/favicon.ico";

    private final WebappDefinition webappDefinition;

    private WebappManager(final Builder builder) {
        this.webappDefinition = builder.webappDefinition;
        // Create the webservice singleton
        service = new WebappServiceImpl(this);
    }

    public WebappServiceInterface getService() {
        return service;
    }

    public WebappDefinition getWebAppDefinition() {
        return webappDefinition;
    }

    public static Builder of(final GenericServerBuilder serverBuilder, final ServletContextBuilder context) {
        return new Builder(serverBuilder, context);
    }

    public static class Builder {

        private final GenericServerBuilder serverBuilder;
        private final ServletContextBuilder context;
        private MimetypesFileTypeMap mimeTypeMap;
        private ScriptEngine scriptEngine;
        private LibraryServiceInterface libraryService;
        private WebappDefinition webappDefinition;

        private Builder(final GenericServerBuilder serverBuilder, final ServletContextBuilder context) {
            this.serverBuilder = serverBuilder;
            this.context = context;
        }

        public Builder libraryService(LibraryServiceInterface libraryService) {
            this.libraryService = libraryService;
            return this;
        }

        public Builder webappDefinition(final java.nio.file.Path parentDirectory,
                                        final WebappDefinition webappDefinition) throws ClassNotFoundException, InstantiationException {

            if (webappDefinition == null)
                return this;

            // Load the static handlers
            if (webappDefinition.statics != null)
                webappDefinition.statics.forEach((urlPath, filePath) -> {
                    final String finalFilePath = SubstitutedVariables.propertyAndEnvironmentSubstitute(filePath);
                    if (finalFilePath.contains(".") && !finalFilePath.contains("/"))
                        registerStaticServlet(urlPath, finalFilePath);
                    else
                        registerStaticServlet(urlPath, parentDirectory.resolve(finalFilePath), DEFAULT_EXPIRATION_TIME);
                });

            // Load the listeners
            if (webappDefinition.listeners != null)
                for (String listenerClass : webappDefinition.listeners)
                    context.listener(Servlets.listener(ClassLoaderUtils.findClass(listenerClass)));

            // Load the controllers
            if (webappDefinition.controllers != null)
                webappDefinition.controllers.forEach(
                        (urlPath, filePath) -> registerController(urlPath, parentDirectory, filePath));

            // Load the filters
            if (webappDefinition.filters != null)
                ConcurrentUtils.forEachEx(webappDefinition.filters,
                        (urlPath, filterClass) -> registerJavaFilter(urlPath, ClassLoaderUtils.findClass(filterClass)));

            // Load the closeable filter
            registerJavaFilter("/*", CloseableFilter.class);

            // Load the filters
            if (webappDefinition.filters != null)
                ConcurrentUtils.forEachEx(webappDefinition.filters, (urlPath, filterClass) -> {
                    final String name = filterClass + "@" + urlPath;
                    context.filter(name, ClassLoaderUtils.findClass(filterClass));
                    context.urlFilterMapping(name, urlPath, DispatcherType.REQUEST);
                });

            // Load the identityManager provider if any
            if (webappDefinition.identity_manager != null) {
                final Class<? extends GenericServer.IdentityManagerProvider> identityManagerClass =
                        ClassLoaderUtils.findClass(webappDefinition.identity_manager);
                final GenericServer.IdentityManagerProvider identityManagerProvider =
                        SmartFactory.from(libraryService, getConstructorParameters(), identityManagerClass)
                                .createInstance()
                                .getInstance();
                serverBuilder.identityManagerProvider(identityManagerProvider);
            }

            // Load the security constraint
            if (webappDefinition.secure_paths != null)
                registerSecurePaths(webappDefinition.secure_paths);

            this.webappDefinition = webappDefinition;
            return this;
        }

        public Builder registerStaticResourceServlet(final String urlPath,
                                                     final String resourcePath,
                                                     final int expirationTimeSec) {
            final ServletInfo servletInfo = new ServletInfo(
                    StaticResourceServlet.class.getName() + '@' + urlPath,
                    StaticResourceServlet.class, GenericFactory.fromInstance(
                    new StaticResourceServlet(resourcePath, getMimeTypeMap(), expirationTimeSec))).
                    addMapping(urlPath);
            context.servlet(servletInfo);
            return this;
        }

        public Builder registerCustomFaviconServlet(final String faviconResourcePath) {
            return registerStaticResourceServlet(DEFAULT_FAVICON_PATH, faviconResourcePath, DEFAULT_EXPIRATION_TIME);
        }

        /**
         * Set the default favicon
         *
         * @return the current builder
         */
        public Builder registerDefaultFaviconServlet() {
            return registerCustomFaviconServlet(DEFAULT_FAVICON_RESOURCE_PATH);
        }

        public Builder persistSessions(final java.nio.file.Path persistenceDirectory) throws IOException {
            if (!Files.exists(persistenceDirectory))
                Files.createDirectory(persistenceDirectory);
            serverBuilder.sessionPersistenceManager(new InFileSessionPersistenceManager(persistenceDirectory));
            return this;
        }

        public Builder registerWebjars(final boolean disableCache, final String... urlMappings) {
            ServletInfo servletInfo = new ServletInfo("WebjarsServlet", WebjarsServlet.class).setLoadOnStartup(2)
                    .addMappings(urlMappings);
            if (disableCache)
                servletInfo = servletInfo.addInitParam("disableCache", Boolean.toString(disableCache));
            context.addServlet(servletInfo);
            return this;
        }

        public Builder registerWebjars(final boolean disableCache) {
            return registerWebjars(disableCache, "/webjars/*");
        }

        public Builder registerWebjars() {
            return registerWebjars(false);
        }

        private synchronized MimetypesFileTypeMap getMimeTypeMap() {
            if (mimeTypeMap == null)
                mimeTypeMap = new MimetypesFileTypeMap(getClass().getResourceAsStream("/com/qwazr/webapps/mime.types"));
            return mimeTypeMap;
        }

        public Builder registerStaticServlet(final String urlPath, final String resourcePath,
                                             final int expirationSecTime) {
            final ServletInfo servletInfo =
                    new ServletInfo(StaticResourceServlet.class.getName() + '@' + urlPath, StaticResourceServlet.class,
                            GenericFactory.fromInstance(
                                    new StaticResourceServlet('/' + StringUtils.replaceChars(resourcePath, '.', '/'),
                                            getMimeTypeMap(), expirationSecTime))).addMapping(urlPath);
            context.servlet(servletInfo);
            return this;
        }

        public Builder registerStaticServlet(final String urlPath, final String resourcePath) {
            return registerStaticServlet(urlPath, resourcePath, DEFAULT_EXPIRATION_TIME);
        }

        public Builder registerStaticServlet(final String urlPath,
                                             final java.nio.file.Path staticsPath,
                                             final int expirationSecTime) {
            final ServletInfo servletInfo =
                    new ServletInfo(StaticFileServlet.class.getName() + '@' + urlPath, StaticFileServlet.class,
                            GenericFactory.fromInstance(new StaticFileServlet(getMimeTypeMap(), staticsPath,
                                    expirationSecTime))).addMapping(urlPath);
            context.servlet(servletInfo);
            return this;
        }

        private Builder registerController(final String urlPath, final java.nio.file.Path parentDirectory,
                                           final String filePath) {
            try {
                if (filePath.endsWith(".js"))
                    registerJavascriptServlet(urlPath,
                            parentDirectory.resolve(SubstitutedVariables.propertyAndEnvironmentSubstitute(filePath)));
                else
                    registerJavaController(urlPath, filePath);
                return this;
            } catch (ReflectiveOperationException e) {
                throw ServerException.of("Cannot build an instance of the controller: " + filePath, e);
            }
        }

        /**
         * Prepare the Javascript interpreter
         *
         * @return
         */
        private synchronized ScriptEngine getScriptEngine() {
            if (scriptEngine == null) {
                final ScriptEngineManager manager = new ScriptEngineManager();
                scriptEngine = manager.getEngineByName("nashorn");
            }
            return scriptEngine;
        }

        private void registerJavascriptServlet(final String urlPath, final java.nio.file.Path controlerPath) {
            context.servlet(new ServletInfo(JavascriptServlet.class.getName() + '@' + urlPath, JavascriptServlet.class,
                    GenericFactory.fromInstance(
                            new JavascriptServlet(getScriptEngine(), libraryService, controlerPath))).addMapping(
                    urlPath));
        }

        private void registerJavaController(final String urlPath, final String classDef)
                throws ReflectiveOperationException {
            if (classDef.contains(" ") || classDef.contains(" ,")) {
                registerJavaJaxRsClassServlet(urlPath, classDef);
                return;
            }
            final Class<?> clazz = ClassLoaderUtils.findClass(classDef);
            Objects.requireNonNull(clazz, "Class not found: " + classDef);
            if (Servlet.class.isAssignableFrom(clazz)) {
                registerJavaServlet(urlPath, (Class<? extends Servlet>) clazz);
                return;
            } else if (Application.class.isAssignableFrom(clazz)) {
                registerJavaJaxRsAppServlet(urlPath, (Class<? extends Application>) clazz);
                return;
            } else if (clazz.isAnnotationPresent(Path.class)) {
                registerJavaJaxRsClassServlet(urlPath, classDef);
                return;
            }
            throw new ServerException("This type of class is not supported: " + classDef + " / " + clazz.getName());
        }

        private ConstructorParametersImpl getConstructorParameters() {
            return (ConstructorParametersImpl) serverBuilder.getConstructorParameters();
        }

        public <T extends Servlet> Builder registerJavaServlet(final String urlPath, final Class<T> servletClass,
                                                               final GenericFactory<T> servletFactory) {
            context.servlet(servletClass.getName() + '@' + urlPath, servletClass, servletFactory == null ?
                    SmartFactory.from(libraryService, getConstructorParameters(), servletClass) :
                    servletFactory, urlPath == null ? null : StringUtils.split(urlPath));
            return this;
        }

        public <T extends Servlet> Builder registerJavaServlet(final String urlPath, final Class<T> servletClass) {
            return registerJavaServlet(urlPath, servletClass, null);
        }

        public <T extends Servlet> Builder registerJavaServlet(final Class<T> servletClass,
                                                               final GenericFactory<T> servletFactory) {
            return registerJavaServlet(null, servletClass, servletFactory);
        }

        public <T extends Servlet> Builder registerJavaServlet(final Class<T> servletClass,
                                                               final Supplier<T> servletSupplier) {
            return registerJavaServlet(null, servletClass, GenericFactory.fromSupplier(servletSupplier));
        }

        public <T extends Servlet> Builder registerJavaServlet(final Class<T> servletClass) {
            return registerJavaServlet(servletClass, (GenericFactory<T>) null);
        }

        public <T extends Filter> Builder registerJavaFilter(final String urlPathes, final Class<T> filterClass,
                                                             final GenericFactory<T> filterFactory) {
            final String filterName = filterClass.getName() + '@' + urlPathes;
            context.filter(filterName, filterClass, filterFactory == null ?
                    SmartFactory.from(libraryService, getConstructorParameters(), filterClass) :
                    filterFactory);
            if (urlPathes != null) {
                String[] urlPaths = StringUtils.split(urlPathes);
                for (String urlPath : urlPaths)
                    context.urlFilterMapping(filterName, urlPath, DispatcherType.REQUEST);
            }
            return this;
        }

        public <T extends Filter> Builder registerJavaFilter(final String urlPath, final Class<T> filterClass) {
            registerJavaFilter(urlPath, filterClass, null);
            return this;
        }

        public Builder registerSecurePaths(final Collection<String> securePaths) {
            context.addSecurityConstraint(Servlets.securityConstraint()
                    .setEmptyRoleSemantic(SecurityInfo.EmptyRoleSemantic.AUTHENTICATE)
                    .addWebResourceCollection(Servlets.webResourceCollection().addUrlPatterns(securePaths)));
            return this;
        }

        private ServletInfo addSwaggerContext(String urlPath, final ServletInfo servletInfo) {
            urlPath = StringUtils.removeEnd(urlPath, "*");
            urlPath = StringUtils.removeEnd(urlPath, "/");
            return servletInfo.addInitParam("swagger.api.basepath", urlPath);
        }

        public void registerJavaJaxRsAppServlet(final String urlPath, final Class<? extends Application> appClass) {
            context.jaxrs(ServletContainer.class.getName() + '@' + urlPath, appClass, servletInfo -> {
                servletInfo.addMapping(urlPath).setLoadOnStartup(1);
                addSwaggerContext(urlPath, servletInfo);
            });
        }

        public void registerJavaJaxRsClassServlet(final String urlPath, final String classList)
                throws ReflectiveOperationException {
            final ApplicationBuilder appBuilder = new ApplicationBuilder(urlPath);
            final String[] classes = StringUtils.split(classList, " ,");
            for (String className : classes)
                appBuilder.classes(ClassLoaderUtils.findClass(className.trim()));
            registerJaxRsResources(appBuilder, true, true);
        }

        public Builder registerJaxRsResources(final ApplicationBuilder applicationBuilder, boolean withSwagger,
                                              boolean withRoleFeature) {
            applicationBuilder.classes(JACKSON_CLASSES);
            if (withRoleFeature)
                applicationBuilder.classes(RolesAllowedDynamicFeature.class);
            if (withSwagger)
                applicationBuilder.classes(SWAGGER_CLASSES);
            context.jaxrs(null, applicationBuilder, servletInfo -> {
                final Collection<String> paths = applicationBuilder.getApplicationPaths();
                if (paths != null && !paths.isEmpty() && withSwagger)
                    addSwaggerContext(paths.iterator().next(), servletInfo);
            });
            return this;
        }

        public Builder registerJaxRsResources(final ApplicationBuilder applicationBuilder) {
            return registerJaxRsResources(applicationBuilder, false, false);
        }

        public WebappManager build() {
            return new WebappManager(this);
        }
    }
}
