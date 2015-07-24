/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.intellij.openapi.diagnostic.Logger;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import jetbrains.buildServer.ServiceLocator;
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptorFactory;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.server.rest.APIController;
import jetbrains.buildServer.server.rest.data.*;
import jetbrains.buildServer.server.rest.errors.BadRequestException;
import jetbrains.buildServer.server.rest.model.Fields;
import jetbrains.buildServer.server.rest.model.PagerData;
import jetbrains.buildServer.server.rest.model.Properties;
import jetbrains.buildServer.server.rest.model.Property;
import jetbrains.buildServer.server.rest.model.build.Builds;
import jetbrains.buildServer.server.rest.model.project.Project;
import jetbrains.buildServer.server.rest.request.BuildTypeRequest;
import jetbrains.buildServer.server.rest.request.InvestigationRequest;
import jetbrains.buildServer.server.rest.util.BeanContext;
import jetbrains.buildServer.server.rest.util.BuildTypeOrTemplate;
import jetbrains.buildServer.server.rest.util.ValueWithDefault;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.artifacts.SArtifactDependency;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.identifiers.BuildTypeIdentifiersManager;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.Converter;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: Yegor Yarko
 * Date: 29.03.2009
 */
@XmlRootElement(name = "buildType")
@XmlType(name = "buildType", propOrder = { "id", "internalId", "name", "templateFlag", "paused", "uuid", "description", "projectName", "projectId", "projectInternalId", "href", "webUrl",
  "project", "template", "vcsRootEntries", "settings", "parameters", "steps", "features", "triggers", "snapshotDependencies",
  "artifactDependencies", "agentRequirements", "builds", "investigations"})
public class BuildType {
  private static final Logger LOG = Logger.getInstance(BuildType.class.getName());

  @Nullable
  protected BuildTypeOrTemplate myBuildType;
  @NotNull private String myExternalId;
  @Nullable private String myInternalId;

  private Fields myFields = Fields.LONG;
  @NotNull private BeanContext myBeanContext;

  public BuildType() {
  }

  public BuildType(@NotNull final BuildTypeOrTemplate buildType, @NotNull final Fields fields, @NotNull final BeanContext beanContext) {
    myBuildType = buildType;
    myExternalId = buildType.getId();
    myInternalId = buildType.getInternalId();
    myFields = fields;
    myBeanContext = beanContext;
  }

  public BuildType(@NotNull final String externalId, @Nullable final String internalId, @NotNull final Fields fields, @NotNull final BeanContext beanContext) {
    myBuildType = null;
    myExternalId = externalId;
    myInternalId = internalId;
    myFields = fields;
    myBeanContext = beanContext;
  }

  /**
   * @return External id of the build configuration
   */
  @XmlAttribute
  public String getId() {
    return myBuildType == null ? myExternalId : ValueWithDefault.decideDefault(myFields.isIncluded("id", true), myBuildType.getId());
  }

  @XmlAttribute
  public String getInternalId() {
    final boolean includeProperty = TeamCityProperties.getBoolean(APIController.INCLUDE_INTERNAL_ID_PROPERTY_NAME);
    return myBuildType == null ? myInternalId : ValueWithDefault.decideDefault(myFields.isIncluded("internalId", includeProperty, includeProperty), myBuildType.getInternalId());
  }

  @XmlAttribute
  public String getName() {
    return myBuildType == null ? null : ValueWithDefault.decideDefault(myFields.isIncluded("name"), myBuildType.getName());
  }

  @XmlAttribute
  public String getProjectId() {
    return myBuildType == null ? null : ValueWithDefault.decideDefault(myFields.isIncluded("projectId"), myBuildType.getProject().getExternalId());
  }

  @XmlAttribute
  public String getProjectInternalId() {
    final boolean includeProperty = TeamCityProperties.getBoolean(APIController.INCLUDE_INTERNAL_ID_PROPERTY_NAME);
    return myBuildType == null
           ? null
           : ValueWithDefault.decideDefault(myFields.isIncluded("projectInternalId", includeProperty, includeProperty), myBuildType.getProject().getProjectId());
  }

