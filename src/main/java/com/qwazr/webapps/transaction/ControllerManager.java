/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
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

public class ControllerManager {

	public static volatile ControllerManager INSTANCE = null;

	public static void load(File dataDir) throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		INSTANCE = new ControllerManager(dataDir);
	}

	final File dataDir;

	final private ScriptEngine scriptEngine;

	private ControllerManager(File dataDir) {
		this.dataDir = dataDir;
		ScriptEngineManager manager = new ScriptEngineManager();
		scriptEngine = manager.getEngineByName("nashorn");
	}

	File findController(ApplicationContext context, String requestPath)
			throws URISyntaxException, IOException {
		// First we try to find the controller using configuration mapping
		File ctrlrFile = context.findController(requestPath);
		if (ctrlrFile == null)
			return null;
		if (!ctrlrFile.exists())
			throw new FileNotFoundException("Controller not found");
		if (!ctrlrFile.isFile())
			throw new FileNotFoundException("Controller not found");
		return ctrlrFile;
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
