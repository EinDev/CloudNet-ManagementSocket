package dev.ein.cloudnet.managementsocket.shared.command.commands;

import dev.ein.cloudnet.managementsocket.shared.command.Response;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TabCompletionResponse implements Response {
    String[] tabCompletions;
}
