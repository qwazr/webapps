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
 */
package com.qwazr.webapps;

import com.qwazr.cluster.ClusterManager;
import com.qwazr.library.LibraryManager;
import com.qwazr.server.BaseServer;
import com.qwazr.server.GenericServer;
import com.qwazr.server.WelcomeShutdownService;
import com.qwazr.server.configuration.ServerConfiguration;
import com.qwazr.utils.FunctionUtils;

import javax.management.MBeanException;
import javax.management.OperationsException;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebappServer implements BaseServer {

	private final GenericServer server;
	private final WebappServiceInterface service;

	public WebappServer(final ServerConfiguration configuration,
			FunctionUtils.BiConsumerEx<WebappManager, GenericServer.Builder, NoSuchMethodException> prebuild)
			throws IOException, URISyntaxException, ReflectiveOperationException {
		final ExecutorService executorService = Executors.newCachedThreadPool();
		final GenericServer.Builder builder =
				GenericServer.of(configuration, executorService).webService(WelcomeShutdownService.class);
		new ClusterManager(executorService, configuration).registerHttpClientMonitoringThread(builder)
				.registerProtocolListener(builder)
				.registerWebService(builder);
		final LibraryManager libraryManager =
				new LibraryManager(configuration.dataDirectory, configuration.getEtcFiles()).registerIdentityManager(
						builder).registerWebService(builder);
		final WebappManager webappManager = new WebappManager(libraryManager, builder);
		if (prebuild != null)
			prebuild.accept(webappManager, builder);
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
			throws IOException, ReflectiveOperationException, OperationsException, ServletException, MBeanException,
			URISyntaxException, InterruptedException {
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