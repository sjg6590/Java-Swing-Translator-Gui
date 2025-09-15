package com.mangaui.ui;

import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ScreenSelectionOverlay {
    public Rectangle captureUserSelection() {
        SelectionDialog dialog = new SelectionDialog();
        dialog.setVisible(true);
        return dialog.getSelection();
    }

    private static class SelectionDialog extends JDialog {
        private Rectangle selection;
        private Point startPoint;
        private Point endPoint;

        SelectionDialog() {
            setUndecorated(true);
            setAlwaysOnTop(true);
            setModal(true);
            // Enable per-pixel transparency so we can see the live screen through the clear region
            setBackground(new Color(0, 0, 0, 0));
            getRootPane().setOpaque(false);
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            setSize(screen);
            setLocation(0, 0);
            SelectionPane pane = new SelectionPane(screen);
            setContentPane(pane);
        }

        Rectangle getSelection() {
            return selection;
        }

        private class SelectionPane extends JPanel {
            private final Dimension screen;

            SelectionPane(Dimension screen) {
                this.screen = screen;
                setOpaque(false);
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        startPoint = e.getPoint();
                        endPoint = e.getPoint();
                        repaint();
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        endPoint = e.getPoint();
                        selection = toRectangle(startPoint, endPoint);
                        setVisible(false);
                        dispose();
                    }
                });
                addMouseMotionListener(new MouseAdapter() {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        endPoint = e.getPoint();
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                // Draw a translucent dim over the entire screen area
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
                g2.setColor(new Color(0, 0, 0, 255));
                g2.fillRect(0, 0, screen.width, screen.height);

                if (startPoint != null && endPoint != null) {
                    Rectangle r = toRectangle(startPoint, endPoint);
                    g2.setComposite(AlphaComposite.Clear);
                    g2.fillRect(r.x, r.y, r.width, r.height);
                    g2.setComposite(AlphaComposite.SrcOver);
                    g2.setColor(Color.GREEN);
                    g2.setStroke(new BasicStroke(2));
                    g2.drawRect(r.x, r.y, r.width, r.height);
                }
                g2.dispose();
            }

            private Rectangle toRectangle(Point a, Point b) {
                int x = Math.min(a.x, b.x);
                int y = Math.min(a.y, b.y);
                int w = Math.abs(a.x - b.x);
                int h = Math.abs(a.y - b.y);
                return new Rectangle(x, y, w, h);
            }
        }
    }
}


