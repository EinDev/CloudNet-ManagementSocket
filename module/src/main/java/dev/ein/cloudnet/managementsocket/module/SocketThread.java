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
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.common.logging.LogLevel;
import dev.ein.cloudnet.managementsocket.shared.command.Request;
import dev.ein.cloudnet.managementsocket.shared.command.Response;
import dev.ein.cloudnet.managementsocket.shared.command.commands.DisconnectRequest;
import dev.ein.cloudnet.managementsocket.shared.command.commands.LogMessage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketException;

public class SocketThread extends Thread {
  private final Socket socket;
  private final CommandHandler handler;
  private final ILogger logger;
  private AsyncObjectOutputStreamThread out;

  SocketThread(Socket socket, CommandHandler handler) {
    this.socket = socket;
    this.handler = handler;
    this.logger = CloudNetManagementSocketModule.getInstance().getLogger();
  }

  @SuppressWarnings("unused")
  @Subscribe
  public void onLogMessage(LogMessage data) {
    assert this.out != null;
    this.out.send(data);
  }

  @SuppressWarnings("unused")
  @Subscribe
  public void onDisconnectRequest(DisconnectRequest data) {
    assert this.out != null;
    this.out.send(data);
  }

  @Override
  public void run() {
    this.setName(SocketThread.class.getName());
    ObjectInputStream in;
    try {
      in = new ObjectInputStream(socket.getInputStream());
      out = new AsyncObjectOutputStreamThread(socket.getOutputStream(), logger);
      out.start();
      CloudNetManagementSocketModule.getInstance().getEventBus().register(this);
      logger.info("Client management socket connected");
      while (socket.isConnected()) {
        try {
          Object data = in.readObject();
          if (!(data instanceof Request)) {
            logger.log(LogLevel.ERROR, "Got known class, but prohibited: " + data.getClass().getName());
          } else {
            Response response = handler.handleCommand((Request) data);
            if (response != null) {
              out.send(response);
            }
          }
        } catch (ClassNotFoundException e) {
          logger.log(LogLevel.WARNING, "Got unknown class: " + e.getClass().getName());
        } catch (EOFException | SocketException e) {
          logger.info("Client management socket closed by client");
          return;
        } catch (IOException ex) {
          logger.log(LogLevel.WARNING, "Caught Exception during socket handling: ", ex);
        }
      }
    } catch (IOException ex) {
      logger.log(LogLevel.WARNING, "Caught Exception during socket handling: ", ex);
    } finally {
      CloudNetManagementSocketModule.getInstance().getEventBus().unregister(this);
      try {
        socket.close();
      } catch (IOException ex) {
        logger.log(LogLevel.WARNING, "Caught Exception during socket handling: ", ex);
      }
    }
  }
}
