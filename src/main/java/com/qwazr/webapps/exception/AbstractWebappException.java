/**
 * Copyright 2014-2015 OpenSearchServer Inc.
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
package com.qwazr.webapps.exception;

import java.io.FileNotFoundException;
import java.security.PrivilegedActionException;

import javax.script.ScriptException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import com.datastax.driver.core.exceptions.QueryValidationException;
import com.qwazr.utils.ExceptionUtils;
import com.qwazr.webapps.exception.WebappHtmlException.Title;

import freemarker.template.TemplateException;

public abstract class AbstractWebappException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2568410123038615106L;

	public abstract void sendQuietly(HttpServletResponse response);

	public static AbstractWebappException newInstance(Exception e) {
		if (e instanceof RuntimeException)
			e = ExceptionUtils.getCauseIfException(e);
		if (e instanceof PrivilegedActionException)
			e = ((PrivilegedActionException) e).getException();
		if (e instanceof AbstractWebappException)
			return (AbstractWebappException) e;
		else if (e instanceof FileNotFoundException)
			return new WebappHtmlException((FileNotFoundException) e);
		else if (e instanceof ScriptException)
			return new WebappHtmlException((ScriptException) e);
		if (e instanceof QueryValidationException)
			return new WebappHtmlException((QueryValidationException) e);
		else if (e instanceof TemplateException)
			return new WebappHtmlException((TemplateException) e);
		System.err.println("EXCEPTION TYPE: " + e.getClass().getName());
		e.printStackTrace();
		return new WebappHtmlException(Status.INTERNAL_SERVER_ERROR,
				Title.ERROR, e);
	}

	public static AbstractWebappException newInstance(Error e) {
		System.err.println("ERROR TYPE: " + e.getClass().getName());
		e.printStackTrace();
		return new WebappHtmlException(Status.INTERNAL_SERVER_ERROR,
				Title.ERROR, e);
	}

}
