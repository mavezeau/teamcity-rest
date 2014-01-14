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

package jetbrains.buildServer.server.rest.model.problem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import jetbrains.buildServer.ServiceLocator;
import jetbrains.buildServer.server.rest.ApiUrlBuilder;
import jetbrains.buildServer.server.rest.data.problem.ProblemWrapper;
import jetbrains.buildServer.server.rest.model.Fields;
import jetbrains.buildServer.server.rest.model.PagerData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Yegor.Yarko
 *         Date: 11.02.12
 */
@SuppressWarnings("PublicField")
@XmlRootElement(name = "problems")
public class Problems {
  @XmlElement(name = "problem") public List<Problem> items;
  @XmlAttribute public long count;
  @XmlAttribute(required = false) @Nullable public String nextHref;
  @XmlAttribute(required = false) @Nullable public String prevHref;

  public Problems() {
  }

  public Problems(@NotNull final List<ProblemWrapper> itemsP,
                  @Nullable final PagerData pagerData,
                  @NotNull final ServiceLocator serviceLocator,
                  @NotNull final ApiUrlBuilder apiUrlBuilder,
                  @NotNull final Fields fields) {
    final List<ProblemWrapper> sortedItems = new ArrayList<ProblemWrapper>(itemsP);
    Collections.sort(sortedItems, new Comparator<ProblemWrapper>() {
      public int compare(final ProblemWrapper o1, final ProblemWrapper o2) {
        return o1.getId().compareTo(o2.getId());
      }
    });
    items = new ArrayList<Problem>(sortedItems.size());  //todo: consider adding ordering/sorting
    for (ProblemWrapper item : sortedItems) {
      items.add(new Problem(item, serviceLocator, apiUrlBuilder, fields.getNestedField("problem")));
    }
    if (pagerData != null) {
      nextHref = pagerData.getNextHref() != null ? apiUrlBuilder.transformRelativePath(pagerData.getNextHref()) : null;
      prevHref = pagerData.getPrevHref() != null ? apiUrlBuilder.transformRelativePath(pagerData.getPrevHref()) : null;
    }
    count = items.size();
  }
}
