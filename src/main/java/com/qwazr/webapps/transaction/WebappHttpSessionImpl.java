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
package com.qwazr.webapps.transaction;

import java.util.HashMap;
import java.util.LinkedHashMap;

import com.qwazr.utils.LockUtils;

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

}
