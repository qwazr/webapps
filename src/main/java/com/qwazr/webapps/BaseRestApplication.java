/**
 * Copyright 2015-2017 Emmanuel Keller / QWAZR
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
package com.qwazr.webapps;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.fasterxml.jackson.jaxrs.xml.JacksonXMLProvider;
import com.qwazr.utils.json.JacksonConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import javax.ws.rs.core.Application;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class BaseRestApplication extends Application {

	final static Class<?>[] PROVIDERS_CLASSES = { ApiListingResource.class,
			SwaggerSerializers.class,
			JacksonConfig.class,
			JacksonXMLProvider.class,
			JacksonJsonProvider.class,
			RolesAllowedDynamicFeature.class };

	final static Collection<Object> PROVIDERS_SINGLETONS;

	static {
		PROVIDERS_SINGLETONS = new LinkedHashSet<>();
		for (Class<?> providerClass : PROVIDERS_CLASSES)
			try {
				PROVIDERS_SINGLETONS.add(providerClass.newInstance());
			} catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
	}

	public Set<Class<?>> getClasses() {
		final Set<Class<?>> set = new LinkedHashSet<>();
		Collections.addAll(set, PROVIDERS_CLASSES);
		return set;
	}

}
