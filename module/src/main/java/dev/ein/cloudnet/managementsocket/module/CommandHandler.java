package dev.ein.cloudnet.managementsocket.module;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.logging.ILogger;
import dev.ein.cloudnet.managementsocket.shared.command.Command;
import dev.ein.cloudnet.managementsocket.shared.command.CommandResult;
import dev.ein.cloudnet.managementsocket.shared.command.commands.ErrorResult;
import dev.ein.cloudnet.managementsocket.shared.command.commands.TextBasedCommand;
import dev.ein.cloudnet.managementsocket.shared.command.commands.TextBasedResult;

public class CommandHandler {
    private final ILogger logger;
    private final CloudNet cloudNet;

    public CommandHandler(ILogger logger, CloudNet cloudNet) {
        this.logger = logger;
        this.cloudNet = cloudNet;
    }
    public CommandResult handleCommand(Command c) {
        if(c instanceof TextBasedCommand) {
            String command = ((TextBasedCommand) c).getCommand();
            String[] result = cloudNet.getNodeInfoProvider().sendCommandLine(command);
            return new TextBasedResult(result);
        } else {
            logger.warning(String.format("Got unknown command: %s", c.getClass().getName()));
            return new ErrorResult("Unknown command");
        }
    }
}
