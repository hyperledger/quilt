package org.interledger.quilt.jackson.sharedsecret;

/*-
 * ========================LICENSE_START=================================
 * Interledger Jackson Datatypes
 * %%
 * Copyright (C) 2017 - 2018 Hyperledger and its contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

import org.interledger.core.SharedSecret;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * A Jackson {@link SimpleModule} for serializing and deserializing {@link SharedSecret} objects.
 *
 * @deprecated This class will go away once {@link SharedSecret} is removed from the project.
 */
@Deprecated
public class SharedSecretModule extends SimpleModule {

  private static final String NAME = SharedSecretModule.class.getName();

  /**
   * No-args Constructor.
   */
  public SharedSecretModule() {

    super(
      NAME,
      new Version(1, 0, 0, null, "org.interledger", "jackson-datatype-shared-secret")
    );

    addSerializer(SharedSecret.class, SharedSecretSerializer.INSTANCE);
    addDeserializer(SharedSecret.class, SharedSecretDeserializer.INSTANCE);
  }

}
