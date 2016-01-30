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
package com.qwazr.webapps.transaction;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import java.util.*;

public class WebappHttpSessionImpl implements WebappHttpSession {

	private final HttpSession session;
	private final AttributesMap attributesMap;

	WebappHttpSessionImpl(HttpSession session) {
		this.session = session;
		this.attributesMap = new AttributesMap();
	}

	@Override
	public long getCreationTime() {
		return session.getCreationTime();
	}

	@Override
	public String getId() {
		return session.getId();
	}

	@Override
	public long getLastAccessedTime() {
		return session.getLastAccessedTime();
	}

	@Override
	public ServletContext getServletContext() {
		return null;
	}

	@Override
	public void setMaxInactiveInterval(int interval) {
		session.setMaxInactiveInterval(interval);
	}

	@Override
	public int getMaxInactiveInterval() {
		return session.getMaxInactiveInterval();
	}

	@Override
	@Deprecated
	public HttpSessionContext getSessionContext() {
		return null;
	}

	@Override
	public Object getAttribute(String name) {
		return session.getAttribute(name);
	}

	@Override
	@Deprecated
	public Object getValue(String name) {
		return session.getValue(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return session.getAttributeNames();
	}

	@Override
	@Deprecated
	public String[] getValueNames() {
		return session.getValueNames();
	}

	@Override
	public void setAttribute(String name, Object value) {
		session.setAttribute(name, value);
	}

	@Override
	@Deprecated
	public void putValue(String name, Object value) {
		session.putValue(name, value);
	}

	@Override
	public boolean isAttribute(String name) {
		return session.getAttribute(name) != null;
	}

	@Override
	public void removeAttribute(String name) {
		session.removeAttribute(name);
	}

	@Override
	@Deprecated
	public void removeValue(String name) {
		session.removeValue(name);
	}

	@Override
	public void invalidate() {
		session.invalidate();
	}

	@Override
	public boolean isNew() {
		return session.isNew();
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributesMap;
	}

	private class AttributesMap implements Map<String, Object> {
		@Override
		public int size() {
			int i = 0;
			Enumeration<String> e = session.getAttributeNames();
			if (e == null)
				return 0;
			while (e.hasMoreElements()) {
				e.nextElement();
				i++;
			}
			return i;
		}

		@Override
		public boolean isEmpty() {
			Enumeration<String> e = session.getAttributeNames();
			if (e == null)
				return true;
			return !e.hasMoreElements();
		}

		@Override
		public boolean containsKey(Object key) {
			if (key == null)
				return false;
			return isAttribute(key.toString());
		}

		@Override
		public boolean containsValue(Object value) {
			if (value == null)
				return false;
			Enumeration<String> e = session.getAttributeNames();
			if (e == null)
				return false;
			while (e.hasMoreElements())
				if (value.equals(e.nextElement()))
					return true;
			return false;
		}

		@Override
		public Object get(Object key) {
			if (key == null)
				return null;
			return session.getAttribute(key.toString());
		}

		@Override
		public Object put(String key, Object value) {
			Object old = session.getAttribute(key);
			session.setAttribute(key, value);
			return old;
		}

		@Override
		public Object remove(Object key) {
			if (key == null)
				return null;
			String attr = key.toString();
			Object old = session.getAttribute(attr);
			session.removeAttribute(attr);
			return old;
		}

		@Override
		public void putAll(Map<? extends String, ?> m) {
			if (m == null)
				return;
			m.forEach((key, value) -> put(key, value));
		}

		@Override
		public void clear() {
			Enumeration<String> e = session.getAttributeNames();
			if (e == null)
				return;
			while (e.hasMoreElements())
				removeAttribute(e.nextElement());
		}

		@Override
		public Set<String> keySet() {
			LinkedHashSet<String> set = new LinkedHashSet<String>();
			Enumeration<String> e = session.getAttributeNames();
			if (e != null)
				while (e.hasMoreElements())
					set.add(e.nextElement());
			return set;
		}

		@Override
		public Collection<Object> values() {
			ArrayList<Object> values = new ArrayList<>();
			Enumeration<String> e = session.getAttributeNames();
			if (e != null)
				while (e.hasMoreElements())
					values.add(session.getAttribute(e.nextElement()));
			return values;
		}

		@Override
		public Set<Entry<String, Object>> entrySet() {
			LinkedHashMap<String, Object> map = new LinkedHashMap();
			Enumeration<String> e = session.getAttributeNames();
			if (e != null) {
				while (e.hasMoreElements()) {
					String key = e.nextElement();
					map.put(key, session.getAttribute(key));
				}
			}
			return map.entrySet();
		}
	}
}
