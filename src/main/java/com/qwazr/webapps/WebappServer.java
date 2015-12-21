/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
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

import com.qwazr.cluster.ClusterServer;
import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.connectors.ConnectorManagerImpl;
import com.qwazr.tools.ToolsManagerImpl;
import com.qwazr.utils.server.AbstractServer;
import com.qwazr.utils.server.RestApplication;
import com.qwazr.utils.server.ServletApplication;
import com.qwazr.webapps.transaction.ControllerManager;
import com.qwazr.webapps.transaction.StaticManager;
import com.qwazr.webapps.transaction.WebappManager;
import io.undertow.security.idm.IdentityManager;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionListener;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.ServletInfo;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebappServer extends AbstractServer {

	public final static String SERVICE_NAME_WEBAPPS = "webapps";

	private final ExecutorService executorService;

	private final static ServerDefinition serverDefinition = new ServerDefinition();

	static {
		serverDefinition.defaultWebApplicationTcpPort = 9095;
		serverDefinition.mainJarPath = "qwazr-webapps.jar";
		serverDefinition.defaultDataDirName = "qwazr";
	}

	private WebappServer() {
		super(serverDefinition);
		executorService = Executors.newCachedThreadPool();
	}

	public static class WebappApplication extends ServletApplication implements SessionListener {

		public WebappApplication() {
		}

		//TODO Parameters for fileupload limitation
		private final static MultipartConfigElement multipartConfigElement = new MultipartConfigElement(
				System.getProperty("java.io.tmpdir"));

		@Override
		protected List<ServletInfo> getServletInfos() {
			List<ServletInfo> servletInfos = new ArrayList<ServletInfo>();
			servletInfos.add(Servlets.servlet("WebAppServlet", WebappHttpServlet.class).addMapping("/*")
					.setMultipartConfig(multipartConfigElement));
			return servletInfos;
		}

		@Override
		protected SessionListener getSessionListener() {
			return this;
		}

		@Override
		public void sessionCreated(Session session, HttpServerExchange exchange) {
		}

		@Override
		public void sessionDestroyed(Session session, HttpServerExchange exchange, SessionDestroyedReason reason) {
			WebappManager.INSTANCE.destroySession(session.getId());
		}

		@Override
		public void attributeAdded(Session session, String name, Object value) {
		}

		@Override
		public void attributeUpdated(Session session, String name, Object newValue, Object oldValue) {
		}

		@Override
		public void attributeRemoved(Session session, String name, Object oldValue) {
		}

		@Override
		public void sessionIdChanged(Session session, String oldSessionId) {
		}

		@Override
		protected String getContextPath() {
			return StringUtils.EMPTY;
		}

	}

	public static void load(ExecutorService executorService, File data_directory) throws IOException {

		File webapps_directory = new File(data_directory, SERVICE_NAME_WEBAPPS);
		if (!webapps_directory.exists())
			webapps_directory.mkdir();
		// Create the singletons
		ControllerManager.load(data_directory);
		StaticManager.load(data_directory);
		WebappManager.load(executorService, webapps_directory);
	}

	@Override
	public void load() throws IOException {
		File currentDataDir = getCurrentDataDir();
		ClusterServer.load(getWebApplicationPublicAddress(), currentDataDir);
		ConnectorManagerImpl.load(currentDataDir);
		ToolsManagerImpl.load(currentDataDir);
		load(executorService, currentDataDir);
	}

	@Override
	public Class<WebappApplication> getServletApplication() {
		return WebappApplication.class;
	}

	@Override
	protected IdentityManager getIdentityManager(String realm) {
		return null;
	}

	@Override
	protected Class<RestApplication> getRestApplication() {
		return null;
	}

	public static void main(String[] args)
			throws IOException, ParseException, ServletException, InstantiationException, IllegalAccessException {
		new WebappServer().start(args);
		ClusterManager.INSTANCE.registerMe(SERVICE_NAME_WEBAPPS);
	}

	@Override
	public void commandLine(CommandLine cmd) throws IOException, ParseException {
	}

}
