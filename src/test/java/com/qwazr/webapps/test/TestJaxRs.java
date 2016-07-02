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
import com.fasterxml.jackson.jaxrs.xml.JacksonXMLProvider;
import com.qwazr.utils.json.JacksonConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Example of JAX-RS
 */
public class TestJaxRs extends Application {

	public final static String TEST_STRING = "JAX_RS_TEST_STRING";

	public Set<Class<?>> getClasses() {
		return new HashSet<>(Arrays.asList(ServiceJson.class, ServiceXml.class, ServiceAuth.class, JacksonConfig.class,
				JacksonJsonProvider.class, JacksonXMLProvider.class));
	}

	@Path("/json")
	public static class ServiceJson {

		@Path("/test/{path-param}")
		@GET
		@Produces(MediaType.APPLICATION_JSON)
		public Data getTestJson(@PathParam("path-param") String pathParam) {
			return new Data(TEST_STRING, pathParam);
		}
	}

	@Path("/xml")
	public static class ServiceXml {

		@Path("/test/{path-param}")
		@POST
		@Produces(MediaType.APPLICATION_XML)
		public Data getTestJson(@PathParam("path-param") String pathParam) {
			return new Data(TEST_STRING, pathParam);
		}
	}

	@Path("/auth")
	@PermitAll
	public static class ServiceAuth {

		@Context
		private HttpServletResponse response;

		@Context
		private SecurityContext securityContext;

		public final static String xAuthUser = "X-AUTH-USER";

		@Path("/test")
		@Produces(MediaType.TEXT_PLAIN)
		@RolesAllowed(TestIdentityProvider.VALID_ROLE)
		@HEAD
		public void testAuth() {
			final Principal principal = securityContext.getUserPrincipal();
			if (principal == null)
				return;
			response.setHeader(xAuthUser, principal.getName());
		}

		@Path("/wrong-role")
		@Produces(MediaType.TEXT_PLAIN)
		@RolesAllowed("dummy")
		@HEAD
		public void testWrongRole() {
			final Principal principal = securityContext.getUserPrincipal();
			if (principal == null)
				return;
			response.setHeader(xAuthUser, principal.getName());
		}
	}

	@PermitAll
	public static class AppAuth extends Application {

		public Set<Class<?>> getClasses() {
			return new HashSet<>(Arrays.asList(ServiceAuth.class, JacksonConfig.class, JacksonJsonProvider.class,
					JacksonXMLProvider.class, RolesAllowedDynamicFeature.class));
		}

	}

	public static class Data {

		public final String param1;
		public final String param2;

		public Data() {
			param1 = null;
			param2 = null;
		}

		private Data(String param1, String param2) {
			this.param1 = param1;
			this.param2 = param2;
		}
	}

}
