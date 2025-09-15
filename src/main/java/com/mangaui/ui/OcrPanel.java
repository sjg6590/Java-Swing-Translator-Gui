package com.mangaui.ui;

import com.mangaui.services.MangaOcrService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OcrPanel extends JPanel {
    private final MangaOcrService ocrService;
    private final JTextArea ocrOutput;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final DebugPanel debugPanel;

    public OcrPanel(MangaOcrService ocrService, DebugPanel debugPanel) {
        super(new BorderLayout());
        this.ocrService = ocrService;
        this.debugPanel = debugPanel;
        
        // Set panel background
        setBackground(ColorTheme.BACKGROUND_MAIN);

        JButton selectButton = createStyledButton("Select Screen Area");
        selectButton.addActionListener(e -> selectAreaAndOcr());

        ocrOutput = new JTextArea();
        ocrOutput.setLineWrap(true);
        ocrOutput.setWrapStyleWord(true);
        ocrOutput.setBackground(ColorTheme.BACKGROUND_CARD);
        ocrOutput.setForeground(ColorTheme.TEXT_PRIMARY);
        ocrOutput.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ocrOutput.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorTheme.BORDER_LIGHT, 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        add(selectButton, BorderLayout.NORTH);
        add(new JScrollPane(ocrOutput), BorderLayout.CENTER);
    }
    
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(ColorTheme.BUTTON_PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setOpaque(true);  // Ensure background is painted
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorTheme.BORDER_MEDIUM, 1),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
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

    private void selectAreaAndOcr() {
        // Optionally bring a specific app to the front (macOS only)
        String os = System.getProperty("os.name", "").toLowerCase();
        String appToActivate = JOptionPane.showInputDialog(this,
                "App name to bring to front before selection (optional):",
                "Bring App to Front",
                JOptionPane.QUESTION_MESSAGE);
        if (appToActivate != null && !appToActivate.trim().isEmpty() && os.contains("mac")) {
            try {
                new ProcessBuilder("osascript", "-e",
                        "tell application \"" + appToActivate.trim() + "\" to activate")
                        .inheritIO()
                        .start()
                        .waitFor();
            } catch (IOException | InterruptedException ignored) {
                // Continue even if bringing the app to front fails
            }
        }

        // Temporarily hide the main window so it doesn't get captured/blocked
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        boolean restoreVisible = false;
        if (parentWindow != null && parentWindow.isVisible()) {
            restoreVisible = true;
            parentWindow.setVisible(false);
        }

        ScreenSelectionOverlay overlay = new ScreenSelectionOverlay();
        Rectangle rect = overlay.captureUserSelection();
        // Capture immediately while window is hidden
        BufferedImage captured = null;
        if (rect != null) {
            try {
                captured = new Robot().createScreenCapture(rect);
            } catch (Exception ignored) {}
        }
        // Restore window visibility (with a small delay to avoid flashing in capture)
        try { Thread.sleep(150); } catch (InterruptedException ignored) {}
        if (parentWindow != null && restoreVisible) {
            parentWindow.setVisible(true);
            parentWindow.toFront();
        }
        if (rect == null) {
            return;
        }
        ocrOutput.setText("Detecting bubbles and running OCR on each...");
        final BufferedImage imageForOcr = captured;
        final Rectangle rectForOcr = rect;
        executor.submit(() -> {
            try {
                BufferedImage img = imageForOcr != null ? imageForOcr : new Robot().createScreenCapture(rectForOcr);
                java.util.List<java.awt.Rectangle> boxes = ocrService.detectBubbles(img);

                SwingUtilities.invokeLater(() -> {
                    if (debugPanel != null) {
                        debugPanel.setDetectedBoxes(boxes);
                        debugPanel.showPreview(img, boxes.size()); // Pass original image, DebugPanel will add labels
                    }
                });

                java.util.List<String> texts = ocrService.ocrBubbles(img, boxes);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < texts.size(); i++) {
                    sb.append("[Bubble ").append(i + 1).append("]\n");
                    sb.append(texts.get(i)).append("\n\n");
                }
                if (texts.isEmpty()) {
                    sb.append("No bubbles detected.");
                }
                ocrOutput.setText(sb.toString());
                // Push detected texts into Translation tab checkbox list
                javax.swing.SwingUtilities.invokeLater(() -> {
                    java.awt.Container top = getTopLevelAncestor();
                    if (top instanceof javax.swing.JFrame) {
                        // Try to find the TranslationPanel in the tabbed pane
                        java.awt.Component[] comps = ((javax.swing.JFrame) top).getContentPane().getComponents();
                        for (java.awt.Component c : comps) {
                            // The app layout: frame -> BorderLayout.CENTER -> JTabbedPane
                            if (c instanceof javax.swing.JTabbedPane) {
                                javax.swing.JTabbedPane tabs = (javax.swing.JTabbedPane) c;
                                for (int i = 0; i < tabs.getTabCount(); i++) {
                                    java.awt.Component tab = tabs.getComponentAt(i);
                                    if (tab instanceof TranslationPanel) {
                                        ((TranslationPanel) tab).setDetectedBubbles(texts);
                                    }
                                }
                            }
                        }
                    }
                });
            } catch (Exception ex) {
                ocrOutput.setText("OCR failed: " + ex.getMessage());
            }
        });
    }
    
    public void processSelectedBubbles(BufferedImage image, List<Rectangle> selectedBoxes) {
        // Create default indices for backward compatibility
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < selectedBoxes.size(); i++) {
            indices.add(i);
        }
        processSelectedBubbles(image, selectedBoxes, indices);
    }
    
    public void processSelectedBubbles(BufferedImage image, List<Rectangle> selectedBoxes, List<Integer> originalIndices) {
        ocrOutput.setText("Processing selected bubbles via OCR...");
        executor.submit(() -> {
            try {
                java.util.List<String> texts = ocrService.ocrBubbles(image, selectedBoxes);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < texts.size(); i++) {
                    int originalBubbleNumber = originalIndices.get(i) + 1; // Convert to 1-based
                    sb.append("[Bubble ").append(originalBubbleNumber).append("]\n");
                    sb.append(texts.get(i)).append("\n\n");
                }
                if (texts.isEmpty()) {
                    sb.append("No text detected in selected bubbles.");
                }
                ocrOutput.setText(sb.toString());
                
                // Push detected texts into Translation tab checkbox list
                javax.swing.SwingUtilities.invokeLater(() -> {
                    java.awt.Container top = getTopLevelAncestor();
                    if (top instanceof javax.swing.JFrame) {
                        // Try to find the TranslationPanel in the tabbed pane
                        java.awt.Component[] comps = ((javax.swing.JFrame) top).getContentPane().getComponents();
                        for (java.awt.Component c : comps) {
                            // The app layout: frame -> BorderLayout.CENTER -> JTabbedPane
                            if (c instanceof javax.swing.JTabbedPane) {
                                javax.swing.JTabbedPane tabs = (javax.swing.JTabbedPane) c;
                                for (int i = 0; i < tabs.getTabCount(); i++) {
                                    java.awt.Component tab = tabs.getComponentAt(i);
                                    if (tab instanceof TranslationPanel) {
                                        ((TranslationPanel) tab).setDetectedBubbles(texts, originalIndices);
                                    }
                                }
                            }
                        }
                    }
                });
            } catch (Exception ex) {
                ocrOutput.setText("OCR failed: " + ex.getMessage());
            }
        });
    }
}


