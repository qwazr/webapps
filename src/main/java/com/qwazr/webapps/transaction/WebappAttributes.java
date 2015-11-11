/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
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

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.function.BiConsumer;

public class WebappAttributes implements Map<String, Object> {

	private final HttpServletRequest request;

	WebappAttributes(HttpServletRequest request) {
		this.request = request;
	}

	@Override
	public int size() {
		int i = 0;
		Enumeration<String> names = request.getAttributeNames();
		while (names.hasMoreElements()) {
			names.nextElement();
			i++;
		}
		return i;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		return request.getAttribute(key.toString()) != null;
	}

	@Override
	public boolean containsValue(Object value) {
		Enumeration<String> names = request.getAttributeNames();
		while (names.hasMoreElements()) {
			if (value.equals(request.getAttribute(names.nextElement())))
				return true;
		}
		return false;
	}

	@Override
	public Object get(Object key) {
		return request.getAttribute(key.toString());
	}

	@Override
	public Object put(String key, Object value) {
		request.setAttribute(key.toString(), value);
		return value;
	}

	@Override
	public Object remove(Object key) {
		Object o = request.getAttribute(key.toString());
		if (o == null)
			return null;
		request.removeAttribute(key.toString());
		return o;
	}

	@Override
	public void putAll(Map<? extends String, ?> m) {
		if (m == null)
			return;
		m.forEach(new BiConsumer<String, Object>() {
			@Override
			public void accept(String name, Object value) {
				request.setAttribute(name, value);
			}
		});
	}

	@Override
	public void clear() {
		Enumeration<String> names = request.getAttributeNames();
		while (names.hasMoreElements())
			remove(names.nextElement());
	}

	@Override
	public Set<String> keySet() {
		return null;
	}

	private Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		Enumeration<String> names = request.getAttributeNames();
		while (names.hasMoreElements()) {
			String key = names.nextElement();
			map.put(key, request.getAttribute(key));
		}
		return map;
	}

	@Override
	public Collection<Object> values() {
		return toMap().values();
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {
		return toMap().entrySet();
	}
}
