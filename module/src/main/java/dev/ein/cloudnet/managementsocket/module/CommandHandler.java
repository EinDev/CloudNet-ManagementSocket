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

import dev.ein.cloudnet.managementsocket.shared.command.Request;
import dev.ein.cloudnet.managementsocket.shared.command.Response;
import dev.ein.cloudnet.managementsocket.shared.command.commands.*;
import eu.cloudnetservice.driver.provider.ClusterNodeProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Singleton
public class CommandHandler {
  private final ClusterNodeProvider clusterNodeProvider;
  protected static final Logger LOGGER = LoggerFactory.getLogger(CommandHandler.class);

  @Inject
  public CommandHandler(
    @NonNull ClusterNodeProvider clusterNodeProvider
  ) {
    this.clusterNodeProvider = clusterNodeProvider;
  }

  public Response handleCommand(Request c) throws ExecutionException, InterruptedException {
        if(c instanceof TextBasedRequest) {
            String command = ((TextBasedRequest) c).getCommand();
          CompletableFuture<String[]> response = clusterNodeProvider.consoleCommandAsync(command).thenApply(info -> {
            if (info == null) {
              return new String[]{"Command not found"};
            }
            return this.clusterNodeProvider.sendCommandLine(command).toArray(new String[0]);
          });
          return new CommandExecutedResponse(response.get());
        } else if (c instanceof TabCompletionRequest) {
            String command = ((TabCompletionRequest) c).getCommand();
          Collection<String> response = clusterNodeProvider.consoleTabCompleteResults(command);
            return new TabCompletionResponse(response.toArray(new String[0]));
        } else {
          LOGGER.warn("Got unknown command: {}", c.getClass().getName());
          return new ErrorResult("Unknown command");
        }
    }
}
