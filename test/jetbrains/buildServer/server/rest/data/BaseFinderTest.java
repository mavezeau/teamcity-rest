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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import jetbrains.buildServer.log.Loggable;
import jetbrains.buildServer.responsibility.ResponsibilityFacadeEx;
import jetbrains.buildServer.server.rest.data.investigations.InvestigationFinder;
import jetbrains.buildServer.server.rest.data.problem.ProblemFinder;
import jetbrains.buildServer.server.rest.data.problem.ProblemOccurrenceFinder;
import jetbrains.buildServer.server.rest.data.problem.TestFinder;
import jetbrains.buildServer.server.rest.data.problem.TestOccurrenceFinder;
import jetbrains.buildServer.server.rest.errors.NotFoundException;
import jetbrains.buildServer.serverSide.CurrentProblemsManager;
import jetbrains.buildServer.serverSide.TestName2IndexImpl;
import jetbrains.buildServer.serverSide.identifiers.VcsRootIdentifiersManagerImpl;
import jetbrains.buildServer.serverSide.impl.BaseServerTestCase;
import jetbrains.buildServer.serverSide.impl.LogUtil;
import jetbrains.buildServer.serverSide.mute.ProblemMutingService;
import jetbrains.buildServer.serverSide.problems.BuildProblemManager;
import jetbrains.buildServer.vcs.impl.VcsManagerImpl;
import org.jetbrains.annotations.Nullable;
import org.testng.annotations.BeforeMethod;

/**
 * Created by yaegor on 13/06/2015.
 */
public abstract class BaseFinderTest<T> extends BaseServerTestCase{
  private AbstractFinder<T> myFinder;
  protected VcsManagerImpl myVcsManager;
  protected PermissionChecker myPermissionChecker;

