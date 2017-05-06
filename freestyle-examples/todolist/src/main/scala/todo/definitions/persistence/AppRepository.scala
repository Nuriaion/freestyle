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
package definitions
package persistence

import todo.definitions.models.{Tag, TodoForm, TodoItem, TodoList}
import doobie.imports._
import freestyle._

@free
trait AppRepository {
  def list: FS[List[(TodoList, Tag, TodoItem)]]
}

class H2AppRepositoryHandler extends AppRepository.Handler[ConnectionIO] {
  def list: ConnectionIO[List[(TodoList, Tag, TodoItem)]] =
    sql"""SELECT lists.title, lists.tag_id, lists.id, tags.name, tags.id, items.item, items.todo_list_id, items.completed, items.id FROM todo_lists AS lists INNER JOIN tags ON lists.tag_id = tags.id INNER JOIN todo_items AS items ON lists.id = items.todo_list_id"""
      .query[(TodoList, Tag, TodoItem)]
      .list
}
