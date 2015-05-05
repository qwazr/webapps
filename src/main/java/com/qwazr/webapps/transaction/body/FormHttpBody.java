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
package com.qwazr.webapps.transaction.body;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MultivaluedMap;

public class FormHttpBody implements HttpBodyInterface {

	private final MultivaluedMap<String, String> multiMap;
	private final Map<String, String[]> simpleMap;

	FormHttpBody(Form form) {
		multiMap = form.asMap();
		simpleMap = null;
	}

	FormHttpBody(HttpServletRequest request) {
		simpleMap = request.getParameterMap();
		multiMap = null;
	}

	/**
	 * Alias of getFirstParameter
	 * 
	 * @param name
	 *            the name of the parameter
	 * @return the value stored for this parameter
	 */
	public String parameter(String name) {
		return firstParameter(name);
	}

	/**
	 * 
	 * @param name
	 *            The name of the parameter
	 * @return the value stored for this parameter
	 */
	public String getParameter(String name) {
		return firstParameter(name);
	}

	/**
	 * 
	 * @param name
	 *            The name of the parameter
	 * @return the first value or null if the form does not contain any value.
	 */
	public String firstParameter(String name) {
		if (simpleMap != null) {
			String[] values = simpleMap.get(name);
			if (values == null || values.length < 1)
				return null;
			return values[0];
		}
		if (multiMap != null)
			return multiMap.getFirst(name);
		return null;
	}

	/**
	 * 
	 * @param name
	 *            The name of the parameter
	 * @return an array containing the values or null if the parameter does not
	 *         contain any value.
	 */
	public String[] getParameters(String name) {
		if (simpleMap != null)
			return simpleMap.get(name);
		if (multiMap != null) {
			List<String> values = multiMap.get(name);
			if (values == null)
				return null;
			return values.toArray(new String[values.size()]);
		}
		return null;
	}

	public String[] parameters(String name) {
		return getParameters(name);
	}
}
