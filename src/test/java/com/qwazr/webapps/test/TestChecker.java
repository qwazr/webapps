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

import com.qwazr.utils.http.HttpRequest;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;

import java.io.IOException;

public interface TestChecker {

	String MIME_TEXT_HTML = "text/html";
	String MIME_TEXT_CSS = "text/css";
	String MIME_IMAGE_X_PNG = "image/x-png";
	String MIME_FAVICON = "image/vnd.microsoft.icon";

	default CloseableHttpResponse checkResponse(HttpRequest request, int expectedStatusCode) throws IOException {
		return checkResponse(request.execute(), expectedStatusCode);
	}

	default CloseableHttpResponse checkResponse(CloseableHttpResponse response, int expectedStatusCode)
			throws IOException {
		Assert.assertNotNull(response);
		Assert.assertEquals(expectedStatusCode, response.getStatusLine().getStatusCode());
		return response;
	}

	default String checkEntity(HttpResponse response, String contentType) throws IOException {
		final HttpEntity entity = response.getEntity();
		Assert.assertNotNull(entity);
		Assert.assertTrue(entity.getContentType().getValue().startsWith(contentType));
		return EntityUtils.toString(entity);
	}

	default void checkContains(String content, String... patterns) {
		for (String pattern : patterns)
			Assert.assertTrue(content.contains(pattern));
	}

}
