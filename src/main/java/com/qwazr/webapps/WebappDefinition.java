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
package com.qwazr.webapps;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebappDefinition {

	public final Map<String, String> controllers;
	public final Map<String, String> filters;
	public final Map<String, String> statics;
	public final Set<String> listeners;
	public final String identity_manager;

	public WebappDefinition() {
		controllers = null;
		filters = null;
		statics = null;
		listeners = null;
		identity_manager = null;
	}

	private WebappDefinition(Builder builder) {
		this.controllers = builder.controllers.isEmpty() ? null : new LinkedHashMap<>(builder.controllers);
		this.filters = builder.controllers.isEmpty() ? null : new LinkedHashMap<>(builder.filters);
		this.statics = builder.statics.isEmpty() ? null : new LinkedHashMap<>(builder.statics);
		this.listeners = builder.listeners.isEmpty() ? null : new LinkedHashSet<>(builder.listeners);
		this.identity_manager = builder.identity_manager;
	}

	@JsonIgnore
	public boolean isEmpty() {
		return (controllers == null || controllers.isEmpty()) && (statics == null || statics.isEmpty()) &&
				(listeners == null || listeners.isEmpty() && (identity_manager == null || identity_manager.isEmpty()));
	}

	public static class Builder {

		private final Map<String, String> controllers;
		private final Map<String, String> filters;
		private final Map<String, String> statics;
		private final Set<String> listeners;
		private String identity_manager;

		Builder() {
			controllers = new LinkedHashMap<>();
			filters = new LinkedHashMap<>();
			statics = new LinkedHashMap<>();
			listeners = new LinkedHashSet<>();
			identity_manager = null;
		}

		public Builder add(WebappDefinition webappDefinition) {
			if (webappDefinition == null)
				return this;
			if (webappDefinition.controllers != null)
				controllers.putAll(webappDefinition.controllers);
			if (webappDefinition.filters != null)
				filters.putAll(webappDefinition.filters);
			if (webappDefinition.statics != null)
				statics.putAll(webappDefinition.statics);
			if (webappDefinition.listeners != null)
				listeners.addAll(webappDefinition.listeners);
			if (webappDefinition.identity_manager != null)
				identity_manager = webappDefinition.identity_manager;
			return this;
		}

		public Builder add(Collection<WebappDefinition> webappDefinitions) {
			if (webappDefinitions == null)
				return this;
			for (WebappDefinition webappDefinition : webappDefinitions)
				add(webappDefinition);
			return this;
		}

		public Builder addController(String route, String className) {
			controllers.put(route, className);
			return this;
		}

		public Builder addFilter(String route, String className) {
			filters.put(route, className);
			return this;
		}

		public Builder addStatic(String route, String path) {
			statics.put(route, path);
			return this;
		}

		public Builder addListener(Class<?>... classes) {
			if (classes != null)
				for (Class<?> clazz : classes)
					listeners.add(clazz.getName());
			return this;
		}

		public Builder setIdentityManager(Class<?> identityManager) {
			this.identity_manager = identityManager == null ? null : identityManager.getName();
			return this;
		}

		public WebappDefinition build() {
			return new WebappDefinition(this);
		}
	}
}
