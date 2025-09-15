package com.mangaui.services;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class BubbleDetector {
    public List<Rectangle> detectBubbles(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        // Build luminance-based masks with slightly relaxed thresholds
        boolean[][] darkMask = new boolean[height][width];
        boolean[][] lightMask = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int lum = (int)(0.2126 * r + 0.7152 * g + 0.0722 * b);
                darkMask[y][x] = lum <= 120;
                lightMask[y][x] = lum >= 180;
            }
        }

        List<Rectangle> lightBoxes = detectComponents(lightMask, width, height, 2, 2);
        List<Rectangle> darkBoxes = detectComponents(darkMask, width, height, 2, 3);

        List<Rectangle> candidates = new ArrayList<>();
        candidates.addAll(filterBoxes(lightBoxes, darkMask, width, height, width * height));
        // Dark pass: filter to text clusters, then expand a bit
        for (Rectangle r : filterBoxes(darkBoxes, darkMask, width, height, width * height)) {
            Rectangle padded = pad(r, 8, width, height);
            candidates.add(padded);
        }

        // Merge and suppress overly large boxes (>45% of selection area)
        List<Rectangle> mergedBoxes = mergeBoxes(candidates);
        List<Rectangle> finalBoxes = new ArrayList<>();
        int totalArea = width * height;
        for (Rectangle r : mergedBoxes) {
            double areaFrac = (double) (r.width * r.height) / (double) totalArea;
            if (areaFrac > 0.45) continue;
            finalBoxes.add(r);
        }

        // Sort reading order: top-to-bottom, then left-to-right
        finalBoxes.sort((r1, r2) -> {
            int dyv = Integer.compare(r1.y, r2.y);
            if (Math.abs(r1.y - r2.y) < 20) {
                return Integer.compare(r1.x, r2.x);
            }
            return dyv;
        });

        // Fallback: if no boxes, return the full region minimally padded
        if (finalBoxes.isEmpty()) {
            finalBoxes.add(new Rectangle(0, 0, width, height));
        }

        return finalBoxes;
    }

    private static List<Rectangle> detectComponents(boolean[][] mask, int width, int height, int radius, int iterations) {
        boolean[][] dil = dilate(mask, width, height, radius, iterations);
        boolean[][] visited = new boolean[height][width];
        List<Rectangle> boxes = new ArrayList<>();
        int[] dx = new int[] {1, -1, 0, 0, 1, 1, -1, -1};
        int[] dy = new int[] {0, 0, 1, -1, 1, -1, 1, -1};
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!dil[y][x] || visited[y][x]) continue;
                int minX = x, maxX = x, minY = y, maxY = y;
                int area = 0;
                Deque<int[]> stack = new ArrayDeque<>();
                stack.push(new int[] {x, y});
                visited[y][x] = true;
                while (!stack.isEmpty()) {
                    int[] p = stack.pop();
                    int cx = p[0];
                    int cy = p[1];
                    area++;
                    if (cx < minX) minX = cx;
                    if (cx > maxX) maxX = cx;
                    if (cy < minY) minY = cy;
                    if (cy > maxY) maxY = cy;
                    for (int k = 0; k < 8; k++) {
                        int nx = cx + dx[k];
                        int ny = cy + dy[k];
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            if (!visited[ny][nx] && dil[ny][nx]) {
                                visited[ny][nx] = true;
                                stack.push(new int[] {nx, ny});
                            }
                        }
                    }
                }
                int w = maxX - minX + 1;
                int h = maxY - minY + 1;
                if (area < 500) continue;
                if (w < 15 || h < 15) continue;
                boxes.add(new Rectangle(minX, minY, w, h));
            }
        }
        return boxes;
    }

    private static List<Rectangle> filterBoxes(List<Rectangle> boxes, boolean[][] darkMask, int width, int height, int totalArea) {
        List<Rectangle> filtered = new ArrayList<>();
        for (Rectangle r : boxes) {
            // Remove boxes hugging edges and very large ones
            boolean touchesLeft = r.x <= 2;
            boolean touchesRight = r.x + r.width >= width - 3;
            boolean touchesTop = r.y <= 2;
            boolean touchesBottom = r.y + r.height >= height - 3;
            if ((touchesLeft && touchesRight) || (touchesTop && touchesBottom)) {
                if (r.width * r.height > totalArea * 0.5) continue;
            }
            double aspect = (double) r.width / (double) r.height;
            if (aspect < 0.25 || aspect > 6.0) continue;
            int darkCount = countMask(darkMask, r, width, height);
            double darkFrac = (double) darkCount / (double) (r.width * r.height);
            if (darkCount < 200) continue;
            if (darkFrac < 0.01) continue;
            filtered.add(r);
        }
        return filtered;
    }

    private static List<Rectangle> mergeBoxes(List<Rectangle> boxes) {
        boolean merged;
        List<Rectangle> working = new ArrayList<>(boxes);
        do {
            merged = false;
            List<Rectangle> next = new ArrayList<>();
            boolean[] used = new boolean[working.size()];
            for (int i = 0; i < working.size(); i++) {
                if (used[i]) continue;
                Rectangle a = working.get(i);
                Rectangle accum = new Rectangle(a);
                for (int j = i + 1; j < working.size(); j++) {
                    if (used[j]) continue;
                    Rectangle b = working.get(j);
                    if (iou(accum, b) > 0.2 || isClose(accum, b, 8)) {
                        accum = accum.union(b);
                        used[j] = true;
                        merged = true;
                    }
                }
                used[i] = true;
                next.add(accum);
            }
            working = next;
        } while (merged);
        return working;
    }

    private static boolean[][] dilate(boolean[][] src, int width, int height, int radius, int iterations) {
        boolean[][] a = src;
        for (int it = 0; it < iterations; it++) {
            boolean[][] b = new boolean[height][width];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    boolean val = false;
                    for (int yy = Math.max(0, y - radius); yy <= Math.min(height - 1, y + radius) && !val; yy++) {
                        for (int xx = Math.max(0, x - radius); xx <= Math.min(width - 1, x + radius); xx++) {
                            if (a[yy][xx]) { val = true; break; }
                        }
                    }
                    b[y][x] = val;
                }
            }
            a = b;
        }
        return a;
    }

    private static double iou(Rectangle a, Rectangle b) {
        int x1 = Math.max(a.x, b.x);
        int y1 = Math.max(a.y, b.y);
        int x2 = Math.min(a.x + a.width, b.x + b.width);
        int y2 = Math.min(a.y + a.height, b.y + b.height);
        int iw = Math.max(0, x2 - x1);
        int ih = Math.max(0, y2 - y1);
        double inter = (double) iw * ih;
        if (inter <= 0) return 0.0;
        double union = (double) a.width * a.height + (double) b.width * b.height - inter;
        return inter / union;
    }

    private static boolean isClose(Rectangle a, Rectangle b, int pad) {
        Rectangle ap = new Rectangle(a.x - pad, a.y - pad, a.width + 2 * pad, a.height + 2 * pad);
        return ap.intersects(b);
    }

    private static int countMask(boolean[][] mask, Rectangle r, int width, int height) {
        int x1 = Math.max(0, r.x);
        int y1 = Math.max(0, r.y);
        int x2 = Math.min(width - 1, r.x + r.width - 1);
        int y2 = Math.min(height - 1, r.y + r.height - 1);
        int count = 0;
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) {
                if (mask[y][x]) count++;
            }
        }
        return count;
    }

    private static Rectangle pad(Rectangle r, int pad, int width, int height) {
        int nx = Math.max(0, r.x - pad);
        int ny = Math.max(0, r.y - pad);
        int nw = Math.min(width - nx, r.width + 2 * pad);
        int nh = Math.min(height - ny, r.height + 2 * pad);
        return new Rectangle(nx, ny, nw, nh);
    }
}


