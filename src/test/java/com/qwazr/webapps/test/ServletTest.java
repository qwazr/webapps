/**
 * Copyright 2017 Emmanuel Keller / QWAZR
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
package com.qwazr.webapps.test;

import com.google.common.io.Files;
import com.qwazr.server.configuration.ServerConfiguration;
import com.qwazr.utils.http.HttpRequest;
import com.qwazr.webapps.WebappServer;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.MBeanException;
import javax.management.OperationsException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URISyntaxException;

public class ServletTest implements TestChecker {

	public static WebappServer server;
	public static String randomString;

	@BeforeClass
	public static void before()
			throws IOException, URISyntaxException, ReflectiveOperationException, MBeanException, OperationsException,
			ServletException {
		randomString = RandomStringUtils.randomAlphanumeric(10);
		server = new WebappServer(ServerConfiguration.of()
				.data(Files.createTempDir())
				.publicAddress("localhost")
				.listenAddress("localhost")
				.etcFilter("*.json")
				.build(), (webapp, builder) -> {
			webapp.registerConstructorParameter(randomString);
			webapp.registerJavaServlet("/", TestServletConstructorParameter.class, builder);
			webapp.registerJavaServlet(TestServletAnnotation.class, builder);
		});
		server.start();
	}

	@Test
	public void testConstructorParameter() throws IOException {
		final HttpResponse response = checkResponse(HttpRequest.Get(TestServer.BASE_SERVLET_URL + "/"), 200);
		final String content = checkEntity(response, MIME_TEXT_HTML);
		checkContains(content, randomString + "CONSTRUCTOR");
	}

	@Test
	public void testAnnotatedServlet() throws IOException {
		final HttpResponse response = checkResponse(HttpRequest.Get(TestServer.BASE_SERVLET_URL + "/test"), 200);
		final String content = checkEntity(response, MIME_TEXT_HTML);
		checkContains(content, randomString + "ANNOTATION");
	}

	@AfterClass
	public static void after() {
		server.stop();
	}

	public static class TestServletConstructorParameter extends HttpServlet {

		public final String testString;

		public TestServletConstructorParameter(String testString) {
			this.testString = testString;
		}

		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.setContentType(MediaType.TEXT_HTML);
			resp.getWriter().write("<html><body>" + testString + "CONSTRUCTOR</body></html>");
		}

	}

	@WebServlet("/test")
	public static class TestServletAnnotation extends HttpServlet {

		public final String testString;

		public TestServletAnnotation(String testString) {
			this.testString = testString;
		}

		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.setContentType(MediaType.TEXT_HTML);
			resp.getWriter().write("<html><body>" + testString + "ANNOTATION</body></html>");
		}

	}
}
