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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.servlet.ServletException;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URISyntaxException;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FullTest {

	@Test
	public void test000StartServer()
			throws URISyntaxException, IOException, InstantiationException, ServletException, IllegalAccessException {
		TestServer.startServer();
	}

	private void checkResult(HttpResponse response) {
		Assert.assertNotNull(response);
		Assert.assertEquals(200, response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		Assert.assertNotNull(entity);
		Assert.assertEquals(MediaType.TEXT_HTML, entity.getContentType().getValue());
		Assert.assertTrue(entity.toString().contains(TestServlet.TEST_STRING));
	}

	@Test
	public void test100javaServlet() throws IOException {
		checkResult(Request.Get(TestServer.BASE_SERVLET_URL + "/java").execute().returnResponse());
	}

	@Test
	public void test200javascriptServlet() throws IOException {
		checkResult(Request.Get(TestServer.BASE_SERVLET_URL + "/javascript").execute().returnResponse());
	}
}
