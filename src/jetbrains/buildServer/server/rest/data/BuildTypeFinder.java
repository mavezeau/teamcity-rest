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

package jetbrains.buildServer.server.rest.data;

import com.intellij.openapi.diagnostic.Logger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import jetbrains.buildServer.ServiceLocator;
import jetbrains.buildServer.parameters.impl.MapParametersProviderImpl;
import jetbrains.buildServer.server.rest.APIController;
import jetbrains.buildServer.server.rest.errors.BadRequestException;
import jetbrains.buildServer.server.rest.errors.LocatorProcessException;
import jetbrains.buildServer.server.rest.errors.NotFoundException;
import jetbrains.buildServer.server.rest.model.PagerData;
import jetbrains.buildServer.server.rest.model.buildType.BuildTypes;
import jetbrains.buildServer.server.rest.util.BuildTypeOrTemplate;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.dependency.Dependency;
import jetbrains.buildServer.serverSide.impl.LogUtil;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.Converter;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Yegor.Yarko
 *         Date: 23.03.13
 */
public class BuildTypeFinder extends AbstractFinder<BuildTypeOrTemplate> {
  private static final Logger LOG = Logger.getInstance(BuildTypeFinder.class.getName());

  public static final String TEMPLATE_ID_PREFIX = "template:"; //used for old ids parsing

  public static final String DIMENSION_ID = AbstractFinder.DIMENSION_ID;
  public static final String DIMENSION_INTERNAL_ID = "internalId";
  public static final String DIMENSION_UUID = "uuid";
  public static final String DIMENSION_PROJECT = "project";
  private static final String AFFECTED_PROJECT = "affectedProject";
  public static final String DIMENSION_NAME = "name";
  public static final String TEMPLATE_DIMENSION_NAME = "template";
  public static final String TEMPLATE_FLAG_DIMENSION_NAME = "templateFlag";
  public static final String PAUSED = "paused";
  protected static final String COMPATIBLE_AGENT = "compatibleAgent";
  protected static final String COMPATIBLE_AGENTS_COUNT = "compatibleAgentsCount";
  protected static final String PARAMETER = "parameter";
  protected static final String FILTER_BUILDS = "filterByBuilds";
  protected static final String SNAPSHOT_DEPENDENCY = "snapshotDependency";

  private final ProjectFinder myProjectFinder;
  @NotNull private final AgentFinder myAgentFinder;
  private final ProjectManager myProjectManager;
  private final ServiceLocator myServiceLocator;

  public BuildTypeFinder(@NotNull final ProjectManager projectManager,
                         @NotNull final ProjectFinder projectFinder,
                         @NotNull final AgentFinder agentFinder,
                         @NotNull final ServiceLocator serviceLocator) {
    super(new String[]{DIMENSION_ID, DIMENSION_INTERNAL_ID, DIMENSION_UUID, DIMENSION_PROJECT, AFFECTED_PROJECT, DIMENSION_NAME, TEMPLATE_FLAG_DIMENSION_NAME, TEMPLATE_DIMENSION_NAME, PAUSED,
      Locator.LOCATOR_SINGLE_VALUE_UNUSED_NAME,
      PagerData.START,
      PagerData.COUNT
    });
    myProjectManager = projectManager;
    myProjectFinder = projectFinder;
    myAgentFinder = agentFinder;
    myServiceLocator = serviceLocator;
  }

  @NotNull
  public static String getLocator(@NotNull final SBuildType buildType) {
    return Locator.createEmptyLocator().setDimension(DIMENSION_ID, buildType.getExternalId()).getStringRepresentation();
  }

  @NotNull
  public static String getLocator(@NotNull final BuildTypeTemplate template) {
    return Locator.createEmptyLocator().setDimension(DIMENSION_ID, template.getExternalId()).getStringRepresentation();
  }

  @NotNull
  @Override
  public Locator createLocator(@Nullable final String locatorText, @Nullable final Locator locatorDefaults) {
    final Locator result = super.createLocator(locatorText, locatorDefaults);
    result.addHiddenDimensions(COMPATIBLE_AGENT, COMPATIBLE_AGENTS_COUNT, PARAMETER, FILTER_BUILDS, SNAPSHOT_DEPENDENCY); //hide these for now
    return result;
  }

