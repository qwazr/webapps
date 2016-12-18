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

import com.qwazr.server.AbstractServiceImpl;
import com.qwazr.server.ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URISyntaxException;

public class WebappManagerServiceImpl extends AbstractServiceImpl implements WebappManagerServiceInterface {

	private static final Logger logger = LoggerFactory.getLogger(WebappManagerServiceImpl.class);

	private volatile WebappManager webappManager;

	WebappManagerServiceImpl(WebappManager webappManager) {
		this.webappManager = webappManager;
	}

	public WebappManagerServiceImpl() {
		this(null);
	}

	@PostConstruct
	public void init() {
		webappManager = getContextAttribute(WebappManager.class);
	}

	@Override
	public WebappDefinition get() {
		try {
			WebappDefinition result = webappManager.getWebAppDefinition();
			return result == null ? new WebappDefinition() : result;
		} catch (IOException | URISyntaxException e) {
			throw ServerException.getJsonException(logger, e);
		}
	}
}
