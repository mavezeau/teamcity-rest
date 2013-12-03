package jetbrains.buildServer.server.rest.model.problem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import jetbrains.buildServer.server.rest.model.Fields;
import jetbrains.buildServer.server.rest.model.PagerData;
import jetbrains.buildServer.server.rest.util.BeanContext;
import jetbrains.buildServer.serverSide.STestRun;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Yegor.Yarko
 *         Date: 16.11.13
 */
@SuppressWarnings("PublicField")
@XmlRootElement(name = "testOccurrences")
public class TestOccurrences {
  @XmlElement(name = "testOccurrence") public List<TestOccurrence> items;
  @XmlAttribute public long count;
  @XmlAttribute(required = false) @Nullable public String nextHref;
  @XmlAttribute(required = false) @Nullable public String prevHref;

  public TestOccurrences() {
  }

  public TestOccurrences(@NotNull final Collection<STestRun> itemsP, @Nullable final PagerData pagerData, @NotNull final BeanContext beanContext, @NotNull final Fields fields) {
    final List<STestRun> sortedItems = new ArrayList<STestRun>(itemsP);
    Collections.sort(sortedItems, STestRun.NEW_FIRST_NAME_COMPARATOR);
    items = new ArrayList<TestOccurrence>(sortedItems.size());
    for (STestRun item : sortedItems) {
      items.add(new TestOccurrence(item, beanContext, fields.getNestedField("testOccurrence")));
    }
    if (pagerData != null) {
      nextHref = pagerData.getNextHref() != null ? beanContext.getApiUrlBuilder().transformRelativePath(pagerData.getNextHref()) : null;
      prevHref = pagerData.getPrevHref() != null ? beanContext.getApiUrlBuilder().transformRelativePath(pagerData.getPrevHref()) : null;
    }
    count = items.size();
  }
}