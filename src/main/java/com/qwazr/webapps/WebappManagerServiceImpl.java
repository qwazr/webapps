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

import com.qwazr.utils.server.ServerException;
import com.qwazr.webapps.transaction.WebappDefinition;
import com.qwazr.webapps.transaction.WebappManager;

import java.io.IOException;
import java.util.Set;

public class WebappManagerServiceImpl implements WebappManagerServiceInterface {

	@Override
	public Set<String> list() {
		return WebappManager.INSTANCE.getNameSet();
	}

	@Override
	public WebappDefinition get(String webappName) {
		try {
			return WebappManager.INSTANCE.getWebAppDefinition(webappName);
		} catch (IOException e) {
			throw ServerException.getJsonException(e);
		}
	}
}
