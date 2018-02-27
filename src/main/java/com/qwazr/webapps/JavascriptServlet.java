/*
 * Copyright 2014-2017 Emmanuel Keller / QWAZR
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

import com.qwazr.library.LibraryServiceInterface;
import com.qwazr.scripts.ScriptConsole;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JavascriptServlet extends HttpServlet {

	private final ScriptEngine scriptEngine;

	private final Path controllerFile;

	private final LibraryServiceInterface libraryService;

	public JavascriptServlet(final ScriptEngine scriptEngine, final LibraryServiceInterface libraryService,
			final Path controllerFile) {
		this.scriptEngine = scriptEngine;
		this.libraryService = libraryService;
		this.controllerFile = controllerFile;
	}

	private void handle(final HttpServletRequest req, final HttpServletResponse rep)
			throws IOException, ServletException {
		WebappHttpRequest request = new WebappHttpRequestImpl(req);
		WebappHttpResponse response = new WebappHttpResponse(rep);
		response.setHeader("Cache-Control", "max-age=0, no-cache, no-store");
		Bindings bindings = scriptEngine.createBindings();
		bindings.put("console", new ScriptConsole(null));
		bindings.put("request", request);
		bindings.put("response", response);
		bindings.put("library", libraryService);
		bindings.put("closeable", req.getAttribute(CloseableFilter.ATTRIBUTE_NAME));
		bindings.put("session", request.getSession());
		bindings.putAll(request.getAttributes());
		try (final BufferedReader reader = Files.newBufferedReader(controllerFile)) {
			scriptEngine.eval(reader, bindings);
		} catch (ScriptException e) {
			throw new ServletException(e);
		}
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse rep) throws IOException, ServletException {
		handle(req, rep);
	}

	@Override
	public void doHead(HttpServletRequest req, HttpServletResponse rep) throws IOException, ServletException {
		handle(req, rep);
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse rep) throws IOException, ServletException {
		handle(req, rep);
	}

	@Override
	public void doPut(HttpServletRequest req, HttpServletResponse rep) throws IOException, ServletException {
		handle(req, rep);
	}

	@Override
	public void doOptions(HttpServletRequest req, HttpServletResponse rep) throws IOException, ServletException {
		handle(req, rep);
	}

	@Override
	public void doTrace(HttpServletRequest req, HttpServletResponse rep) throws IOException, ServletException {
		handle(req, rep);
	}

	@Override
	public void doDelete(HttpServletRequest req, HttpServletResponse rep) throws IOException, ServletException {
		handle(req, rep);
	}

}
