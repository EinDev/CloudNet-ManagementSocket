package dev.ein.cloudnet.managementsocket.cli;

import dev.ein.cloudnet.managementsocket.shared.command.commands.TextBasedCommand;
import dev.ein.cloudnet.managementsocket.shared.command.commands.TextBasedResult;
import org.apache.commons.cli.*;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class CommandLineInterface {
    public static void main(String[] args) throws InterruptedException {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        Option socketFilename = new Option("s", "socket", true, "Socket file to use");
        socketFilename.setRequired(true);
        options.addOption(socketFilename);
        try {
            CommandLine commandLine = parser.parse(options, args);
            String cmd = String.join(" ", commandLine.getArgList());
            File socketFile = new File(commandLine.getOptionValue("socket"));
            try (AFUNIXSocket socket = AFUNIXSocket.newInstance()) {
                socket.connect(AFUNIXSocketAddress.of(socketFile), 5000);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                out.writeObject(new TextBasedCommand(cmd));
                out.flush();
                Object result = in.readObject();
                if(result instanceof TextBasedResult) {
                    String[] answer = ((TextBasedResult) result).getResult();
                    for (String line : answer) {
                        System.out.println(line);
                    }
                } else {
                    System.err.printf("Got invalid class as a result: %s", result.getClass().getName());
                    System.exit(-1);
                }
            } catch (ClassNotFoundException e) {
                System.err.printf("Got invalid class %s as result", e.getClass().getName());
                System.exit(-1);
            }
        } catch (ParseException | IOException e) {
            System.err.println("Unable to communicate via socket!");
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }
}
