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

import com.fasterxml.jackson.databind.JsonNode;
import com.qwazr.utils.http.HttpRequest;
import com.qwazr.utils.json.JsonMapper;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
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
	public void test000StartServer() throws Exception {
		TestServer.startServer();
	}

	@Test
	public void test50Listener() {
		Assert.assertEquals(1, TestListener.initializedListeners.size());
		TestListener listener = TestListener.initializedListeners.iterator().next();
		Assert.assertEquals(TestListener.class, listener.getClass());
	}

	private CloseableHttpResponse checkResponse(HttpRequest request, int expectedStatusCode) throws IOException {
		return checkResponse(request.execute(), expectedStatusCode);
	}

	private CloseableHttpResponse checkResponse(CloseableHttpResponse response, int expectedStatusCode)
			throws IOException {
		Assert.assertNotNull(response);
		Assert.assertEquals(expectedStatusCode, response.getStatusLine().getStatusCode());
		return response;
	}

	private String checkEntity(HttpResponse response, String contentType)
			throws IOException {
		final HttpEntity entity = response.getEntity();
		Assert.assertNotNull(entity);
		Assert.assertTrue(entity.getContentType().getValue().startsWith(contentType));
		return EntityUtils.toString(entity);
	}

	private void checkContains(String content, String... patterns) {
		for (String pattern : patterns)
			Assert.assertTrue(content.contains(pattern));
	}

	@Test
	public void test100javaServlet() throws IOException {
		final HttpResponse response = checkResponse(HttpRequest.Get(TestServer.BASE_SERVLET_URL + "/java"), 200);
		final String content = checkEntity(response, MIME_TEXT_HTML);
		checkContains(content, TestServlet.TEST_STRING);
	}

	@Test
	public void test150JaxRsAppJson() throws IOException {
		final String pathParam = "sub-path-app-json";
		final HttpResponse response =
				checkResponse(HttpRequest.Get(TestServer.BASE_SERVLET_URL + "/jaxrs-app/json/test/" + pathParam), 200);
		final String content = checkEntity(response, "application/json");
		checkContains(content, TestJaxRsResources.TEST_STRING, pathParam);
		checkResponse(HttpRequest.Get(TestServer.BASE_SERVLET_URL + "/jaxrs-app/swagger.json"), 404);
	}

	@Test
	public void test151JaxRsAppXml() throws IOException {
		final String pathParam = "sub-path-app-xml";
		final HttpResponse response =
				checkResponse(HttpRequest.Post(TestServer.BASE_SERVLET_URL + "/jaxrs-app/xml/test/" + pathParam), 200);
		final String content = checkEntity(response, "application/xml");
		checkContains(content, TestJaxRsResources.TEST_STRING, pathParam);
		checkResponse(HttpRequest.Get(TestServer.BASE_SERVLET_URL + "/jaxrs-app/swagger.json"), 404);
	}

	@Test
	public void test160JaxRsClassJson() throws IOException {
		final String pathParam = "sub-path-class-json";
		final HttpResponse response =
				checkResponse(HttpRequest.Get(TestServer.BASE_SERVLET_URL + "/jaxrs-class-json/json/test/" + pathParam),
						200);
		final String content = checkEntity(response, "application/json");
		checkContains(content, TestJaxRsResources.TEST_STRING, pathParam);
		checkSwagger(
				checkResponse(HttpRequest.Get(TestServer.BASE_SERVLET_URL + "/jaxrs-class-json/swagger.json"), 200),
				"ServiceJson", "/jaxrs-class-json");
	}

	@Test
	public void test161JaxRsClassXml() throws IOException {
		final String pathParam = "sub-path-class-xml";
		final HttpResponse response =
				checkResponse(HttpRequest.Post(TestServer.BASE_SERVLET_URL + "/jaxrs-class-xml/xml/test/" + pathParam),
						200);
		final String content = checkEntity(response, "application/xml");
		checkContains(content, TestJaxRsResources.TEST_STRING, pathParam);
		checkSwagger(checkResponse(HttpRequest.Get(TestServer.BASE_SERVLET_URL + "/jaxrs-class-xml/swagger.json"), 200),
				"ServiceXml", "/jaxrs-class-xml");
	}

	@Test
	public void test170JaxRsClassBoth() throws IOException {
		final String pathParam = "sub-path-class-both";
		HttpResponse response =
				checkResponse(HttpRequest.Post(TestServer.BASE_SERVLET_URL + "/jaxrs-class-both/xml/test/" + pathParam),
						200);
		String content = checkEntity(response, "application/xml");
		checkContains(content, TestJaxRsResources.TEST_STRING, pathParam);
		response =
				checkResponse(HttpRequest.Get(TestServer.BASE_SERVLET_URL + "/jaxrs-class-both/json/test/" + pathParam),
						200);
		content = checkEntity(response, "application/json");
		checkContains(content, TestJaxRsResources.TEST_STRING, pathParam);
		checkSwagger(
				checkResponse(HttpRequest.Get(TestServer.BASE_SERVLET_URL + "/jaxrs-class-both/swagger.json"), 200),
				"ServiceBoth", "/jaxrs-class-both");
	}

	private CloseableHttpResponse auth(HttpRequest request, String user, String pass) throws IOException {
		final HttpClientContext context = HttpClientContext.create();
		final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, pass));
		context.setAttribute(HttpClientContext.CREDS_PROVIDER, credentialsProvider);
		return request.execute(context);
	}

	private CloseableHttpResponse validAuth(HttpRequest request) throws IOException {
		return auth(request, TestIdentityProvider.TEST_USER, TestIdentityProvider.TEST_PASSWORD);
	}

	private CloseableHttpResponse wrongAuth(HttpRequest request) throws IOException {
		return auth(request, "dummy", "dummy");
	}

	private void checkSwagger(final HttpResponse response, final String title, final String basePath)
			throws IOException {
		JsonNode root = JsonMapper.MAPPER.readTree(response.getEntity().getContent());
		Assert.assertNotNull(root);
		Assert.assertTrue(root.has("swagger"));
		Assert.assertEquals("2.0", root.get("swagger").asText());
		Assert.assertTrue(root.has("basePath"));
		Assert.assertEquals(basePath, root.get("basePath").asText());
		Assert.assertTrue(root.has("info"));
		JsonNode info = root.get("info");
		Assert.assertEquals("v1.2.3", info.get("version").asText());
		Assert.assertEquals(title, info.get("title").asText());
	}

	private void testAuth(final String appPath, final String appTitle) throws IOException {
		checkResponse(HttpRequest.Head(TestServer.BASE_SERVLET_URL + appPath + "/auth/test"), 401);
		checkResponse(wrongAuth(HttpRequest.Head(TestServer.BASE_SERVLET_URL + appPath + "/auth/test")), 401);
		checkResponse(validAuth(HttpRequest.Head(TestServer.BASE_SERVLET_URL + appPath + "/auth/wrong-role")), 403);
		final HttpResponse response =
				checkResponse(validAuth(HttpRequest.Head(TestServer.BASE_SERVLET_URL + appPath + "/auth/test")), 204);
		final Header userHeader = response.getFirstHeader(TestJaxRsResources.ServiceAuth.xAuthUser);
		Assert.assertNotNull(userHeader);
		checkSwagger(
				checkResponse(validAuth(HttpRequest.Get(TestServer.BASE_SERVLET_URL + appPath + "/swagger.json")), 200),
				appTitle, appPath);
	}

	@Test
	public void test180JaxRsAuth() throws IOException {
		testAuth("/jaxrs-auth", "ServiceAuth");
		Assert.assertEquals(4, TestIdentityProvider.authCount.get());
		Assert.assertEquals(3, TestIdentityProvider.authSuccessCount.get());
		testAuth("/jaxrs-app-auth", "TestJaxRsAppAuth");
		Assert.assertEquals(8, TestIdentityProvider.authCount.get());
		Assert.assertEquals(6, TestIdentityProvider.authSuccessCount.get());

	}

	@Test
	public void test200javascriptServlet() throws IOException {
		HttpResponse response = checkResponse(HttpRequest.Get(TestServer.BASE_SERVLET_URL + "/javascript"), 200);
		checkContains(checkEntity(response, MIME_TEXT_HTML), TestServlet.TEST_STRING);
	}

	private final static String PARAM_TEST_STRING = "testParam=testValue";

	@Test
	public void test210javascriptServletWithParam() throws IOException {
		try (final CloseableHttpResponse response =
				     checkResponse(HttpRequest.Get(TestServer.BASE_SERVLET_URL + "/javascript?" + PARAM_TEST_STRING),
						     200)) {
			final String content = checkEntity(response, MIME_TEXT_HTML);
			checkContains(content, TestServlet.TEST_STRING, PARAM_TEST_STRING);
		}
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
		checkResponse(HttpRequest.Head(badUrl), 404);
		checkResponse(HttpRequest.Get(badUrl), 404);
		final String goodUrl = TestServer.BASE_SERVLET_URL + "/css/test.css";
		checkContentType(checkResponse(HttpRequest.Head(goodUrl), 200), MIME_TEXT_CSS);
		checkContains(checkEntity(checkResponse(HttpRequest.Get(goodUrl), 200), MIME_TEXT_CSS), ".qwazr {");
	}

	@Test
	public void test301staticResource() throws IOException {
		final String badUrl = TestServer.BASE_SERVLET_URL + "/img/dummy.png";
		checkResponse(HttpRequest.Head(badUrl), 404);
		checkResponse(HttpRequest.Get(badUrl), 404);
		final String goodUrl = TestServer.BASE_SERVLET_URL + "/img/logo.png";
		checkContentType(checkResponse(HttpRequest.Head(goodUrl), 200), MIME_IMAGE_X_PNG);
		checkEntity(checkResponse(HttpRequest.Get(goodUrl), 200), MIME_IMAGE_X_PNG);
	}

	@Test
	public void test302favicon() throws IOException {
		final String url = TestServer.BASE_SERVLET_URL + "/favicon.ico";
		checkResponse(HttpRequest.Head(url), 200);
		HttpResponse response = checkResponse(HttpRequest.Get(url), 200);
		checkEntity(response, MIME_FAVICON);
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
