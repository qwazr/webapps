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
import com.qwazr.server.ServletContextBuilder;
import com.qwazr.server.configuration.ServerConfiguration;
import com.qwazr.utils.RandomUtils;
import com.qwazr.utils.http.HttpRequest;
import com.qwazr.webapps.WebappServer;
import org.apache.http.HttpResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.JMException;
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
	public static String randomString1;
	public static String randomString2;

	@BeforeClass
	public static void before()
			throws IOException, URISyntaxException, ReflectiveOperationException, JMException, ServletException {
		randomString1 = RandomUtils.alphanumeric(10);
		randomString2 = RandomUtils.alphanumeric(10);
		server = new WebappServer(ServerConfiguration.of()
				.data(Files.createTempDir())
				.publicAddress("localhost")
				.listenAddress("localhost")
				.etcFilter("*.json")
				.build(), (webapp, builder) -> {
			final ServletContextBuilder context = builder.getWebAppContext();
			builder.getConstructorParameters().registerConstructorParameter(randomString1);
			webapp.registerJavaServlet("/", TestServletConstructorParameter.class, context);
			webapp.registerJavaServlet(TestServletAnnotation1.class, context);
			webapp.registerJavaServlet(TestServletAnnotation2.class, () -> new TestServletAnnotation2(randomString2),
					context);
		});
		server.start();
	}

	@Test
	public void testConstructorParameter() throws IOException {
		final HttpResponse response = checkResponse(HttpRequest.Get(TestServer.BASE_SERVLET_URL + "/"), 200);
		final String content = checkEntity(response, MIME_TEXT_HTML);
		checkContains(content, randomString1 + "CONSTRUCTOR");
	}

	@Test
	public void testAnnotatedServlet1() throws IOException {
		final HttpResponse response = checkResponse(HttpRequest.Get(TestServer.BASE_SERVLET_URL + "/test1"), 200);
		final String content = checkEntity(response, MIME_TEXT_HTML);
		checkContains(content, randomString1 + "ANNOTATION");
	}

	@Test
	public void testAnnotatedServlet2() throws IOException {
		final HttpResponse response = checkResponse(HttpRequest.Get(TestServer.BASE_SERVLET_URL + "/test2"), 200);
		final String content = checkEntity(response, MIME_TEXT_HTML);
		checkContains(content, randomString2 + "ANNOTATION");
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

	@WebServlet("/test1")
	public static class TestServletAnnotation1 extends HttpServlet {

		public final String testString;

		public TestServletAnnotation1(String testString) {
			this.testString = testString;
		}

		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.setContentType(MediaType.TEXT_HTML);
			resp.getWriter().write("<html><body>" + testString + "ANNOTATION</body></html>");
		}

	}

	@WebServlet("/test2")
	public static class TestServletAnnotation2 extends TestServletAnnotation1 {

		public TestServletAnnotation2(String testString) {
			super(testString);
		}
	}
}
