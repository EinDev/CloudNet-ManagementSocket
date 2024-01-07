package dev.ein.cloudnet.managementsocket.shared.command.commands;

import dev.ein.cloudnet.managementsocket.shared.command.Request;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TabCompletionRequest implements Request {
    private final String command;
}
