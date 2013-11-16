package jetbrains.buildServer.server.rest.request;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import jetbrains.buildServer.ServiceLocator;
import jetbrains.buildServer.server.rest.ApiUrlBuilder;
import jetbrains.buildServer.server.rest.data.PagedSearchResult;
import jetbrains.buildServer.server.rest.data.investigations.InvestigationFinder;
import jetbrains.buildServer.server.rest.data.investigations.InvestigationWrapper;
import jetbrains.buildServer.server.rest.model.PagerData;
import jetbrains.buildServer.server.rest.model.buildType.Investigations;
import org.jetbrains.annotations.NotNull;

/**
 *  Experimental, the requests and results returned will change in future versions!
 * @author Yegor.Yarko
 *         Date: 09.11.13
 */
@Path(InvestigationRequest.API_SUB_URL)
public class InvestigationRequest {
  @Context @NotNull private ServiceLocator myServiceLocator;
  @Context @NotNull private InvestigationFinder myInvestigationFinder;
  @Context @NotNull private ApiUrlBuilder myApiUrlBuilder;

  public static final String API_SUB_URL = Constants.API_URL + "/investigations";

  public static String getHref() {
    return API_SUB_URL;
  }

  /*
  public static String getInvestigationHref(@NotNull final InvestigationWrapper investigation) {
    return API_SUB_URL + "?locator=" + VcsRootFinder.VCS_ROOT_DIMENSION + ":(id:" + vcsRoot.getExternalId() + ")";
  }
  */

  /**
   * Experimental, the requests and results returned will change in future versions!
   * @param locatorText
   * @param uriInfo
   * @param request
   * @return
   */
  @GET
  @Produces({"application/xml", "application/json"})
  public Investigations getInvestigations(@QueryParam("locator") String locatorText, @Context UriInfo uriInfo, @Context HttpServletRequest request) {
    final PagedSearchResult<InvestigationWrapper> result = myInvestigationFinder.getItems(locatorText);

    return new Investigations(result.myEntries,
                              new PagerData(uriInfo.getRequestUriBuilder(), request.getContextPath(), result.myStart,
                                            result.myCount, result.myEntries.size(),
                                            locatorText,
                                            "locator"), myServiceLocator, myApiUrlBuilder);
  }

  /*
  @GET
  @Path("/{investigationLocator}")
  @Produces({"application/xml", "application/json"})
  public Investigation serveInstance(@PathParam("investigationLocator") String locatorText) {
    return new Investigation(myInvestigationFinder.getItem(locatorText), myDataProvider, myApiUrlBuilder);
  }

  @GET
  @Path("/{investigationLocator}/{field}")
  @Produces("text/plain")
  public String serveInstanceField(@PathParam("investigationLocator") String locatorText, @PathParam("field") String fieldName) {
    InvestigationWrapper investigation = myInvestigationFinder.getItem(locatorText);
    return Investigation.getFieldValue(investigation, fieldName, myDataProvider);
  }

  @PUT
  @Path("/{investigationLocator}/{field}")
  @Consumes("text/plain")
  @Produces("text/plain")
  public String setInstanceField(@PathParam("investigationLocator") String locatorText,
                                 @PathParam("field") String fieldName, String newValue) {
    InvestigationWrapper investigation = myInvestigationFinder.getItem(locatorText);
    Investigation.setFieldValue(investigation, fieldName, newValue, myDataProvider);
    investigation.persist();
    return Investigation.getFieldValue(investigation, fieldName, myDataProvider);
  }
  */
}