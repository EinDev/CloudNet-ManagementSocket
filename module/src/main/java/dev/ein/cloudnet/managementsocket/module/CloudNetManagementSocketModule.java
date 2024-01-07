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
import lombok.Getter;

import java.io.File;

public class CloudNetManagementSocketModule extends NodeCloudNetModule {
    @Getter
    private static CloudNetManagementSocketModule instance;
    private String socketPath = "./control.socket";
    private ServerSocketThread serverSocketThread;
    @Getter
    private EventBus logEventBus = new EventBus();
    private AbstractLogHandler logHandler = new RemoteConsoleLogHandler(s -> logEventBus.post(s)).setFormatter(new ColouredLogFormatter());

    public CloudNetManagementSocketModule() {
        instance = this;
    }

    @ModuleTask(order = 126, event = ModuleLifeCycle.LOADED)
    public void initConfig() {
        socketPath = this.getConfig().getString("socketPath", socketPath);

        this.saveConfig();
    }

    @ModuleTask(event = ModuleLifeCycle.STARTED)
    public void initSocket() {
        ensureSocketStopped();
        CloudNetManagementSocketModule.getInstance().getLogger().log(LogLevel.INFO, "Starting Socket " + socketPath);
        File socketFile = new File(socketPath);
        serverSocketThread = new ServerSocketThread(socketFile, new CommandHandler(CloudNetManagementSocketModule.getInstance().getLogger(), CloudNet.getInstance()));
        serverSocketThread.start();
        this.getLogger().addLogHandler(logHandler);
    }

    @ModuleTask(event = ModuleLifeCycle.STOPPED)
    public void teardownSocket() {
        ensureSocketStopped();
        this.getLogger().removeLogHandler(logHandler);
    }

    private void ensureSocketStopped() {
        if (serverSocketThread != null && serverSocketThread.isAlive()) {
            CloudNetManagementSocketModule.getInstance().getLogger().log(LogLevel.INFO, "Stopping Socket " + socketPath);
            try {
                serverSocketThread.interrupt();
                serverSocketThread.join(5000);
                if (serverSocketThread.isAlive()) {
                    CloudNetManagementSocketModule.getInstance().getLogger().log(LogLevel.ERROR, "SocketThread still alive even after 5s. Interrupting");
                }
            } catch (InterruptedException ex) {
                CloudNetManagementSocketModule.getInstance().getLogger().log(LogLevel.WARNING, "Caught Exception during socket closing", ex);
            }
        }
    }

}
