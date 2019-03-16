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

import com.fasterxml.jackson.databind.JsonNode

/**
  * Resource within a collection.
  *
  * @param id
  *     Identifier used to lookup a particular resource in the collection. It must be unique
  *     for all resources within a given collection.
  * @param data
  *     Raw data for the resource.
  */
case class Resource(id: String, data: JsonNode)
