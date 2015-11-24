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
package com.qwazr.webapps.transaction;

import com.qwazr.job.script.ScriptConsole;
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

public class ControllerManager {

	public static volatile ControllerManager INSTANCE = null;

	public static void load(File dataDir) throws IOException {
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

	File findController(ApplicationContext context, String requestPath) throws URISyntaxException, IOException {
		// First we try to find the controller using configuration mapping
		String ctrlrPath = context.findController(requestPath);
		if (ctrlrPath == null)
			return null;
		File ctrlrFile = new File(dataDir, ctrlrPath);
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
			pm.add(new MBeanPermission("com.codahale.metrics.JmxReporter$JmxCounter#-[*:*]", "registerMBean"));
			pm.add(new MBeanPermission("com.codahale.metrics.JmxReporter$JmxCounter#-[*:*]", "unregisterMBean"));
			pm.add(new MBeanPermission("com.codahale.metrics.JmxReporter$JmxGauge#-[*:*]", "registerMBean"));
			pm.add(new MBeanPermission("com.codahale.metrics.JmxReporter$JmxGauge#-[*:*]", "unregisterMBean"));
			pm.add(new MBeanPermission("com.codahale.metrics.JmxReporter$JmxTimer#-[*:*]", "registerMBean"));
			pm.add(new MBeanPermission("com.codahale.metrics.JmxReporter$JmxTimer#-[*:*]", "unregisterMBean"));
			pm.add(new SocketPermission("*.qwazr.net:9042", "connect,accept,resolve"));

			// Required for templates
			pm.add(new FilePermission("<<ALL FILES>>", "read"));

			INSTANCE = new AccessControlContext(new ProtectionDomain[] {
							new ProtectionDomain(new CodeSource(null, (Certificate[]) null), pm) });
		}
	}

	void handle(WebappTransaction transaction, File controllerFile)
					throws IOException, ScriptException, PrivilegedActionException, InterruptedException,
					ReflectiveOperationException, ServletException {
		String ext = FilenameUtils.getExtension(controllerFile.getName());
		if (StringUtils.isEmpty(ext))
			throw new ScriptException("Unsupported controller extension: " + ext);
		if ("js".equals(ext))
			handleJavascript(transaction, controllerFile);
		else if ("java".equals(ext))
			handleJava(transaction, controllerFile);
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

	private void handleJava(WebappTransaction transaction, File controllerFile)
					throws IOException, InterruptedException, ScriptException, ReflectiveOperationException,
					ServletException {
		IOUtils.CloseableList closeables = new IOUtils.CloseableList();
		WebappHttpResponse response = transaction.getResponse();
		response.getVariables().put("closeable", closeables);
		HttpServlet servlet = (HttpServlet) transaction.getContext().getCompilerLoader().loadClass(controllerFile)
						.newInstance();
		try {
			servlet.service(transaction.getRequest(), transaction.getResponse());
		} finally {
			closeables.close();
		}
	}

}
