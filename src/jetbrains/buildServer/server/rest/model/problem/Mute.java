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

package jetbrains.buildServer.server.rest.model.problem;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import jetbrains.buildServer.server.rest.model.Comment;
import jetbrains.buildServer.server.rest.model.Fields;
import jetbrains.buildServer.server.rest.model.buildType.ProblemScope;
import jetbrains.buildServer.server.rest.model.buildType.ProblemTarget;
import jetbrains.buildServer.server.rest.util.BeanContext;
import jetbrains.buildServer.server.rest.util.ValueWithDefault;
import jetbrains.buildServer.serverSide.mute.MuteInfo;
import org.jetbrains.annotations.NotNull;

/**
 * @author Yegor.Yarko
 *         Date: 11.02.12
 */
@SuppressWarnings("PublicField")
@XmlType(name = "mute")
public class Mute {
  @XmlAttribute public Integer id;

  @XmlElement public Comment assignment;
  @XmlElement public ProblemScope scope;
  @XmlElement public ProblemTarget target;
  @XmlElement public Resolution resolution;

  public Mute() {
  }

  public Mute(final @NotNull MuteInfo item, @NotNull final Fields fields, final @NotNull BeanContext beanContext) {
    id = ValueWithDefault.decideDefault(fields.isIncluded("id"), item.getId());

    assignment = ValueWithDefault.decideDefault(fields.isIncluded("assignment", false), new ValueWithDefault.Value<Comment>() {
      public Comment get() {
        return new Comment(item.getMutingUser(), item.getMutingTime(), item.getMutingComment(), fields.getNestedField("assignment", Fields.NONE, Fields.LONG), beanContext);
      }
    });

    scope = ValueWithDefault.decideDefault(fields.isIncluded("scope", false), new ValueWithDefault.Value<ProblemScope>() {
      public ProblemScope get() {
        return new ProblemScope(item.getScope(), fields.getNestedField("scope", Fields.NONE, Fields.LONG), beanContext);
      }
    });
    target = ValueWithDefault.decideDefault(fields.isIncluded("target", false), new ValueWithDefault.Value<ProblemTarget>() {
      public ProblemTarget get() {
        return new ProblemTarget(item, fields.getNestedField("target", Fields.NONE, Fields.LONG), beanContext);
      }
    });
    resolution = ValueWithDefault.decideDefault(fields.isIncluded("resolution", false), new ValueWithDefault.Value<Resolution>() {
      public Resolution get() {
        return new Resolution(item.getAutoUnmuteOptions(), fields.getNestedField("resolution", Fields.NONE, Fields.LONG));
      }
    });
  }
}
