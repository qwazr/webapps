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
package com.qwazr.webapps.transaction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.FileReader;
import java.io.IOException;
import java.net.SocketPermission;
import java.net.URISyntaxException;
import java.security.AccessControlContext;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.PrivilegedActionException;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Map;

import javax.management.MBeanPermission;
import javax.management.MBeanServerPermission;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.qwazr.utils.ScriptUtils;
import com.qwazr.webapps.exception.WebappRedirectException;
import com.qwazr.webapps.transaction.FilePathResolver.FilePath;

public class ControllerManager {

	public static volatile ControllerManager INSTANCE = null;

	public static void load() throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		INSTANCE = new ControllerManager();
	}

	final private ScriptEngine scriptEngine;

	private ControllerManager() {
		ScriptEngineManager manager = new ScriptEngineManager();
		scriptEngine = manager.getEngineByName("nashorn");
	}

	private File checkController(File file) throws FileNotFoundException {
		if (!file.exists())
			throw new FileNotFoundException("Controller not found");
		if (!file.isFile())
			throw new FileNotFoundException("Controller not found");
		return file;
	}

	File findController(ApplicationContext context, WebappHttpRequest request,
			FilePath filePath) throws URISyntaxException, IOException {

		// First we try to find the controller using configuration mapping
		String controllerPath = context.findController(filePath);
		if (controllerPath != null)
			return checkController(filePath.buildFile("controller",
					controllerPath));

		// Then we check the related file.
		File file = filePath.buildFile("controller");
		if (file == null)
			return null;

		// Check if it is a directory
		if (file.exists() && file.isDirectory()) {
			if (!request.getPathInfo().endsWith("/"))
				throw new WebappRedirectException(request.getRequestURI() + "/");
			return checkController(new File(file, "index.js"));
		}

		// Check if we have a controller file
		file = new File(file.getParent(), file.getName() + ".js");
		if (file.exists())
			return checkController(file);

		// Nothing found, returning null give the control the next handler
		// (static)
		return null;
	}

	public static class RestrictedAccessControlContext {

		public static final AccessControlContext INSTANCE;

		static {
			Permissions pm = new Permissions();
			// Required by Cassandra
			pm.add(new RuntimePermission("modifyThread"));
			pm.add(new MBeanServerPermission("createMBeanServer"));
			pm.add(new MBeanPermission(
					"com.codahale.metrics.JmxReporter$JmxCounter#-[*:*]",
					"registerMBean"));
			pm.add(new MBeanPermission(
					"com.codahale.metrics.JmxReporter$JmxCounter#-[*:*]",
					"unregisterMBean"));
			pm.add(new MBeanPermission(
					"com.codahale.metrics.JmxReporter$JmxGauge#-[*:*]",
					"registerMBean"));
			pm.add(new MBeanPermission(
					"com.codahale.metrics.JmxReporter$JmxGauge#-[*:*]",
					"unregisterMBean"));
			pm.add(new MBeanPermission(
					"com.codahale.metrics.JmxReporter$JmxTimer#-[*:*]",
					"registerMBean"));
			pm.add(new MBeanPermission(
					"com.codahale.metrics.JmxReporter$JmxTimer#-[*:*]",
					"unregisterMBean"));
			pm.add(new SocketPermission("*.qwazr.net:9042",
					"connect,accept,resolve"));

			// Required for templates
			pm.add(new FilePermission("<<ALL FILES>>", "read"));

			INSTANCE = new AccessControlContext(
					new ProtectionDomain[] { new ProtectionDomain(
							new CodeSource(null, (Certificate[]) null), pm) });
		}
	}

	void handle(WebappResponse response, File controllerFile)
			throws IOException, ScriptException, PrivilegedActionException {
		Bindings bindings = scriptEngine.createBindings();
		Map<String, Object> variables = response.getVariables();
		if (variables != null)
			for (Map.Entry<String, Object> entry : variables.entrySet())
				bindings.put(entry.getKey(), entry.getValue());
		FileReader fileReader = new FileReader(controllerFile);
		try {
			ScriptUtils.evalScript(scriptEngine,
					RestrictedAccessControlContext.INSTANCE, fileReader,
					bindings);
		} finally {
			fileReader.close();
		}
	}
}
