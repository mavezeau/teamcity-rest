/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package jetbrains.buildServer.server.rest.model.files;

import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import jetbrains.buildServer.server.rest.model.Fields;
import jetbrains.buildServer.server.rest.util.BeanContext;
import jetbrains.buildServer.server.rest.util.ValueWithDefault;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.Converter;
import jetbrains.buildServer.util.browser.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vladislav.Rassokhin
 * @since 8.0
 */
@SuppressWarnings({"UnusedDeclaration", "PublicField"})
@XmlRootElement(name = "files")
@XmlType
public class Files {

  @XmlAttribute public Integer count;
  @XmlAttribute(name = "href") public String href;

  public static final String FILE = "file";
  @Nullable
  @XmlElement(name = FILE)
  public List<File> files;

  public Files() {
  }

  public Files(@Nullable final String shortHref, @Nullable final ValueWithDefault.Value<List<? extends Element>> children, @NotNull final FileApiUrlBuilder builder,
               @NotNull final Fields fields, @NotNull final BeanContext beanContext) {
    href = shortHref == null ? null : ValueWithDefault.decideDefault(fields.isIncluded("href", true), beanContext.getApiUrlBuilder().transformRelativePath(shortHref));
    files = children == null ? null : ValueWithDefault.decideDefault(fields.isIncluded(FILE, false, true), new ValueWithDefault.Value<List<File>>() {
      @Nullable
      public List<File> get() {
        return CollectionsUtil.convertCollection(children.get(), new Converter<File, Element>() {
          public File createFrom(@NotNull final Element source) {
            return new File(source, null, builder, fields.getNestedField(FILE, Fields.SHORT, Fields.LONG), beanContext);
          }
        });
      }
    });

    final boolean countIsCheap = files != null;
    count = children == null ? null : ValueWithDefault.decideIncludeByDefault(fields.isIncluded("count", countIsCheap, countIsCheap), new ValueWithDefault.Value<Integer>() {
      @Nullable
      public Integer get() {
        if (files != null) {
          return ((Integer)files.size());
        } else {
          // files are not included, but count is requested
          final List<? extends Element> elements = children.get();
          return elements == null ? null : elements.size();
        }
      }
    });
  }
}
