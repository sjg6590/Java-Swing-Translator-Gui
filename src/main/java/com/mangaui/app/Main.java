package com.mangaui.app;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SettingsManager.applySystemPropertiesFromDisk();
            TranslatorApp app = new TranslatorApp();
            app.show();
        });
    }
}


