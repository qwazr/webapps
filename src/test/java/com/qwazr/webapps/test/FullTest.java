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

import org.apache.http.Header;
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

	private final static String MIME_TEXT_HTML = "text/html";
	private final static String MIME_TEXT_CSS = "text/css";
	private final static String MIME_IMAGE_X_PNG = "image/x-png";
	private final static String MIME_FAVICON = "image/vnd.microsoft.icon";

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
		final HttpResponse response = checkResponse(Request.Get(TestServer.BASE_SERVLET_URL + "/java"), 200);
		checkEntity(response, MIME_TEXT_HTML, TestServlet.TEST_STRING);
	}

	@Test
	public void test150JaxRsAppJson() throws IOException {
		final String pathParam = "sub-path-app-json";
		final HttpResponse response =
				checkResponse(Request.Get(TestServer.BASE_SERVLET_URL + "/jaxrs-app/json/test/" + pathParam), 200);
		checkEntity(response, "application/json", TestJaxRs.TEST_STRING, pathParam);
	}

	@Test
	public void test151JaxRsAppXml() throws IOException {
		final String pathParam = "sub-path-app-xml";
		final HttpResponse response =
				checkResponse(Request.Post(TestServer.BASE_SERVLET_URL + "/jaxrs-app/xml/test/" + pathParam), 200);
		checkEntity(response, "application/xml", TestJaxRs.TEST_STRING, pathParam);
	}

	@Test
	public void test160JaxRsClassJson() throws IOException {
		final String pathParam = "sub-path-class-json";
		final HttpResponse response =
				checkResponse(Request.Get(TestServer.BASE_SERVLET_URL + "/jaxrs-class-json/json/test/" + pathParam),
						200);
		checkEntity(response, "application/json", TestJaxRs.TEST_STRING, pathParam);
	}

	@Test
	public void test161JaxRsClassXml() throws IOException {
		final String pathParam = "sub-path-class-xml";
		final HttpResponse response =
				checkResponse(Request.Post(TestServer.BASE_SERVLET_URL + "/jaxrs-class-xml/xml/test/" + pathParam),
						200);
		checkEntity(response, "application/xml", TestJaxRs.TEST_STRING, pathParam);
	}

	@Test
	public void test170JaxRsClassBoth() throws IOException {
		final String pathParam = "sub-path-class-both";
		HttpResponse response =
				checkResponse(Request.Post(TestServer.BASE_SERVLET_URL + "/jaxrs-class-both/xml/test/" + pathParam),
						200);
		checkEntity(response, "application/xml", TestJaxRs.TEST_STRING, pathParam);
		response = checkResponse(Request.Get(TestServer.BASE_SERVLET_URL + "/jaxrs-class-both/json/test/" + pathParam),
				200);
		checkEntity(response, "application/json", TestJaxRs.TEST_STRING, pathParam);
	}

	@Test
	public void test200javascriptServlet() throws IOException {
		HttpResponse response = checkResponse(Request.Get(TestServer.BASE_SERVLET_URL + "/javascript"), 200);
		checkEntity(response, MIME_TEXT_HTML, TestServlet.TEST_STRING);
	}

	private final static String PARAM_TEST_STRING = "testParam=testValue";

	@Test
	public void test210javascriptServletWithParam() throws IOException {
		HttpResponse response =
				checkResponse(Request.Get(TestServer.BASE_SERVLET_URL + "/javascript?" + PARAM_TEST_STRING), 200);
		checkEntity(response, MIME_TEXT_HTML, TestServlet.TEST_STRING);
		checkEntity(response, MIME_TEXT_HTML, PARAM_TEST_STRING);
	}

	private final void checkContentType(HttpResponse response, String contentTypePrefix) {
		Assert.assertNotNull(response);
		Header contentType = response.getFirstHeader("Content-Type");
		Assert.assertNotNull(contentType);
		Assert.assertTrue(contentType.getValue().startsWith(contentTypePrefix));
	}

	@Test
	public void test300staticFile() throws IOException {
		final String badUrl = TestServer.BASE_SERVLET_URL + "/css/dummy.css";
		checkResponse(Request.Head(badUrl), 404);
		checkResponse(Request.Get(badUrl), 404);
		final String goodUrl = TestServer.BASE_SERVLET_URL + "/css/test.css";
		checkContentType(checkResponse(Request.Head(goodUrl), 200), MIME_TEXT_CSS);
		checkEntity(checkResponse(Request.Get(goodUrl), 200), MIME_TEXT_CSS, ".qwazr {");
	}

	@Test
	public void test301staticResource() throws IOException {
		final String badUrl = TestServer.BASE_SERVLET_URL + "/img/dummy.png";
		checkResponse(Request.Head(badUrl), 404);
		checkResponse(Request.Get(badUrl), 404);
		final String goodUrl = TestServer.BASE_SERVLET_URL + "/img/logo.png";
		checkContentType(checkResponse(Request.Head(goodUrl), 200), MIME_IMAGE_X_PNG);
		checkEntity(checkResponse(Request.Get(goodUrl), 200), MIME_IMAGE_X_PNG, null);
	}

	@Test
	public void test302favicon() throws IOException {
		final String url = TestServer.BASE_SERVLET_URL + "/favicon.ico";
		checkResponse(Request.Head(url), 200);
		HttpResponse response = checkResponse(Request.Get(url), 200);
		checkEntity(response, MIME_FAVICON, null);
	}

	@Test
	public void test800Filters()
			throws URISyntaxException, IOException, InstantiationException, ServletException, IllegalAccessException {
		Assert.assertEquals(1, TestFilter.initializedFilters.size());
		Assert.assertEquals(TestFilter.class, TestFilter.initializedFilters.iterator().next().getClass());
		Assert.assertEquals(1, TestFilter.calledFilters.size());
		Assert.assertEquals(TestFilter.class, TestFilter.calledFilters.iterator().next().getClass());
	}
}
