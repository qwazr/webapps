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
package com.qwazr.webapps;

import com.qwazr.utils.LoggerUtils;
import com.qwazr.utils.ObjectMappers;
import com.qwazr.utils.concurrent.ReadWriteLock;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

class GlobalConfiguration {

	private static final Logger logger = LoggerUtils.getLogger(GlobalConfiguration.class);

	private final ReadWriteLock mapLock = ReadWriteLock.stamped();

	private final LinkedHashMap<File, WebappDefinition> webappFileMap;

	GlobalConfiguration() {
		webappFileMap = new LinkedHashMap<>();
	}

	WebappDefinition getWebappDefinition() {
		return mapLock.read(() -> {
			final WebappDefinition.Builder builder = new WebappDefinition.Builder();
			builder.add(webappFileMap.values());
			return builder.build();
		});
	}

	void loadWebappDefinition(final File jsonFile) {
		try {
			final WebappDefinition webappDefinition = ObjectMappers.JSON.readValue(jsonFile, WebappDefinition.class);

			if (webappDefinition == null || webappDefinition.isEmpty()) {
				unloadWebappDefinition(jsonFile);
				return;
			}

			logger.info(() -> "Load WebApp configuration file: " + jsonFile.getAbsolutePath());

			mapLock.write(() -> {
				webappFileMap.put(jsonFile, webappDefinition);
			});

		} catch (IOException e) {
			logger.log(Level.SEVERE, e, e::getMessage);
		}
	}

	private void unloadWebappDefinition(final File jsonFile) {
		mapLock.write(() -> {
			final WebappDefinition webappDefinition = webappFileMap.remove(jsonFile);
			if (webappDefinition == null)
				return;
			logger.info(() -> "Unload WebApp configuration file: " + jsonFile.getAbsolutePath());
		});
	}

}
