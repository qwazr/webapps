/**
 * Copyright 2015-2016 Emmanuel Keller / QWAZR
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

import org.apache.commons.lang.StringUtils;

public class FilePath {

	final String pathInfo;
	final String contextPath;
	final String localPath;

	FilePath(String pathInfo, boolean forceRoot) {
		this.pathInfo = pathInfo;
		int i1 = pathInfo.indexOf('/');
		if (i1 == -1 || forceRoot) {
			contextPath = StringUtils.EMPTY;
			localPath = pathInfo;
			return;
		}
		int i2 = pathInfo.indexOf('/', ++i1);
		if (i2 == -1) {
			contextPath = StringUtils.EMPTY;
			localPath = pathInfo.substring(i1);
			return;
		}
		contextPath = pathInfo.substring(i1, i2);
		localPath = pathInfo.substring(i2);
	}

}