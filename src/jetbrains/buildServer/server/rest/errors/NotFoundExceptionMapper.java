/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.server.rest.errors;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import jetbrains.buildServer.server.rest.jersey.ExceptionMapperUtil;

/**
 * User: Yegor Yarko
 * Date: 30.03.2009
 */
@Provider
public class NotFoundExceptionMapper extends ExceptionMapperUtil implements ExceptionMapper<NotFoundException> {

  public Response toResponse(NotFoundException exception) {
    return reportError(Response.Status.NOT_FOUND, exception, "Could not find the entity requested. Check the reference is correct and the user has permissions to access the entity.");
  }
}