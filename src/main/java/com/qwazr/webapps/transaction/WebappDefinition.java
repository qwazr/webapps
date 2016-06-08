/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
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
package com.qwazr.webapps.transaction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebappDefinition {

	public final Map<String, String> controllers;
	public final Map<String, String> statics;
	public final Set<String> listeners;
	public final String identity_manager;

	public WebappDefinition() {
		controllers = null;
		statics = null;
		identity_manager = null;
		listeners = null;
	}

	WebappDefinition(Map<String, String> controllers, Map<String, String> statics, Set<String> listeners,
			String identity_manager) {
		this.controllers = controllers;
		this.statics = statics;
		this.listeners = listeners;
		this.identity_manager = identity_manager;
	}

	static WebappDefinition merge(Collection<WebappDefinition> webappDefinitions) {
		if (webappDefinitions == null)
			return null;
		final Map<String, String> controllers = new HashMap<>();
		final Map<String, String> statics = new HashMap<>();
		final Set<String> listeners = new LinkedHashSet<>();
		AtomicReference<String> identityManagerRef = new AtomicReference<>(null);
		webappDefinitions.forEach(webappDefinition -> {
			if (webappDefinition.controllers != null)
				controllers.putAll(webappDefinition.controllers);
			if (webappDefinition.statics != null)
				statics.putAll(webappDefinition.statics);
			if (webappDefinition.listeners != null)
				listeners.addAll(webappDefinition.listeners);
			if (webappDefinition.identity_manager != null)
				identityManagerRef.set(webappDefinition.identity_manager);
		});
		return new WebappDefinition(controllers, statics, listeners, identityManagerRef.get());
	}

	@JsonIgnore
	public boolean isEmpty() {
		return (controllers == null || controllers.isEmpty()) && (statics == null || statics.isEmpty()) &&
				(listeners == null || listeners.isEmpty());
	}
}
