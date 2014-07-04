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
package com.google.devtools.build.lib.blaze;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.util.OptionsUtils;
import com.google.devtools.build.lib.util.SkyframeMode;
import com.google.devtools.build.lib.vfs.PathFragment;
import com.google.devtools.common.options.Converter;
import com.google.devtools.common.options.EnumConverter;
import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionsBase;
import com.google.devtools.common.options.OptionsParsingException;

import java.util.Map;

/**
 * Options that will be evaluated by the blaze client startup code and passed
 * to the blaze server upon startup.
 *
 * <h4>IMPORTANT</h4> These options and their defaults must be kept in sync with those in the
 * source of the launcher.  The latter define the actual default values; this class exists only to
 * provide the help message, which displays the default values.
 *
 * The same relationship holds between {@link HostJvmStartupOptions} and the launcher.
 */
public class BlazeServerStartupOptions extends OptionsBase {
  /**
   * Converter for the <code>option_sources</code> option. Takes a string in the form of
   * "option_name1:source1:option_name2:source2:.." and converts it into an option name to
   * source map.
   */
  public static class OptionSourcesConverter implements Converter<Map<String, String>> {
    private String unescape(String input) {
      return input.replace("_C", ":").replace("_U", "_");
    }

    @Override
    public Map<String, String> convert(String input) {
      ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
      if (input.isEmpty()) {
        return builder.build();
      }

      String[] elements = input.split(":");
      for (int i = 0; i < (elements.length + 1) / 2; i++) {
        String name = elements[i * 2];
        String value = "";
        if (elements.length > i * 2 + 1) {
          value = elements[i * 2 + 1];
        }
        builder.put(unescape(name), unescape(value));
      }
      return builder.build();
    }

    @Override
    public String getTypeDescription() {
      return "a list of option-source pairs";
    }
  }

  /** Converter for the {@link #skyframe} option. */
  // TODO(bazel-team): Turn this back into a normal EnumConverter when experimental_full is gone.
  public static class SkyframeModeConverter implements Converter<SkyframeMode> {
    private final EnumConverter<SkyframeMode> enumConverter;
    public SkyframeModeConverter() {
      enumConverter = new EnumConverter<SkyframeMode>(SkyframeMode.class, "Skyframe mode") {};
    }

    @Override
    public SkyframeMode convert(String input) throws OptionsParsingException {
      SkyframeMode result = enumConverter.convert(input);
      return result == SkyframeMode.EXPERIMENTAL_FULL ? SkyframeMode.FULL : result;
    }

    @Override
    public String getTypeDescription() {
      return enumConverter.getTypeDescription();
    }

  }

  /* Passed from the client to the server, specifies the installation
   * location. The location should be of the form:
   * $OUTPUT_BASE/_blaze_${USER}/install/${MD5_OF_INSTALL_MANIFEST}.
   * The server code will only accept a non-empty path; it's the
   * responsibility of the client to compute a proper default if
   * necessary.
   */
  @Option(name = "install_base",
      defaultValue = "", // NOTE: purely decorative!  See class docstring.
      category = "hidden",
      converter = OptionsUtils.PathFragmentConverter.class,
      help = "This launcher option is intended for use only by tests.")
  public PathFragment installBase;

  /* Note: The help string in this option applies to the client code; not
   * the server code. The server code will only accept a non-empty path; it's
   * the responsibility of the client to compute a proper default if
   * necessary.
   */
  @Option(name = "output_base",
      defaultValue = "null", // NOTE: purely decorative!  See class docstring.
      category = "server startup",
      converter = OptionsUtils.PathFragmentConverter.class,
      help = "If set, specifies the output location to which all build output will be written. "
          + "Otherwise, the location will be "
          + "${OUTPUT_ROOT}/_blaze_${USER}/${MD5_OF_WORKSPACE_ROOT}. Note: If you specify a "
          + "different option from one to the next Blaze invocation for this value, you'll likely "
          + "start up a new, additional Blaze server. Blaze starts exactly one server per "
          + "specified output base. Typically there is one output base per google3 "
          + "workspace--however, with this option you may have multiple output bases per google3 "
          + "workspace and thereby run multiple builds for the same client on the same machine "
          + "concurrently. See 'blaze help shutdown' on how to shutdown a Blaze server.")
  public PathFragment outputBase;

  /* Note: This option is only used by the C++ client, never by the Java server.
   * It is included here to make sure that the option is documented in the help
   * output, which is auto-generated by Java code.
   */
  @Option(name = "output_user_root",
      defaultValue = "null", // NOTE: purely decorative!  See class docstring.
      category = "server startup",
      converter = OptionsUtils.PathFragmentConverter.class,
      help = "The user-specific directory beneath which all build outputs are written; "
          + "by default, this is a function of $USER, but by specifying a constant, build outputs "
          + "can be shared between collaborating users.  "
          + "See http://wiki/Main/BlazeOutputDirectoryStructure or the manual for more details.")
  public PathFragment outputUserRoot;

