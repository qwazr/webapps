/**
 * Copyright 2015-2016 Emmanuel Keller / QWAZR
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
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.json.JacksonConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import javax.ws.rs.core.Application;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class BaseRestApplication extends Application {

	public final static Class<?>[] PROVIDERS = { ApiListingResource.class,
			SwaggerSerializers.class,
			JacksonConfig.class,
			JacksonXMLProvider.class,
			JacksonJsonProvider.class,
			RolesAllowedDynamicFeature.class };

	public static void fill(final Collection<String> collection, Class<?>[] classes) {
		for (Class<?> cl : classes)
			collection.add(cl.getName());
	}

	public static String joinResources(final String[] resources) {
		LinkedHashSet<String> set = new LinkedHashSet<>();
		fill(set, PROVIDERS);
		for (String resource : resources)
			set.add(resource);
		return StringUtils.join(set, ',');
	}

	@Override
	public Set<Class<?>> getClasses() {
		return new LinkedHashSet<>(Arrays.asList(PROVIDERS));
	}
}
