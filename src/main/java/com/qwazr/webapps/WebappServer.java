/**   
 * License Agreement for QWAZR
 *
 * Copyright (C) 2014-2015 OpenSearchServer Inc.
 * 
 * http://www.qwazr.com
 * 
 * This file is part of QWAZR.
 *
 * QWAZR is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * QWAZR is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with QWAZR. 
 *  If not, see <http://www.gnu.org/licenses/>.
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
import com.qwazr.utils.server.AbstractServer;
import com.qwazr.utils.server.RestApplication;
import com.qwazr.utils.server.ServletApplication;
import com.qwazr.webapps.template.FileTemplateManager;
import com.qwazr.webapps.transaction.ApplicationContextManager;
import com.qwazr.webapps.transaction.ControllerManager;
import com.qwazr.webapps.transaction.FilePathResolver;
import com.qwazr.webapps.transaction.StaticManager;

public class WebappServer extends AbstractServer {

	public final static String DEFAULT_ROOT_PATH = "/web/";
	public final static int DEFAULT_ROOT_DEPTH = 1;
	public final static String SERVICE_NAME = "webapps";

	private final static ServerDefinition serverDefinition = new ServerDefinition();
	static {
		serverDefinition.defaultWebApplicationTcpPort = 9095;
		serverDefinition.mainJarPath = "qwazr-renderer.jar";
		serverDefinition.defaultDataDirPath = "qwazr/renderer";
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
			servletInfos.add(new ServletInfo("RendererServlet",
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

		// Create the singletons
		FilePathResolver.load(data_directory, depth);
		ControllerManager.load();
		FileTemplateManager.load(data_directory);
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
		ClusterServer.load(this, getCurrentDataDir(), null, null);
		load(contextRootPath, confFile, depth, getCurrentDataDir());
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
		ClusterManager.INSTANCE.registerMe(SERVICE_NAME);
	}

}