  @Option(name = "workspace_directory",
      defaultValue = "",
      category = "server startup",
      converter = OptionsUtils.PathFragmentConverter.class,
      help = "The root of the workspace, that is, the directory that Blaze uses as the root of the "
          + "build.")
  public PathFragment workspaceDirectory;

  @Option(name = "max_idle_secs",
      defaultValue = "" + (3 * 3600), // NOTE: purely decorative!  See class docstring.
      category = "server startup",
      help = "The number of seconds the build server will wait idling " +
             "before shutting down. Note: Blaze will ignore this option " +
             "unless you are starting a new instance. See also 'blaze help " +
             "shutdown'.")
  public int maxIdleSeconds;

  @Option(name = "batch",
      defaultValue = "false", // NOTE: purely decorative!  See class docstring.
      category = "server startup",
      help = "If set, Blaze will be run in batch mode, instead of " +
             "the standard client/server. Doing so may provide " +
             "more predictable semantics with respect to signal handling and job control, " +
             "Batch mode retains proper queueing semantics within the same output_base. " +
             "That is, simultaneous invocations will be processed in order, without overlap. " +
             "If a batch mode Blaze is run on a client with a running server, it first kills "  +
             "the server before processing the command." +
             "Blaze will run slower in batch mode, compared to client/server mode. " +
             "Among other things, the build file cache is memory-resident, so it is not " +
             "preserved between sequential batch invocations. Therefore, using batch mode " +
             "often makes more sense in cases where performance is less critical, " +
             "such as continuous builds.")
  public boolean batch;

  @Option(name = "block_for_lock",
      defaultValue = "true", // NOTE: purely decorative!  See class docstring.
      category = "server startup",
      help = "If set, Blaze will exit immediately instead of waiting for other " +
             "Blaze commands holding the server lock to complete.")
  public boolean noblock_for_lock;

  @Option(name = "io_nice_level",
      defaultValue = "-1",  // NOTE: purely decorative!
      category = "server startup",
      help = "Set a level from 0-7 for best-effort IO scheduling. 0 is highest priority, " +
             "7 is lowest. The anticipatory scheduler may only honor up to priority 4. " +
             "Negative values are ignored.")
  public int ioNiceLevel;

  @Option(name = "batch_cpu_scheduling",
      defaultValue = "false",  // NOTE: purely decorative!
      category = "server startup",
      help = "Use 'batch' CPU scheduling for Blaze. This policy is useful for workloads that " +
             "are non-interactive, but do not want to lower their nice value. " +
             "See 'man 2 sched_setscheduler'.")
  public boolean batchCpuScheduling;

  @Option(name = "blazerc",
      defaultValue = "./.blazerc, then ~/.blazerc",  // NOTE: purely decorative!
      category = "misc",
      help = "The location of the .blazerc file containing default values of "
          + "Blaze command options.  Use /dev/null to disable the search for a "
          + "blazerc file, e.g. in release builds.")
  public String blazerc;

  @Option(name = "master_blazerc",
      defaultValue = "true",  // NOTE: purely decorative!
      category = "misc",
      help = "If this option is false, the master blazerc next to the binary "
          + "is not read.")
  public boolean masterBlazerc;

  @Option(name = "blaze_cpu",
      defaultValue = "piii",
      category = "server startup",
      help = "Determines whether to use a 32-bit or 64-bin blaze binary. 'blaze --blaze_cpu=k8' "
          + "will switch to 'blaze64', and 'blaze64 --blaze_cpu=pii' will switch to 'blaze'. "
          + "This only works if the 'blaze' and 'blaze64' binaries are located next to each "
          + "other in the same directory. The Blaze release process checks in two related "
          + "binaries under //tools:blaze and //tools:blaze64, but custom Blaze binaries don't "
          + "necessarily come in pairs. If the other binary is not found, a warning is printed.")
  public String blazeCpu;

  @Option(name = "skyframe",
      converter = SkyframeModeConverter.class,
      defaultValue = "loading_and_analysis",
      category = "server startup",
      help = "Use Skyframe to drive the build. --skyframe=experimental_full will use skyframe for "
          + "all phases of the build, while --skyframe=loading_and_analysis will use "
          + "skyframe just for loading and analysis, and the legacy codepath for execution.")
  public SkyframeMode skyframe;

  @Option(name = "allow_configurable_attributes",
      defaultValue = "false",  // NOTE: purely decorative!
      category = "undocumented",
      help = "Whether or not to allow configurable attribute syntax in BUILD files. Experimental.")
  public boolean allowConfigurableAttributes;

  @Option(name = "fatal_event_bus_exceptions",
      defaultValue = "false",  // NOTE: purely decorative!
      category = "undocumented",
      help = "Whether or not to allow EventBus exceptions to be fatal. Experimental.")
  public boolean fatalEventBusExceptions;

  @Option(name = "option_sources",
      converter = OptionSourcesConverter.class,
      defaultValue = "",
      category = "hidden",
      help = "")
  public Map<String, String> optionSources;
}