package com.mangaui.ui;

import java.awt.Color;

/**
 * Color theme for the Manga Translator GUI application.
 * Provides a modern, manga-inspired color palette with good contrast and readability.
 */
public class ColorTheme {
    
    // Primary colors - modern, clean palette
    public static final Color PRIMARY_BLUE = new Color(52, 144, 220);      // Modern blue
    public static final Color PRIMARY_PURPLE = new Color(155, 89, 182);    // Soft purple
    public static final Color ACCENT_CYAN = new Color(26, 188, 156);       // Teal accent
    
    // Background colors - clean and modern
    public static final Color BACKGROUND_LIGHT = new Color(245, 245, 245); // Very light gray
    public static final Color BACKGROUND_MAIN = new Color(255, 255, 255);  // Pure white
    public static final Color BACKGROUND_DARK = new Color(44, 62, 80);     // Dark slate
    public static final Color BACKGROUND_CARD = new Color(248, 249, 250);  // Off-white
    
    // Text colors - excellent readability
    public static final Color TEXT_PRIMARY = new Color(33, 37, 41);        // Dark gray
    public static final Color TEXT_SECONDARY = new Color(73, 80, 87);      // Medium gray
    public static final Color TEXT_MUTED = new Color(108, 117, 125);       // Light gray
    public static final Color TEXT_ON_DARK = new Color(255, 255, 255);     // White
    
    // Status colors - modern and accessible
    public static final Color SUCCESS_GREEN = new Color(40, 167, 69);      // Bootstrap green
    public static final Color WARNING_ORANGE = new Color(255, 193, 7);     // Bootstrap warning
    public static final Color ERROR_RED = new Color(220, 53, 69);          // Bootstrap danger
    public static final Color INFO_BLUE = new Color(23, 162, 184);         // Bootstrap info
    
    // Interactive colors - professional and modern
    public static final Color BUTTON_PRIMARY = new Color(52, 144, 220);    // Modern blue
    public static final Color BUTTON_PRIMARY_HOVER = new Color(41, 128, 185); // Darker blue
    public static final Color BUTTON_SECONDARY = new Color(108, 117, 125);   // Bootstrap secondary
    public static final Color BUTTON_SECONDARY_HOVER = new Color(90, 98, 104); // Darker gray
    
    // Border colors - subtle and clean
    public static final Color BORDER_LIGHT = new Color(222, 226, 230);     // Bootstrap border
    public static final Color BORDER_MEDIUM = new Color(206, 212, 218);    // Medium border
    public static final Color BORDER_DARK = new Color(173, 181, 189);      // Dark border
    
    // Selection colors - modern and accessible
    public static final Color SELECTION_BG = new Color(52, 144, 220, 30);  // Blue with transparency
    public static final Color SELECTION_FG = new Color(33, 37, 41);       // Dark text
    public static final Color HOVER_BG = new Color(248, 249, 250);         // Light hover
    
    // Bubble/OCR specific colors - clear and distinct
    public static final Color BUBBLE_SELECTED = new Color(40, 167, 69);    // Success green
    public static final Color BUBBLE_UNSELECTED = new Color(108, 117, 125); // Muted gray
    public static final Color OCR_DETECTION_BOX = new Color(40, 167, 69, 120); // Green with transparency
    
    // Tab colors
    public static final Color TAB_SELECTED = PRIMARY_BLUE;
    public static final Color TAB_UNSELECTED = TEXT_SECONDARY;
    public static final Color TAB_BACKGROUND = BACKGROUND_LIGHT;
    
    private ColorTheme() {
        // Utility class - prevent instantiation
    }
}
