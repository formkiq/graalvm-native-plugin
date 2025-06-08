/**
 * Copyright [2020] FormKiQ Inc. Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may obtain a copy of the License
 * at
 *
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.formkiq.gradle;

import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.api.services.BuildServiceParameters.None;

/** Graalvm {@link BuildService}. */
public class GraalvmBuildService implements BuildService<BuildServiceParameters.None> {

  @Override
  public None getParameters() {
    return null;
  }
}
