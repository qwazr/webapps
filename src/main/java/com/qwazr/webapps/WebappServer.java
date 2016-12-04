/**
 * Copyright 2015-2016 Emmanuel Keller / QWAZR
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

import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.utils.server.GenericServer;
import com.qwazr.utils.server.ServerBuilder;
import com.qwazr.utils.server.ServerConfiguration;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

public class WebappServer extends GenericServer {

	private WebappServer(final ServerConfiguration serverConfiguration) throws IOException {
		super(serverConfiguration);
	}

	@Override
	protected void build(final ExecutorService executorService, final ServerBuilder builder,
			final ServerConfiguration configuration) throws IOException {
		ClusterManager.load(builder, configuration);
		WebappManager.load(builder, configuration);
	}

	public static void main(final String... args) throws Exception {
		new WebappServer(new ServerConfiguration(args)).start(true);
	}

}