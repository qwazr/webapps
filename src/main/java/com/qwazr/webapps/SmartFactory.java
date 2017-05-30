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
import com.qwazr.server.GenericFactory;
import com.qwazr.server.ServerException;
import com.qwazr.utils.reflection.ConstructorParameters;
import com.qwazr.utils.reflection.InstanceFactory;
import io.undertow.servlet.util.ImmediateInstanceHandle;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

class SmartFactory<T> implements GenericFactory<T> {

	private final ConstructorParameters constructorParameters;
	private final Class<T> clazz;

	private SmartFactory(final ConstructorParameters constructorParameters, final Class<T> clazz)
			throws NoSuchMethodException {
		this.constructorParameters = constructorParameters;
		this.clazz = clazz;
	}

	@Override
	public ImmediateInstanceHandle<T> createInstance() throws InstantiationException {
		final T instance;
		try {
			final InstanceFactory<T> instanceFactory = Objects.requireNonNull(
					constructorParameters.findBestMatchingConstructor(clazz),
					() -> "No matching constructor found for class: " + clazz);
			instance = instanceFactory.newInstance();
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new ServerException(e.getMessage(), e);
		}
		return new ImmediateInstanceHandle<>(instance);
	}

	final static class WithLibrary<T> extends SmartFactory<T> {

		private final LibraryManager libraryManager;

		private WithLibrary(final LibraryManager libraryManager, final ConstructorParameters constructorParameters,
				final Class<T> clazz) throws NoSuchMethodException {
			super(constructorParameters, clazz);
			this.libraryManager = libraryManager;
		}

		@Override
		public ImmediateInstanceHandle<T> createInstance() throws InstantiationException {
			final ImmediateInstanceHandle<T> result = super.createInstance();
			libraryManager.inject(result.getInstance());
			return result;
		}
	}

	static <T> SmartFactory<T> from(final LibraryManager libraryManager,
			final ConstructorParameters constructorParameters, final Class<T> clazz) throws NoSuchMethodException {
		return libraryManager == null ? new SmartFactory<>(constructorParameters, clazz) : new WithLibrary<>(
				libraryManager, constructorParameters, clazz);
	}
}