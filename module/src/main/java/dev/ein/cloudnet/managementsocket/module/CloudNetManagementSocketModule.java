package dev.ein.cloudnet.managementsocket.module;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.module.NodeCloudNetModule;
import lombok.Getter;

import java.io.File;

public class CloudNetManagementSocketModule extends NodeCloudNetModule {
    @Getter
    private static CloudNetManagementSocketModule instance;
    private String socketPath = "/var/run/cloudnet.socket";
    private ServerSocketThread serverSocketThread;

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
    }

    @ModuleTask(event = ModuleLifeCycle.STOPPED)
    public void teardownSocket() {
        ensureSocketStopped();
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