  @NotNull
  @Override
  public ItemHolder<BuildTypeOrTemplate> getAllItems() {
    final List<BuildTypeOrTemplate> result = new ArrayList<BuildTypeOrTemplate>();
    result.addAll(BuildTypes.fromBuildTypes(myProjectManager.getAllBuildTypes()));
    result.addAll(BuildTypes.fromTemplates(myProjectManager.getAllTemplates()));
    return getItemHolder(result);
  }

  @Override
  @Nullable
  protected BuildTypeOrTemplate findSingleItem(@NotNull final Locator locator) {
    if (locator.isSingleValue()) {
      // no dimensions found, assume it's an internal id, external id or name
      final String value = locator.getSingleValue();
      assert value != null;
      BuildTypeOrTemplate buildType = findBuildTypeOrTemplateByInternalId(value, null);
      if (buildType != null) {
        return buildType;
      }

      buildType = findBuildTypeOrTemplateByExternalId(value, null);
      if (buildType != null) {
        return buildType;
      }

      // assume it's a name
      final BuildTypeOrTemplate buildTypeByName = findBuildTypebyName(value, null, null);
      if (buildTypeByName != null) {
        return buildTypeByName;
      }
      throw new NotFoundException("No build type or template is found by id, internal id or name '" + value + "'.");
    }

    String internalId = locator.getSingleDimensionValue(DIMENSION_INTERNAL_ID);
    if (!StringUtil.isEmpty(internalId)) {
      Boolean template = locator.getSingleDimensionValueAsBoolean(TEMPLATE_FLAG_DIMENSION_NAME);
      if (template == null) {
        //legacy support for boolean value
        try {
          template = locator.getSingleDimensionValueAsBoolean(TEMPLATE_DIMENSION_NAME);
        } catch (LocatorProcessException e) {
          //override default message as it might be confusing here due to legacy support
          throw new BadRequestException("Try omitting dimension '" + TEMPLATE_DIMENSION_NAME + "' here");
        }
      }
      BuildTypeOrTemplate buildType = findBuildTypeOrTemplateByInternalId(internalId, template);
      if (buildType != null) {
        return buildType;
      }
      throw new NotFoundException("No " + getName(template) + " is found by internal id '" + internalId + "'.");
    }

    String uuid = locator.getSingleDimensionValue(DIMENSION_UUID);
    if (!StringUtil.isEmpty(uuid)) {
      Boolean template = locator.getSingleDimensionValueAsBoolean(TEMPLATE_FLAG_DIMENSION_NAME);
      BuildTypeOrTemplate buildType = findBuildTypeOrTemplateByUuid(uuid, template);
      if (buildType != null) {
        return buildType;
      }
      //protecting against brute force uuid guessing
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        //ignore
      }
      throw new NotFoundException("No " + getName(template) + " is found by uuid '" + uuid + "'.");
    }

    String id = locator.getSingleDimensionValue(DIMENSION_ID);
    if (!StringUtil.isEmpty(id)) {
      Boolean template = locator.getSingleDimensionValueAsBoolean(TEMPLATE_FLAG_DIMENSION_NAME);
      if (template == null) {
        //legacy support for boolean value
        try {
          template = locator.getSingleDimensionValueAsBoolean(TEMPLATE_DIMENSION_NAME);
        } catch (LocatorProcessException e) {
          //override default message as it might be confusing here due to legacy support
          throw new BadRequestException("Try omitting dimension '" + TEMPLATE_DIMENSION_NAME + "' here");
        }
      }
      BuildTypeOrTemplate buildType = findBuildTypeOrTemplateByExternalId(id, template);
      if (buildType != null) {
        return buildType;
      }

      // support pre-8.0 style of template ids
      final BuildTypeOrTemplate templateByOldIdWithPrefix = findTemplateByOldIdWithPrefix(id);
      if (templateByOldIdWithPrefix != null) {
        return templateByOldIdWithPrefix;
      }

      if (TeamCityProperties.getBoolean(APIController.REST_COMPATIBILITY_ALLOW_EXTERNAL_ID_AS_INTERNAL)) {
        buildType = findBuildTypeOrTemplateByInternalId(id, template);
        if (buildType != null) {
          return buildType;
        }
        throw new NotFoundException("No " + getName(template) + " is found by id '" + id + "' in compatibility mode." +
                                    " Cannot be found by external or internal id '" + id + "'.");
      }
      throw new NotFoundException("No " + getName(template) + " is found by id '" + id + "'.");
    }

