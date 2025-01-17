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
import dev.ein.cloudnet.managementsocket.shared.command.Util;
import dev.ein.cloudnet.managementsocket.shared.command.commands.DisconnectRequest;
import dev.ein.cloudnet.managementsocket.shared.command.commands.LogMessage;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.provider.ClusterNodeProvider;
import lombok.Getter;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.LogManager;

public class CloudNetManagementSocketModule extends DriverModule {
  protected static final Logger LOGGER = LoggerFactory.getLogger(CloudNetManagementSocketModule.class);

  @Getter
  private static CloudNetManagementSocketModule instance;
  private ServerSocketConfiguration configuration;
  private ServerSocketThread serverSocketThread;
  @Getter
  private EventBus eventBus = new EventBus();
  private Handler logHandler = new RemoteConsoleLogHandler(s -> eventBus.post(new LogMessage(s)));

  public CloudNetManagementSocketModule() {
    instance = this;
  }

  @SuppressWarnings("unused")
  @ModuleTask(order = 126, lifecycle = ModuleLifeCycle.LOADED)
  public void initConfig() {
    this.configuration = this.readConfig(ServerSocketConfiguration.class,
      () -> new ServerSocketConfiguration("./control.socket"),
      DocumentFactory.json());
  }

  @SuppressWarnings("unused")
  @ModuleTask(lifecycle = ModuleLifeCycle.STARTED)
  public void initSocket(@NonNull ClusterNodeProvider clusterNodeProvider) {
    ensureSocketStopped();
    LOGGER.info("Starting Socket {}", this.configuration.socketFile());
    File socketFile = new File(this.configuration.socketFile());
    serverSocketThread = new ServerSocketThread(socketFile, new CommandHandler(clusterNodeProvider));
    serverSocketThread.start();
    LogManager.getLogManager().getLogger("").addHandler(logHandler);
  }

  @SuppressWarnings("unused")
  @ModuleTask(lifecycle = ModuleLifeCycle.STOPPED)
  public void teardownSocket() {
    ensureSocketStopped();
    LogManager.getLogManager().getLogger("").removeHandler(logHandler);
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
        LOGGER.error("Caught exception while shutting down socket", e);
      }
      LOGGER.info("Stopping Socket {}", configuration.socketFile());
      try {
        serverSocketThread.join(5000);
        if (serverSocketThread.isAlive()) {
          LOGGER.warn("ServerSocketThread still running after 5s. Stack trace: ");
          LOGGER.warn(Util.getStackTrace(serverSocketThread));
        }
      } catch (InterruptedException ex) {
        LOGGER.warn("Caught Exception during socket closing", ex);
      }
    }
  }

}
