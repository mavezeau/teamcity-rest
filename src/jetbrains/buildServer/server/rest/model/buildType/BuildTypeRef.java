/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
import jetbrains.buildServer.server.rest.ApiUrlBuilder;
import jetbrains.buildServer.server.rest.data.DataProvider;
import jetbrains.buildServer.server.rest.util.BuildTypeOrTemplate;
import jetbrains.buildServer.serverSide.BuildTypeTemplate;
import jetbrains.buildServer.serverSide.SBuildType;
import org.jetbrains.annotations.NotNull;

/**
 * User: Yegor Yarko
 * Date: 29.03.2009
 */
@XmlRootElement(name = "buildType")
@XmlType(propOrder = {"webUrl", "projectId", "projectName", "href", "name", "id"})
public class BuildTypeRef {
  protected BuildTypeOrTemplate myBuildType;
  private DataProvider myDataProvider;
  private ApiUrlBuilder myApiUrlBuilder;

  public BuildTypeRef() {
  }

  public BuildTypeRef(SBuildType buildType, @NotNull final DataProvider dataProvider, final ApiUrlBuilder apiUrlBuilder) {
    myBuildType = new BuildTypeOrTemplate(buildType);
    myDataProvider = dataProvider;
    myApiUrlBuilder = apiUrlBuilder;
  }

  public BuildTypeRef(BuildTypeTemplate buildType, @NotNull final DataProvider dataProvider, final ApiUrlBuilder apiUrlBuilder) {
    myBuildType = new BuildTypeOrTemplate(buildType);
    myDataProvider = dataProvider;
    myApiUrlBuilder = apiUrlBuilder;
  }

  @XmlAttribute
  public String getId() {
    return myBuildType.getId();
  }

  @XmlAttribute
  public String getName() {
    return myBuildType.getName();
  }

  @XmlAttribute
  public String getHref() {
    return myApiUrlBuilder.getHref(myBuildType);
  }

  @XmlAttribute
  public String getProjectId() {
    return myBuildType.getProject().getProjectId();
  }

  @XmlAttribute
  public String getProjectName() {
    return myBuildType.getProject().getName();
  }

  @XmlAttribute
  public String getWebUrl() {
    return myBuildType.isBuildType() ? myDataProvider.getBuildTypeUrl(myBuildType.getBuildType()) : null;
  }
}
