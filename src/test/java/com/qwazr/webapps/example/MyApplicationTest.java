/*
 * Copyright 2016-2017 Emmanuel Keller / QWAZR
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
package com.qwazr.webapps.example;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import javax.management.JMException;
import javax.servlet.ServletException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

public class MyApplicationTest {

	@Test
	public void test() throws JMException, ReflectiveOperationException, ServletException, IOException {
		MyApplication.main(null);
		Assert.assertNotNull(MyApplication.serverInstance);

		final WebTarget target = ClientBuilder.newClient().target("http://localhost:8081");

		final String html = target.path("/test").request(MediaType.TEXT_HTML_TYPE).get(String.class);
		Assert.assertEquals("<html>Hello World</html>\n", html);

		final String css = target.path("webjars/bootstrap/4.3.1/css/bootstrap.css")
				.request(MediaType.TEXT_HTML_TYPE)
				.get(String.class);
		Assert.assertTrue(css.contains("bootstrap"));
	}

	@After
	public void cleanup() {
		if (MyApplication.serverInstance != null)
			MyApplication.serverInstance.close();
	}
}
