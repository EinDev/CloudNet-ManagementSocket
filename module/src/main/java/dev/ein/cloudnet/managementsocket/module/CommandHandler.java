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
