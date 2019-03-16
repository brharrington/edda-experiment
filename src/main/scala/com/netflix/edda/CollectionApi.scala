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

import java.time.Duration
import java.time.Instant

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers
import akka.http.scaladsl.server.Route
import com.netflix.atlas.akka.CustomDirectives._
import com.netflix.atlas.akka.DiagnosticMessage
import com.netflix.atlas.akka.WebApi
import com.netflix.atlas.json.Json

class CollectionApi(mgr: CollectionManager) extends WebApi {

  override def routes: Route = {
    // On root path show a basic summary view with the available collections and the age
    // of the data for that collection.
    val root = pathEndOrSingleSlash {
      get {
        extractUri { uri =>
          val baseUri = s"${uri.scheme}://${uri.authority.toString()}/api"
          val data = mgr.getAll.mapValues { rs =>
            val now = Instant.now()
            val age = Duration.between(Instant.ofEpochMilli(rs.updateTime), now)
            Map(
              "link" -> s"$baseUri/${rs.path}",
              "age"  -> age.toString
            )
          }
          val payload = Json.encode(data)
          complete(HttpResponse(StatusCodes.OK, entity = payload))
        }
      }
    }

    // Generate routes for each collection
    mgr.paths.foldLeft(root) { (acc, collectionPath) =>
      val pm = PathMatchers.separateOnSlashes(s"api/$collectionPath")

      // If no id is provided, then generate a listing of all resources
      val listingRoute = endpointPath(pm) {
        get {
          parameters("expand".?) { expand =>
            val response = mgr.get(collectionPath)
              .map { rs =>
                val payload = expand
                  .map { _ =>
                    Json.encode(rs.resources.values.map(_.data))
                  }
                  .getOrElse {
                    Json.encode(rs.resources.values.map(_.id))
                  }
                HttpResponse(StatusCodes.OK, entity = payload)
              }
              .getOrElse {
                DiagnosticMessage.error(StatusCodes.NotFound, collectionPath)
              }
            complete(response)
          }
        }
      }

      // If the id is provided, then lookup that specific resource
      val idRoute = endpointPath(pm, RemainingPath) { id =>
        get {
          val response = mgr.get(collectionPath)
            .flatMap(_.resources.get(id.toString()))
            .map { resource =>
              val payload = Json.encode(resource.data)
              HttpResponse(StatusCodes.OK, entity = payload)
            }
            .getOrElse {
              DiagnosticMessage.error(StatusCodes.NotFound, collectionPath)
            }
          complete(response)
        }
      }
      acc ~ listingRoute ~ idRoute
    }
  }
}
