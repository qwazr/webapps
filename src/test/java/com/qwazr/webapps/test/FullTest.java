/**
 * Copyright 2016 Emmanuel Keller / QWAZR
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

import com.qwazr.utils.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.servlet.ServletException;
import java.io.IOException;
import java.net.URISyntaxException;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FullTest {

	private final static String TEXT_HTML = "text/html";
	private final static String TEXT_CSS = "text/css";

	@Test
	public void test000StartServer()
			throws URISyntaxException, IOException, InstantiationException, ServletException, IllegalAccessException {
		TestServer.startServer();
	}

	@Test
	public void test50Listener()
			throws URISyntaxException, IOException, InstantiationException, ServletException, IllegalAccessException {
		Assert.assertEquals(1, TestListener.initializedListeners.size());
		TestListener listener = TestListener.initializedListeners.iterator().next();
		Assert.assertEquals(TestListener.class, listener.getClass());
	}

	private HttpResponse checkResponse(Request request, int expectedStatusCode) throws IOException {
		final HttpResponse response = request.execute().returnResponse();
		Assert.assertNotNull(response);
		Assert.assertEquals(expectedStatusCode, response.getStatusLine().getStatusCode());
		return response;
	}

	private HttpEntity checkEntity(HttpResponse response, String contentType, String... testStrings)
			throws IOException {
		final HttpEntity entity = response.getEntity();
		Assert.assertNotNull(entity);
		Assert.assertTrue(entity.getContentType().getValue().startsWith(contentType));
		String content = EntityUtils.toString(entity);
		if (testStrings != null)
			for (String testString : testStrings)
				Assert.assertTrue(content.contains(testString));
		return entity;
	}

	@Test
	public void test100javaServlet() throws IOException {
		HttpResponse response = checkResponse(Request.Get(TestServer.BASE_SERVLET_URL + "/java"), 200);
		checkEntity(response, TEXT_HTML, TestServlet.TEST_STRING);
	}

	@Test
	public void test150JaxRs() throws IOException {
		final String pathParam = "sub-path";
		HttpResponse response =
				checkResponse(Request.Post(TestServer.BASE_SERVLET_URL + "/jaxrs/service/test/" + pathParam), 200);
		checkEntity(response, "application/json", TestJaxRs.TEST_STRING, pathParam);
	}

	@Test
	public void test200javascriptServlet() throws IOException {
		HttpResponse response = checkResponse(Request.Get(TestServer.BASE_SERVLET_URL + "/javascript"), 200);
		checkEntity(response, TEXT_HTML, TestServlet.TEST_STRING);
	}

	private final static String PARAM_TEST_STRING = "testParam=testValue";

	@Test
	public void test210javascriptServletWithParam() throws IOException {
		HttpResponse response =
				checkResponse(Request.Get(TestServer.BASE_SERVLET_URL + "/javascript?" + PARAM_TEST_STRING), 200);
		checkEntity(response, TEXT_HTML, TestServlet.TEST_STRING);
		checkEntity(response, TEXT_HTML, PARAM_TEST_STRING);
	}

	@Test
	public void test300static() throws IOException {
		HttpResponse response = checkResponse(Request.Get(TestServer.BASE_SERVLET_URL + "/css/test.css"), 200);
		checkEntity(response, TEXT_CSS, ".qwazr {");
	}
}
