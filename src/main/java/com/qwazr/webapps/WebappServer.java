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

import com.qwazr.cluster.ClusterManager;
import com.qwazr.cluster.ClusterServiceInterface;
import com.qwazr.library.LibraryManager;
import com.qwazr.library.LibraryServiceInterface;
import com.qwazr.server.ApplicationBuilder;
import com.qwazr.server.BaseServer;
import com.qwazr.server.GenericServer;
import com.qwazr.server.GenericServerBuilder;
import com.qwazr.server.RestApplication;
import com.qwazr.server.WelcomeShutdownService;
import com.qwazr.server.configuration.ServerConfiguration;
import com.qwazr.utils.FunctionUtils;

import javax.management.JMException;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebappServer implements BaseServer {

	private final GenericServer server;
	private final WebappServiceInterface service;

	public WebappServer(final ServerConfiguration configuration,
			FunctionUtils.BiConsumerEx<WebappManager, GenericServerBuilder, NoSuchMethodException> prebuild)
			throws IOException, URISyntaxException, ReflectiveOperationException {

		final ExecutorService executorService = Executors.newCachedThreadPool();
		final GenericServerBuilder builder = GenericServer.of(configuration, executorService);

		final Set<String> services = new HashSet<>();
		services.add(ClusterServiceInterface.SERVICE_NAME);
		services.add(LibraryServiceInterface.SERVICE_NAME);
		services.add(WebappServiceInterface.SERVICE_NAME);

		final ApplicationBuilder webServices = ApplicationBuilder.of("/*").classes(RestApplication.JSON_CLASSES).
				singletons(new WelcomeShutdownService());

		new ClusterManager(executorService, configuration).registerProtocolListener(builder, services)
				.registerContextAttribute(builder)
				.registerWebService(webServices);

		final LibraryManager libraryManager =
				new LibraryManager(configuration.dataDirectory, configuration.getEtcFiles()).registerIdentityManager(
						builder).registerContextAttribute(builder).registerWebService(webServices);

		final WebappManager webappManager = new WebappManager(libraryManager, builder).registerContextAttribute(builder)
				.registerWebService(webServices);
		if (prebuild != null)
			prebuild.accept(webappManager, builder);

		builder.getWebServiceContext().jaxrs(webServices);
		server = builder.build();
		service = webappManager.getService();
	}

	@Override
	public GenericServer getServer() {
		return server;
	}

	private static volatile WebappServer INSTANCE;

	public static synchronized WebappServer getInstance() {
		return INSTANCE;
	}

	public WebappServiceInterface getService() {
		return service;
	}

	public static synchronized void main(final String... args)
			throws IOException, ReflectiveOperationException, ServletException, JMException, URISyntaxException,
			InterruptedException {
		if (INSTANCE != null)
			shutdown();
		INSTANCE = new WebappServer(new ServerConfiguration(args), null);
		INSTANCE.start();
	}

	public static synchronized void shutdown() {
		if (INSTANCE != null)
			INSTANCE.stop();
		INSTANCE = null;
	}

}