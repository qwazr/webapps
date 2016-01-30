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

import com.qwazr.utils.LockUtils;

import java.util.List;

public class ApplicationContext {

	private final WebappDefinition webappDefinition;

	private final List<PathBind> controllerMatchers;

	private final List<PathBind> staticMatchers;

	private final LockUtils.ReadWriteLock sessionsLock = new LockUtils.ReadWriteLock();

	ApplicationContext(WebappDefinition webappDefinitions) {
		this.webappDefinition = webappDefinitions;

		// Load the resources
		controllerMatchers = PathBind.loadMatchers(webappDefinition.controllers);
		staticMatchers = PathBind.loadMatchers(webappDefinition.statics);

	}

	WebappDefinition getWebappDefinition() {
		return webappDefinition;
	}

	String findStatic(String requestPath) {
		return PathBind.findMatchingPath(requestPath, staticMatchers);
	}

	String findController(String requestPath) {
		return PathBind.findMatchingPath(requestPath, controllerMatchers);
	}

}
