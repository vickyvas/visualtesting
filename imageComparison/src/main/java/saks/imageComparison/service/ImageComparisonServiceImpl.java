package saks.imageComparison.service;

import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.List;
import java.util.ArrayList;
import net.sourceforge.tess4j.*;

@Service
public class ImageComparisonServiceImpl implements ImageComparisonService{

    private BufferedImage normalizeImage(BufferedImage img) {
        BufferedImage normalized = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = normalized.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return normalized;
    }

    private boolean isPixelDifferent(int rgb1, int rgb2, int tolerance) {
        Color c1 = new Color(rgb1);
        Color c2 = new Color(rgb2);
        return Math.abs(c1.getRed() - c2.getRed()) > tolerance ||
                Math.abs(c1.getGreen() - c2.getGreen()) > tolerance ||
                Math.abs(c1.getBlue() - c2.getBlue()) > tolerance;
    }

    private Rectangle floodFill(boolean[][] map, boolean[][] visited, int startX, int startY, int width, int height) {
        int minX = startX, minY = startY, maxX = startX, maxY = startY;
        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(startX, startY));
        visited[startX][startY] = true;

        int[] dx = {-1, 0, 1, 0};
        int[] dy = {0, -1, 0, 1};

        while (!queue.isEmpty()) {
            Point p = queue.poll();
            for (int i = 0; i < 4; i++) {
                int nx = p.x + dx[i];
                int ny = p.y + dy[i];
                if (nx >= 0 && ny >= 0 && nx < width && ny < height && map[nx][ny] && !visited[nx][ny]) {
                    visited[nx][ny] = true;
                    queue.add(new Point(nx, ny));
                    minX = Math.min(minX, nx);
                    minY = Math.min(minY, ny);
                    maxX = Math.max(maxX, nx);
                    maxY = Math.max(maxY, ny);
                }
            }
        }
        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private void generateSideBySide(BufferedImage img1, BufferedImage img2, BufferedImage diff, File outputFile) throws IOException {
        int labelHeight = 30;
        int width = img1.getWidth() + img2.getWidth() + diff.getWidth();
        int height = labelHeight + Math.max(Math.max(img1.getHeight(), img2.getHeight()), diff.getHeight());

        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = combined.createGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.setColor(Color.BLACK);

        g.drawString("Actual", img1.getWidth() / 2 - 30, 25);
        g.drawString("Expected", img1.getWidth() + img2.getWidth() / 2 - 40, 25);
        g.drawString("Difference", img1.getWidth() + img2.getWidth() + diff.getWidth() / 2 - 50, 25);

        g.drawImage(img1, 0, labelHeight, null);
        g.drawImage(img2, img1.getWidth(), labelHeight, null);
        g.drawImage(diff, img1.getWidth() + img2.getWidth(), labelHeight, null);
        g.dispose();

        boolean sideBySideWritten = ImageIO.write(combined, "png", outputFile);
        System.out.println("Side-by-side image written? " + sideBySideWritten);
    }

