package com.example.imagecomparison;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.awt.Point;
import java.util.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import java.io.IOException;

public class ImageDiffHighlighter {

    // Normalize input image to RGB
    private static BufferedImage normalizeImage(BufferedImage img) {
        BufferedImage normalized = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = normalized.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return normalized;
    }

    // Compare color channels with a tolerance
    private static boolean isPixelSimilar(int rgb1, int rgb2, int tolerance) {
        Color c1 = new Color(rgb1);
        Color c2 = new Color(rgb2);
        return Math.abs(c1.getRed() - c2.getRed()) <= tolerance &&
                Math.abs(c1.getGreen() - c2.getGreen()) <= tolerance &&
                Math.abs(c1.getBlue() - c2.getBlue()) <= tolerance;
    }

    // Generate side-by-side image
    private static void generateSideBySideImage(BufferedImage img1, BufferedImage img2, BufferedImage diff, String outputPath) throws IOException {
        int labelHeight = 30; // space for text labels above images
        int height = labelHeight + Math.max(Math.max(img1.getHeight(), img2.getHeight()), diff.getHeight());
        int width = img1.getWidth() + img2.getWidth() + diff.getWidth();

        BufferedImage sideBySide = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sideBySide.createGraphics();

        // White background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // Font setup
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 45));

        // Draw labels
        g.drawString("Actual", img1.getWidth() / 2 - 30, 22);
        g.drawString("Expected", img1.getWidth() + img2.getWidth() / 2 - 40, 22);
        g.drawString("Difference", img1.getWidth() + img2.getWidth() + diff.getWidth() / 2 - 50, 22);

        // Draw images below labels
        g.drawImage(img1, 0, labelHeight, null);
        g.drawImage(img2, img1.getWidth(), labelHeight, null);
        g.drawImage(diff, img1.getWidth() + img2.getWidth(), labelHeight, null);

        g.dispose();
        ImageIO.write(sideBySide, "PNG", new File(outputPath));
    }

    // Flood fill to group difference regions and return bounding box
    private static Rectangle floodFill(boolean[][] map, boolean[][] visited, int startX, int startY, int width, int height) {
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
                if (nx >= 0 && ny >= 0 && nx < width && ny < height &&
                        map[nx][ny] && !visited[nx][ny]) {
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

    public static void main(String[] args) throws IOException {
        String path1 = "/Users/nmvv412/Documents/opencv-image-diff/src/main/resources/Image/Expected.jpeg";
        String path2 = "/Users/nmvv412/Documents/opencv-image-diff/src/main/resources/Image/Actual.jpeg";

        String diffPath = "/Users/nmvv412/Documents/opencv-image-diff/src/main/resources/Image/difference.png";
        String sideBySidePath = "/Users/nmvv412/Documents/opencv-image-diff/src/main/resources/Image/side_by_side.png";

        try {
            BufferedImage rawImg1 = ImageIO.read(new File(path1));
            BufferedImage rawImg2 = ImageIO.read(new File(path2));
            BufferedImage img1 = normalizeImage(rawImg1);
            BufferedImage img2 = normalizeImage(rawImg2);

            int width = Math.max(img1.getWidth(), img2.getWidth());
            int height = Math.max(img1.getHeight(), img2.getHeight());

            BufferedImage diffImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            boolean[][] diffMap = new boolean[width][height];
            boolean hasDifference = false;

            // Step 1: Compare pixel by pixel
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb1 = (x < img1.getWidth() && y < img1.getHeight()) ? img1.getRGB(x, y) : Color.BLACK.getRGB();
                    int rgb2 = (x < img2.getWidth() && y < img2.getHeight()) ? img2.getRGB(x, y) : Color.BLACK.getRGB();

                    if (isPixelSimilar(rgb1, rgb2, 30)) {
                        diffImage.setRGB(x, y, rgb1);
                    } else {
                        diffImage.setRGB(x, y, new Color(255, 0, 0, 128).getRGB());
                        diffMap[x][y] = true;
                        hasDifference = true;
                    }
                }
            }

            // Step 2: Draw bounding boxes around differences
            if (hasDifference) {
                System.out.println("Differences found. Highlighting...");

                Graphics2D g = diffImage.createGraphics();
                g.setColor(Color.RED);
                g.setStroke(new BasicStroke(2));
                boolean[][] visited = new boolean[width][height];

                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        if (diffMap[x][y] && !visited[x][y]) {
                            Rectangle box = floodFill(diffMap, visited, x, y, width, height);
                            if (box.width * box.height >= 100) { // Only draw boxes with at least 100 pixels
                                g.drawRect(box.x, box.y, box.width, box.height);
                            }
                        }
                    }
                }

                g.dispose();
                ImageIO.write(diffImage, "PNG", new File(diffPath));
                generateSideBySideImage(img1, img2, diffImage, sideBySidePath);
                System.out.println("Saved: difference.png & side_by_side.png");
            } else {
                System.out.println("Images are identical. No difference image generated.");
            }

        } catch (IOException e) {
            System.err.println("Error reading or writing images: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
