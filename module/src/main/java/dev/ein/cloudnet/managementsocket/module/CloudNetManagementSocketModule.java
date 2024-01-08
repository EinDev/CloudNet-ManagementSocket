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

import com.google.common.eventbus.EventBus;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.logging.AbstractLogHandler;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.console.log.ColouredLogFormatter;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.module.NodeCloudNetModule;
import dev.ein.cloudnet.managementsocket.shared.command.Util;
import dev.ein.cloudnet.managementsocket.shared.command.commands.DisconnectRequest;
import dev.ein.cloudnet.managementsocket.shared.command.commands.LogMessage;
import lombok.Getter;

import java.io.File;
import java.io.IOException;

public class CloudNetManagementSocketModule extends NodeCloudNetModule {
  @Getter
  private static CloudNetManagementSocketModule instance;
  private String socketPath = "./control.socket";
  private ServerSocketThread serverSocketThread;
  @Getter
  private EventBus eventBus = new EventBus();
  private AbstractLogHandler logHandler = new RemoteConsoleLogHandler(s -> eventBus.post(new LogMessage(s))).setFormatter(new ColouredLogFormatter());

  public CloudNetManagementSocketModule() {
    instance = this;
  }

  @SuppressWarnings("unused")
  @ModuleTask(order = 126, event = ModuleLifeCycle.LOADED)
  public void initConfig() {
    socketPath = this.getConfig().getString("socketPath", socketPath);

    this.saveConfig();
  }

  @SuppressWarnings("unused")
  @ModuleTask(event = ModuleLifeCycle.STARTED)
  public void initSocket() {
    ensureSocketStopped();
    CloudNetManagementSocketModule.getInstance().getLogger().log(LogLevel.INFO, "Starting Socket " + socketPath);
    File socketFile = new File(socketPath);
    serverSocketThread = new ServerSocketThread(socketFile, new CommandHandler(CloudNetManagementSocketModule.getInstance().getLogger(), CloudNet.getInstance()));
    serverSocketThread.start();
    this.getLogger().addLogHandler(logHandler);
  }

  @SuppressWarnings("unused")
  @ModuleTask(event = ModuleLifeCycle.STOPPED)
  public void teardownSocket() {
    ensureSocketStopped();
    this.getLogger().removeLogHandler(logHandler);
  }

  private void ensureSocketStopped() {
    if (serverSocketThread != null && serverSocketThread.isAlive()) {
      eventBus.post(new DisconnectRequest("Master process exiting"));
      try {
        Thread.sleep(100); // Wait for clients to recieve the disconnect request
      } catch (InterruptedException ignored) {}
      try {
        serverSocketThread.shutdown();
      } catch (IOException e) {
        CloudNetManagementSocketModule.getInstance().getLogger().log(LogLevel.ERROR, "Caught exception while shutting down socket", e);
      }
      CloudNetManagementSocketModule.getInstance().getLogger().log(LogLevel.INFO, "Stopping Socket " + socketPath);
      try {
        serverSocketThread.join(5000);
        if (serverSocketThread.isAlive()) {
          CloudNetManagementSocketModule.getInstance().getLogger().warning("ServerSocketThread still running after 5s. Stack trace: ");
          CloudNetManagementSocketModule.getInstance().getLogger().warning(Util.getStackTrace(serverSocketThread));
        }
      } catch (InterruptedException ex) {
        CloudNetManagementSocketModule.getInstance().getLogger().log(LogLevel.WARNING, "Caught Exception during socket closing", ex);
      }
    }
  }

}
