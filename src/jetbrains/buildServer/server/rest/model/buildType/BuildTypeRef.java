/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package jetbrains.buildServer.server.rest.model.buildType;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import jetbrains.buildServer.ServiceLocator;
import jetbrains.buildServer.server.rest.APIController;
import jetbrains.buildServer.server.rest.data.BuildTypeFinder;
import jetbrains.buildServer.server.rest.errors.BadRequestException;
import jetbrains.buildServer.server.rest.util.BeanContext;
import jetbrains.buildServer.server.rest.util.BuildTypeOrTemplate;
import jetbrains.buildServer.serverSide.BuildTypeTemplate;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.WebLinks;
import jetbrains.buildServer.serverSide.identifiers.BuildTypeIdentifiersManager;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: Yegor Yarko
 * Date: 29.03.2009
 */
@XmlRootElement(name = "buildType-ref")
@XmlType(name = "buildType-ref", propOrder = {"id", "internalId", "name", "href", "projectName", "projectId", "projectInternalId", "webUrl"})
public class BuildTypeRef {
  @Nullable protected BuildTypeOrTemplate myBuildType;
  @NotNull private BeanContext myBeanContext;

  /**
   * @return External id of the build configuration
   */
  @XmlAttribute public String id;
  @XmlAttribute public String internalId;
  /**
   * This is used only when posting a link to a build type.
   */
  @XmlAttribute public String locator;

  public BuildTypeRef() {
  }

  public BuildTypeRef(@NotNull final SBuildType buildType, @NotNull final BeanContext beanContext) {
    myBuildType = new BuildTypeOrTemplate(buildType);
    myBeanContext = beanContext;
    init(myBuildType);
  }

  public BuildTypeRef(@NotNull final BuildTypeTemplate buildType, @NotNull final BeanContext beanContext) {
    myBuildType = new BuildTypeOrTemplate(buildType);
    myBeanContext = beanContext;

    init(myBuildType);
  }

  private void init(@NotNull final BuildTypeOrTemplate buildType) {
    id = buildType.getId();
    internalId = TeamCityProperties.getBoolean(APIController.INCLUDE_INTERNAL_ID_PROPERTY_NAME) ? buildType.getInternalId() : null;
  }

  /**
   * Creates a reference on a missing build type which has only external id known.
   * @param externalId
   */
  public BuildTypeRef(@Nullable final String externalId, @Nullable final String internalId,@NotNull final BeanContext beanContext) {
    myBuildType = null;
    id = externalId;
    this.internalId = (TeamCityProperties.getBoolean(APIController.INCLUDE_INTERNAL_ID_PROPERTY_NAME) || externalId == null) ? internalId : null;

    myBeanContext = beanContext;
  }

  @XmlAttribute
  public String getName() {
    return myBuildType == null ? null : myBuildType.getName();
  }

  @XmlAttribute
  public String getHref() {
    return myBuildType == null ? null : myBeanContext.getApiUrlBuilder().getHref(myBuildType);
  }

  @XmlAttribute
  public String getProjectId() {
    return myBuildType == null ? null : myBuildType.getProject().getExternalId();
  }

  @XmlAttribute
  public String getProjectInternalId() {
    return myBuildType == null ? null : (TeamCityProperties.getBoolean(APIController.INCLUDE_INTERNAL_ID_PROPERTY_NAME)
           ? myBuildType.getProject().getProjectId()
           : null);
  }

  @XmlAttribute
  public String getProjectName() {
    return myBuildType == null ? null : myBuildType.getProject().getName();
  }

  @XmlAttribute
  public String getWebUrl() {
    return myBuildType == null
           ? null
           : (myBuildType.isBuildType() ? myBeanContext.getSingletonService(WebLinks.class).getConfigurationHomePageUrl(myBuildType.getBuildType()) : null);
  }

  @Nullable
  public String getExternalIdFromPosted(@NotNull final ServiceLocator serviceLocator) {
    if (id != null) {
      if (internalId == null) {
        return id;
      }
      String externalByInternal = serviceLocator.getSingletonService(BuildTypeIdentifiersManager.class).internalToExternal(internalId);
      if (externalByInternal == null || id.equals(externalByInternal)) {
        return id;
      }
      throw new BadRequestException("Both external id '" + id + "' and internal id '" + internalId + "' attributes are present and they reference different build types.");
    }
    if (internalId != null) {
      return serviceLocator.getSingletonService(BuildTypeIdentifiersManager.class).internalToExternal(internalId);
    }
    if (locator != null){
      return serviceLocator.getSingletonService(BuildTypeFinder.class).getBuildType(null, locator).getExternalId();
    }
    throw new BadRequestException("Could not find build type by the data. Either 'id' or 'internalId' or 'locator' attributes should be specified.");
  }

  @NotNull
  public BuildTypeOrTemplate getBuildTypeFromPosted(@NotNull final BuildTypeFinder buildTypeFinder) {
    String locatorText = "";
    if (internalId != null) locatorText = "internalId:" + internalId;
    if (id != null) locatorText += (!locatorText.isEmpty() ? "," : "") + "id:" + id;
    if (locatorText.isEmpty()) {
      locatorText = locator;
    } else {
      if (locator != null) {
        throw new BadRequestException("Both 'locator' and 'id' or 'internalId' attributes are specified. Only one should be present.");
      }
    }
    if (StringUtil.isEmpty(locatorText)){
      throw new BadRequestException("No build type specified. Either 'id', 'internalId' or 'locator' attribute should be present.");
    }
    return buildTypeFinder.getBuildTypeOrTemplate(null, locatorText);
  }
}
