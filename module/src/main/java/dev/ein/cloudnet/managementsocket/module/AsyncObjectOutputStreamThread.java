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

import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.common.logging.LogLevel;
import dev.ein.cloudnet.managementsocket.shared.command.Response;
import org.newsclub.net.unix.SocketClosedException;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AsyncObjectOutputStreamThread extends Thread {
  private final BlockingQueue<Response> responses = new LinkedBlockingQueue<>();
  private final ObjectOutputStream out;
  private final ILogger logger;

  public AsyncObjectOutputStreamThread(OutputStream out, ILogger logger) throws IOException {
    this.out = new ObjectOutputStream(out);
    this.logger = logger;
  }

  @Override
  public void run() {
    this.setName(AsyncObjectOutputStreamThread.class.getSimpleName());
    boolean continueRunning = true;
    while (continueRunning) {
      try {
        Response message = responses.take();
        out.writeObject(message);
      } catch (EOFException | InterruptedException | SocketClosedException e) {
        continueRunning = false;
      } catch (IOException e) {
        logger.log(LogLevel.ERROR, "Caught Exception while writing object: ", e);
      }
    }
  }

  public void send(Response message) {
    responses.add(message);
  }
}
