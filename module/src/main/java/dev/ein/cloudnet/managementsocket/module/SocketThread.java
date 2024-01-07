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

import com.google.common.eventbus.Subscribe;
import de.dytanic.cloudnet.common.logging.LogLevel;
import dev.ein.cloudnet.managementsocket.shared.command.Request;
import dev.ein.cloudnet.managementsocket.shared.command.Response;
import dev.ein.cloudnet.managementsocket.shared.command.commands.LogMessage;
import org.checkerframework.checker.units.qual.A;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.*;

public class SocketThread extends Thread {
    private final Socket socket;
    private final CommandHandler handler;
    private AsyncObjectOutputStream out;

    SocketThread(Socket socket, CommandHandler handler) {
        this.socket = socket;
        this.handler = handler;
    }

    @Subscribe
    public void onLogLine(String data) {
        assert this.out != null;
        this.out.submit(new LogMessage(data));
    }

    @Override
    public void run() {
        ObjectInputStream in;
        try {
            in = new ObjectInputStream(socket.getInputStream());
            out = new AsyncObjectOutputStream(socket.getOutputStream());
            CloudNetManagementSocketModule.getInstance().getLogEventBus().register(this);
            while (socket.isConnected()) {
                try {
                    Object data = in.readObject();
                    if (!(data instanceof Request)) {
                        CloudNetManagementSocketModule.getInstance().getLogger().log(LogLevel.ERROR, "Got known class, but prohibited: " + data.getClass().getName());
                    } else {
                        Response response = handler.handleCommand((Request) data);
                        if(response != null) {
                            out.submit(response);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    CloudNetManagementSocketModule.getInstance().getLogger().log(LogLevel.WARNING, "Got unknown class: " + e.getClass().getName());
                } catch (EOFException e) {
                    return; // Client closed the connection
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException ex) {
            CloudNetManagementSocketModule.getInstance().getLogger().log(LogLevel.WARNING, "Caught Exception during socket handling: ", ex);
        } finally {
            CloudNetManagementSocketModule.getInstance().getLogEventBus().unregister(this);
            try {
                socket.close();
            } catch (IOException ex) {
                CloudNetManagementSocketModule.getInstance().getLogger().log(LogLevel.WARNING, "Caught Exception during socket handling: ", ex);
            }
        }
    }
}
