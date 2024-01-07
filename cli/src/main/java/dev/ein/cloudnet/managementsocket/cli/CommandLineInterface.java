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
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CommandLineInterface {
    public static void main(String[] args) throws Exception {
        IConsole console = new JLine3Console();
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        Option socketFilename = new Option("s", "socket", true, "Socket file to use");
        socketFilename.setRequired(true);
        options.addOption(socketFilename);
        Lock consoleStopIssued = new ReentrantLock();
        ExecutorService socketExec = Executors.newFixedThreadPool(1);
        try {
            CommandLine commandLine = parser.parse(options, args);
            String cmd = String.join(" ", commandLine.getArgList());
            File socketFile = new File(commandLine.getOptionValue("socket"));
            try (AFUNIXSocket socket = AFUNIXSocket.newInstance()) {
                socket.connect(AFUNIXSocketAddress.of(socketFile), 5000);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                LinkedBlockingQueue<Response> responseQueue = new LinkedBlockingQueue<>();
                new Thread(() -> {
                    while(socket.isConnected()) {
                        try {
                            Object data = in.readObject();
                            if(data instanceof LogMessage) {
                                console.writeLine(((LogMessage) data).getLineColored());
                            } else if(data instanceof Response) {
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
