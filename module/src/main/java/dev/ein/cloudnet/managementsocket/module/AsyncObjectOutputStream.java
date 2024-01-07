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
