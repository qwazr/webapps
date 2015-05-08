/**
 * Copyright 2014-2015 OpenSearchServer Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.qwazr.webapps;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionListener;
import io.undertow.servlet.api.ServletInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.qwazr.cluster.ClusterServer;
import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.connectors.ConnectorManager;
import com.qwazr.tools.ToolsManager;
import com.qwazr.utils.server.AbstractServer;
import com.qwazr.utils.server.RestApplication;
import com.qwazr.utils.server.ServletApplication;
import com.qwazr.webapps.template.FileTemplateManager;
import com.qwazr.webapps.transaction.ApplicationContextManager;
import com.qwazr.webapps.transaction.ControllerManager;
import com.qwazr.webapps.transaction.FilePathResolver;
import com.qwazr.webapps.transaction.StaticManager;

public class WebappServer extends AbstractServer {

	public final static String DEFAULT_ROOT_PATH = "";
	public final static int DEFAULT_ROOT_DEPTH = 0;
	public final static String SERVICE_NAME_WEBAPPS = "webapps";

	private final static ServerDefinition serverDefinition = new ServerDefinition();
	static {
		serverDefinition.defaultWebApplicationTcpPort = 9095;
		serverDefinition.mainJarPath = "qwazr-webapps.jar";
		serverDefinition.defaultDataDirName = "qwazr";
	}

	/**
	 * The document ROOT is: /data_dir/ROOT/..
	 */
	public final static Option DEPTH_OPTION = new Option("e", "depth", true,
			"The depth of ROOT directories");

	public final static Option CONF_OPTION = new Option("c", "conf", true,
			"A generic configuration file");

	public final static Option ROOTPATH_OPTION = new Option("r", "root-path",
			true, "The path of the root");

	private String contextRootPath = DEFAULT_ROOT_PATH;

	private int depth = DEFAULT_ROOT_DEPTH;
	private String confFile = null;

	private WebappServer() {
		super(serverDefinition);

	}

	public static class WebappApplication extends ServletApplication implements
			SessionListener {

		private final String contextPath;

		public WebappApplication(String contextPath) {
			this.contextPath = contextPath;
		}

		@Override
		protected List<ServletInfo> getServletInfos() {
			List<ServletInfo> servletInfos = new ArrayList<ServletInfo>();
			servletInfos.add(new ServletInfo("WebAppServlet",
					WebappHttpServlet.class).addMapping("/*"));
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
		public void sessionDestroyed(Session session,
				HttpServerExchange exchange, SessionDestroyedReason reason) {
			ApplicationContextManager.INSTANCE.destroySession(session.getId());
		}

		@Override
		public void attributeAdded(Session session, String name, Object value) {
		}

		@Override
		public void attributeUpdated(Session session, String name,
				Object newValue, Object oldValue) {
		}

		@Override
		public void attributeRemoved(Session session, String name,
				Object oldValue) {
		}

		@Override
		public void sessionIdChanged(Session session, String oldSessionId) {
		}

		@Override
		protected String getContextPath() {
			return contextPath;
		}

	}

	@Override
	public void defineOptions(Options options) {
		super.defineOptions(options);
		options.addOption(DEPTH_OPTION);
		options.addOption(CONF_OPTION);
		options.addOption(ROOTPATH_OPTION);
	}

	public static void load(String contextPath, String confFile, int depth,
			File data_directory) throws IOException {

		File webapps_directory = new File(data_directory, SERVICE_NAME_WEBAPPS);
		if (!webapps_directory.exists())
			webapps_directory.mkdir();
		// Create the singletons
		FilePathResolver.load(webapps_directory, depth);
		ControllerManager.load();
		FileTemplateManager.load(webapps_directory);
		StaticManager.load();
		ApplicationContextManager.load(contextPath, confFile);
	}

	@Override
	public void commandLine(CommandLine cmd) throws IOException, ParseException {
		// Get the depth option
		depth = cmd.hasOption(DEPTH_OPTION.getOpt()) ? Integer.parseInt(cmd
				.getOptionValue(DEPTH_OPTION.getOpt())) : DEFAULT_ROOT_DEPTH;

		confFile = cmd.getOptionValue(CONF_OPTION.getOpt());

		contextRootPath = DEFAULT_ROOT_PATH;
		if (cmd.hasOption(ROOTPATH_OPTION.getOpt()))
			contextRootPath = cmd.getOptionValue(ROOTPATH_OPTION.getOpt());

	}

	@Override
	public void load() throws IOException {
		File currentDataDir = getCurrentDataDir();
		ClusterServer.load(getWebApplicationPublicAddress(), currentDataDir,
				null);
		ConnectorManager.load(currentDataDir, null);
		ToolsManager.load(currentDataDir, null);
		load(contextRootPath, confFile, depth, currentDataDir);
	}

	@Override
	public ServletApplication getServletApplication() {
		return new WebappApplication(contextRootPath);
	}

	@Override
	protected RestApplication getRestApplication() {
		return null;
	}

	public static void main(String[] args) throws IOException, ParseException,
			ServletException {
		new WebappServer().start(args);
		ClusterManager.INSTANCE.registerMe(SERVICE_NAME_WEBAPPS);
	}

}