    return null;
  }


  @NotNull
  @Override
  protected AbstractFilter<BuildTypeOrTemplate> getFilter(final Locator locator) {
    if (locator.isSingleValue()) {
      throw new BadRequestException("Single value locator '" + locator.getSingleValue() + "' is not supported for several items query.");
    }

    final Long countFromFilter = locator.getSingleDimensionValueAsLong(PagerData.COUNT);
    final MultiCheckerFilter<BuildTypeOrTemplate> result =
      new MultiCheckerFilter<BuildTypeOrTemplate>(locator.getSingleDimensionValueAsLong(PagerData.START), countFromFilter != null ? countFromFilter.intValue() : null, null);

    final String name = locator.getSingleDimensionValue(DIMENSION_NAME);
    if (name != null) {
      result.add(new FilterConditionChecker<BuildTypeOrTemplate>() {
        public boolean isIncluded(@NotNull final BuildTypeOrTemplate item) {
          return name.equalsIgnoreCase(item.getName());
        }
      });
    }

    final String projectLocator = locator.getSingleDimensionValue(DIMENSION_PROJECT);
    SProject project = null;
    if (projectLocator != null) {
      project = myProjectFinder.getItem(projectLocator);
      final SProject internalProject = project;
      result.add(new FilterConditionChecker<BuildTypeOrTemplate>() {
        public boolean isIncluded(@NotNull final BuildTypeOrTemplate item) {
          return internalProject.getProjectId().equals(item.getProject().getProjectId());
        }
      });
    }

    final String affectedProjectDimension = locator.getSingleDimensionValue(AFFECTED_PROJECT);
    if (affectedProjectDimension != null) {
      @NotNull final SProject parentProject = myProjectFinder.getItem(affectedProjectDimension);
      result.add(new FilterConditionChecker<BuildTypeOrTemplate>() {
        public boolean isIncluded(@NotNull final BuildTypeOrTemplate item) {
          return ProjectFinder.isSameOrParent(parentProject, item.getProject());
        }
      });
    }

    final Boolean paused = locator.getSingleDimensionValueAsBoolean(PAUSED);
    if (paused != null) {
      result.add(new FilterConditionChecker<BuildTypeOrTemplate>() {
        public boolean isIncluded(@NotNull final BuildTypeOrTemplate item) {
          final Boolean pausedState = item.isPaused();
          return FilterUtil.isIncludedByBooleanFilter(paused, pausedState != null && pausedState);
        }
      });
    }

    final String compatibleAagentLocator = locator.getSingleDimensionValue(COMPATIBLE_AGENT); //experimental
    if (compatibleAagentLocator != null) {
      final SBuildAgent agent = myAgentFinder.getItem(compatibleAagentLocator);
      result.add(new FilterConditionChecker<BuildTypeOrTemplate>() {
        public boolean isIncluded(@NotNull final BuildTypeOrTemplate item) {
          return item.getBuildType() != null && item.getBuildType().getCompatibleAgents().contains(agent); //ineffective!
        }
      });
    }

    final Long compatibleAgentsCount = locator.getSingleDimensionValueAsLong(COMPATIBLE_AGENTS_COUNT); //experimental
    if (compatibleAgentsCount != null) {
      result.add(new FilterConditionChecker<BuildTypeOrTemplate>() {
        public boolean isIncluded(@NotNull final BuildTypeOrTemplate item) {
          return item.getBuildType() != null && compatibleAgentsCount.equals(Integer.valueOf(item.getBuildType().getCompatibleAgents().size()).longValue());
        }
      });
    }

    final String parameterDimension = locator.getSingleDimensionValue(PARAMETER);
    if (parameterDimension != null) {
      final ParameterCondition parameterCondition = ParameterCondition.create(parameterDimension);
      result.add(new FilterConditionChecker<BuildTypeOrTemplate>() {
        public boolean isIncluded(@NotNull final BuildTypeOrTemplate item) {
          return parameterCondition.matches(new MapParametersProviderImpl(item.get().getParameters())); //includes project params? template params?
        }
      });
    }

    final Boolean template = locator.getSingleDimensionValueAsBoolean(TEMPLATE_FLAG_DIMENSION_NAME);
    if (template != null) {
      result.add(new FilterConditionChecker<BuildTypeOrTemplate>() {
        public boolean isIncluded(@NotNull final BuildTypeOrTemplate item) {
          return FilterUtil.isIncludedByBooleanFilter(template, item.isTemplate());
        }
      });
    }

    final String filterBuilds = locator.getSingleDimensionValue(FILTER_BUILDS); //experimental
    if (filterBuilds != null) {
      final Locator filterBuildsLocator = new Locator(filterBuilds, "search", "match");  // support for conditions like "in which the last build is successful"
      final String search = filterBuildsLocator.getSingleDimensionValue("search");
      final String match = filterBuildsLocator.getSingleDimensionValue("match");
      if (search != null) {
        result.add(new FilterConditionChecker<BuildTypeOrTemplate>() {
          public boolean isIncluded(@NotNull final BuildTypeOrTemplate item) {
            if (item.getBuildType() == null) return false;
            final BuildFinder buildFinder = myServiceLocator.getSingletonService(BuildFinder.class);
            final List<BuildPromotion> buildPromotions = buildFinder.getBuilds(item.getBuildType(), search).myEntries;
            if (buildPromotions.isEmpty()) {
              return false;
            }
            if (match == null) {
              return buildPromotions.size() > 0;
            }
            final BuildPromotion buildPromotion = buildPromotions.get(0);
            final SBuild associatedBuild = buildPromotion.getAssociatedBuild();
            if (associatedBuild == null) {
              return false; //queued builds are not yet supported
            }
            return buildFinder.getBuildsFilter(null, match).isIncluded(associatedBuild);
          }
        });
      }
    }

    return result;
  }

  @NotNull
  @Override
  protected ItemHolder<BuildTypeOrTemplate> getPrefilteredItems(@NotNull final Locator locator) {
    List<BuildTypeOrTemplate> result = new ArrayList<BuildTypeOrTemplate>();
    final String snapshotDependencies = locator.getSingleDimensionValue(SNAPSHOT_DEPENDENCY);
    if (snapshotDependencies != null) {
      final GraphFinder<BuildTypeOrTemplate> graphFinder = new GraphFinder<BuildTypeOrTemplate>(this, new SnapshotDepsTraverser());
      return getItemHolder(graphFinder.getItems(snapshotDependencies).myEntries);
    }

    SProject project = null;
    final String projectLocator = locator.getSingleDimensionValue(DIMENSION_PROJECT);
    if (projectLocator != null) {
      project = myProjectFinder.getItem(projectLocator);
    }

    SProject affectedProject = null;
    final String affectedProjectLocator = locator.getSingleDimensionValue(AFFECTED_PROJECT);
    if (affectedProjectLocator != null) {
      affectedProject = myProjectFinder.getItem(affectedProjectLocator);
    }

    final String templateLocator = locator.getSingleDimensionValue(TEMPLATE_DIMENSION_NAME);
    if (templateLocator != null) {
      final BuildTypeTemplate buildTemplate;
      try {
        buildTemplate = getBuildTemplate(null, templateLocator);
      } catch (NotFoundException e) {
        throw new NotFoundException("No templates found by locator '" + templateLocator + "' specified in '" + TEMPLATE_DIMENSION_NAME + "' dimension : " + e.getMessage());
      } catch (BadRequestException e) {
        throw new BadRequestException(
          "Error while searching for templates by locator '" + templateLocator + "' specified in '" + TEMPLATE_DIMENSION_NAME + "' dimension : " + e.getMessage(), e);
      }
      return getItemHolder(BuildTypes.fromBuildTypes(buildTemplate.getUsages()));
    }

    Boolean template = locator.getSingleDimensionValueAsBoolean(TEMPLATE_FLAG_DIMENSION_NAME);
    if (template == null || !template) {
      if (project != null) {
        result.addAll(BuildTypes.fromBuildTypes(project.getOwnBuildTypes()));
      } else if (affectedProject != null) {
        result.addAll(BuildTypes.fromBuildTypes(affectedProject.getBuildTypes()));
      } else {
        result.addAll(BuildTypes.fromBuildTypes(myProjectManager.getAllBuildTypes()));
      }
    }
    if (template == null || template) {
      if (project != null) {
        result.addAll(BuildTypes.fromTemplates(project.getOwnBuildTypeTemplates()));
      } else if (affectedProject != null) {
        result.addAll(BuildTypes.fromTemplates(affectedProject.getBuildTypeTemplates()));
      } else {
        result.addAll(BuildTypes.fromTemplates(myProjectManager.getAllTemplates()));
      }
    }

    return getItemHolder(result);
  }

  @NotNull
  public BuildTypeOrTemplate getBuildTypeOrTemplate(@Nullable final SProject project, @Nullable final String buildTypeLocator) {
    if (project == null){
      return getItem(buildTypeLocator);
    }

    final Locator locator = buildTypeLocator != null ? new Locator(buildTypeLocator) : null;
    if (locator == null || !locator.isSingleValue()) {
      return getItem(Locator.setDimensionIfNotPresent(buildTypeLocator, DIMENSION_PROJECT, ProjectFinder.getLocator(project)));
    }

    // single value locator

    final BuildTypeOrTemplate result = getItem(buildTypeLocator);
    if (!result.getProject().getProjectId().equals(project.getProjectId())) {
      throw new BadRequestException("Found " + LogUtil.describe(result) + " but it does not belong to project " + LogUtil.describe(project) + ".");
    }
    return result;
  }

  private String getName(final Boolean template) {
    if (template == null) {
      return "build type nor template";
    }
    return template ? "template" : "build type";
  }

  @NotNull
  public SBuildType getBuildType(@Nullable final SProject project, @Nullable final String buildTypeLocator) {
    final BuildTypeOrTemplate buildTypeOrTemplate = getBuildTypeOrTemplate(project, buildTypeLocator);
    if (buildTypeOrTemplate.getBuildType() != null) {
      return buildTypeOrTemplate.getBuildType();
    }
    throw new NotFoundException("No build type is found by locator '" + buildTypeLocator + "'. Template is found instead.");
  }

  @NotNull
  public BuildTypeTemplate getBuildTemplate(@Nullable final SProject project, @Nullable final String buildTypeLocator) {
    final BuildTypeOrTemplate buildTypeOrTemplate = getBuildTypeOrTemplate(project, buildTypeLocator);
    if (buildTypeOrTemplate.getTemplate() != null) {
      return buildTypeOrTemplate.getTemplate();
    }
    throw new BadRequestException("No build type template found by locator '" + buildTypeLocator + "'. Build type is found instead.");
  }

  @NotNull
  public List<SBuildType> getBuildTypes(@Nullable final SProject project, @Nullable final String buildTypeLocator) {
    final PagedSearchResult<BuildTypeOrTemplate> items = getBuildTypesPaged(project, buildTypeLocator, true);
    return CollectionsUtil.convertCollection(items.myEntries, new Converter<SBuildType, BuildTypeOrTemplate>() {
      public SBuildType createFrom(@NotNull final BuildTypeOrTemplate source) {
        if (project != null && !source.getProject().equals(project)) {
          throw new BadRequestException("Found " + LogUtil.describe(source.getBuildType()) + " but it does not belong to project " + LogUtil.describe(project) + ".");
        }
        return source.getBuildType();
      }
    });
  }

  @NotNull
  public PagedSearchResult<BuildTypeOrTemplate> getBuildTypesPaged(final @Nullable SProject project, final @Nullable String buildTypeLocator, final boolean buildType) {
    String actualLocator = Locator.setDimension(buildTypeLocator, TEMPLATE_FLAG_DIMENSION_NAME, String.valueOf(buildType));

    if (project != null) {
      actualLocator = Locator.setDimensionIfNotPresent(actualLocator, DIMENSION_PROJECT, ProjectFinder.getLocator(project));
    }

    return getItems(actualLocator);
  }

  @Nullable
  public SBuildType getBuildTypeIfNotNull(@Nullable final String buildTypeLocator) {
    return buildTypeLocator == null ? null : getBuildType(null, buildTypeLocator);
  }

  @Nullable
  public SBuildType deriveBuildTypeFromLocator(@Nullable SBuildType contextBuildType, @Nullable final String buildTypeLocator) {
    if (buildTypeLocator != null) {
      final SBuildType buildTypeFromLocator = getBuildType(null, buildTypeLocator);
      if (contextBuildType == null) {
        return buildTypeFromLocator;
      } else if (!contextBuildType.getBuildTypeId().equals(buildTypeFromLocator.getBuildTypeId())) {
        throw new BadRequestException("Explicit build type (" + contextBuildType.getBuildTypeId() +
                                      ") does not match build type in 'buildType' locator (" + buildTypeLocator + ").");
      }
    }
    return contextBuildType;
  }


  @Nullable
  private static BuildTypeOrTemplate getOwnBuildTypeOrTemplateByName(@NotNull final SProject project, @NotNull final String name, final Boolean isTemplate) {
    if (isTemplate == null || !isTemplate) {
      final SBuildType buildType = project.findBuildTypeByName(name);
      if (buildType != null) {
        return new BuildTypeOrTemplate(buildType);
      }
      if (isTemplate != null) return null;
    }
    final BuildTypeTemplate buildTypeTemplate = project.findBuildTypeTemplateByName(name);
    if (buildTypeTemplate != null) {
      return new BuildTypeOrTemplate(buildTypeTemplate);
    }
    return null;
  }

  @Nullable
  private BuildTypeOrTemplate findBuildTypebyName(@NotNull final String name, @Nullable List<SProject> projects, final Boolean isTemplate) {
    if (projects == null) {
      projects = myProjectManager.getProjects();
    }
    BuildTypeOrTemplate firstFound = null;
    for (SProject project : projects) {
      final BuildTypeOrTemplate found = getOwnBuildTypeOrTemplateByName(project, name, isTemplate);
      if (found != null) {
        if (firstFound != null) {
          String message = "Several matching ";
          if (isTemplate == null) {
            message += "build types/templates";
          } else if (isTemplate) {
            message += "templates";
          } else {
            message += "build types";
          }
          throw new BadRequestException(message + " found for name '" + name + "'.");
        }
        firstFound = found;
      }
    }
    return firstFound;
  }

  @Nullable
  private BuildTypeOrTemplate findBuildTypeOrTemplateByInternalId(@NotNull final String internalId, @Nullable final Boolean isTemplate) {
    if (isTemplate == null || !isTemplate) {
      SBuildType buildType = myProjectManager.findBuildTypeById(internalId);
      if (buildType != null) {
        return new BuildTypeOrTemplate(buildType);
      }
    }
    if (isTemplate == null || isTemplate) {
      final BuildTypeTemplate buildTypeTemplate = myProjectManager.findBuildTypeTemplateById(internalId);
      if (buildTypeTemplate != null) {
        return new BuildTypeOrTemplate(buildTypeTemplate);
      }
    }
    return null;
  }

  @Nullable
  private BuildTypeOrTemplate findBuildTypeOrTemplateByUuid(@NotNull final String uuid, @Nullable final Boolean isTemplate) {
    if (isTemplate == null || !isTemplate) {
      SBuildType buildType = myProjectManager.findBuildTypeByConfigId(uuid);
      if (buildType != null) {
        return new BuildTypeOrTemplate(buildType);
      }
    }
    if (isTemplate == null || isTemplate) {
      final BuildTypeTemplate buildTypeTemplate = myProjectManager.findBuildTypeTemplateByConfigId(uuid);
      if (buildTypeTemplate != null) {
        return new BuildTypeOrTemplate(buildTypeTemplate);
      }
    }
    return null;
  }

  @Nullable
  private BuildTypeOrTemplate findBuildTypeOrTemplateByExternalId(@NotNull final String internalId, @Nullable final Boolean isTemplate) {
    if (isTemplate == null || !isTemplate) {
      SBuildType buildType = myProjectManager.findBuildTypeByExternalId(internalId);
      if (buildType != null) {
        return new BuildTypeOrTemplate(buildType);
      }
    }
    if (isTemplate == null || isTemplate) {
      final BuildTypeTemplate buildTypeTemplate = myProjectManager.findBuildTypeTemplateByExternalId(
        internalId);
      if (buildTypeTemplate != null) {
        return new BuildTypeOrTemplate(buildTypeTemplate);
      }
    }
    return null;
  }

  @Nullable
  private BuildTypeOrTemplate findTemplateByOldIdWithPrefix(@NotNull final String idWithPrefix) {
    if (!idWithPrefix.startsWith(TEMPLATE_ID_PREFIX)) {
      return null;
    }

    String templateId = idWithPrefix.substring(TEMPLATE_ID_PREFIX.length());
    final BuildTypeTemplate buildTypeTemplateByStrippedId = myProjectManager.findBuildTypeTemplateById(templateId);
    if (buildTypeTemplateByStrippedId != null) {
      return new BuildTypeOrTemplate(buildTypeTemplateByStrippedId);
    }
    return null;
  }

  @NotNull
  public static List<SBuildType> getBuildTypesByInternalIds(@NotNull final Collection<String> buildTypeIds, @NotNull final ProjectManager projectManager) {
    final ArrayList<SBuildType> result = new ArrayList<SBuildType>(buildTypeIds.size());
    for (String buildTypeId : buildTypeIds) {
      final SBuildType buildType = getBuildTypeByInternalId(buildTypeId, projectManager);
      result.add(buildType);
    }
    return result;
  }

  @NotNull
  public static SBuildType getBuildTypeByInternalId(@NotNull final String buildTypeInternalId, @NotNull final ProjectManager projectManager) {
    final SBuildType result = projectManager.findBuildTypeById(buildTypeInternalId);
    if (result == null) {
      throw new NotFoundException("No buildType found by internal id '" + buildTypeInternalId + "'.");
    }
    return result;
  }

  private class SnapshotDepsTraverser implements GraphFinder.Traverser<BuildTypeOrTemplate> {
    @NotNull
    public GraphFinder.LinkRetriever<BuildTypeOrTemplate> getChildren() {
      return new GraphFinder.LinkRetriever<BuildTypeOrTemplate>() {
        @NotNull
        public List<BuildTypeOrTemplate> getLinked(@NotNull final BuildTypeOrTemplate item) {
          return getNotNullBuildTypes(item.get().getDependencies());
        }
      };
    }

    @NotNull
    public GraphFinder.LinkRetriever<BuildTypeOrTemplate> getParents() {
      return new GraphFinder.LinkRetriever<BuildTypeOrTemplate>() {
        @NotNull
        public List<BuildTypeOrTemplate> getLinked(@NotNull final BuildTypeOrTemplate item) {
          final SBuildType buildType = item.getBuildType();
          if (buildType == null){
            return new ArrayList<BuildTypeOrTemplate>(); //template should have no dependnecies on it
          }
          return getDependingOn(buildType);
        }
      };
    }
  }

  @NotNull
  private List<BuildTypeOrTemplate> getDependingOn(@NotNull final SBuildType buildType) {
    final Set<String> internalIds = ((BuildTypeEx)buildType).getDependedOnMe().keySet(); //TeamCity open API issue
    final ArrayList<BuildTypeOrTemplate> result = new ArrayList<BuildTypeOrTemplate>();
    for (String internalId : internalIds) {
      final SBuildType buildTypeById = myProjectManager.findBuildTypeById(internalId);
      if (buildTypeById != null){
        result.add(new BuildTypeOrTemplate(buildTypeById));
      }
    }
    return result;
  }

  @NotNull
  private List<BuildTypeOrTemplate> getNotNullBuildTypes(@NotNull final List<Dependency> dependencies) {
    final ArrayList<BuildTypeOrTemplate> result = new ArrayList<BuildTypeOrTemplate>();
    for (Dependency dependency : dependencies) {
      final SBuildType dependOn = dependency.getDependOn();
      if (dependOn != null) {
        result.add(new BuildTypeOrTemplate(dependOn));
      }
      //todo: else expose this somehow
    }
    return result;
  }
}
