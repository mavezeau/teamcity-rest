/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

package jetbrains.buildServer.server.rest.model.change;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import jetbrains.buildServer.server.rest.ApiUrlBuilder;
import jetbrains.buildServer.server.rest.model.Properties;
import org.jetbrains.annotations.NotNull;

/**
 * @author Yegor.Yarko
 *         Date: 16.04.2009
 */
@XmlRootElement(name = "vcs-root")
public class VcsRoot {
  @XmlAttribute
  public Long id;

  @XmlAttribute
  public String name;

  @XmlAttribute
  public String vcsName;

  @XmlAttribute
  public long version;

  @XmlElement
  public Properties properties;

  public VcsRoot() {
  }

  public VcsRoot(jetbrains.buildServer.vcs.VcsRoot root) {
    id = root.getId();
    name = root.getName();
    vcsName = root.getVcsName();
    version = root.getRootVersion();
    properties = new Properties(root.getProperties());
  }

  /**
   * @author Yegor.Yarko
   *         Date: 16.04.2009
   */
  @XmlRootElement(name = "vcs-root")
  public static class VcsRootRef {
    @XmlAttribute
    public String name;
    @XmlAttribute
    public String href;

    public VcsRootRef() {
    }

    public VcsRootRef(jetbrains.buildServer.vcs.VcsRoot root, @NotNull final ApiUrlBuilder apiUrlBuilder) {
      this.href = apiUrlBuilder.getHref(root);
      this.name = root.getName();
    }
  }
}

