/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.qwazr.webapps.transaction;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.qwazr.utils.LockUtils;
import com.qwazr.utils.json.JsonMapper;
import com.qwazr.webapps.WebappConfigurationFile;
import com.qwazr.webapps.transaction.FilePathResolver.FilePath;

public class ApplicationContextManager {

	private static final Logger logger = LoggerFactory
			.getLogger(ApplicationContextManager.class);

	public static volatile ApplicationContextManager INSTANCE = null;

	public static void load(String rootPath, String genericConfigurationFilePath)
			throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		INSTANCE = new ApplicationContextManager(rootPath,
				genericConfigurationFilePath);
	}

	private final File globalConfFile;
	private final WebappConfigurationFile globalConfiguration;

	private final String rootPath;

	private final Map<String, ApplicationContext> applicationContextMap;
	private final LockUtils.ReadWriteLock contextsLock = new LockUtils.ReadWriteLock();

	private ApplicationContextManager(String rootPath,
			String genericConfigurationFilePath) throws IOException {
		this.rootPath = rootPath;
		applicationContextMap = new ConcurrentHashMap<String, ApplicationContext>();
		if (genericConfigurationFilePath != null) {
			globalConfFile = new File(genericConfigurationFilePath);
			if (!globalConfFile.exists() && !globalConfFile.isFile())
				throw new IOException("Invalid configuration file: "
						+ genericConfigurationFilePath);
			globalConfiguration = JsonMapper.MAPPER.readValue(globalConfFile,
					WebappConfigurationFile.class);
		} else {
			globalConfFile = null;
			globalConfiguration = null;
		}
	}

	private ApplicationContext getApplicationContext(FilePath filePath)
			throws JsonParseException, JsonMappingException, IOException {
		contextsLock.r.lock();
		try {
			ApplicationContext existingContext = applicationContextMap
					.get(filePath.contextPath);
			if (existingContext != null && !existingContext.mustBeReloaded())
				return existingContext;
		} finally {
			contextsLock.r.unlock();
		}
		contextsLock.w.lock();
		try {
			ApplicationContext existingContext = applicationContextMap
					.get(filePath.contextPath);
			if (existingContext != null && !existingContext.mustBeReloaded())
				return existingContext;
			if (existingContext != null)
				existingContext.close();
			logger.info("Load application " + filePath.contextPath);
			ApplicationContext applicationContext = new ApplicationContext(
					rootPath, filePath.contextPath, globalConfiguration,
					filePath.fileDepth, existingContext);
			applicationContextMap.put(filePath.contextPath, applicationContext);
			return applicationContext;
		} finally {
			contextsLock.w.unlock();
		}
	}

	public ApplicationContext applyConf(FilePath filePath,
			WebappTransaction transaction) throws JsonParseException,
			JsonMappingException, IOException {
		if (filePath == null || filePath.fileDepth == null)
			return null;
		ApplicationContext applicationContext = getApplicationContext(filePath);
		if (applicationContext == null)
			throw new IOException("Not context found.");
		applicationContext.apply(transaction.getResponse());
		return applicationContext;
	}

	public void destroySession(String id) {
		id = id.intern();
		logger.info("Invalid session " + id);
		contextsLock.r.lock();
		try {
			for (ApplicationContext context : applicationContextMap.values())
				context.invalidateSession(id);
		} finally {
			contextsLock.r.unlock();
		}
	}
}