  protected ProjectFinder myProjectFinder;
  protected AgentFinder myAgentFinder;
  protected BuildTypeFinder myBuildTypeFinder;
  protected VcsRootFinder myVcsRootFinder;
  protected VcsRootInstanceFinder myVcsRootInstanceFinder;
  protected UserFinder myUserFinder;
  protected TestFinder myTestFinder;
  protected BuildPromotionFinder myBuildPromotionFinder;
  protected BuildFinder myBuildFinder;
  protected ProblemFinder myProblemFinder;
  protected ProblemOccurrenceFinder myProblemOccurrenceFinder;
  protected TestOccurrenceFinder myTestOccurrenceFinder;
  protected InvestigationFinder myInvestigationFinder;
  protected AgentPoolsFinder myAgentPoolsFinder;
  protected QueuedBuildFinder myQueuedBuildFinder;
  protected BranchFinder myBranchFinder;

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    initFinders();
  }

  protected void initFinders() {
    myVcsManager = myFixture.getVcsManager();
    myFixture.addService(myVcsManager);
    myFixture.addService(myProjectManager);
    myPermissionChecker = new PermissionChecker(myServer.getSecurityContext());
    myFixture.addService(myPermissionChecker);

    myProjectFinder = new ProjectFinder(myProjectManager, myPermissionChecker, myServer);
    myFixture.addService(myProjectFinder);

    myAgentFinder = new AgentFinder(myAgentManager, myFixture);
    myFixture.addService(myAgentFinder);

    myAgentPoolsFinder = new AgentPoolsFinder(myFixture, myAgentFinder);
    myFixture.addService(myAgentPoolsFinder);

    myBuildTypeFinder = new BuildTypeFinder(myProjectManager, myProjectFinder, myAgentFinder, myPermissionChecker, myServer);
    myFixture.addService(myBuildTypeFinder);

    final VcsRootIdentifiersManagerImpl vcsRootIdentifiersManager = myFixture.getSingletonService(VcsRootIdentifiersManagerImpl.class);

    myVcsRootFinder = new VcsRootFinder(myVcsManager, myProjectFinder, myBuildTypeFinder, myProjectManager,
                                        vcsRootIdentifiersManager,
                                        myPermissionChecker);
    myFixture.addService(myVcsRootFinder);
    myVcsRootInstanceFinder = new VcsRootInstanceFinder(myVcsRootFinder, myVcsManager, myProjectFinder, myBuildTypeFinder, myProjectManager,
                                                        vcsRootIdentifiersManager,
                                                        myPermissionChecker);
    myFixture.addService(myVcsRootInstanceFinder);


    final PermissionChecker permissionChecker = new PermissionChecker(myServer.getSecurityContext());
    myFixture.addService(permissionChecker);

    myUserFinder = new UserFinder(myFixture);
    myFixture.addService(myUserFinder);

    myBranchFinder = new BranchFinder(myBuildTypeFinder);

    myBuildPromotionFinder = new BuildPromotionFinder(myFixture.getBuildPromotionManager(), myFixture.getBuildQueue(), myServer, myVcsRootFinder,
                                                      myProjectFinder, myBuildTypeFinder, myUserFinder, myAgentFinder, myBranchFinder);
    myFixture.addService(myBuildPromotionFinder);

    myBuildFinder = new BuildFinder(myServer, myBuildTypeFinder, myProjectFinder, myUserFinder, myBuildPromotionFinder, myAgentFinder);
    myFixture.addService(myBuildFinder);

    final TestName2IndexImpl testName2Index = myFixture.getSingletonService(TestName2IndexImpl.class);
    final ProblemMutingService problemMutingService = myFixture.getSingletonService(ProblemMutingService.class);
    myTestFinder = new TestFinder(myProjectFinder, myFixture.getTestManager(), testName2Index, myFixture.getCurrentProblemsManager(), problemMutingService);
    myFixture.addService(myTestFinder);

    final CurrentProblemsManager currentProblemsManager = myServer.getSingletonService(CurrentProblemsManager.class);
    myTestOccurrenceFinder = new TestOccurrenceFinder(myTestFinder, myBuildFinder, myBuildTypeFinder, myProjectFinder, myServer.getHistory(), currentProblemsManager);
    myFixture.addService(myTestOccurrenceFinder);

    final BuildProblemManager buildProblemManager = myFixture.getSingletonService(BuildProblemManager.class);
    myProblemFinder = new ProblemFinder(myProjectFinder, buildProblemManager, myProjectManager, myFixture, problemMutingService);
    myFixture.addService(myProblemFinder);
    myProblemOccurrenceFinder = new ProblemOccurrenceFinder(myProjectFinder, myBuildFinder, myProblemFinder, buildProblemManager, myProjectManager, myFixture);
    myFixture.addService(myProblemOccurrenceFinder);

    final ResponsibilityFacadeEx responsibilityFacade = myFixture.getResponsibilityFacadeEx();
    myInvestigationFinder = new InvestigationFinder(myProjectFinder, myBuildTypeFinder, myProblemFinder, myTestFinder, myUserFinder,
                                                    responsibilityFacade, responsibilityFacade, responsibilityFacade);
    myFixture.addService(myInvestigationFinder);

    myQueuedBuildFinder =
      new QueuedBuildFinder(myServer.getQueue(), myProjectFinder, myBuildTypeFinder, myUserFinder, myAgentFinder, myFixture.getBuildPromotionManager(), myServer);
    myFixture.addService(myQueuedBuildFinder);
  }

  public void setFinder(AbstractFinder<T> finder){
    myFinder = finder;
  }

  public AbstractFinder<T> getFinder() {
    return myFinder;
  }

  public void check(@Nullable final String locator, T... items) {
    final List<T> result = myFinder.getItems(locator).myEntries;
    final String expected = getDescription(Arrays.asList(items));
    final String actual = getDescription(result);
    assertEquals("For itemS locator \"" + locator + "\"\n" +
                 "Expected:\n" + expected + "\n\n" +
                 "Actual:\n" + actual, items.length, result.size());

    for (int i = 0; i < items.length; i++) {
      if (!items[i].equals(result.get(i))) {
        fail("Wrong item found for locator \"" + locator + "\" at position " + (i + 1) + "/" + items.length + "\n" +
             "Expected:\n" + expected + "\n" +
             "\nActual:\n" + actual);
      }
    }

    //check single item retrieve
    if (locator != null) {
      if (items.length == 0) {
        try {
          T singleResult = myFinder.getItem(locator);
          fail("No items should be found by locator \"" + locator + "\", but found: " + getDescription(singleResult));
        } catch (NotFoundException e) {
          //exception is expected
        }
      } else {
        T singleResult = myFinder.getItem(locator);
        final T item = items[0];
        if (!item.equals(singleResult)) {
          fail("While searching for single item with locator \"" + locator + "\"\n" +
               "Expected: " + getDescription(item) + "\n" +
               "Actual: " + getDescription(singleResult));
        }
      }
    }
  }

  private String getDescription(final T singleResult) {
    if (singleResult instanceof Loggable){
      return LogUtil.describeInDetail(((Loggable)singleResult));
    }
    return LogUtil.describe(singleResult);
  }

  public <E extends Throwable> void checkExceptionOnItemsSearch(final Class<E> exception, final String multipleSearchLocator) {
    BuildPromotionFinderTest.checkException(exception, new Runnable() {
      public void run() {
        myFinder.getItems(multipleSearchLocator);
      }
    }, "searching for itemS with locator \"" + multipleSearchLocator + "\"");
  }

  public <E extends Throwable> void checkExceptionOnItemSearch(final Class<E> exception, final String singleSearchLocator) {
    BuildPromotionFinderTest.checkException(exception, new Runnable() {
      public void run() {
        myFinder.getItem(singleSearchLocator);
      }
    }, "searching for item with locator \"" + singleSearchLocator + "\"");
  }

  public String getDescription(final List<T> result) {
    if (result == null) {
      return LogUtil.describe((Object)null);
    }

    final StringBuilder result1 = new StringBuilder();
    final Iterator<T> it = result.iterator();
    while(it.hasNext()) {
      T item = it.next();
      if (item != null) {
        result1.append("").append(LogUtil.describe(item)).append("");
        if (it.hasNext()) {
          result1.append("\n");
        }
      }
    }
    return result1.toString();
  }
}