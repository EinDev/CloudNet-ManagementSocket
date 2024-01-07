package dev.ein.cloudnet.managementsocket.module;

import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.common.logging.LogLevel;
import org.newsclub.net.unix.AFUNIXServerSocket;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public class ServerSocketThread extends Thread {
    private final File socketFile;
    private final CommandHandler handler;

    ServerSocketThread(File socketFile, CommandHandler handler) {
        this.socketFile = socketFile;
        this.handler = handler;
    }

    @Override
    public void run() {
        AFUNIXServerSocket ss = null;
        try {
            ss = AFUNIXServerSocket.bindOn(socketFile, true);
            while(ss.isBound()) {
                Socket s;
                try {
                    s = ss.accept();
                    SocketThread thread = new SocketThread(s, handler);
                    thread.start();
                } catch (SocketException ignored) { // Socket got closed
                } catch (IOException ex) {
                    CloudNetManagementSocketModule.getInstance().getLogger().log(LogLevel.WARNING, "Caught Exception during socket handling", ex);
                }
            }
        } catch (IOException ex) {
            CloudNetManagementSocketModule.getInstance().getLogger().log(LogLevel.WARNING, "Caught Exception during socket handling", ex);
        } finally {
            try {
                if(ss != null) ss.close();
            } catch (IOException ex) {
                CloudNetManagementSocketModule.getInstance().getLogger().log(LogLevel.WARNING, "Caught Exception during socket handling", ex);
            }
        }
    }
}
