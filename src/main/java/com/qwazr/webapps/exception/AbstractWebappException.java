/**   
 * License Agreement for QWAZR
 *
 * Copyright (C) 2014-2015 OpenSearchServer Inc.
 * 
 * http://www.qwazr.com
 * 
 * This file is part of QWAZR.
 *
 * QWAZR is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * QWAZR is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with QWAZR. 
 *  If not, see <http://www.gnu.org/licenses/>.
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
