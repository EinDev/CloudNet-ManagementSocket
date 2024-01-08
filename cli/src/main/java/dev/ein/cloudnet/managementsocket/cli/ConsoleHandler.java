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

package dev.ein.cloudnet.managementsocket.cli;

import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.console.IConsole;
import dev.ein.cloudnet.managementsocket.shared.command.Response;
import dev.ein.cloudnet.managementsocket.shared.command.Util;
import dev.ein.cloudnet.managementsocket.shared.command.commands.CommandExecutedResponse;
import dev.ein.cloudnet.managementsocket.shared.command.commands.TabCompletionRequest;
import dev.ein.cloudnet.managementsocket.shared.command.commands.TabCompletionResponse;
import dev.ein.cloudnet.managementsocket.shared.command.commands.TextBasedRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor
public class ConsoleHandler {
  @Getter
  private final Lock consoleStopIssued = new ReentrantLock();
  private final ExecutorService socketExec;
  private final ObjectOutputStream out;
  private final LinkedBlockingQueue<Response> responseQueue;
  private final IConsole console;

  public void commandHandler(String commandLine) {
    if (commandLine.equals("exit") || commandLine.equals("stop") || commandLine.equals("shutdown")) {
      synchronized (consoleStopIssued) {
        consoleStopIssued.notify();
      }
      return;
    }
    socketExec.execute(() -> {
      try {
        handleCommand(commandLine);
      } catch (InterruptedException e) {
        console.writeLine(String.format("[ERROR] Caught exception while handling command '%s'", commandLine));
        console.writeLine(Util.getStackTrace(e));
      }
    });
  }

  private void handleCommand(String s) throws InterruptedException {
    try {
      out.writeObject(new TextBasedRequest(s));
      out.flush();
      Object result = responseQueue.take();
      if (!(result instanceof CommandExecutedResponse)) {
        console.writeLine(String.format("[ERROR] Got invalid class as a result: %s", result.getClass().getName()));
      }
    } catch (IOException e) {
      console.writeLine("[ERROR] An error occured sending the command");
      console.write(Util.getStackTrace(e));
    }
  }

  public Collection<String> tabCompletionHandler(String commandLine, String[] args, Properties properties) {
    try {
      return socketExec.submit(() -> handleTabComplete(commandLine)).get();
    } catch (InterruptedException | ExecutionException e) {
      console.writeLine("[ERROR] Unable to get tab completions");
      console.writeLine(Util.getStackTrace(e));
      return Collections.emptyList();
    }
  }

  private Collection<String> handleTabComplete(String s) throws InterruptedException {
    try {
      out.writeObject(new TabCompletionRequest(s));
      out.flush();
      Object result = responseQueue.take();
      if (result instanceof TabCompletionResponse) {
        String[] answer = ((TabCompletionResponse) result).getTabCompletions();
        return Arrays.asList(answer);
      } else {
        console.writeLine(String.format("[ERROR] Got invalid class as a result: %s", result.getClass().getName()));
      }
    } catch (IOException e) {
      console.writeLine("[ERROR] An error occured sending the command");
      console.write(Util.getStackTrace(e));
    }
    return Collections.emptyList();
  }

  public void waitUntilStop() throws InterruptedException {
    synchronized (consoleStopIssued) {
      consoleStopIssued.wait();
    }
  }
}
