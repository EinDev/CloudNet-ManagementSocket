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
import dev.ein.cloudnet.managementsocket.shared.command.Response;
import dev.ein.cloudnet.managementsocket.shared.command.Util;
import dev.ein.cloudnet.managementsocket.shared.command.commands.DisconnectRequest;
import dev.ein.cloudnet.managementsocket.shared.command.commands.LogMessage;
import lombok.AllArgsConstructor;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.SocketClosedException;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;

@AllArgsConstructor
public class ResponseConsoleWriterThread extends Thread {
  private final AFUNIXSocket socket;
  private final IConsole console;
  private final LinkedBlockingQueue<Response> responseQueue;
  private final Lock consoleStopIssued;

  @Override
  public void run() {
    this.setName(ResponseConsoleWriterThread.class.getName());
    try {
      ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
      while (socket.isConnected()) {
        try {
          Object data = in.readObject();
          if (data instanceof LogMessage) {
            console.writeLine(((LogMessage) data).getLineColored());
          } else if (data instanceof DisconnectRequest) {
            String reason = ((DisconnectRequest) data).getReason();
            console.writeLine("[INFO] Connection closed by backend. Reason: " + reason);
            socket.close();
          } else if (data instanceof Response) {
            responseQueue.put((Response) data);
          } else {
            console.writeLine("[ERROR] Recieved unknown message class: " + data.getClass().getName());
          }
        } catch (EOFException ignored) {
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
          console.writeLine("[ERROR] Exception while receiving packet");
          console.writeLine(Util.getStackTrace(e));
        }
      }
    } catch (SocketClosedException ignored) {
      console.writeLine("[ERROR] Socket to master process unexpectedly closed");
    } catch (IOException e) {
      console.writeLine("[ERROR] Exception in connection");
      console.writeLine(Util.getStackTrace(e));
    }
    synchronized (consoleStopIssued) {
      consoleStopIssued.notify();
    }
  }
}
