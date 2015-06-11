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
import com.qwazr.utils.json.DirectoryJsonManager;
import com.qwazr.utils.server.ServerException;

public class WebappManager extends DirectoryJsonManager<WebappDefinition> {

	private static final Logger logger = LoggerFactory
			.getLogger(WebappManager.class);

	public static volatile WebappManager INSTANCE = null;

	public static void load(File webappDirectory) throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		try {
			INSTANCE = new WebappManager(webappDirectory);
		} catch (ServerException e) {
			throw new RuntimeException(e);
		}
	}

	private final Map<String, ApplicationContext> applicationContextMap;
	private final LockUtils.ReadWriteLock contextsLock = new LockUtils.ReadWriteLock();

	private WebappManager(File webappDirectory) throws IOException,
			ServerException {
		super(webappDirectory, WebappDefinition.class);
		applicationContextMap = new ConcurrentHashMap<String, ApplicationContext>();
	}

	private ApplicationContext getApplicationContext(String contextPath,
			WebappDefinition webappDefinition) throws JsonParseException,
			JsonMappingException, IOException {
		contextsLock.r.lock();
		try {
			ApplicationContext existingContext = applicationContextMap
					.get(contextPath);
			if (existingContext != null)
				return existingContext;
		} finally {
			contextsLock.r.unlock();
		}
		contextsLock.w.lock();
		try {
			ApplicationContext existingContext = applicationContextMap
					.get(contextPath);
			if (existingContext != null)
				return existingContext;
			logger.info("Load application " + contextPath);
			ApplicationContext applicationContext = new ApplicationContext(
					contextPath, webappDefinition, existingContext);
			applicationContextMap.put(contextPath, applicationContext);
			return applicationContext;
		} finally {
			contextsLock.w.unlock();
		}
	}

	public ApplicationContext findApplicationContext(FilePath filePath,
			WebappTransaction transaction) throws JsonParseException,
			JsonMappingException, IOException {
		if (filePath == null)
			return null;
		if (filePath.contextPath == null)
			return null;
		WebappDefinition webappDefinition = get(filePath.contextPath.isEmpty() ? "ROOT"
				: filePath.contextPath);
		if (webappDefinition == null)
			return null;
		ApplicationContext applicationContext = getApplicationContext(
				filePath.contextPath, webappDefinition);
		if (applicationContext == null)
			return null;
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
