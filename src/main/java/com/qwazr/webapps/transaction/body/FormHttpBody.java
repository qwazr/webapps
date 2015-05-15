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
