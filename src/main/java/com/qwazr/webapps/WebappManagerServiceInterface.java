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
package com.qwazr.webapps;

import com.qwazr.utils.server.ServiceInterface;
import com.qwazr.utils.server.ServiceName;
import com.qwazr.webapps.transaction.WebappDefinition;
import com.qwazr.webapps.transaction.WebappManager;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Set;

@RolesAllowed(WebappManager.SERVICE_NAME_WEBAPPS)
@Path("/webapps")
@ServiceName(WebappManager.SERVICE_NAME_WEBAPPS)
public interface WebappManagerServiceInterface extends ServiceInterface {

	@GET
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	Set<String> list();

	@GET
	@Path("/{webapp-name}")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	WebappDefinition get(@PathParam("webapp-name") String webappName);
}
