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

import de.dytanic.cloudnet.console.IConsole;
import de.dytanic.cloudnet.console.JLine3Console;
import dev.ein.cloudnet.managementsocket.shared.command.Response;
import dev.ein.cloudnet.managementsocket.shared.command.Util;
import org.apache.commons.cli.*;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import java.io.File;
import java.io.ObjectOutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class CommandLineInterface {
  public static void main(String[] args) throws Exception {
    IConsole console = new JLine3Console();
    CommandLineParser parser = new DefaultParser();
    Options options = new Options();

    Option socketFilename = new Option("s", "socket", true, "Socket file to use");
    socketFilename.setRequired(false);
    socketFilename.setType(File.class);

    Option showHelp = new Option("h", "help", false, "Show this help");
    showHelp.setRequired(false);

    options.addOption(socketFilename);
    ExecutorService socketExec = Executors.newFixedThreadPool(1);
    try {
      CommandLine commandLine = parser.parse(options, args);
      File socketFile = new File(commandLine.getOptionValue("socket", "./control.socket"));
      try (AFUNIXSocket socket = AFUNIXSocket.newInstance()) {
        socket.connect(AFUNIXSocketAddress.of(socketFile), 5000);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        LinkedBlockingQueue<Response> responseQueue = new LinkedBlockingQueue<>();
        ConsoleHandler consoleHandler = new ConsoleHandler(socketExec, out, responseQueue, console);
        new ResponseConsoleWriterThread(socket, console, responseQueue, consoleHandler.getConsoleStopIssued()).start();
        console.addCommandHandler(UUID.randomUUID(), consoleHandler::commandHandler);
        console.addTabCompletionHandler(UUID.randomUUID(), consoleHandler::tabCompletionHandler);
        consoleHandler.waitUntilStop();
        console.close();
      }
    } catch (Exception e) {
      console.writeLine("[ERROR] Unable to communicate via socket!");
      console.writeLine(Util.getStackTrace(e));
      System.exit(-1);
    }
  }


}
