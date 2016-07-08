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
package com.qwazr.webapps;

import com.qwazr.utils.LockUtils;
import com.qwazr.utils.file.TrackedInterface;
import com.qwazr.utils.json.JsonMapper;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

class GlobalConfiguration implements TrackedInterface.FileChangeConsumer {

	private static final Logger logger = LoggerFactory.getLogger(GlobalConfiguration.class);

	private final LockUtils.ReadWriteLock mapLock = new LockUtils.ReadWriteLock();

	private final TrackedInterface etcTracker;
	private final LinkedHashMap<File, WebappDefinition> webappFileMap;

	GlobalConfiguration(final TrackedInterface etcTracker) {
		this.etcTracker = etcTracker;
		webappFileMap = new LinkedHashMap<>();
		etcTracker.register(this);
	}

	WebappDefinition getWebappDefinition() {
		etcTracker.check();
		return mapLock.read(() -> {
			final WebappDefinition.Builder builder = new WebappDefinition.Builder();
			builder.add(webappFileMap.values());
			return builder.build();
		});
	}

	private void loadWebappDefinition(final File jsonFile) {
		try {
			final WebappDefinition webappDefinition = JsonMapper.MAPPER.readValue(jsonFile, WebappDefinition.class);

			if (webappDefinition == null || webappDefinition.isEmpty()) {
				unloadWebappDefinition(jsonFile);
				return;
			}

			if (logger.isInfoEnabled())
				logger.info("Load WebApp configuration file: " + jsonFile.getAbsolutePath());

			mapLock.write(() -> {
				webappFileMap.put(jsonFile, webappDefinition);
			});

		} catch (IOException e) {
			if (logger.isErrorEnabled())
				logger.error(e.getMessage(), e);
		}
	}

	private void unloadWebappDefinition(final File jsonFile) {
		mapLock.write(() -> {
			final WebappDefinition webappDefinition = webappFileMap.remove(jsonFile);
			if (webappDefinition == null)
				return;
			if (logger.isInfoEnabled())
				logger.info("Unload WebApp configuration file: " + jsonFile.getAbsolutePath());
		});
	}

	@Override
	public void accept(final TrackedInterface.ChangeReason changeReason, final File jsonFile) {
		String extension = FilenameUtils.getExtension(jsonFile.getName());
		if (!"json".equals(extension))
			return;
		switch (changeReason) {
			case UPDATED:
				loadWebappDefinition(jsonFile);
				break;
			case DELETED:
				unloadWebappDefinition(jsonFile);
				break;
		}
	}
}
