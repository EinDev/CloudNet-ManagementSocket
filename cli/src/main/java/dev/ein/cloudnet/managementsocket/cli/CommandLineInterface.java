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
import dev.ein.cloudnet.managementsocket.shared.command.commands.*;
import org.apache.commons.cli.*;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CommandLineInterface {
    public static void main(String[] args) throws Exception {
        IConsole console = new JLine3Console();
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        Option socketFilename = new Option("s", "socket", true, "Socket file to use");
        socketFilename.setRequired(false);
        options.addOption(socketFilename);
        Lock consoleStopIssued = new ReentrantLock();
        ExecutorService socketExec = Executors.newFixedThreadPool(1);
        try {
            CommandLine commandLine = parser.parse(options, args);
            File socketFile = new File(commandLine.getOptionValue("socket", "./control.socket"));
            try (AFUNIXSocket socket = AFUNIXSocket.newInstance()) {
                socket.connect(AFUNIXSocketAddress.of(socketFile), 5000);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                LinkedBlockingQueue<Response> responseQueue = new LinkedBlockingQueue<>();
                new Thread(() -> {
                    while (socket.isConnected()) {
                        try {
                            Object data = in.readObject();
                            if (data instanceof LogMessage) {
                                console.writeLine(((LogMessage) data).getLineColored());
                            } else if (data instanceof Response) {
                                responseQueue.put((Response) data);
                            } else {
                                console.writeLine("[ERROR] Got unknown message class: " + data.getClass().getName());
                            }
                        } catch (EOFException ignored) {
                        } catch (IOException | ClassNotFoundException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).start();
                console.addCommandHandler(UUID.randomUUID(), s -> {
                    if (s.equals("exit") || s.equals("stop") || s.equals("shutdown")) {
                        synchronized (consoleStopIssued) {
                            consoleStopIssued.notify();
                        }
                        return;
                    }
                    socketExec.execute(() -> {
                        try {
                            handleCommand(s, out, responseQueue, console);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });
                console.addTabCompletionHandler(UUID.randomUUID(), (commandLine1, args1, properties) ->
                {
                    try {
                        return socketExec.submit(() -> handleTabComplete(commandLine1, out, responseQueue, console)).get();
                    } catch (InterruptedException | ExecutionException e) {
                        console.writeLine("[ERROR] Unable to get tab completions");
                        console.writeLine(Util.getStackTrace(e));
                        return Collections.emptyList();
                    }
                });
                synchronized (consoleStopIssued) {
                    consoleStopIssued.wait();
                }
                console.close();
            }
        } catch (ParseException | IOException e) {
            System.err.println("Unable to communicate via socket!");
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }

    private static Collection<String> handleTabComplete(String s, ObjectOutputStream out, LinkedBlockingQueue<Response> in, IConsole console) throws InterruptedException {
        try {
            out.writeObject(new TabCompletionRequest(s));
            out.flush();
            Object result = in.take();
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

    private static void handleCommand(String s, ObjectOutputStream out, LinkedBlockingQueue<Response> in, IConsole console) throws InterruptedException {
        try {
            out.writeObject(new TextBasedRequest(s));
            out.flush();
            Object result = in.take();
            if (!(result instanceof CommandExecutedResponse)) {
                console.writeLine(String.format("[ERROR] Got invalid class as a result: %s", result.getClass().getName()));
            }
        } catch (IOException e) {
            console.writeLine("[ERROR] An error occured sending the command");
            console.write(Util.getStackTrace(e));
        }
    }
}
