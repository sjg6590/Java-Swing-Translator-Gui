package com.mangaui.services;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class MangaOcrService {
    public String ocrSelection(Rectangle rect) throws Exception {
        BufferedImage capture = new Robot().createScreenCapture(rect);
        File temp = File.createTempFile("mangaocr-", ".png");
        ImageIO.write(capture, "png", temp);
        try {
            String raw = runMangaOcr(temp.getAbsolutePath());
            return extractOcrText(raw);
        } finally {
            try { Files.deleteIfExists(temp.toPath()); } catch (IOException ignored) {}
        }
    }

    public List<String> ocrBubbles(Rectangle rect) throws Exception {
        BufferedImage capture = new Robot().createScreenCapture(rect);
        return ocrBubbles(capture);
    }

    public List<String> ocrBubbles(BufferedImage capture) throws Exception {
        BubbleDetector detector = new BubbleDetector();
        List<Rectangle> boxes = detector.detectBubbles(capture);
        List<String> results = new ArrayList<>();
        for (Rectangle box : boxes) {
            BufferedImage sub = capture.getSubimage(box.x, box.y, box.width, box.height);
            File temp = File.createTempFile("mangaocr-bubble-", ".png");
            ImageIO.write(sub, "png", temp);
            try {
                String raw = runMangaOcr(temp.getAbsolutePath());
                results.add(extractOcrText(raw));
            } finally {
                try { Files.deleteIfExists(temp.toPath()); } catch (IOException ignored) {}
            }
        }
        return results;
    }

    public List<String> ocrBubbles(BufferedImage capture, List<Rectangle> boxes) throws Exception {
        List<String> results = new ArrayList<>();
        for (Rectangle box : boxes) {
            BufferedImage sub = capture.getSubimage(box.x, box.y, box.width, box.height);
            File temp = File.createTempFile("mangaocr-bubble-", ".png");
            ImageIO.write(sub, "png", temp);
            try {
                String raw = runMangaOcr(temp.getAbsolutePath());
                results.add(extractOcrText(raw));
            } finally {
                try { Files.deleteIfExists(temp.toPath()); } catch (IOException ignored) {}
            }
        }
        return results;
    }

    public List<Rectangle> detectBubbles(BufferedImage capture) {
        BubbleDetector detector = new BubbleDetector();
        return detector.detectBubbles(capture);
    }

    private String extractOcrText(String raw) {
        if (raw == null) return "";
        String[] lines = raw.split("\r?\n");
        String last = "";
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            // Skip obvious logs/progress
            if (trimmed.startsWith("[") && trimmed.contains("]") && trimmed.toLowerCase().contains("bubble")) continue;
            if (trimmed.matches("\\d{4}-\\d{2}-\\d{2}.*INFO.*")) continue;
            if (trimmed.contains("Fetching") && trimmed.contains("files")) continue;
            if (trimmed.contains("it/s")) continue;
            last = trimmed; // keep last meaningful line
        }
        if (last.isEmpty()) {
            // Fallback to absolute last non-empty line even if it matches patterns
            for (int i = lines.length - 1; i >= 0; i--) {
                if (!lines[i].trim().isEmpty()) return lines[i].trim();
            }
        }
        return last;
    }

    private String runMangaOcr(String imagePath) throws Exception {
        String pythonCmd = resolvePythonCommand();
        List<String> command = new ArrayList<>();
        command.add(pythonCmd);
        command.add("-c");
        command.add("import sys; from PIL import Image; from manga_ocr import MangaOcr; mocr=MangaOcr(); img=Image.open(sys.argv[1]); print(mocr(img))");
        command.add(imagePath);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            int code = process.waitFor();
            if (code != 0) {
                throw new RuntimeException("manga-ocr exited with code " + code + ":\n" + sb);
            }
            return sb.toString().trim();
        }
    }

    private String resolvePythonCommand() {
        String configured = System.getProperty("PYTHON_CMD", System.getenv().getOrDefault("PYTHON_CMD", ""));
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        // Auto-detect local .venv
        String projectDir = System.getProperty("user.dir", "");
        String[] candidates = new String[] {
                projectDir + File.separator + ".venv" + File.separator + "bin" + File.separator + "python",
                projectDir + File.separator + "venv" + File.separator + "bin" + File.separator + "python",
                ".venv/bin/python",
                "venv/bin/python",
                "python3",
                "python"
        };
        for (String c : candidates) {
            File f = new File(c);
            if ((c.equals("python3") || c.equals("python")) || (f.exists() && f.canExecute())) {
                return c;
            }
        }
        return "python3";
    }
}