  /**
   * @deprecated
   * @return
   */
  @XmlAttribute
  public String getProjectName() {
    return myBuildType == null ? null : ValueWithDefault.decideDefault(myFields.isIncluded("projectName"), myBuildType.getProject().getFullName());
  }

  @XmlAttribute
  public String getHref() {
    return myBuildType == null ? null : ValueWithDefault.decideDefault(myFields.isIncluded("href"), myBeanContext.getApiUrlBuilder().getHref(myBuildType));
  }

  @XmlAttribute
  public String getDescription() {
    if (myBuildType == null){
      return null;
    }
    final String description = myBuildType.getDescription();
    return ValueWithDefault.decideDefault(myFields.isIncluded("description"), StringUtil.isEmpty(description) ? null : description);
  }

  @XmlAttribute (name = "templateFlag")
  public Boolean getTemplateFlag() {
    return myBuildType == null ? null : ValueWithDefault.decideDefault(myFields.isIncluded("templateFlag"), !myBuildType.isBuildType());
  }

  @XmlAttribute
  public Boolean isPaused() {
    return myBuildType == null ? null : ValueWithDefault.decideDefault(myFields.isIncluded("paused"), myBuildType.isPaused());
  }

  @XmlAttribute
  public String getUuid() {
    if (myBuildType != null && myFields.isIncluded("uuid", false, false)) {
      //do not expose uuid to usual users as uuid can be considered secure information, e.g. see https://youtrack.jetbrains.com/issue/TW-38605
      if (myBeanContext.getSingletonService(PermissionChecker.class).isPermissionGranted(Permission.EDIT_PROJECT, myBuildType.getProject().getProjectId())) {
        return ((BuildTypeIdentityEx)myBuildType.getIdentity()).getEntityId().getConfigId();
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  @XmlAttribute
  public String getWebUrl() {
    //template has no user link
    return  myBuildType == null || myBuildType.getBuildType() == null
           ? null
           : ValueWithDefault
             .decideDefault(myFields.isIncluded("webUrl"), myBeanContext.getSingletonService(WebLinks.class).getConfigurationHomePageUrl(myBuildType.getBuildType()));
  }

  @XmlElement(name = "project")
  public Project getProject() {
    return ValueWithDefault.decideDefault(myFields.isIncluded("project", false), new ValueWithDefault.Value<Project>() {
      public Project get() {
        return myBuildType == null ? null : new Project(myBuildType.getProject(), myFields.getNestedField("project"), myBeanContext);
      }
    });
  }

  @XmlElement(name = "template")
  public BuildType getTemplate() {
    if (myBuildType == null || myBuildType.getBuildType() == null){
      return null;
    }
    final BuildTypeTemplate template = myBuildType.getBuildType().getTemplate();
    return template == null
           ? null
           : ValueWithDefault
             .decideDefault(myFields.isIncluded("template", false), new ValueWithDefault.Value<BuildType>() {
               public BuildType get() {
                 return new BuildType(new BuildTypeOrTemplate(template), myFields.getNestedField("template"), myBeanContext);
               }
             });
  }

  @XmlElement(name = "vcs-root-entries")
  public VcsRootEntries getVcsRootEntries() {
    return ValueWithDefault.decideIncludeByDefault(myFields.isIncluded("vcs-root-entries", false), new ValueWithDefault.Value<VcsRootEntries>() {
      public VcsRootEntries get() {
        return myBuildType == null ? null : new VcsRootEntries(myBuildType, myFields.getNestedField("vcs-root-entries"), myBeanContext);
      }
    });
  }

  /**
   * Link to builds of this build configuration. Is not present for templates.
   * @return
   */
  @XmlElement(name = "builds")
  public Builds getBuilds() {
    if (myBuildType == null || !myBuildType.isBuildType()) return null;
    if (!myFields.isIncluded("builds", false, true)){
      return null;
    }

    return ValueWithDefault.decideDefault(myFields.isIncluded("builds", false), new ValueWithDefault.Value<Builds>() {
      public Builds get() {
        String buildsHref;
        List<BuildPromotion> builds = null;
        final Fields buildsFields = myFields.getNestedField("builds");
        final String buildsLocator = buildsFields.getLocator();
        if (buildsLocator != null){
          builds = myBeanContext.getSingletonService(BuildFinder.class).getBuilds(myBuildType.getBuildType(), buildsLocator).myEntries;
          buildsHref = BuildTypeRequest.getBuildsHref(myBuildType.getBuildType(), buildsLocator);
        }else{
          buildsHref = BuildTypeRequest.getBuildsHref(myBuildType.getBuildType());
        }
        return Builds.createFromBuildPromotions(builds, new PagerData(buildsHref), buildsFields, myBeanContext);
      }
    });
  }

  @XmlElement
  public Properties getParameters() {
    return myBuildType == null ? null : ValueWithDefault
      .decideIncludeByDefault(myFields.isIncluded("parameters", false), new ValueWithDefault.Value<Properties>() {
        public Properties get() {
          return new Properties(myBuildType.get().getParametersCollection(), myBuildType.get().getOwnParametersCollection(), BuildTypeRequest.getParametersHref(myBuildType),
                                myFields.getNestedField("parameters", Fields.NONE, Fields.LONG), myBeanContext.getServiceLocator());
        }
      });
  }

  @XmlElement(name = "steps")
  public PropEntitiesStep getSteps() {
    return myBuildType == null ? null : ValueWithDefault.decideIncludeByDefault(myFields.isIncluded("steps", false), new ValueWithDefault.Value<PropEntitiesStep>() {
      public PropEntitiesStep get() {
        return new PropEntitiesStep(myBuildType.get(), myFields.getNestedField("steps", Fields.NONE, Fields.LONG));
      }
    });
  }

  @XmlElement(name = "features")
  public PropEntitiesFeature getFeatures() {
    return myBuildType == null ? null : ValueWithDefault.decideIncludeByDefault(myFields.isIncluded("features", false), new ValueWithDefault.Value<PropEntitiesFeature>() {
      public PropEntitiesFeature get() {
        return new PropEntitiesFeature(myBuildType.get(), myFields.getNestedField("features", Fields.NONE, Fields.LONG));
      }
    });
  }

  @XmlElement(name = "triggers")
  public PropEntitiesTrigger getTriggers() {
    return myBuildType == null ? null : ValueWithDefault.decideIncludeByDefault(myFields.isIncluded("triggers", false), new ValueWithDefault.Value<PropEntitiesTrigger>() {
      public PropEntitiesTrigger get() {
        return new PropEntitiesTrigger(myBuildType.get(), myFields.getNestedField("triggers", Fields.NONE, Fields.LONG));
      }
    });
  }


  @XmlElement(name = "snapshot-dependencies")
  public PropEntitiesSnapshotDep getSnapshotDependencies() {
    return myBuildType == null ? null : ValueWithDefault.decideIncludeByDefault(myFields.isIncluded("snapshot-dependencies", false),
                                                                                new ValueWithDefault.Value<PropEntitiesSnapshotDep>() {
                                                                                  public PropEntitiesSnapshotDep get() {
                                                                                    return new PropEntitiesSnapshotDep(myBuildType.get(), myFields
                                                                                      .getNestedField("snapshot-dependencies", Fields.NONE, Fields.LONG), myBeanContext);
                                                                                  }
    });
  }

  @XmlElement(name = "artifact-dependencies")
  public PropEntitiesArtifactDep getArtifactDependencies() {
    return myBuildType == null ? null : ValueWithDefault
      .decideIncludeByDefault(myFields.isIncluded("artifact-dependencies", false), new ValueWithDefault.Value<PropEntitiesArtifactDep>() {
        public PropEntitiesArtifactDep get() {
          return new PropEntitiesArtifactDep(myBuildType.get().getArtifactDependencies(), myFields.getNestedField("artifact-dependencies", Fields.NONE, Fields.LONG),
                                             myBeanContext);
        }
      });
  }

  @XmlElement(name = "agent-requirements")
  public PropEntitiesAgentRequirement getAgentRequirements() {
    return myBuildType == null ? null : ValueWithDefault.decideIncludeByDefault(myFields.isIncluded("agent-requirements", false), new ValueWithDefault.Value<PropEntitiesAgentRequirement>() {
      public PropEntitiesAgentRequirement get() {
        return new PropEntitiesAgentRequirement(myBuildType.get(), myFields.getNestedField("agent-requirements", Fields.NONE, Fields.LONG));
      }
    });
  }

  @XmlElement(name="settings")
  public Properties getSettings() {
    return myBuildType == null ? null : ValueWithDefault.decideDefault(myFields.isIncluded("settings", false), new ValueWithDefault.Value<Properties>() {
      public Properties get() {
        return new Properties(BuildTypeUtil.getSettingsParameters(myBuildType), null, myFields.getNestedField("settings", Fields.NONE, Fields.LONG));
      }
    });
  }

  /**
   * Link to investigations for this build type
   *
   * @return
   */
  @XmlElement(name = "investigations")
  public Investigations getInvestigations() {
    if (myBuildType == null || myBuildType.getBuildType() == null) {
      return null;
    }
    if (myFields.isIncluded("investigations", false, true)) {
      final ResponsibilityEntry.State state = myBuildType.getBuildType().getResponsibilityInfo().getState();
      if (!state.equals(ResponsibilityEntry.State.NONE)) {
        //todo: include list by default, add support for locator + filter here, like for builds in BuildType
        return new Investigations(null, new PagerData(InvestigationRequest.getHref(myBuildType.getBuildType())), myFields.getNestedField("investigations"), myBeanContext);
      }
    }
    return null;
  }

  /**
   * This is used only when posting a link to the build
   */
  private String submittedId;
  private String submittedInternalId;
  private String submittedLocator;

  public void setId(String id) {
    submittedId = id;
  }

  public void setInternalId(String id) {
    submittedInternalId = id;
  }

  @XmlAttribute
  public String getLocator() {
    return null;
  }

  public void setLocator(final String locator) {
    submittedLocator = locator;
  }

  @Nullable
  public String getExternalIdFromPosted(@NotNull final ServiceLocator serviceLocator) {
    if (submittedId != null) {
      if (submittedInternalId == null) {
        return submittedId;
      }
      String externalByInternal = serviceLocator.getSingletonService(BuildTypeIdentifiersManager.class).internalToExternal(submittedInternalId);
      if (externalByInternal == null || submittedId.equals(externalByInternal)) {
        return submittedId;
      }
      throw new BadRequestException(
        "Both external id '" + submittedId + "' and internal id '" + submittedInternalId + "' attributes are present and they reference different build types.");
    }
    if (submittedInternalId != null) {
      return serviceLocator.getSingletonService(BuildTypeIdentifiersManager.class).internalToExternal(submittedInternalId);
    }
    if (submittedLocator != null) {
      return serviceLocator.getSingletonService(BuildTypeFinder.class).getBuildType(null, submittedLocator).getExternalId();
    }
    throw new BadRequestException("Could not find build type by the data. Either 'id' or 'internalId' or 'locator' attributes should be specified.");
  }

  /**
   * @return null if nothing is customized
   */
  @Nullable
  public BuildTypeOrTemplate getCustomizedBuildTypeFromPosted(@NotNull final BuildTypeFinder buildTypeFinder, @NotNull final ServiceLocator serviceLocator) {
    final BuildTypeOrTemplate bt = getBuildTypeFromPosted(buildTypeFinder);

    final BuildTypeEx buildType = (BuildTypeEx)bt.getBuildType();
    if (buildType == null) {
      throw new BadRequestException("Cannot change build type template, only build types are supported");
    }

    if (submittedTemplateFlag != null && submittedTemplateFlag) {
      throw new BadRequestException("Cannot change build type to template, only build types are supported");
    }

    if (submittedName != null && !submittedName.equals(buildType.getName())) {
      throw new BadRequestException("Cannot change build type name from '" + buildType.getName() + "' to '" + submittedName + "'. Remove the name from submitted build type.");
    }

    // consider checking other unsupported options here, see https://confluence.jetbrains.com/display/TCINT/Versioned+Settings+Freeze
    // At time of 9.1:
    //VCS
    //VCS roots and checkout rules
    //build triggers
    //snapshot dependencies
    //fail build on error message from build runner
    //build features executed on server
    //VCS labeling
    //auto-merge
    //status widget
    //enable/disable of personal builds

    final BuildTypeOrTemplatePatcher buildTypeOrTemplatePatcher = new BuildTypeOrTemplatePatcher() {
      private BuildTypeOrTemplate myCached = null;

      @NotNull
      public BuildTypeOrTemplate getBuildTypeOrTemplate() {
        if (myCached == null) myCached = new BuildTypeOrTemplate(buildType.createEditableCopy(false));  //todo: support "true" value for build type "patching"
        return myCached;
      }
    };

    try {
      if (fillBuildTypeOrTemplate(buildTypeOrTemplatePatcher, serviceLocator)) {
        return buildTypeOrTemplatePatcher.getBuildTypeOrTemplate();
      }
    } catch (UnsupportedOperationException e) {
      //this gets thrown when we try to set not supported settings to an editable build type
      throw new BadRequestException("Error changing build type as per submitted settings", e);
    }

    return null;
  }

  @NotNull
  public BuildTypeOrTemplate getBuildTypeFromPosted(@NotNull final BuildTypeFinder buildTypeFinder) {
    String locatorText = "";
    if (submittedInternalId != null) {
      locatorText = "internalId:" + submittedInternalId;
    } else {
      if (submittedId != null) locatorText += (!locatorText.isEmpty() ? "," : "") + "id:" + submittedId;
    }
    if (locatorText.isEmpty()) {
      locatorText = submittedLocator;
    } else {
      if (submittedLocator != null) {
        throw new BadRequestException("Both 'locator' and 'id' or 'internalId' attributes are specified. Only one should be present.");
      }
    }
    if (StringUtil.isEmpty(locatorText)) {
      throw new BadRequestException("No build type specified. Either 'id', 'internalId' or 'locator' attribute should be present.");
    }
    return buildTypeFinder.getBuildTypeOrTemplate(null, locatorText);
  }

  @Nullable private  String submittedProjectId;
  @Nullable private  Project submittedProject;
  @Nullable private  String submittedName;
  @Nullable private  String submittedDescription;
  @Nullable private  Boolean submittedTemplateFlag;
  @Nullable private  Boolean submittedPaused;
  @Nullable private  BuildType submittedTemplate;
  @Nullable private  VcsRootEntries submittedVcsRootEntries;
  @Nullable private  Properties submittedParameters;
  @Nullable private  PropEntitiesStep submittedSteps;
  @Nullable private  PropEntitiesFeature submittedFeatures;
  @Nullable private  PropEntitiesTrigger submittedTriggers;
  @Nullable private  PropEntitiesSnapshotDep submittedSnapshotDependencies;
  @Nullable private  PropEntitiesArtifactDep submittedArtifactDependencies;
  @Nullable private  PropEntitiesAgentRequirement submittedAgentRequirements;
  @Nullable private  Properties submittedSettings;

  public void setProjectId(@Nullable final String submittedProjectId) {
    this.submittedProjectId = submittedProjectId;
  }

  public void setProject(@Nullable final Project submittedProject) {
    this.submittedProject = submittedProject;
  }

  public void setName(@Nullable final String submittedName) {
    this.submittedName = submittedName;
  }

  public void setDescription(@Nullable final String submittedDescription) {
    this.submittedDescription = submittedDescription;
  }

  public void setTemplateFlag(@Nullable final Boolean submittedTemplateFlag) {
    this.submittedTemplateFlag = submittedTemplateFlag;
  }

  public void setPaused(@Nullable final Boolean submittedPaused) {
    this.submittedPaused = submittedPaused;
  }

  public void setTemplate(@Nullable final BuildType submittedTemplate) {
    this.submittedTemplate = submittedTemplate;
  }

  public void setVcsRootEntries(@Nullable final VcsRootEntries submittedVcsRootEntries) {
    this.submittedVcsRootEntries = submittedVcsRootEntries;
  }

  public void setParameters(@Nullable final Properties submittedParameters) {
    this.submittedParameters = submittedParameters;
  }

  public void setSteps(@Nullable final PropEntitiesStep submittedSteps) {
    this.submittedSteps = submittedSteps;
  }

  public void setFeatures(@Nullable final PropEntitiesFeature submittedFeatures) {
    this.submittedFeatures = submittedFeatures;
  }

  public void setTriggers(@Nullable final PropEntitiesTrigger submittedTriggers) {
    this.submittedTriggers = submittedTriggers;
  }

  public void setSnapshotDependencies(@Nullable final PropEntitiesSnapshotDep submittedSnapshotDependencies) {
    this.submittedSnapshotDependencies = submittedSnapshotDependencies;
  }

  public void setArtifactDependencies(@Nullable final PropEntitiesArtifactDep submittedArtifactDependencies) {
    this.submittedArtifactDependencies = submittedArtifactDependencies;
  }

  public void setAgentRequirements(@Nullable final PropEntitiesAgentRequirement submittedAgentRequirements) {
    this.submittedAgentRequirements = submittedAgentRequirements;
  }

  public void setSettings(@Nullable final Properties submittedSettings) {
    this.submittedSettings = submittedSettings;
  }

  @NotNull
  public BuildTypeOrTemplate createNewBuildTypeFromPosted(@NotNull final ServiceLocator serviceLocator) {
    SProject project;
    if (submittedProject == null) {
      if (submittedProjectId == null) {
        throw new BadRequestException("Build type creation request should contain project node.");
      }
      //noinspection ConstantConditions
      project = serviceLocator.findSingletonService(ProjectManager.class).findProjectByExternalId(submittedProjectId);
      if (project == null) {
        throw new BadRequestException("Cannot find project with id '" + submittedProjectId + "'.");
      }
    } else {
      //noinspection ConstantConditions
      project = submittedProject.getProjectFromPosted(serviceLocator.findSingletonService(ProjectFinder.class));
    }

    if (StringUtil.isEmpty(submittedName)) {
      throw new BadRequestException("When creating a build type, non empty name should be provided.");
    }

    final BuildTypeOrTemplate resultingBuildType = createEmptyBuildTypeOrTemplate(serviceLocator, project, submittedName);

    fillBuildTypeOrTemplate(new BuildTypeOrTemplatePatcher() {
      @NotNull
      public BuildTypeOrTemplate getBuildTypeOrTemplate() {
        return resultingBuildType;
      }
    }, serviceLocator);

    return resultingBuildType;
  }

  @NotNull
  private BuildTypeOrTemplate createEmptyBuildTypeOrTemplate(final @NotNull ServiceLocator serviceLocator, final @NotNull SProject project, final @NotNull String name) {
    if (submittedTemplateFlag == null || !submittedTemplateFlag) {
      return new BuildTypeOrTemplate(project.createBuildType(getIdForBuildType(serviceLocator, project, name), name));
    } else {
      return new BuildTypeOrTemplate(project.createBuildTypeTemplate(getIdForBuildType(serviceLocator, project, name), name));
    }
  }

  private interface BuildTypeOrTemplatePatcher {
    @NotNull
    BuildTypeOrTemplate getBuildTypeOrTemplate();
  }

  /**
   * @param buildTypeOrTemplatePatcher provider of the build type to patch. Build type/template will only be retrieved if patching is necessary
   * @return true if there were modification attempts
   */
  private boolean fillBuildTypeOrTemplate(final @NotNull BuildTypeOrTemplatePatcher buildTypeOrTemplatePatcher, final @NotNull ServiceLocator serviceLocator) {
    boolean result = false;
    if (submittedDescription != null) {
      result = true;
      buildTypeOrTemplatePatcher.getBuildTypeOrTemplate().setDescription(submittedDescription);
    }
    if (submittedPaused != null) {
      if (buildTypeOrTemplatePatcher.getBuildTypeOrTemplate().getBuildType() == null) {
        throw new BadRequestException("Cannot set paused state for a template");
      }
      result = true;
      buildTypeOrTemplatePatcher.getBuildTypeOrTemplate().getBuildType().setPaused(Boolean.valueOf(submittedPaused),
                                                                                   serviceLocator.getSingletonService(DataProvider.class).getCurrentUser(),
                                                                                   TeamCityProperties.getProperty("rest.defaultActionComment"));
    }
    if (submittedTemplate != null) {
      if (buildTypeOrTemplatePatcher.getBuildTypeOrTemplate().getBuildType() == null) {
        throw new BadRequestException("Cannot set template for a template");
      }
      //noinspection ConstantConditions
      final BuildTypeOrTemplate templateFromPosted = submittedTemplate.getBuildTypeFromPosted(serviceLocator.findSingletonService(BuildTypeFinder.class));
      if (templateFromPosted.getTemplate() == null) {
        throw new BadRequestException("'template' field should reference a template, not build type");
      }
      result = true;
      buildTypeOrTemplatePatcher.getBuildTypeOrTemplate().getBuildType().attachToTemplate(templateFromPosted.getTemplate());
    }
    if (submittedVcsRootEntries != null && submittedVcsRootEntries.vcsRootAssignments != null) {
      for (VcsRootEntry entity : submittedVcsRootEntries.vcsRootAssignments) {
        result = true;
        BuildTypeRequest.addVcsRoot(buildTypeOrTemplatePatcher.getBuildTypeOrTemplate(), entity, serviceLocator.getSingletonService(VcsRootFinder.class));
      }
    }
    if (submittedParameters != null && submittedParameters.properties != null) {
      for (Property p : submittedParameters.properties) {
        result = true;
        BuildTypeUtil.changeParameter(p.name, p.value, buildTypeOrTemplatePatcher.getBuildTypeOrTemplate().get(), serviceLocator);
      }
    }
    if (submittedSteps != null && submittedSteps.propEntities != null) {
      for (PropEntityStep entity : submittedSteps.propEntities) {
        result = true;
        entity.addStep(buildTypeOrTemplatePatcher.getBuildTypeOrTemplate().get());
      }
    }
    if (submittedFeatures != null && submittedFeatures.propEntities != null) {
      for (PropEntityFeature entity : submittedFeatures.propEntities) {
        result = true;
        entity.addFeature(buildTypeOrTemplatePatcher.getBuildTypeOrTemplate().get(), serviceLocator.getSingletonService(BuildFeatureDescriptorFactory.class));
      }
    }
    if (submittedTriggers != null && submittedTriggers.propEntities != null) {
      for (PropEntityTrigger entity : submittedTriggers.propEntities) {
        result = true;
        entity.addTrigger(buildTypeOrTemplatePatcher.getBuildTypeOrTemplate().get(), serviceLocator.getSingletonService(BuildTriggerDescriptorFactory.class));
      }
    }
    if (submittedSnapshotDependencies != null && submittedSnapshotDependencies.propEntities != null) {
      for (PropEntitySnapshotDep entity : submittedSnapshotDependencies.propEntities) {
        result = true;
        entity.addSnapshotDependency(buildTypeOrTemplatePatcher.getBuildTypeOrTemplate().get(), serviceLocator);
      }
    }
    if (submittedArtifactDependencies != null && submittedArtifactDependencies.propEntities != null) {
      final List<SArtifactDependency> dependencyObjects =
        CollectionsUtil.convertCollection(submittedArtifactDependencies.propEntities, new Converter<SArtifactDependency, PropEntityArtifactDep>() {
          public SArtifactDependency createFrom(@NotNull final PropEntityArtifactDep source) {
            return source.createDependency(serviceLocator);
          }
        });
      result = true;
      buildTypeOrTemplatePatcher.getBuildTypeOrTemplate().get().setArtifactDependencies(dependencyObjects);
    }
    if (submittedAgentRequirements != null && submittedAgentRequirements.propEntities != null) {
      for (PropEntityAgentRequirement entity : submittedAgentRequirements.propEntities) {
        result = true;
        entity.addRequirement(buildTypeOrTemplatePatcher.getBuildTypeOrTemplate());
      }
    }
    if (submittedSettings != null && submittedSettings.properties != null) {
      for (Property property : submittedSettings.properties) {
        try {
          BuildTypeRequest.setSetting(buildTypeOrTemplatePatcher.getBuildTypeOrTemplate(), property.name, property.value);
          result = true;
        } catch (java.lang.UnsupportedOperationException e) {  //can be thrown from EditableBuildTypeCopy
          LOG.debug("Error setting property '" + property.name + "' to value '" + property.value + "': " + e.getMessage());
        }
      }
    }
    return result;
  }

  @NotNull
  public String getIdForBuildType(@NotNull final ServiceLocator serviceLocator, @NotNull SProject project, @NotNull final String name) {
    if (submittedId != null) {
      return submittedId;
    }
    return serviceLocator.getSingletonService(BuildTypeIdentifiersManager.class).generateNewExternalId(project.getExternalId(), name, null);
  }
}
