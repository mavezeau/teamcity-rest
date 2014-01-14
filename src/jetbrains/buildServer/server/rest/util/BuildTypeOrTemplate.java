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

package jetbrains.buildServer.server.rest.util;

import jetbrains.buildServer.log.Loggable;
import jetbrains.buildServer.server.rest.data.DataProvider;
import jetbrains.buildServer.server.rest.errors.BadRequestException;
import jetbrains.buildServer.server.rest.errors.NotFoundException;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.impl.LogUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Yegor.Yarko
 *         Date: 14.01.12
 */
public class BuildTypeOrTemplate implements Loggable {
  final private SBuildType myBuildType;
  final private BuildTypeTemplate myTemplate;
  final private BuildTypeIdentity myBuildTypeIdentity;
  final private  boolean hasBuildType;

  public BuildTypeOrTemplate(@NotNull SBuildType buildType) {
    myBuildType = buildType;
    myTemplate = null;
    myBuildTypeIdentity = buildType;
    hasBuildType = true;
  }

  public BuildTypeOrTemplate(@NotNull BuildTypeTemplate template) {
    myTemplate = template;
    myBuildType = null;
    myBuildTypeIdentity = template;
    hasBuildType = false;
  }

  @NotNull
  public BuildTypeSettings get(){
    return hasBuildType ? myBuildType : myTemplate;
  }

  @NotNull
  public String getId(){
    return myBuildTypeIdentity.getExternalId();
  }

  @NotNull
  public String getInternalId(){
    return myBuildTypeIdentity.getInternalId();
  }

  public String getName(){
    return myBuildTypeIdentity.getName();
  }

  @NotNull
  public SProject getProject() {
    return myBuildTypeIdentity.getProject();
  }

  public boolean isBuildType() {
    return hasBuildType;
  }

  public boolean isTemplate() {
    return !hasBuildType;
  }

  public SBuildType getBuildType() {
    return myBuildType;
  }

  public BuildTypeTemplate getTemplate() {
    return myTemplate;
  }

  @Nullable
  public String getDescription() {
    return hasBuildType ? myBuildType.getDescription() : null;
  }

  @Nullable
  public Boolean isPaused() {
    return hasBuildType ? myBuildType.isPaused() : null;
  }

  @NotNull
  public String getText() {
    return hasBuildType ? "Build type": "Template";
  }

  public void setName(@NotNull final String value) {
    if (hasBuildType){
      myBuildType.setName(value);
    }else{
      myTemplate.setName(value);
    }
  }

  public void setDescription(@Nullable final String value) {
    if (hasBuildType){
      myBuildType.setDescription(value);
    }else{
      throw new BadRequestException("Template does not have description field");
    }
  }

  public void remove() {
    myBuildTypeIdentity.remove();
  }

  public void setFieldValue(@NotNull final String field, @Nullable final String value, @NotNull final DataProvider dataProvider) {
    if ("id".equals(field)) {
      if (value != null){
        myBuildTypeIdentity.setExternalId(value);
      }else{
        throw new BadRequestException("Id cannot be empty");
      }
      return;
    } else if ("name".equals(field)) {
      if (value != null){
        setName(value);
      }else{
        throw new BadRequestException("Name cannot be empty");
      }
      return;
    } else if ("description".equals(field)) {
      setDescription(value);
      return;
    }
    if (isBuildType()){
      if ("paused".equals(field)){
        myBuildType.setPaused(Boolean.valueOf(value), dataProvider.getCurrentUser(), TeamCityProperties.getProperty("rest.defaultActionComment"));
        //todo (TeamCity) why not use current user by default?
        return;
      }
    }

    throw new BadRequestException("Setting field '" + field + "' is not supported. Supported are: name, description, paused");
  }

  @Nullable
  public String getFieldValue(final String field) {
    if ("id".equals(field)) {
      return getId();
    } else if ("internalId".equals(field)) {
      return getInternalId();
    } else if ("description".equals(field)) {
      return getDescription();
    } else if ("name".equals(field)) {
      return getName();
    }
    if (isBuildType()){
      if ("paused".equals(field)){
        return String.valueOf(myBuildType.isPaused());
      } else if ("status".equals(field)){ //Experimental support
        return  myBuildType.getStatus().getText();
      }
    }
    throw new NotFoundException("Field '" + field + "' is not supported. Supported are: id, name, description, paused, internalId.");
  }

  public boolean isEnabled(final String id) {
    return hasBuildType ? myBuildType.isEnabled(id) : myTemplate.isEnabled(id);
  }

  @NotNull
  public String describe(final boolean verbose) {
      return hasBuildType ? LogUtil.describe(myBuildType) : LogUtil.describe(myTemplate);
  }
}

