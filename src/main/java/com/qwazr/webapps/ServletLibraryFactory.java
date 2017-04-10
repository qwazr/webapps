/**
 * Copyright 2017 Emmanuel Keller / QWAZR
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
package com.qwazr.webapps;

import com.qwazr.library.LibraryManager;
import com.qwazr.server.ServerException;
import com.qwazr.server.ServletFactory;
import com.qwazr.utils.ReflectiveUtils;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.servlet.util.ImmediateInstanceHandle;

import javax.servlet.Servlet;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Objects;

class ServletLibraryFactory<T extends Servlet> implements ServletFactory<T> {

	private final LibraryManager libraryManager;
	private final ReflectiveUtils.InstanceFactory<T> instanceFactory;

	ServletLibraryFactory(final LibraryManager libraryManager, final Map<Class<?>, ?> parameterMap,
			final Class<T> clazz) throws NoSuchMethodException {
		instanceFactory = Objects.requireNonNull(ReflectiveUtils.findBestMatchingConstructor(parameterMap, clazz),
				() -> "No matching constructor found for class: " + clazz);
		this.libraryManager = libraryManager;
	}

	@Override
	public InstanceHandle<T> createInstance() throws InstantiationException {
		final T instance;
		try {
			instance = instanceFactory.newInstance();
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new ServerException(e.getMessage(), e);
		}
		if (libraryManager != null)
			libraryManager.inject(instance);
		return new ImmediateInstanceHandle<>(instance);
	}

}