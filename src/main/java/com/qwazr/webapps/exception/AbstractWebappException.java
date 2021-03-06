/*
 * Copyright 201-2016 Emmanuel Keller / QWAZR
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
package com.qwazr.webapps.exception;

import com.qwazr.webapps.exception.WebappException.Title;

import javax.script.ScriptException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;
import java.io.FileNotFoundException;
import java.security.PrivilegedActionException;

public abstract class AbstractWebappException extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = 2568410123038615106L;

	public abstract void sendQuietly(HttpServletResponse response);

	public static AbstractWebappException newInstance(Throwable e) {
		if (e instanceof RuntimeException)
			e = e.getCause() != null ? e.getCause() : e;
		if (e instanceof PrivilegedActionException)
			e = ((PrivilegedActionException) e).getException();
		if (e instanceof AbstractWebappException)
			return (AbstractWebappException) e;
		else if (e instanceof FileNotFoundException)
			return new WebappException(Status.NOT_FOUND, Title.NOT_FOUND_ERROR, e);
		else if (e instanceof ScriptException)
			return new WebappException(Status.INTERNAL_SERVER_ERROR, Title.SCRIPT_ERROR, e);
		else
			return new WebappException(Status.INTERNAL_SERVER_ERROR, Title.ERROR, e);
	}

	public static AbstractWebappException newInstance(Error e) {
		return new WebappException(Status.INTERNAL_SERVER_ERROR, Title.ERROR, e);
	}

}
