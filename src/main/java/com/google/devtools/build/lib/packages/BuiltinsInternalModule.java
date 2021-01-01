// Copyright 2020 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.lib.packages;

import com.google.devtools.build.docgen.annot.DocCategory;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

// TODO(#11437): Factor an API out into starlarkbuildapi, for stardoc's benefit. Otherwise, stardoc
// can't run on @_builtins bzls. Alternatively, wait it out until stardoc's rewritten to be
// integrated into bazel query.
/**
 * The {@code _builtins} Starlark object, visible only to {@code @_builtins} .bzl files, supporting
 * access to internal APIs.
 *
 * <p>Fields:
 *
 * <ul>
 *   <li>{@code _builtins.native}: A view of the {@code native} object as it would exist if builtins
 *       injection were disabled. For example, if builtins injection provides a Starlark definition
 *       for {@code cc_library} in {@code exported_rules}, then {@code native.cc_library} in user
 *       .bzl files would refer to that definition, but {@code _builtins.native.cc_library} would
 *       still be the one defined in Java code. (Note that for clarity and to avoid a conceptual
 *       cycle, the regular top-level {@code native} object is not defined for {@code @_builtins}
 *       .bzl files.)
 *   <li>{@code _builtins.toplevel}: A similar view of the top-level .bzl symbols that would exist
 *       if builtins injection were disabled. For example, if {@code CcInfo} is overridden by {@code
 *       exported_toplevels}, then {@code _builtins.toplevel.CcInfo} is the original Java
 *       definition, not the Starlark one. Note that the regular {@code CcInfo} top-level is not
 *       available to {@code @_builtins} .bzl files.
 *   <li>{@code _builtins.internal}: A view of symbols that were registered (via {@link
 *       ConfiguredRuleClassProvider#addStarlarkBuiltinsInternal}) to be made available here but not
 *       to user .bzl files.
 *   <li>{@code _builtins.get_flag()}: A method that takes a StarlarkSemantics flag name and returns
 *       its Starlark value, which is usually a boolean. Returns the given default if the flag was
 *       not set or does not exist.
 * </ul>
 */
@StarlarkBuiltin(
    name = "_builtins",
    category = DocCategory.BUILTIN,
    documented = false,
    doc =
        "A module accessible only to @_builtins .bzls, that permits access to the original "
            + "(uninjected) native builtins, as well as internal-only symbols not accessible to "
            + "users.")
public class BuiltinsInternalModule implements StarlarkValue {

  // _builtins.native
  private final Object uninjectedNativeObject;
  // _builtins.toplevel
  private final Object uninjectedToplevelObject;
  // _builtins.internal
  private final Object internalObject;

  public BuiltinsInternalModule(
      Object uninjectedNativeObject, Object uninjectedToplevelObject, Object internalObject) {
    this.uninjectedNativeObject = uninjectedNativeObject;
    this.uninjectedToplevelObject = uninjectedToplevelObject;
    this.internalObject = internalObject;
  }

  @Override
  public void repr(Printer printer) {
    printer.append("<_builtins module>");
  }

  @Override
  public boolean isImmutable() {
    return true;
  }

  @StarlarkMethod(
      name = "native",
      doc =
          "A struct containing the <i>native</i> definitions of the symbols that appear in the"
              + " top-level <code>native</code> object, before applying the overrides in"
              + " <code>exported_rules</code>.",
      documented = false,
      structField = true)
  public Object getUninjectedNativeObject() {
    return uninjectedNativeObject;
  }

  @StarlarkMethod(
      name = "toplevel",
      doc =
          "A struct containing the <i>native</i> definitions of the top-level symbols of a normal"
              + " bzl module, before applying the overrides in <code>exported_toplevels</code>.",
      documented = false,
      structField = true)
  public Object getUninjectedToplevelObject() {
    return uninjectedToplevelObject;
  }

  @StarlarkMethod(
      name = "internal",
      doc =
          "A struct containing native symbols explicitly registered to be visible to <code>"
              + "@_builtins</code> code but not necessarily user code.",
      documented = false,
      structField = true)
  public Object getInternalObject() {
    return internalObject;
  }

  @StarlarkMethod(
      name = "get_flag",
      doc =
          "Returns the value of the StarlarkSemantics flag, or a default value if it could not be"
              + " retrieved. Fails if the flag value is not a Starlark value.",
      documented = false,
      parameters = {
        @Param(name = "name", doc = "Name of the flag, without the leading dashes"),
        @Param(
            name = "default",
            doc =
                "Value to return if flag was not set or does not exist. This should generally be"
                    + " the same as the flag's default value.")
      },
      useStarlarkThread = true)
  public Object getFlag(String name, Object defaultValue, StarlarkThread thread) {
    Object value = thread.getSemantics().getGeneric(name, defaultValue);
    return Starlark.fromJava(value, thread.mutability());
  }
}
