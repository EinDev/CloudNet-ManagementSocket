package dev.ein.cloudnet.managementsocket.shared.command;

import lombok.experimental.UtilityClass;

import java.io.PrintWriter;
import java.io.StringWriter;

@UtilityClass
public class Util {
    public String getStackTrace(Throwable ball) {
        StringWriter sw = new StringWriter();
        ball.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
