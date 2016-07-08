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

import com.qwazr.webapps.BaseRestApplication;
import io.swagger.annotations.Api;
import io.swagger.annotations.Info;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.jaxrs.config.BeanConfig;

import javax.annotation.security.PermitAll;
import javax.ws.rs.ApplicationPath;
import java.util.Set;

/**
 * Example of JAX-RS with authentication
 */
@PermitAll
@Api
@ApplicationPath("/jaxrs-app-auth")
public class TestJaxRsAppAuth extends BaseRestApplication {
	
	public Set<Class<?>> getClasses() {
		final Set<Class<?>> classes = super.getClasses();
		classes.add(TestJaxRsResources.ServiceAuth.class);
		return classes;
	}
}