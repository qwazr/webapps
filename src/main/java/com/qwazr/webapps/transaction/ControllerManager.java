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

import com.qwazr.classloader.ClassLoaderManager;
import com.qwazr.library.LibraryManager;
import com.qwazr.scripts.ScriptConsole;
import com.qwazr.utils.*;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.spec.ServletConfigImpl;
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

	final private AccessTimeCacheMap<Class<? extends HttpServlet>, HttpServlet> servletMap;

	private ControllerManager(File dataDir) {
		this.dataDir = dataDir;
		ScriptEngineManager manager = new ScriptEngineManager();
		scriptEngine = manager.getEngineByName("nashorn");
		servletMap = new AccessTimeCacheMap<>(3600);
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
		bindings.put("console", new ScriptConsole());
		bindings.put("request", transaction.getRequest());
		bindings.put("response", transaction.getResponse());
		bindings.put("session", transaction.getRequest().getSession());
		bindings.putAll(transaction.getRequest().getAttributes());
		try (final FileReader fileReader = new FileReader(controllerFile)) {
			ScriptUtils.evalScript(scriptEngine, RestrictedAccessControlContext.INSTANCE, fileReader, bindings);
		}
	}

	private void handleJavaClass(WebappTransaction transaction, String className)
			throws IOException, InterruptedException, ScriptException, ReflectiveOperationException, ServletException {
		final Class<? extends HttpServlet> servletClass =
				ClassLoaderUtils.findClass(ClassLoaderManager.classLoader, className);
		Objects.requireNonNull(servletClass, "Class not found: " + className);
		final ServletInfo servletInfo = new ServletInfo(className, servletClass);
		servletInfo.getInstanceFactory().createInstance();
		final HttpServlet servlet = servletMap.getOrCreate(servletClass, () -> {
			try {
				HttpServlet servlet1 = servletClass.newInstance();
				//WebServlet webServlet = AnnotationsUtils.getFirstAnnotation(servletClass, WebServlet.class);
				servlet1.init(new ServletConfigImpl(servletInfo, transaction.getRequest().getServletContext()));
				LibraryManager.inject(servlet1);
				return servlet1;
			} catch (InstantiationException | IllegalAccessException | ServletException e) {
				throw new RuntimeException(e);
			}
		});
		servlet.service(transaction.getRequest(), transaction.getResponse());
	}

}
