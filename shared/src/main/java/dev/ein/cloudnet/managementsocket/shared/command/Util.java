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

package dev.ein.cloudnet.managementsocket.shared.command;

import lombok.experimental.UtilityClass;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.stream.Collectors;

@UtilityClass
public class Util {
    public String getStackTrace(Throwable ball) {
        StringWriter sw = new StringWriter();
        ball.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public String getStackTrace(Thread t) {
      StackTraceElement[] elements = t.getStackTrace();
      return Arrays.stream(elements).map(StackTraceElement::toString).collect(Collectors.joining("\n"));
    }
}
