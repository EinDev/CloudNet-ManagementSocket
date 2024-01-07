package dev.ein.cloudnet.managementsocket.module;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.console.log.ColouredLogFormatter;
import de.dytanic.cloudnet.event.command.CommandNotFoundEvent;
import dev.ein.cloudnet.managementsocket.shared.command.Request;
import dev.ein.cloudnet.managementsocket.shared.command.Response;
import dev.ein.cloudnet.managementsocket.shared.command.commands.*;

import java.util.List;

public class CommandHandler {
    private final ILogger logger;
    private final CloudNet cloudNet;
    private final ColouredLogFormatter formatter;

    public CommandHandler(ILogger logger, CloudNet cloudNet) {
        this.logger = logger;
        this.cloudNet = cloudNet;
        this.formatter = new ColouredLogFormatter();
    }
    public Response handleCommand(Request c) {
        if(c instanceof TextBasedRequest) {
            String command = ((TextBasedRequest) c).getCommand();
            boolean success = cloudNet.getCommandMap().dispatchCommand(cloudNet.getConsoleCommandSender(), command);
            if(!success) {
                cloudNet.getEventManager().callEvent(new CommandNotFoundEvent(command));
                cloudNet.getLogger().warning(LanguageManager.getMessage("command-not-found"));
            }
            return new CommandExecutedResponse();
        } else if (c instanceof TabCompletionRequest) {
            String command = ((TabCompletionRequest) c).getCommand();
            List<String> response = cloudNet.getCommandMap().tabCompleteCommand(command);
            return new TabCompletionResponse(response.toArray(new String[0]));
        } else {
            logger.warning(String.format("Got unknown command: %s", c.getClass().getName()));
            return new ErrorResult("Unknown command");
        }
    }
}
