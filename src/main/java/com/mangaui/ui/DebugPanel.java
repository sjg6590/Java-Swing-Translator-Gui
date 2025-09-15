package com.mangaui.ui;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.DefaultListModel;
import javax.swing.ListCellRenderer;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.awt.Font;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;
import java.util.ArrayList;

public class DebugPanel extends JPanel {
    private final JLabel infoLabel;
    private final JLabel imageLabel;
    private final JList<BubbleItem> bubbleList;
    private final DefaultListModel<BubbleItem> bubbleModel;
    private final JButton processSelectedButton;
    private final JButton manualSelectButton;
    private List<Rectangle> detectedBoxes;
    private BufferedImage currentImage;
    private BufferedImage baseImage;
    private OcrPanel ocrPanel;
    
    // Manual selection state
    private boolean manualSelectMode = false;
    private boolean isDrawing = false;
    private int startX, startY, endX, endY;
    private BufferedImage originalImage;

    public DebugPanel() {
        super(new BorderLayout());
        
        // Set panel background
        setBackground(ColorTheme.BACKGROUND_MAIN);
        
        infoLabel = new JLabel("No debug image yet.");
        infoLabel.setForeground(ColorTheme.TEXT_PRIMARY);
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        infoLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorTheme.BORDER_LIGHT, 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        infoLabel.setBackground(ColorTheme.BACKGROUND_CARD);
        infoLabel.setOpaque(true);
        
        imageLabel = new JLabel();
        imageLabel.setBackground(ColorTheme.BACKGROUND_CARD);
        imageLabel.setOpaque(true);
        
        // Bubble selection list
        bubbleModel = new DefaultListModel<>();
        bubbleList = new JList<>(bubbleModel);
        bubbleList.setCellRenderer(new BubbleItemRenderer());
        bubbleList.setBackground(ColorTheme.BACKGROUND_CARD);
        bubbleList.setForeground(ColorTheme.TEXT_PRIMARY);
        bubbleList.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        
        // Add mouse listener for checkbox toggling
        bubbleList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int idx = bubbleList.locationToIndex(e.getPoint());
                if (idx >= 0 && idx < bubbleModel.size()) {
                    BubbleItem item = bubbleModel.get(idx);
                    item.selected = !item.selected;
                    bubbleList.repaint(bubbleList.getCellBounds(idx, idx));
                }
            }
        });
        
        // Process selected button
        processSelectedButton = createStyledButton("Process Selected Bubbles");
        processSelectedButton.addActionListener(e -> processSelectedBubbles());
        processSelectedButton.setEnabled(false);
        
        // Manual select button
        manualSelectButton = createStyledButton("Manual Select");
        manualSelectButton.addActionListener(e -> toggleManualSelectMode());
        manualSelectButton.setEnabled(false);
        
        // Control panel
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBackground(ColorTheme.BACKGROUND_MAIN);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(ColorTheme.BACKGROUND_MAIN);
        buttonPanel.add(processSelectedButton);
        buttonPanel.add(manualSelectButton);
        
        controlPanel.add(buttonPanel, BorderLayout.WEST);
        controlPanel.add(infoLabel, BorderLayout.CENTER);
        
        // Split pane for image and bubble list
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(new JScrollPane(imageLabel));
        splitPane.setRightComponent(new JScrollPane(bubbleList));
        splitPane.setResizeWeight(0.7); // Give more space to image
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);
        
        add(controlPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        
        // Add mouse listeners for manual selection
        setupManualSelectionListeners();
    }
    
    private void setupManualSelectionListeners() {
        imageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (manualSelectMode && currentImage != null) {
                    isDrawing = true;
                    int[] coords = getImageCoordinates(e.getX(), e.getY());
                    startX = coords[0];
                    startY = coords[1];
                    endX = startX;
                    endY = startY;
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (manualSelectMode && isDrawing && currentImage != null) {
                    isDrawing = false;
                    int[] coords = getImageCoordinates(e.getX(), e.getY());
                    endX = coords[0];
                    endY = coords[1];
                    
                    // Create rectangle from selection
                    int x = Math.min(startX, endX);
                    int y = Math.min(startY, endY);
                    int width = Math.abs(endX - startX);
                    int height = Math.abs(endY - startY);
                    
                    if (width > 10 && height > 10) { // Minimum size threshold
                        Rectangle newBox = new Rectangle(x, y, width, height);
                        addManualBubble(newBox);
                    } else {
                        // If selection too small, just redraw without the preview
                        redrawImageWithAllBoxes();
                    }
                }
            }
        });
        
        imageLabel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (manualSelectMode && isDrawing && currentImage != null) {
                    int[] coords = getImageCoordinates(e.getX(), e.getY());
                    endX = coords[0];
                    endY = coords[1];
                    
                    // Draw preview rectangle
                    drawPreviewRectangle();
                }
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {
                if (manualSelectMode && currentImage != null) {
                    // Show mouse position for debugging
                    int[] coords = getImageCoordinates(e.getX(), e.getY());
                    infoLabel.setText("Manual Select Mode: Mouse at (" + coords[0] + ", " + coords[1] + ") - Click and drag to create bounding boxes");
                }
            }
        });
    }
    
    private int[] getImageCoordinates(int mouseX, int mouseY) {
        if (currentImage == null) {
            return new int[]{mouseX, mouseY};
        }
        
        // Get the image icon and its display size
        ImageIcon icon = (ImageIcon) imageLabel.getIcon();
        if (icon == null) {
            return new int[]{mouseX, mouseY};
        }
        
        int imageWidth = currentImage.getWidth();
        int imageHeight = currentImage.getHeight();
        
        // Get the actual displayed icon dimensions
        int iconWidth = icon.getIconWidth();
        int iconHeight = icon.getIconHeight();
        
        // Get the label size
        int labelWidth = imageLabel.getWidth();
        int labelHeight = imageLabel.getHeight();
        
        // Calculate the offset (image is centered in the label)
        int offsetX = (labelWidth - iconWidth) / 2;
        int offsetY = (labelHeight - iconHeight) / 2;
        
        // Adjust mouse coordinates by removing the offset
        int adjustedMouseX = mouseX - offsetX;
        int adjustedMouseY = mouseY - offsetY;
        
        // Calculate scale factor from icon size to actual image size
        double scaleX = (double) imageWidth / iconWidth;
        double scaleY = (double) imageHeight / iconHeight;
        
        // Convert to image coordinates
        int imageX = (int) (adjustedMouseX * scaleX);
        int imageY = (int) (adjustedMouseY * scaleY);
        
        // Try a different approach - use the label's insets
        java.awt.Insets insets = imageLabel.getInsets();
        if (insets != null) {
            adjustedMouseX = mouseX - offsetX - insets.left;
            adjustedMouseY = mouseY - offsetY - insets.top;
        }
        
        // Convert to image coordinates first
        imageX = (int) (adjustedMouseX * scaleX);
        imageY = (int) (adjustedMouseY * scaleY);
        
        // Manual X offset adjustment - change this value to dial in the correct position
        // This offset is now in image coordinates, so it's consistent regardless of scale
        int manualXOffset = 0; // Adjust this value to move selection right/left (in image pixels)
        imageX += manualXOffset;
        
        // Clamp to image bounds
        imageX = Math.max(0, Math.min(imageX, imageWidth - 1));
        imageY = Math.max(0, Math.min(imageY, imageHeight - 1));
        
        return new int[]{imageX, imageY};
    }
    
    private void drawPreviewRectangle() {
        if (baseImage == null) return;
        
        BufferedImage preview = new BufferedImage(baseImage.getWidth(), baseImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = preview.createGraphics();
        g2.drawImage(baseImage, 0, 0, null);
        
        // Draw all existing boxes first
        if (detectedBoxes != null) {
            g2.setColor(ColorTheme.OCR_DETECTION_BOX);
            g2.setStroke(new java.awt.BasicStroke(2f));
            
            for (int i = 0; i < detectedBoxes.size(); i++) {
                Rectangle b = detectedBoxes.get(i);
                g2.drawRect(b.x, b.y, b.width, b.height);
                
                // Draw number
                String number = String.valueOf(i + 1);
                g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
                
                int textX = b.x + 5;
                int textY = b.y + 20;
                
                // Draw background circle
                int circleSize = 20;
                g2.setColor(new Color(255, 255, 255, 200));
                g2.fillOval(textX - 2, textY - 18, circleSize, circleSize);
                
                g2.setColor(ColorTheme.OCR_DETECTION_BOX);
                g2.setStroke(new java.awt.BasicStroke(2f));
                g2.drawOval(textX - 2, textY - 18, circleSize, circleSize);
                
                g2.setColor(new Color(0, 0, 0, 255));
                g2.drawString(number, textX + 2, textY - 2);
            }
        }
        
        // Draw preview rectangle (current selection)
        g2.setColor(new Color(255, 0, 0, 150)); // Semi-transparent red
        g2.setStroke(new java.awt.BasicStroke(3f));
        
        int x = Math.min(startX, endX);
        int y = Math.min(startY, endY);
        int width = Math.abs(endX - startX);
        int height = Math.abs(endY - startY);
        
        g2.drawRect(x, y, width, height);
        
        // Fill with semi-transparent red
        g2.setColor(new Color(255, 0, 0, 50));
        g2.fillRect(x, y, width, height);
        
        g2.dispose();
        
        imageLabel.setIcon(new ImageIcon(preview));
    }
    
    private void toggleManualSelectMode() {
        manualSelectMode = !manualSelectMode;
        
        if (manualSelectMode) {
            manualSelectButton.setText("Exit Manual Select");
            manualSelectButton.setBackground(ColorTheme.ERROR_RED);
            infoLabel.setText("Manual Select Mode: Click and drag to create bounding boxes");
            if (baseImage != null) {
                originalImage = new BufferedImage(baseImage.getWidth(), baseImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = originalImage.createGraphics();
                g2.drawImage(baseImage, 0, 0, null);
                g2.dispose();
                imageLabel.setIcon(new ImageIcon(originalImage));
            }
        } else {
            manualSelectButton.setText("Manual Select");
            manualSelectButton.setBackground(ColorTheme.BUTTON_PRIMARY);
            infoLabel.setText("Detected bubbles: " + (detectedBoxes != null ? detectedBoxes.size() : 0) + " - Select which ones to process");
            if (baseImage != null) {
                // Redraw labeled image for display when exiting manual mode
                BufferedImage display = createImageWithLabels(baseImage);
                imageLabel.setIcon(display != null ? new ImageIcon(display) : null);
            }
        }
    }
    
    private void addManualBubble(Rectangle box) {
        if (detectedBoxes == null) {
            detectedBoxes = new ArrayList<>();
        }
        
        detectedBoxes.add(box);
        
        // Add to bubble list
        bubbleModel.addElement(new BubbleItem(detectedBoxes.size() - 1, box, true));
        
        // Update info label
        infoLabel.setText("Detected bubbles: " + detectedBoxes.size() + " - Select which ones to process");
        
        // Enable buttons
        processSelectedButton.setEnabled(true);
        
        // Redraw image with all boxes including the new one
        redrawImageWithAllBoxes();
        
        // Scroll to the new item in the list
        bubbleList.ensureIndexIsVisible(bubbleModel.size() - 1);
    }
    
    private void redrawImageWithAllBoxes() {
        if (baseImage == null || detectedBoxes == null) return;
        
        BufferedImage preview = new BufferedImage(baseImage.getWidth(), baseImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = preview.createGraphics();
        g2.drawImage(baseImage, 0, 0, null);
        
        // Enable anti-aliasing
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        g2.setColor(ColorTheme.OCR_DETECTION_BOX);
        g2.setStroke(new java.awt.BasicStroke(2f));
        
        // Draw all boxes with numbers
        for (int i = 0; i < detectedBoxes.size(); i++) {
            Rectangle b = detectedBoxes.get(i);
            
            // Draw the rectangle
            g2.drawRect(b.x, b.y, b.width, b.height);
            
            // Draw the number label
            String number = String.valueOf(i + 1);
            g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
            
            int textX = b.x + 5;
            int textY = b.y + 20;
            
            // Draw background circle
            int circleSize = 20;
            g2.setColor(new Color(255, 255, 255, 200));
            g2.fillOval(textX - 2, textY - 18, circleSize, circleSize);
            
            g2.setColor(ColorTheme.OCR_DETECTION_BOX);
            g2.setStroke(new java.awt.BasicStroke(2f));
            g2.drawOval(textX - 2, textY - 18, circleSize, circleSize);
            
            g2.setColor(new Color(0, 0, 0, 255));
            g2.drawString(number, textX + 2, textY - 2);
        }
        g2.dispose();
        
        imageLabel.setIcon(new ImageIcon(preview));
    }
    
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text) {
            @Override
            public Color getForeground() {
                return isEnabled() ? super.getForeground() : ColorTheme.TEXT_MUTED;
            }
        };
        button.setBackground(ColorTheme.BUTTON_PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorTheme.BORDER_MEDIUM, 1),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        
        // Add hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(ColorTheme.BUTTON_PRIMARY_HOVER);
                }
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(ColorTheme.BUTTON_PRIMARY);
                }
            }
        });
        
        return button;
    }
    
    public void setOcrPanel(OcrPanel ocrPanel) {
        this.ocrPanel = ocrPanel;
    }
    
    private BufferedImage createImageWithLabels(BufferedImage originalImage) {
        if (originalImage == null || detectedBoxes == null) {
            return originalImage;
        }
        
        BufferedImage labeledImage = new BufferedImage(
            originalImage.getWidth(), 
            originalImage.getHeight(), 
            BufferedImage.TYPE_INT_RGB
        );
        Graphics2D g2 = labeledImage.createGraphics();
        g2.drawImage(originalImage, 0, 0, null);
        
        // Draw numbered labels
        g2.setColor(ColorTheme.OCR_DETECTION_BOX);
        g2.setStroke(new BasicStroke(2f));
        
        for (int i = 0; i < detectedBoxes.size(); i++) {
            Rectangle b = detectedBoxes.get(i);
            
            // Draw the rectangle
            g2.drawRect(b.x, b.y, b.width, b.height);
            
            // Draw the number label
            String number = String.valueOf(i + 1);
            g2.setFont(new Font("Arial", Font.BOLD, 16));
            
            // Calculate text position (top-left corner of the box)
            int textX = b.x + 5;
            int textY = b.y + 20;
            
            // Draw background circle for the number
            int circleSize = 20;
            g2.setColor(new Color(255, 255, 255, 200)); // Semi-transparent white
            g2.fillOval(textX - 2, textY - 18, circleSize, circleSize);
            
            // Draw circle border
            g2.setColor(ColorTheme.OCR_DETECTION_BOX);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(textX - 2, textY - 18, circleSize, circleSize);
            
            // Draw the number text
            g2.setColor(new Color(0, 0, 0, 255)); // Black text
            g2.drawString(number, textX + 2, textY - 2);
        }
        g2.dispose();
        return labeledImage;
    }

    public void showPreview(BufferedImage image, int detectedCount) {
        this.baseImage = image;
        this.currentImage = image;
        infoLabel.setText("Detected bubbles: " + detectedCount + " - Select which ones to process");
        
        // Create a copy with numbered labels for display
        BufferedImage displayImage = createImageWithLabels(image);
        imageLabel.setIcon(displayImage != null ? new ImageIcon(displayImage) : null);
        
        // Populate bubble list
        bubbleModel.clear();
        if (detectedBoxes != null) {
            for (int i = 0; i < detectedBoxes.size(); i++) {
                Rectangle box = detectedBoxes.get(i);
                bubbleModel.addElement(new BubbleItem(i, box, true)); // Default to selected
            }
        }
        
        processSelectedButton.setEnabled(detectedCount > 0);
        manualSelectButton.setEnabled(true);
        revalidate();
        repaint();
    }
    
    public void setDetectedBoxes(List<Rectangle> boxes) {
        this.detectedBoxes = boxes;
    }
    
    private void processSelectedBubbles() {
        if (ocrPanel == null || currentImage == null || detectedBoxes == null) {
            return;
        }
        
        // Get selected bubbles with their original indices
        List<Rectangle> selectedBoxes = new ArrayList<>();
        List<Integer> selectedIndices = new ArrayList<>();
        for (int i = 0; i < bubbleModel.size(); i++) {
            BubbleItem item = bubbleModel.get(i);
            if (item.selected) {
                selectedBoxes.add(item.box);
                selectedIndices.add(item.index); // Keep original bubble number
            }
        }
        
        if (selectedBoxes.isEmpty()) {
            return;
        }
        
        // Process selected bubbles through OCR with original indices using the unmodified base image
        ocrPanel.processSelectedBubbles(baseImage, selectedBoxes, selectedIndices);
    }
    
    // Inner class for bubble items
    private static class BubbleItem {
        final int index;
        final Rectangle box;
        boolean selected;
        
        BubbleItem(int index, Rectangle box, boolean selected) {
            this.index = index;
            this.box = box;
            this.selected = selected;
        }
        
        @Override
        public String toString() {
            return String.format("Bubble %d: %dx%d at (%d,%d)", 
                index + 1, box.width, box.height, box.x, box.y);
        }
    }
    
    // Custom renderer for bubble list
    private static class BubbleItemRenderer extends JCheckBox implements ListCellRenderer<BubbleItem> {
        @Override
        public Component getListCellRendererComponent(JList<? extends BubbleItem> list, 
                BubbleItem value, int index, boolean isSelected, boolean cellHasFocus) {
            
            setText(value.toString());
            setSelected(value.selected);
            
            if (isSelected) {
                setBackground(ColorTheme.SELECTION_BG);
                setForeground(ColorTheme.SELECTION_FG);
            } else {
                setBackground(ColorTheme.BACKGROUND_CARD);
                setForeground(ColorTheme.TEXT_PRIMARY);
            }
            
            if (value.selected) {
                setForeground(ColorTheme.BUBBLE_SELECTED);
            } else {
                setForeground(ColorTheme.BUBBLE_UNSELECTED);
            }
            
            setFont(new Font("Segoe UI", Font.PLAIN, 11));
            setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            
            return this;
        }
    }
}


