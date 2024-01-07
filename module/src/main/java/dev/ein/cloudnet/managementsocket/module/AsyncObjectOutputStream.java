package dev.ein.cloudnet.managementsocket.module;

import dev.ein.cloudnet.managementsocket.shared.command.Request;
import dev.ein.cloudnet.managementsocket.shared.command.Response;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncObjectOutputStream {
    private ExecutorService thread = Executors.newFixedThreadPool(1);
    private final ObjectOutputStream out;

    public AsyncObjectOutputStream(OutputStream out) throws IOException {
        this.out = new ObjectOutputStream(out);
    }

    public void submit(Response message) {
        thread.submit(() -> {
            try {
                out.writeObject(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
