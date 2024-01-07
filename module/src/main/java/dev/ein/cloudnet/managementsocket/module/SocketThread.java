package dev.ein.cloudnet.managementsocket.module;

import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.common.logging.LogLevel;
import dev.ein.cloudnet.managementsocket.shared.command.Command;
import org.checkerframework.checker.units.qual.C;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketThread extends Thread {
    private final Socket socket;
    private final CommandHandler handler;

    SocketThread(Socket socket, CommandHandler handler) {
        this.socket = socket;
        this.handler = handler;
    }

    @Override
    public void run() {
        ObjectInputStream in;
        ObjectOutputStream out;
        try {
           in = new ObjectInputStream(socket.getInputStream());
           out = new ObjectOutputStream(socket.getOutputStream());
           while(socket.isConnected()) {
               try {
                   Object data = in.readObject();
                   if(!(data instanceof Command)) {
                       CloudNetManagementSocketModule.getInstance().getLogger().log(LogLevel.ERROR, "Got known class, but prohibited: " + data.getClass().getName());
                       continue;
                   }
                   out.writeObject(handler.handleCommand((Command) data));
               } catch (ClassNotFoundException e) {
                   CloudNetManagementSocketModule.getInstance().getLogger().log(LogLevel.WARNING, "Got unknown class: " + e.getClass().getName());
               } catch (EOFException e) {
                   return; // Client closed the connection
               }
           }
        } catch (IOException ex) {
            CloudNetManagementSocketModule.getInstance().getLogger().log(LogLevel.WARNING, "Caught Exception during socket handling: ", ex);
        } finally {
            try {
                socket.close();
            } catch (IOException ex) {
                CloudNetManagementSocketModule.getInstance().getLogger().log(LogLevel.WARNING, "Caught Exception during socket handling: ", ex);
            }
        }
    }
}
