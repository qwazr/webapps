/*
 * Copyright 2017-2018 Emmanuel Keller / QWAZR
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
 */
package com.qwazr.webapps;

import com.qwazr.library.LibraryServiceInterface;
import com.qwazr.server.GenericFactory;
import com.qwazr.server.ServerException;
import com.qwazr.utils.reflection.ConstructorParametersImpl;
import com.qwazr.utils.reflection.InstanceFactory;
import io.undertow.servlet.util.ImmediateInstanceHandle;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

class SmartFactory<T> implements GenericFactory<T> {

	private final ConstructorParametersImpl constructorParameters;
	private final Class<T> clazz;

	private SmartFactory(final ConstructorParametersImpl constructorParameters, final Class<T> clazz) {
		this.constructorParameters = constructorParameters;
		this.clazz = clazz;
	}

	@Override
	public ImmediateInstanceHandle<T> createInstance() throws InstantiationException {
		final T instance;
		try {
			final InstanceFactory<T> instanceFactory =
					Objects.requireNonNull(constructorParameters.findBestMatchingConstructor(clazz),
							() -> "No matching constructor found for class: " + clazz);
			instance = instanceFactory.newInstance();
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw ServerException.of(e);
		}
		return new ImmediateInstanceHandle<>(instance);
	}

	final static class WithLibrary<T> extends SmartFactory<T> {

		private final LibraryServiceInterface libraryService;

		private WithLibrary(final LibraryServiceInterface libraryService,
				final ConstructorParametersImpl constructorParameters, final Class<T> clazz) {
			super(constructorParameters, clazz);
			this.libraryService = libraryService;
		}

		@Override
		public ImmediateInstanceHandle<T> createInstance() throws InstantiationException {
			final ImmediateInstanceHandle<T> result = super.createInstance();
			libraryService.inject(result.getInstance());
			return result;
		}
	}

	static <T> SmartFactory<T> from(final LibraryServiceInterface libraryService,
			final ConstructorParametersImpl constructorParameters, final Class<T> clazz) {
		return libraryService == null ?
				new SmartFactory<>(constructorParameters, clazz) :
				new WithLibrary<>(libraryService, constructorParameters, clazz);
	}
}