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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;

import com.qwazr.utils.LockUtils;

import javax.servlet.ServletContext;

import javax.servlet.http.HttpSessionContext;

public class WebappHttpSessionImpl implements WebappHttpSession {

	private ApplicationContext context;
	private final String id;
	private final long creationTime;
	private HashMap<String, Object> attributes;
	private final LockUtils.ReadWriteLock attrRwl = new LockUtils.ReadWriteLock();

	WebappHttpSessionImpl(ApplicationContext context, String id) {
		this.context = context;
		this.id = id;
		this.creationTime = System.currentTimeMillis();
		this.attributes = null;
	}

	@Override
	public long getCreationTime() {
		return creationTime;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public long getLastAccessedTime() {
		return 0;
	}

	@Override
	public ServletContext getServletContext() {
		return null;
	}

	@Override
	public void setMaxInactiveInterval(int interval) {

	}

	@Override
	public int getMaxInactiveInterval() {
		return 0;
	}

	@Override
	@Deprecated
	public HttpSessionContext getSessionContext() {
		return null;
	}

	@Override
	public Object getAttribute(String name) {
		if (name == null)
			return null;
		attrRwl.r.lock();
		try {
			if (attributes == null)
				return null;
			return attributes.get(name.intern());
		} finally {
			attrRwl.r.unlock();
		}
	}

	@Override
	@Deprecated
	public Object getValue(String name) {
		return getAttribute(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return null;
	}

	@Override
	@Deprecated
	public String[] getValueNames() {
		return new String[0];
	}

	@Override
	public void setAttribute(String name, Object value) {
		if (name == null)
			return;
		attrRwl.w.lock();
		try {
			if (attributes == null) {
				if (value == null)
					return;
				attributes = new LinkedHashMap<String, Object>();
			}
			attributes.put(name.intern(), value);
		} finally {
			attrRwl.w.unlock();
		}
	}

	@Override
	@Deprecated
	public void putValue(String name, Object value) {
		setAttribute(name, value);
	}

	@Override
	public boolean isAttribute(String name) {
		if (name == null)
			return false;
		attrRwl.r.lock();
		try {
			if (attributes == null)
				return false;
			return attributes.containsKey(name.intern());
		} finally {
			attrRwl.r.unlock();
		}
	}

	@Override
	public void removeAttribute(String name) {
		if (name == null)
			return;
		attrRwl.w.lock();
		try {
			if (attributes == null)
				return;
			attributes.remove(name.intern());
		} finally {
			attrRwl.w.unlock();
		}
	}

	@Override
	@Deprecated
	public void removeValue(String name) {
		removeAttribute(name);
	}

	@Override
	public void invalidate() {
		attrRwl.w.lock();
		try {
			if (attributes != null)
				attributes.clear();
		} finally {
			attrRwl.w.unlock();
		}
		context.invalidateSession(id);
	}

	@Override
	public boolean isNew() {
		return false;
	}

}
