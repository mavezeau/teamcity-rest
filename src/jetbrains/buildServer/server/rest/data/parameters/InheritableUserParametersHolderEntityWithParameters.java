/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package jetbrains.buildServer.server.rest.data.parameters;

import java.util.Collection;
import jetbrains.buildServer.serverSide.InheritableUserParametersHolder;
import jetbrains.buildServer.serverSide.Parameter;
import org.jetbrains.annotations.NotNull;

/**
 * @author Yegor.Yarko
 *         Date: 07/07/2016
 */
public class InheritableUserParametersHolderEntityWithParameters extends UserParametersHolderEntityWithParameters {
  @NotNull private final InheritableUserParametersHolder myEntity;

  public InheritableUserParametersHolderEntityWithParameters(@NotNull final InheritableUserParametersHolder entity) {
    super(entity);
    myEntity = entity;
  }

  @NotNull
  public Collection<Parameter> getOwnParametersCollection() {
    return myEntity.getOwnParametersCollection();
  }
}