package dev.ein.cloudnet.managementsocket.module;

import de.dytanic.cloudnet.common.logging.AbstractLogHandler;
import de.dytanic.cloudnet.common.logging.LogEntry;
import lombok.AllArgsConstructor;

import java.util.function.Consumer;

@AllArgsConstructor
public class RemoteConsoleLogHandler extends AbstractLogHandler {
    private Consumer<String> consumer;
    @Override
    public void handle(LogEntry logEntry) {
        consumer.accept(this.getFormatter().format(logEntry));
    }
}
