// Copyright 2014 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.bazel.rules.genrule;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.actions.ActionExecutionContext;
import com.google.devtools.build.lib.actions.ActionOwner;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.actions.ExecException;
import com.google.devtools.build.lib.actions.ResourceSet;
import com.google.devtools.build.lib.events.ErrorEventListener;
import com.google.devtools.build.lib.vfs.PathFragment;
import com.google.devtools.build.lib.view.actions.CommandLine;
import com.google.devtools.build.lib.view.actions.SpawnAction;
import com.google.devtools.build.lib.view.config.BuildConfiguration;

import java.util.List;

/**
 * A spawn action for genrules. Genrules are handled specially in that inputs and outputs are
 * checked for directories.
 */
public final class GenRuleAction extends SpawnAction {

  private static final ResourceSet GENRULE_RESOURCES =
      // Not chosen scientifically/carefully.  300MB memory, 100% CPU, 20% of total I/O.
      new ResourceSet(300, 1.0, 0.2);

  public GenRuleAction(ActionOwner owner,
      Iterable<Artifact> inputs,
      Iterable<Artifact> outputs,
      BuildConfiguration configuration,
      List<String> argv,
      ImmutableMap<String, String> environment,
      ImmutableMap<String, String> executionInfo,
      ImmutableMap<PathFragment, Artifact> runfilesManifests,
      String progressMessage) {
    super(owner, inputs, outputs, configuration, GENRULE_RESOURCES,
        CommandLine.of(argv, false), environment, executionInfo, progressMessage,
        runfilesManifests,
        "Genrule", null);
  }

  @Override
  protected void internalExecute(
      ActionExecutionContext actionExecutionContext) throws ExecException {
    ErrorEventListener reporter = actionExecutionContext.getExecutor().getReporter();
    checkInputsForDirectories(reporter);
    super.internalExecute(actionExecutionContext);
    checkOutputsForDirectories(reporter);
  }
}