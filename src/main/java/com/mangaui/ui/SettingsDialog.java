package com.mangaui.ui;

import com.mangaui.app.SettingsManager;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Font;

public class SettingsDialog extends JDialog {
    private final JTextField pythonField;
    private final JTextField deeplApiKeyField;
    private final JComboBox<String> languageComboBox;

    public SettingsDialog(Frame owner) {
        super(owner, "Settings", true);
        setSize(600, 250);
        setLocationRelativeTo(owner);
        
        // Set dialog background
        getContentPane().setBackground(ColorTheme.BACKGROUND_MAIN);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBackground(ColorTheme.BACKGROUND_MAIN);
        
        JLabel pythonLabel = new JLabel("Python command (with manga-ocr):");
        pythonLabel.setForeground(ColorTheme.TEXT_PRIMARY);
        pythonLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        form.add(pythonLabel);
        
        java.util.Properties saved = SettingsManager.load();
        String savedPython = saved.getProperty("PYTHON_CMD",
                System.getProperty("PYTHON_CMD", System.getenv().getOrDefault("PYTHON_CMD", "python3")));
        pythonField = new JTextField(savedPython);
        pythonField.setBackground(ColorTheme.BACKGROUND_CARD);
        pythonField.setForeground(ColorTheme.TEXT_PRIMARY);
        pythonField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pythonField.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createLineBorder(ColorTheme.BORDER_LIGHT, 1),
            javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        form.add(pythonField);

        JLabel apiKeyLabel = new JLabel("DeepL API Key:");
        apiKeyLabel.setForeground(ColorTheme.TEXT_PRIMARY);
        apiKeyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        form.add(apiKeyLabel);
        
        String savedKey = saved.getProperty("DEEPL_API_KEY",
                System.getProperty("DEEPL_API_KEY", System.getenv().getOrDefault("DEEPL_API_KEY", "")));
        deeplApiKeyField = new JTextField(savedKey);
        deeplApiKeyField.setBackground(ColorTheme.BACKGROUND_CARD);
        deeplApiKeyField.setForeground(ColorTheme.TEXT_PRIMARY);
        deeplApiKeyField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        deeplApiKeyField.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createLineBorder(ColorTheme.BORDER_LIGHT, 1),
            javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        form.add(deeplApiKeyField);

        JLabel languageLabel = new JLabel("OCR Language:");
        languageLabel.setForeground(ColorTheme.TEXT_PRIMARY);
        languageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        form.add(languageLabel);
        
        String[] languages = {"Japanese (manga-ocr)", "Portuguese (Tesseract)", "English (Tesseract)", "Spanish (Tesseract)", "French (Tesseract)", "German (Tesseract)"};
        String savedLanguage = saved.getProperty("OCR_LANGUAGE", "Japanese (manga-ocr)");
        languageComboBox = new JComboBox<>(languages);
        languageComboBox.setSelectedItem(savedLanguage);
        languageComboBox.setBackground(ColorTheme.BACKGROUND_CARD);
        languageComboBox.setForeground(ColorTheme.TEXT_PRIMARY);
        languageComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        languageComboBox.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createLineBorder(ColorTheme.BORDER_LIGHT, 1),
            javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        form.add(languageComboBox);

        JButton save = createStyledButton("Save");
        save.addActionListener(e -> {
            String py = pythonField.getText().trim();
            String key = deeplApiKeyField.getText().trim();
            String language = (String) languageComboBox.getSelectedItem();
            System.setProperty("PYTHON_CMD", py);
            System.setProperty("DEEPL_API_KEY", key);
            System.setProperty("OCR_LANGUAGE", language);
            SettingsManager.save(py, key, language);
            setVisible(false);
        });

        setLayout(new BorderLayout(8, 8));
        add(form, BorderLayout.CENTER);
        add(save, BorderLayout.SOUTH);
    }
    
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(ColorTheme.BUTTON_PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setOpaque(true);  // Ensure background is painted
        button.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createLineBorder(ColorTheme.BORDER_MEDIUM, 1),
            javax.swing.BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        
        // Add hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(ColorTheme.BUTTON_PRIMARY_HOVER);
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(ColorTheme.BUTTON_PRIMARY);
            }
        });
        
        return button;
    }
}


