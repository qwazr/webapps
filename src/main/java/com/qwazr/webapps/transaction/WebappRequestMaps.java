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

public abstract class WebappRequestMaps<T> implements Map<String, T> {

	protected abstract Enumeration<String> getNames();

	protected abstract T getValue(String name);

	@Override
	public int size() {
		int i = 0;
		Enumeration<String> names = getNames();
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
		return getValue(key.toString()) != null;
	}

	@Override
	public boolean containsValue(Object value) {
		Enumeration<String> names = getNames();
		while (names.hasMoreElements()) {
			if (value.equals(getValue(names.nextElement())))
				return true;
		}
		return false;
	}

	@Override
	public T get(Object key) {
		return getValue(key.toString());
	}

	@Override
	public T put(String key, T value) {
		throw new RuntimeException("This map is read only");
	}

	@Override
	public T remove(Object key) {
		throw new RuntimeException("This map is read only");
	}

	@Override
	public void putAll(Map<? extends String, ? extends T> m) {
		throw new RuntimeException("This map is read only");
	}

	@Override
	public void clear() {
		throw new RuntimeException("This map is read only");
	}

	@Override
	public Set<String> keySet() {
		final LinkedHashSet<String> keys = new LinkedHashSet<String>();
		Enumeration<String> names = getNames();
		while (names.hasMoreElements())
			keys.add(names.nextElement());
		return keys;
	}

	private Map<String, T> toMap() {
		final Map<String, T> map = new LinkedHashMap<String, T>();
		final Enumeration<String> names = getNames();
		while (names.hasMoreElements()) {
			final String key = names.nextElement();
			map.put(key, getValue(key));
		}
		return map;
	}

	@Override
	public Collection<T> values() {
		return toMap().values();
	}

	@Override
	public Set<Entry<String, T>> entrySet() {
		return toMap().entrySet();
	}

	public static class WebappAttributes extends WebappRequestMaps<Object> {

		private final HttpServletRequest request;

		WebappAttributes(HttpServletRequest request) {
			this.request = request;
		}

		@Override
		protected Enumeration<String> getNames() {
			return request.getAttributeNames();
		}

		@Override
		protected Object getValue(String name) {
			return request.getAttribute(name);
		}
	}

	public static class WebappHeaders extends WebappRequestMaps<String> {

		private final HttpServletRequest request;

		WebappHeaders(HttpServletRequest request) {
			this.request = request;
		}

		@Override
		protected Enumeration<String> getNames() {
			return request.getHeaderNames();
		}

		@Override
		protected String getValue(String name) {
			return request.getHeader(name);
		}

		public long getDate(String name) {
			return request.getDateHeader(name);
		}

		public int getInt(String name) {
			return request.getIntHeader(name);
		}
	}
}
