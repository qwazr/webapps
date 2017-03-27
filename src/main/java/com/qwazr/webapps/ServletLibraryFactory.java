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
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.servlet.util.ConstructorInstanceFactory;

import javax.servlet.Servlet;

class ServletLibraryFactory<T extends Servlet> extends ConstructorInstanceFactory<T> {

	private final LibraryManager libraryManager;

	ServletLibraryFactory(final LibraryManager libraryManager, final Class<T> clazz) throws NoSuchMethodException {
		super(clazz.getDeclaredConstructor());
		this.libraryManager = libraryManager;
	}

	@Override
	public InstanceHandle<T> createInstance() throws InstantiationException {
		final InstanceHandle<T> instanceHandle = super.createInstance();
		if (libraryManager != null)
			libraryManager.inject(instanceHandle.getInstance());
		return instanceHandle;
	}
}