/**
 * Copyright 2016 Emmanuel Keller / QWAZR
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
package com.qwazr.webapps.test;

import com.qwazr.utils.server.GenericServer;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;

import java.io.IOException;

public class TestIdentityProvider implements GenericServer.IdentityManagerProvider {

	@Override
	public IdentityManager getIdentityManager(String realm) throws IOException {
		return new TestIdentityManager();
	}

	public class TestIdentityManager implements IdentityManager {

		@Override
		public Account verify(Account account) {
			System.out.println(account);
			return null;
		}

		@Override
		public Account verify(String s, Credential credential) {
			System.out.println(s);
			return null;
		}

		@Override
		public Account verify(Credential credential) {
			System.out.println(credential);
			return null;
		}
	}
}
