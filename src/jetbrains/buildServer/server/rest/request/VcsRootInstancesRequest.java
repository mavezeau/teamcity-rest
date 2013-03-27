/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package jetbrains.buildServer.server.rest.request;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import jetbrains.buildServer.server.rest.ApiUrlBuilder;
import jetbrains.buildServer.server.rest.data.DataProvider;
import jetbrains.buildServer.server.rest.data.Locator;
import jetbrains.buildServer.server.rest.data.PagedSearchResult;
import jetbrains.buildServer.server.rest.data.VcsRootFinder;
import jetbrains.buildServer.server.rest.model.PagerData;
import jetbrains.buildServer.server.rest.model.Properties;
import jetbrains.buildServer.server.rest.model.buildType.VcsRootInstances;
import jetbrains.buildServer.server.rest.model.change.VcsRootInstance;
import org.jetbrains.annotations.NotNull;

/* todo: investigate logging issues:
    - disable initialization lines into stdout
    - too long number passed as finish for builds produces 404
*/

@Path(VcsRootInstancesRequest.API_VCS_ROOT_INSTANCES_URL)
public class VcsRootInstancesRequest {
  @Context @NotNull private DataProvider myDataProvider;
  @Context @NotNull private VcsRootFinder myVcsRootFinder;
  @Context @NotNull private ApiUrlBuilder myApiUrlBuilder;

  public static final String API_VCS_ROOT_INSTANCES_URL = Constants.API_URL + "/vcs-root-instances";

  public static String getVcsRootInstanceHref(final jetbrains.buildServer.vcs.VcsRootInstance vcsRootInstance) {
    return API_VCS_ROOT_INSTANCES_URL + "/id:" + vcsRootInstance.getId();
  }

  @GET
  @Produces({"application/xml", "application/json"})
  public VcsRootInstances serveInstances(@QueryParam("locator") String vcsRootInstanceLocator,
                                         @Context UriInfo uriInfo,
                                         @Context HttpServletRequest request) {
    final PagedSearchResult<jetbrains.buildServer.vcs.VcsRootInstance> vcsRootInstances =
      myVcsRootFinder.getVcsRootInstances(vcsRootInstanceLocator != null ? new Locator(vcsRootInstanceLocator) : null);

    return new VcsRootInstances(vcsRootInstances.myEntries,
                                new PagerData(uriInfo.getRequestUriBuilder(), request.getContextPath(), vcsRootInstances.myStart,
                                              vcsRootInstances.myCount, vcsRootInstances.myEntries.size(),
                                              vcsRootInstanceLocator,
                                              "locator"),
                                myApiUrlBuilder);
  }

  @GET
  @Path("/{vcsRootInstanceLocator}")
  @Produces({"application/xml", "application/json"})
  public VcsRootInstance serveInstance(@PathParam("vcsRootInstanceLocator") String vcsRootInstanceLocator) {
    return new VcsRootInstance(myVcsRootFinder.getVcsRootInstance(vcsRootInstanceLocator), myDataProvider, myApiUrlBuilder);
  }

  @GET
  @Path("/{vcsRootInstanceLocator}/properties")
  @Produces({"application/xml", "application/json"})
  public Properties serveRootInstanceProperties(@PathParam("vcsRootInstanceLocator") String vcsRootInstanceLocator) {
    return new Properties(myVcsRootFinder.getVcsRootInstance(vcsRootInstanceLocator).getProperties());
  }


  @GET
  @Path("/{vcsRootInstanceLocator}/{field}")
  @Produces("text/plain")
  public String serveInstanceField(@PathParam("vcsRootInstanceLocator") String vcsRootInstanceLocator,
                                   @PathParam("field") String fieldName) {
    final jetbrains.buildServer.vcs.VcsRootInstance rootInstance = myVcsRootFinder.getVcsRootInstance(vcsRootInstanceLocator);
    return VcsRootInstance.getFieldValue(rootInstance, fieldName, myDataProvider);
  }

  @PUT
  @Path("/{vcsRootInstanceLocator}/{field}")
  @Consumes("text/plain")
  public void setInstanceField(@PathParam("vcsRootInstanceLocator") String vcsRootInstanceLocator,
                               @PathParam("field") String fieldName, String newValue) {
    final jetbrains.buildServer.vcs.VcsRootInstance rootInstance = myVcsRootFinder.getVcsRootInstance(vcsRootInstanceLocator);
    VcsRootInstance.setFieldValue(rootInstance, fieldName, newValue, myDataProvider);
    myDataProvider.getVcsManager().persistVcsRoots();
  }
}