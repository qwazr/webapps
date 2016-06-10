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

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.qwazr.utils.json.JacksonConfig;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import java.util.*;

/**
 * Example of JAX-RS
 */
public class TestJaxRs extends Application {

	public final static String TEST_STRING = "JAX_RS_TEST_STRING";

	public Set<Class<?>> getClasses() {
		return new HashSet<>(Arrays.asList(ServiceJson.class, JacksonConfig.class, JacksonJsonProvider.class));
	}

	@Path("/service")
	public static class ServiceJson {

		@Path("/test/{path-param}")
		@POST
		@Produces("application/json")
		public Map<String, String> getTestJson(@PathParam("path-param") String pathParam) {
			final Map<String, String> map = new HashMap<>();
			map.put(TEST_STRING, pathParam);
			return map;
		}
	}

	@Path("/service")
	public static class Service {

		@Path("/test/{path-param}")
		@POST
		@Produces("text/plain")
		public String getTestJson(@PathParam("path-param") String pathParam) {
			return TEST_STRING + " " + pathParam;
		}
	}

}
