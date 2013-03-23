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

package jetbrains.buildServer.server.rest.data.build;

import com.intellij.openapi.diagnostic.Logger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jetbrains.buildServer.server.rest.data.AbstractFilter;
import jetbrains.buildServer.server.rest.data.FilterItemProcessor;
import jetbrains.buildServer.serverSide.BuildHistory;
import jetbrains.buildServer.serverSide.RunningBuildsManager;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.users.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Yegor.Yarko
 *         Date: 14.01.13
 */
public class BuildsFilterProcessor {
  final static Logger LOG = Logger.getInstance(BuildsFilterProcessor.class.getName());

  public static List<SFinishedBuild> getMatchingFinishedBuilds(@NotNull final BuildsFilter buildsFilter, @NotNull final BuildHistory buildHistory) {
    if (buildsFilter.getRunning() != null && buildsFilter.getRunning()){
      return Collections.emptyList();
    }

    final FilterItemProcessor<SFinishedBuild> buildsFilterItemProcessor =
      new FilterItemProcessor<SFinishedBuild>(new FinishedBuildsFilter(buildsFilter));
    if (buildsFilter.getBuildType() != null) {
        buildHistory.processEntries(buildsFilter.getBuildType().getBuildTypeId(),
                                    getUserForProcessEntries(buildsFilter),
                                    buildsFilter.getPersonal() == null || buildsFilter.getPersonal(),
                                    buildsFilter.getCanceled() == null || buildsFilter.getCanceled(),
                                    false,
                                    buildsFilterItemProcessor);
    } else {
      buildHistory.processEntries(buildsFilterItemProcessor);
    }
    final ArrayList<SFinishedBuild> result = buildsFilterItemProcessor.getResult();
    LOG.debug("Processed " + buildsFilterItemProcessor.getProcessedItemsCount() + " builds, " + result.size() + " selected.");
    return result;
  }

  public static List<SRunningBuild> getMatchingRunningBuilds(@NotNull final BuildsFilter buildsFilter,
                                                             @NotNull final RunningBuildsManager runningBuildsManager) {
    final FilterItemProcessor<SRunningBuild> buildsFilterItemProcessor =
      new FilterItemProcessor<SRunningBuild>(new RunningBuildsFilter(buildsFilter));
    AbstractFilter.processList(runningBuildsManager.getRunningBuilds(), buildsFilterItemProcessor);
    return buildsFilterItemProcessor.getResult();
  }

  @Nullable
  public static User getUserForProcessEntries(@NotNull final BuildsFilter buildsFilter) {
    if ((buildsFilter.getPersonal() == null || buildsFilter.getPersonal()) && buildsFilter.getUser() != null) {
      return buildsFilter.getUser();
    }
    return null;
  }

  private static class FinishedBuildsFilter extends AbstractFilter<SFinishedBuild> {
    private int processedItems;
    @NotNull private final BuildsFilter myBuildsFilter;

    public FinishedBuildsFilter(@NotNull final BuildsFilter buildsFilter) {
      super(buildsFilter.getStart(), buildsFilter.getCount());
      processedItems = 0;
      myBuildsFilter = buildsFilter;
    }

    @Override
    protected boolean isIncluded(@NotNull final SFinishedBuild item) {
      ++processedItems;
      return myBuildsFilter.isIncluded(item);
    }

    @Override
    public boolean shouldStop(final SFinishedBuild item) {
      if (myBuildsFilter.getLookupLimit() != null && processedItems >= myBuildsFilter.getLookupLimit()){
        return true;
      }
      //assume the builds are processed from most recent to older
      return myBuildsFilter.isExcludedBySince(item);
    }
  }

  private static class RunningBuildsFilter extends AbstractFilter<SRunningBuild> {
    @NotNull private final BuildsFilter myBuildsFilter;

    public RunningBuildsFilter(@NotNull final BuildsFilter buildsFilter) {
      super(buildsFilter.getStart(), buildsFilter.getCount());
      this.myBuildsFilter = buildsFilter;
    }

    @Override
    protected boolean isIncluded(@NotNull final SRunningBuild item) {
      return myBuildsFilter.isIncluded(item);
    }
  }
}