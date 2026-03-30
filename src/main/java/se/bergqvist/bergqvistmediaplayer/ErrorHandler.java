package se.bergqvist.bergqvistmediaplayer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Bergqvist Media Player
 *
 * @author Daniel Bergqvist (C) 2026
 */
public class ErrorHandler implements Thread.UncaughtExceptionHandler {

    public static void install() {
        Thread.setDefaultUncaughtExceptionHandler(new ErrorHandler());
    }

    private ErrorHandler() {
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        String fileName = SystemProperties.SETTINGS_FOLDER + "error.log";
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)))) {
            LocalDateTime dateTime = LocalDateTime.now();
            DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            pw.println(dateTime.format(dateTimeFormat));
            pw.println();
            e.printStackTrace(pw);
            pw.println();
            pw.println();
            pw.println("================================================================================");
            pw.println();
            pw.println();
            pw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.exit(1);
    }

}
