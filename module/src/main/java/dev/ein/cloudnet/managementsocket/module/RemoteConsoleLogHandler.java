/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package dev.ein.cloudnet.managementsocket.module;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import lombok.AllArgsConstructor;

import java.util.function.Consumer;

@AllArgsConstructor
public class RemoteConsoleLogHandler extends ConsoleAppender<ILoggingEvent> {
    private Consumer<String> consumer;

  @Override
  protected void append(ILoggingEvent eventObject) {
    consumer.accept(new String(super.encoder.encode(eventObject)));
  }
}
