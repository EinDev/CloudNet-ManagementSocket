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

import de.dytanic.cloudnet.common.logging.LogLevel;
import org.newsclub.net.unix.AFUNIXServerSocket;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerSocketThread extends Thread {
  private final File socketFile;
  private final CommandHandler handler;
  private final AtomicBoolean continueRunning = new AtomicBoolean(true);
  private AFUNIXServerSocket ss;

  ServerSocketThread(File socketFile, CommandHandler handler) {
    this.socketFile = socketFile;
    this.handler = handler;
  }

  @Override
  public void run() {
    try {
      ss = AFUNIXServerSocket.bindOn(socketFile, true);
      while (continueRunning.get() && ss.isBound()) {
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
        if (ss != null) ss.close();
      } catch (IOException ex) {
        CloudNetManagementSocketModule.getInstance().getLogger().log(LogLevel.WARNING, "Caught Exception during socket handling", ex);
      }
    }
  }

  public void shutdown() throws IOException {
    continueRunning.set(true);
    if(ss != null) ss.close();
  }
}
