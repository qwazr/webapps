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

	protected abstract void setValue(String name, T value);

	protected abstract T getValue(String name);

	protected abstract void removeValue(String name);

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
		return get(key.toString()) != null;
	}

	@Override
	public boolean containsValue(Object value) {
		Enumeration<String> names = getNames();
		while (names.hasMoreElements()) {
			if (value.equals(get(names.nextElement())))
				return true;
		}
		return false;
	}

	@Override
	public T get(Object key) {
		if (key == null)
			return null;
		return getValue(key.toString());
	}

	@Override
	public T put(String key, Object value) {
		if (key == null)
			return null;
		if (value == null)
			return remove(key);
		T oldValue = getValue(key);
		setValue(key, (T) value);
		return oldValue;
	}

	@Override
	public T remove(Object key) {
		if (key == null)
			return null;
		T oldValue = get(key);
		removeValue(key.toString());
		return oldValue;
	}

	@Override
	public void putAll(Map<? extends String, ? extends T> m) {
		if (m == null)
			return;
		for (Map.Entry<? extends String, ? extends T> entry : m.entrySet())
			put(entry.getKey(), entry.getValue());
	}

	@Override
	public void clear() {
		final Enumeration<String> names = getNames();
		while (names.hasMoreElements())
			removeValue(names.nextElement());
	}

	private Map<String, T> toMap() {
		final Map<String, T> map = new LinkedHashMap<String, T>();
		final Enumeration<String> names = getNames();
		while (names.hasMoreElements()) {
			final String key = names.nextElement();
			map.put(key, get(key));
		}
		return map;
	}

	@Override
	public Set<String> keySet() {
		return toMap().keySet();
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
		protected void setValue(String name, Object value) {
			request.setAttribute(name, value);
		}

		@Override
		protected Object getValue(String name) {
			return request.getAttribute(name);
		}

		@Override
		protected void removeValue(String name) {
			request.removeAttribute(name);
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

		@Override
		protected void setValue(String name, String value) {
			throw new RuntimeException("This map is read only");
		}

		@Override
		protected void removeValue(String name) {
			throw new RuntimeException("This map is read only");
		}

		public long getDate(String name) {
			return request.getDateHeader(name);
		}

		public int getInt(String name) {
			return request.getIntHeader(name);
		}
	}
}
