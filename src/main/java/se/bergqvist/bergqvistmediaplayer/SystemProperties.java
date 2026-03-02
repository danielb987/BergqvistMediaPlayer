package se.bergqvist.bergqvistmediaplayer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * System properties.
 *
 * @author Daniel Bergqvist (C) 2026
 */
public class SystemProperties {

    public static final SystemProperties get() {
        return INSTANCE;
    }

    private static final SystemProperties INSTANCE = new SystemProperties();

    private final Properties sysProp;

    private final SortedMap<Integer, String> mainFolders = new TreeMap<>();
//    private final Map<Integer, Long> bookmarkTimes = new HashMap<>();
//    private final Properties movieProperties = new Properties();

    private SystemProperties() {
        File settingsFile = new File("/BergqvistMediaPlayer/settings.bergqvist");
        sysProp = new Properties();
        if (!settingsFile.exists()) {
            System.out.format("Settings file doesn't exists: %s%n", settingsFile.getAbsolutePath());
            System.exit(1);
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(settingsFile, StandardCharsets.UTF_8))) {
            sysProp.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        loadSettings();
    }

    public void store() {

    }

    private void loadSettings() {
        for (Object k : sysProp.keySet()) {
            String key = k.toString();
            if (key.startsWith("MainFolder_")) {
                String idStr = key.substring("MainFolder_".length());
                int id = Integer.parseInt(idStr);
                String label = sysProp.getProperty(key);
                mainFolders.put(id, label);
            }
        }
    }

    public List<String> getMainFolders() {
        return Collections.unmodifiableList(new ArrayList<>(mainFolders.values()));
    }

}
