/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
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
package com.qwazr.webapps.transaction;

import com.qwazr.scripts.ScriptConsole;
import com.qwazr.utils.ClassLoaderUtils;
import com.qwazr.utils.IOUtils;
import com.qwazr.utils.ScriptUtils;
import com.qwazr.utils.StringUtils;
import org.apache.commons.io.FilenameUtils;

import javax.management.MBeanPermission;
import javax.management.MBeanServerPermission;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.*;
import java.net.SocketPermission;
import java.net.URISyntaxException;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Map;
import java.util.Objects;

public class ControllerManager {

	static ControllerManager INSTANCE = null;

	public synchronized static void load(File dataDir) throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		INSTANCE = new ControllerManager(dataDir);
	}

	final private File dataDir;

	final private ScriptEngine scriptEngine;

	private ControllerManager(File dataDir) {
		this.dataDir = dataDir;
		ScriptEngineManager manager = new ScriptEngineManager();
		scriptEngine = manager.getEngineByName("nashorn");
	}

	void handle(WebappTransaction transaction, String controllerPath)
			throws URISyntaxException, IOException, InterruptedException, ReflectiveOperationException,
			ServletException, ScriptException, PrivilegedActionException {
		if (controllerPath == null)
			return;
		File controllerFile = new File(dataDir, controllerPath);
		if (controllerFile.exists()) {
			if (!controllerFile.isFile())
				throw new FileNotFoundException("Controller not found: " + controllerPath);
			handleFile(transaction, controllerFile);
		} else
			handleJavaClass(transaction, controllerPath);
	}

	public static class RestrictedAccessControlContext {

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

	private void handleFile(WebappTransaction transaction, File controllerFile)
			throws IOException, ScriptException, PrivilegedActionException, InterruptedException,
			ReflectiveOperationException, ServletException {
		String ext = FilenameUtils.getExtension(controllerFile.getName());
		if (StringUtils.isEmpty(ext))
			throw new ScriptException("Unsupported controller " + controllerFile.getName());
		if ("js".equals(ext))
			handleJavascript(transaction, controllerFile);
		else
			throw new ScriptException("Unsupported controller extension: " + controllerFile.getName());
	}

	private void handleJavascript(WebappTransaction transaction, File controllerFile)
			throws IOException, ScriptException, PrivilegedActionException {
		WebappHttpResponse response = transaction.getResponse();
		response.setHeader("Cache-Control", "max-age=0, no-cache, no-store");
		Bindings bindings = scriptEngine.createBindings();
		IOUtils.CloseableList closeables = new IOUtils.CloseableList();
		bindings.put("closeable", closeables);
		bindings.put("console", new ScriptConsole());
		Map<String, Object> variables = response.getVariables();
		if (variables != null)
			for (Map.Entry<String, Object> entry : variables.entrySet())
				bindings.put(entry.getKey(), entry.getValue());
		FileReader fileReader = new FileReader(controllerFile);
		try {
			ScriptUtils.evalScript(scriptEngine, RestrictedAccessControlContext.INSTANCE, fileReader, bindings);
		} finally {
			fileReader.close();
			closeables.close();
		}
	}

	private void handleJavaClass(WebappTransaction transaction, String className)
			throws IOException, InterruptedException, ScriptException, ReflectiveOperationException, ServletException {
		IOUtils.CloseableList closeables = new IOUtils.CloseableList();
		WebappHttpResponse response = transaction.getResponse();
		response.getVariables().put("closeable", closeables);
		Class<? extends HttpServlet> servletClass = ClassLoaderUtils.findClass(null, className);
		Objects.requireNonNull(servletClass, "Class not found: " + className);
		HttpServlet servlet = servletClass.newInstance();
		try {
			servlet.service(transaction.getRequest(), transaction.getResponse());
		} finally {
			closeables.close();
		}
	}

}
