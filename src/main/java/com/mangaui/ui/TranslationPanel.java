package com.mangaui.ui;

import com.mangaui.services.DeepLClient;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.BoxLayout;
import javax.swing.JSplitPane;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.TransferHandler;
import javax.swing.JComponent;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.awt.Color;
import java.awt.Font;

public class TranslationPanel extends JPanel {
    private final DeepLClient deepLClient;
    private final JTextArea inputArea;
    private final JTextArea outputArea;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final JPanel bubblesPanel;
    private final JButton translateSelectedButton;
    private DefaultListModel<BubbleItem> bubbleModel = new DefaultListModel<>();
    private JList<BubbleItem> bubbleList;

    public TranslationPanel(DeepLClient deepLClient) {
        super(new BorderLayout());
        this.deepLClient = deepLClient;
        
        // Set panel background
        setBackground(ColorTheme.BACKGROUND_MAIN);
        
        inputArea = new JTextArea();
        inputArea.setBorder(BorderFactory.createTitledBorder("Input (detected by OCR)"));
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBackground(ColorTheme.BACKGROUND_CARD);
        inputArea.setForeground(ColorTheme.TEXT_PRIMARY);
        inputArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        inputArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorTheme.BORDER_LIGHT, 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        outputArea = new JTextArea();
        outputArea.setBorder(BorderFactory.createTitledBorder("English Translation"));
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setEditable(false);
        outputArea.setBackground(ColorTheme.BACKGROUND_CARD);
        outputArea.setForeground(ColorTheme.TEXT_PRIMARY);
        outputArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        outputArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorTheme.BORDER_LIGHT, 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        JButton translateButton = createStyledButton("Translate to English");
        translateButton.addActionListener(e -> translate());

        bubblesPanel = new JPanel();
        bubblesPanel.setLayout(new BoxLayout(bubblesPanel, BoxLayout.Y_AXIS));
        bubblesPanel.setBackground(ColorTheme.BACKGROUND_MAIN);
        
        JPanel bubblesContainer = new JPanel(new BorderLayout());
        bubblesContainer.setBackground(ColorTheme.BACKGROUND_MAIN);
        bubblesContainer.setBorder(BorderFactory.createTitledBorder("Bubbles (select to translate)"));
        bubblesContainer.add(new JScrollPane(bubblesPanel), BorderLayout.CENTER);

        translateSelectedButton = createStyledButton("Translate Selected Bubbles");
        translateSelectedButton.addActionListener(e -> translateSelected());

        // Drag-and-drop reorderable checkbox list
        bubbleList = new JList<>(bubbleModel);
        bubbleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bubbleList.setCellRenderer(new BubbleRenderer());
        bubbleList.setDragEnabled(true);
        bubbleList.setDropMode(javax.swing.DropMode.INSERT);
        bubbleList.setTransferHandler(new BubbleReorderHandler());
        bubbleList.setBackground(ColorTheme.BACKGROUND_CARD);
        bubbleList.setForeground(ColorTheme.TEXT_PRIMARY);
        bubbleList.setSelectionBackground(ColorTheme.SELECTION_BG);
        bubbleList.setSelectionForeground(ColorTheme.SELECTION_FG);
        bubbleList.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        bubbleList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int idx = bubbleList.locationToIndex(e.getPoint());
                if (idx >= 0 && idx < bubbleModel.size()) {
                    BubbleItem item = bubbleModel.get(idx);
                    item.selected = !item.selected;
                    bubbleList.repaint(bubbleList.getCellBounds(idx, idx));
                }
            }
        });

        JPanel north = new JPanel(new BorderLayout());
        north.add(translateSelectedButton, BorderLayout.WEST);
        north.add(translateButton, BorderLayout.EAST);

        add(north, BorderLayout.NORTH);

        // Create resizable split panes: left = bubbles list, right = vertical split of input/output
        JScrollPane bubblesScroll = new JScrollPane(bubbleList);
        JScrollPane inputScroll = new JScrollPane(inputArea);
        JScrollPane outputScroll = new JScrollPane(outputArea);
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputScroll, outputScroll);
        verticalSplit.setResizeWeight(0.5);
        verticalSplit.setContinuousLayout(true);
        verticalSplit.setOneTouchExpandable(true);

        JSplitPane horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, bubblesScroll, verticalSplit);
        horizontalSplit.setResizeWeight(0.2);
        horizontalSplit.setContinuousLayout(true);
        horizontalSplit.setOneTouchExpandable(true);

        add(horizontalSplit, BorderLayout.CENTER);
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

    private void translate() {
        String text = inputArea.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        outputArea.setText("Translating via DeepL...");
        executor.submit(() -> {
            try {
                String translated = deepLClient.translateToEnglish(text);
                outputArea.setText(translated);
            } catch (Exception ex) {
                outputArea.setText("Translation failed: " + ex.getMessage());
            }
        });
    }

    private void translateSelected() {
        List<Integer> selectedIndexes = new java.util.ArrayList<>();
        for (int i = 0; i < bubbleModel.size(); i++) {
            if (bubbleModel.get(i).selected) selectedIndexes.add(i);
        }
        if (selectedIndexes.isEmpty()) {
            return;
        }
        outputArea.setText("Translating selected via DeepL...");
        executor.submit(() -> {
            try {
                StringBuilder out = new StringBuilder();
                for (int idx : selectedIndexes) {
                    String source = bubbleModel.get(idx).text;
                    String translated = deepLClient.translateToEnglish(source);
                    if (out.length() > 0) out.append("\n\n");
                    out.append("[").append(idx + 1).append("] ").append(translated);
                }
                outputArea.setText(out.toString());
            } catch (Exception ex) {
                outputArea.setText("Translation failed: " + ex.getMessage());
            }
        });
    }

    public void setDetectedBubbles(List<String> texts) {
        // Create default indices for backward compatibility
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            indices.add(i);
        }
        setDetectedBubbles(texts, indices);
    }
    
    public void setDetectedBubbles(List<String> texts, List<Integer> originalIndices) {
        bubbleModel.clear();
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            int originalIndex = originalIndices.get(i);
            bubbleModel.addElement(new BubbleItem(text, true, originalIndex));
        }
        bubbleList.repaint();
    }

    private static class BubbleItem implements Serializable {
        String text;
        boolean selected;
        int originalIndex;
        
        BubbleItem(String text, boolean selected, int originalIndex) { 
            this.text = text; 
            this.selected = selected; 
            this.originalIndex = originalIndex;
        }
        
        @Override public String toString() { return text; }
    }

    private static class BubbleRenderer extends JCheckBox implements ListCellRenderer<BubbleItem> {
        @Override
        public Component getListCellRendererComponent(JList<? extends BubbleItem> list, BubbleItem value, int index, boolean isSelected, boolean cellHasFocus) {
            String label = value.text;
            if (label.length() > 80) label = label.substring(0, 80) + "...";
            
            // Use original index if available, otherwise fall back to sequential numbering
            int bubbleNumber = (value.originalIndex >= 0) ? (value.originalIndex + 1) : (index + 1);
            setText("[" + bubbleNumber + "] " + label);
            setSelected(value.selected);
            
            // Apply colors based on selection state
            if (isSelected) {
                setBackground(ColorTheme.SELECTION_BG);
                setForeground(ColorTheme.SELECTION_FG);
            } else {
                setBackground(ColorTheme.BACKGROUND_CARD);
                setForeground(ColorTheme.TEXT_PRIMARY);
            }
            
            // Color the checkbox based on whether the bubble is selected
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

    private class BubbleReorderHandler extends TransferHandler {
        private final DataFlavor flavor = new DataFlavor(BubbleItem.class, "BubbleItem");
        private int fromIndex = -1;

        @Override
        public int getSourceActions(JComponent c) { return MOVE; }

        @Override
        protected Transferable createTransferable(JComponent c) {
            fromIndex = bubbleList.getSelectedIndex();
            BubbleItem item = bubbleModel.get(fromIndex);
            return new Transferable() {
                @Override public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[] { flavor }; }
                @Override public boolean isDataFlavorSupported(DataFlavor f) { return flavor.equals(f); }
                @Override public Object getTransferData(DataFlavor f) { return item; }
            };
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(flavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            try {
                int toIndex = ((JList.DropLocation) support.getDropLocation()).getIndex();
                if (fromIndex == -1 || toIndex == fromIndex) return false;
                BubbleItem item = bubbleModel.remove(fromIndex);
                if (toIndex > fromIndex) toIndex--; // account for removal
                bubbleModel.add(toIndex, item);
                bubbleList.setSelectedIndex(toIndex);
                bubbleList.repaint();
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}