    // Method to extract text from specific regions only
    private String extractTextFromDifferenceRegions(BufferedImage actualImage, BufferedImage expectedImage, List<Rectangle> differenceRegions) {
        StringBuilder ocrResult = new StringBuilder();

        try {
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath("/opt/homebrew/share/tessdata");
            tesseract.setLanguage("eng");

            int regionCount = 1;
            for (Rectangle region : differenceRegions) {
                try {
                    // Add some padding to the region for better OCR
                    int padding = 5;
                    int x = Math.max(0, region.x - padding);
                    int y = Math.max(0, region.y - padding);
                    int width = Math.min(actualImage.getWidth() - x, region.width + 2 * padding);
                    int height = Math.min(actualImage.getHeight() - y, region.height + 2 * padding);

                    if (width > 0 && height > 0) {
                        // Extract from actual image
                        BufferedImage actualRegion = actualImage.getSubimage(x, y, width, height);
                        String actualText = tesseract.doOCR(actualRegion).trim();

                        // Extract from expected image
                        BufferedImage expectedRegion = expectedImage.getSubimage(x, y, width, height);
                        String expectedText = tesseract.doOCR(expectedRegion).trim();

                        // Only add if there's meaningful text and they're different
                        if (!actualText.isEmpty() || !expectedText.isEmpty()) {
                            if (!actualText.equals(expectedText)) {
                                ocrResult.append("=== DIFFERENCE REGION ").append(regionCount).append(" ===\n");
                                ocrResult.append("Actual: ").append(actualText.isEmpty() ? "[No text]" : actualText).append("\n");
                                ocrResult.append("Expected: ").append(expectedText.isEmpty() ? "[No text]" : expectedText).append("\n");
                                ocrResult.append("Location: x=").append(region.x).append(", y=").append(region.y)
                                        .append(", width=").append(region.width).append(", height=").append(region.height).append("\n\n");
                                regionCount++;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error processing region " + regionCount + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "OCR initialization failed: " + e.getMessage();
        }

        return ocrResult.length() > 0 ? ocrResult.toString() : "No text differences found in the different regions.";
    }

    public String extractTextFromRegion(BufferedImage image, Rectangle region) {
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath("/opt/homebrew/share/tessdata");

        BufferedImage subImage = image.getSubimage(region.x, region.y, region.width, region.height);
        try {
            return tesseract.doOCR(subImage);
        } catch (TesseractException e) {
            e.printStackTrace();
            return "OCR failed";
        }
    }

    public double calculateMatchScore(BufferedImage img1, BufferedImage img2, int tolerance) {
        int width = Math.max(img1.getWidth(), img2.getWidth());
        int height = Math.max(img1.getHeight(), img2.getHeight());

        long differingPixels = 0;
        long totalPixels = (long) width * height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb1 = (x < img1.getWidth() && y < img1.getHeight()) ? img1.getRGB(x, y) : Color.BLACK.getRGB();
                int rgb2 = (x < img2.getWidth() && y < img2.getHeight()) ? img2.getRGB(x, y) : Color.BLACK.getRGB();
                if (isPixelDifferent(rgb1, rgb2, tolerance)) {
                    differingPixels++;
                }
            }
        }

        double rawScore = 100.0 - (differingPixels * 100.0 / totalPixels);
        return Math.round(rawScore * 100.0) / 100.0;
    }

    @Override
    public String compare(File actualFile, File expectedFile, File diffFile, File sideBySideFile) throws IOException {
        BufferedImage img1 = ImageIO.read(actualFile);
        BufferedImage img2 = ImageIO.read(expectedFile);

        if (img1 == null || img2 == null) {
            throw new IOException("One of the uploaded images is not a valid image file.");
        }

        int width = Math.max(img1.getWidth(), img2.getWidth());
        int height = Math.max(img1.getHeight(), img2.getHeight());

        BufferedImage diffImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        boolean[][] diffMap = new boolean[width][height];
        boolean hasDifference = false;
        List<Rectangle> differenceRegions = new ArrayList<>();

        // Compare pixels
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb1 = (x < img1.getWidth() && y < img1.getHeight()) ? img1.getRGB(x, y) : Color.BLACK.getRGB();
                int rgb2 = (x < img2.getWidth() && y < img2.getHeight()) ? img2.getRGB(x, y) : Color.BLACK.getRGB();

                if (isPixelDifferent(rgb1, rgb2, 10)) {
                    diffImage.setRGB(x, y, img1.getRGB(x, y)); // Show actual image in diff regions
                    diffMap[x][y] = true;
                    hasDifference = true;
                } else {
                    diffImage.setRGB(x, y, img2.getRGB(x, y));
                }
            }
        }

        // Draw bounding boxes and collect difference regions
        if (hasDifference) {
            Graphics2D g = diffImage.createGraphics();
            g.setColor(Color.RED);
            g.setStroke(new BasicStroke(4));
            boolean[][] visited = new boolean[width][height];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (diffMap[x][y] && !visited[x][y]) {
                        Rectangle box = floodFill(diffMap, visited, x, y, width, height);
                        if (box.width * box.height > 4) {
                            g.drawRect(box.x, box.y, box.width, box.height);
                            differenceRegions.add(box); // Collect regions for OCR
                        }
                    }
                }
            }
            g.dispose();
        }

        // Write difference image
        diffImage = normalizeImage(diffImage);
        ImageIO.write(diffImage, "png", diffFile);

        // Generate side-by-side view
        generateSideBySide(img1, img2, diffImage, sideBySideFile);

        // Extract OCR text only from difference regions
        String ocrResult = "";
        if (!differenceRegions.isEmpty()) {
            ocrResult = extractTextFromDifferenceRegions(img1, img2, differenceRegions);
            System.out.println("Extracted OCR Text from Difference Regions:\n" + ocrResult);
        } else {
            ocrResult = "No differences found between images.";
        }

        return ocrResult;
    }
}