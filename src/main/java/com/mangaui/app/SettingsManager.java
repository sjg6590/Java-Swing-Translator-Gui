package com.mangaui.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class SettingsManager {
    private static final String APP_DIR = System.getProperty("user.home") + File.separator + ".mangaui";
    private static final String SETTINGS_FILE = APP_DIR + File.separator + "settings.properties";

    public static Properties load() {
        Properties props = new Properties();
        File f = new File(SETTINGS_FILE);
        if (f.exists()) {
            try (FileInputStream in = new FileInputStream(f)) {
                props.load(in);
            } catch (IOException ignored) {}
        }
        return props;
    }

    public static void save(String pythonCmd, String deeplApiKey) {
        Properties props = load();
        if (pythonCmd != null) props.setProperty("PYTHON_CMD", pythonCmd);
        if (deeplApiKey != null) props.setProperty("DEEPL_API_KEY", deeplApiKey);
        File dir = new File(APP_DIR);
        if (!dir.exists()) {
            // noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        try (FileOutputStream out = new FileOutputStream(SETTINGS_FILE)) {
            props.store(out, "MangaUI Settings");
        } catch (IOException ignored) {}
    }

    public static void applySystemPropertiesFromDisk() {
        Properties props = load();
        String python = props.getProperty("PYTHON_CMD", "");
        if (!python.isBlank()) {
            System.setProperty("PYTHON_CMD", python);
        }
        String deepl = props.getProperty("DEEPL_API_KEY", "");
        if (!deepl.isBlank()) {
            System.setProperty("DEEPL_API_KEY", deepl);
        }
    }
}


