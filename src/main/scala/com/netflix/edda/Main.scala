/*
 * Copyright 2019 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.edda

import com.google.inject.AbstractModule
import com.netflix.iep.config.ConfigManager
import com.netflix.spectator.api.NoopRegistry
import com.netflix.spectator.api.Registry
import com.typesafe.config.Config

object Main {

  def main(args: Array[String]): Unit = {
    if (System.getenv("NETFLIX_ENVIRONMENT") == null) {
      val localModule = new AbstractModule {
        override def configure(): Unit = {
          bind(classOf[Config]).toInstance(ConfigManager.load())
          bind(classOf[Registry]).toInstance(new NoopRegistry)
        }
      }
      com.netflix.iep.guice.Main.run(args, new AppModule, localModule)
    } else {
      com.netflix.iep.guice.Main.run(args, new AppModule)
    }
  }
}
