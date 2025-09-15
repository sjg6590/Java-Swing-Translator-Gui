package com.mangaui.app;

import com.mangaui.services.DeepLClient;
import com.mangaui.services.MangaOcrService;
import com.mangaui.ui.OcrPanel;
import com.mangaui.ui.DebugPanel;
import com.mangaui.ui.SettingsDialog;
import com.mangaui.ui.TranslationPanel;
import com.mangaui.ui.ColorTheme;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Font;

public class TranslatorApp {
    private final JFrame frame;
    private final JTabbedPane tabs;
    private final OcrPanel ocrPanel;
    private final DebugPanel debugPanel;
    private final TranslationPanel translationPanel;
    private final SettingsDialog settingsDialog;

    public TranslatorApp() {
        frame = new JFrame("Manga Translator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        
        // Set frame background
        frame.getContentPane().setBackground(ColorTheme.BACKGROUND_MAIN);

        tabs = new JTabbedPane();
        tabs.setBackground(ColorTheme.BACKGROUND_MAIN);
        tabs.setForeground(ColorTheme.TEXT_PRIMARY);
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        MangaOcrService ocrService = new MangaOcrService();
        DeepLClient deepLClient = new DeepLClient();

        debugPanel = new DebugPanel();
        ocrPanel = new OcrPanel(ocrService, debugPanel);
        debugPanel.setOcrPanel(ocrPanel); // Connect debug panel to OCR panel
        translationPanel = new TranslationPanel(deepLClient);

        tabs.addTab("DEBUG", debugPanel);
        tabs.addTab("OCR", ocrPanel);
        tabs.addTab("Translation", translationPanel);
        
        // Customize tab colors after tabs are added
        tabs.setBackgroundAt(0, ColorTheme.BACKGROUND_MAIN);
        tabs.setForegroundAt(0, ColorTheme.TEXT_PRIMARY);
        tabs.setBackgroundAt(1, ColorTheme.BACKGROUND_MAIN);
        tabs.setForegroundAt(1, ColorTheme.TEXT_PRIMARY);
        tabs.setBackgroundAt(2, ColorTheme.BACKGROUND_MAIN);
        tabs.setForegroundAt(2, ColorTheme.TEXT_PRIMARY);

        frame.setLayout(new BorderLayout());
        frame.add(tabs, BorderLayout.CENTER);

        settingsDialog = new SettingsDialog(frame);
        setupMenuBar();
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(ColorTheme.BACKGROUND_LIGHT);
        menuBar.setForeground(ColorTheme.TEXT_PRIMARY);
        
        JMenu appMenu = new JMenu("App");
        appMenu.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        appMenu.setForeground(ColorTheme.TEXT_PRIMARY);
        
        JMenuItem settingsItem = new JMenuItem("Settings");
        settingsItem.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        settingsItem.setForeground(ColorTheme.TEXT_PRIMARY);
        settingsItem.addActionListener(e -> settingsDialog.setVisible(true));
        
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        aboutItem.setForeground(ColorTheme.TEXT_PRIMARY);
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(frame,
                "Manga Translator GUI\nOCR: manga-ocr (Python)\nTranslation: DeepL",
                "About", JOptionPane.INFORMATION_MESSAGE));
        
        JMenuItem quitItem = new JMenuItem("Quit");
        quitItem.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        quitItem.setForeground(ColorTheme.TEXT_PRIMARY);
        quitItem.addActionListener(e -> System.exit(0));
        
        appMenu.add(settingsItem);
        appMenu.add(aboutItem);
        appMenu.add(quitItem);
        menuBar.add(appMenu);
        frame.setJMenuBar(menuBar);
    }

    public void show() {
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}


