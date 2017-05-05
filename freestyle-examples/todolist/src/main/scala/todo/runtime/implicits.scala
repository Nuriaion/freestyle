/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
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

package todo
package runtime

import cats.~>
import com.twitter.util.{Future, Promise}
import doobie.imports._
import fs2.Task
import fs2.interop.cats._
import todo.definitions.persistence.{H2TodoItemRepositoryHandler, TodoItemRepository}
import todo.definitions.persistence.{H2TodoListRepositoryHandler, TodoListRepository}
import todo.definitions.persistence.{H2TagRepositoryHandler, TagRepository}
import todo.definitions.TodoApp

import freestyle.implicits._
import freestyle.doobie.implicits._

import freestyle.logging._
import freestyle.loggingJVM.implicits._

import freestyle.config._
import freestyle.config.implicits._
import freestyle.effects.error._
import freestyle.effects.error.implicits._

object implicits {

  implicit val xa = DriverManagerTransactor[Task](
    "org.h2.Driver",
    "jdbc:h2:mem:freestyle-todo;DB_CLOSE_DELAY=-1",
    "sa",
    ""
  )

  implicit val todoItemRepositoryHandler: TodoItemRepository.Handler[ConnectionIO] =
    new H2TodoItemRepositoryHandler

  implicit val todoListRepositoryHandler: TodoListRepository.Handler[ConnectionIO] =
    new H2TodoListRepositoryHandler

  implicit val tagRepositoryHandler: TagRepository.Handler[ConnectionIO] =
    new H2TagRepositoryHandler

  implicit val connectionIO2Task: ConnectionIO ~> Task =
    λ[ConnectionIO ~> Task](_.transact(xa))

  implicit val task2Future: Task ~> Future = new (Task ~> Future) {
    override def apply[A](fa: Task[A]): Future[A] = {
      val promise = new Promise[A]()
      fa.unsafeRunAsync(_.fold(promise.setException, promise.setValue))
      promise
    }
  }

  implicit val todoItemRepoTaskHandler: TodoItemRepository.Op ~> Task =
    todoItemRepositoryHandler andThen connectionIO2Task

  implicit val todoListRepoTaskHandler: TodoListRepository.Op ~> Task =
    todoListRepositoryHandler andThen connectionIO2Task

  implicit val tagRepoTaskHandler: TagRepository.Op ~> Task =
    tagRepositoryHandler andThen connectionIO2Task

  implicit val futureHandler: TodoApp.Op ~> Future =
    implicitly[TodoApp.Op ~> Task] andThen task2Future

}
