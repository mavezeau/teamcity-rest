package jetbrains.buildServer.server.rest.request;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import jetbrains.buildServer.ServiceLocator;
import jetbrains.buildServer.server.rest.ApiUrlBuilder;
import jetbrains.buildServer.server.rest.data.PagedSearchResult;
import jetbrains.buildServer.server.rest.data.problem.TestFinder;
import jetbrains.buildServer.server.rest.model.Fields;
import jetbrains.buildServer.server.rest.model.PagerData;
import jetbrains.buildServer.server.rest.model.problem.Test;
import jetbrains.buildServer.server.rest.model.problem.Tests;
import jetbrains.buildServer.server.rest.util.BeanContext;
import jetbrains.buildServer.server.rest.util.BeanFactory;
import jetbrains.buildServer.serverSide.STest;
import org.jetbrains.annotations.NotNull;

/**
 * @author Yegor.Yarko
 *         Date: 16.11.13
 */
@Path(TestRequest.API_SUB_URL)
public class TestRequest {
  @Context @NotNull private ServiceLocator myServiceLocator;
  @Context @NotNull private TestFinder myTestFinder;
  @Context @NotNull private ApiUrlBuilder myApiUrlBuilder;
  @Context @NotNull private BeanFactory myBeanFactory;

  public static final String API_SUB_URL = Constants.API_URL + "/tests";

  public static String getHref() {
    return API_SUB_URL;
  }

  public static String getHref(final @NotNull STest test) {
    return API_SUB_URL + "/" + TestFinder.getTestLocator(test);
  }

  /**
   * Experimental, the requests and results returned will change in future versions!
   *
   * @param locatorText
   * @param uriInfo
   * @param request
   * @return
   */
  @GET
  @Produces({"application/xml", "application/json"})
  public Tests getTests(@QueryParam("locator") String locatorText, @QueryParam("fields") String fields, @Context UriInfo uriInfo, @Context HttpServletRequest request) {
    final PagedSearchResult<STest> result = myTestFinder.getItems(locatorText);

    return new Tests(result.myEntries,
                     new PagerData(uriInfo.getRequestUriBuilder(), request.getContextPath(), result.myStart,
                                   result.myCount, result.myEntries.size(),
                                   locatorText,
                                   "locator"),
                     new BeanContext(myBeanFactory, myServiceLocator, myApiUrlBuilder),
                     new Fields(fields)
    );
  }
  
  @GET
  @Path("/{testLocator}")
  @Produces({"application/xml", "application/json"})
  public Test serveInstance(@PathParam("testLocator") String locatorText, @QueryParam("fields") String fields) {
    return new Test(myTestFinder.getItem(locatorText), new BeanContext(myBeanFactory, myServiceLocator, myApiUrlBuilder), new Fields(fields, Fields.ALL_FIELDS));
  }
}