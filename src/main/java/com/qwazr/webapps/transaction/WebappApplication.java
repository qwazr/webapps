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

import com.qwazr.utils.server.InFileSessionPersistenceManager;
import com.qwazr.utils.server.ServletApplication;
import com.qwazr.webapps.WebappHttpServlet;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.api.SessionPersistenceManager;

import javax.servlet.MultipartConfigElement;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

class WebappApplication extends ServletApplication {

	public final static String SESSIONS_PERSISTENCE_DIR = "webapp-sessions";

	private final SessionPersistenceManager sessionPersistenceManager;

	WebappApplication(File tempDirectory) {
		File sessionPersistenceDir = new File(tempDirectory, SESSIONS_PERSISTENCE_DIR);
		if (!sessionPersistenceDir.exists())
			sessionPersistenceDir.mkdir();
		sessionPersistenceManager = new InFileSessionPersistenceManager(sessionPersistenceDir);
	}

	//TODO Parameters for fileupload limitation
	private final static MultipartConfigElement multipartConfigElement = new MultipartConfigElement(
					System.getProperty("java.io.tmpdir"));

	@Override
	protected List<ServletInfo> getServletInfos() {
		List<ServletInfo> servletInfos = new ArrayList<ServletInfo>();
		servletInfos.add(Servlets.servlet("WebAppServlet", WebappHttpServlet.class).addMapping("/*")
						.setMultipartConfig(multipartConfigElement));
		return servletInfos;
	}

	@Override
	protected SessionPersistenceManager getSessionPersistenceManager() {
		return sessionPersistenceManager;
	}

}
