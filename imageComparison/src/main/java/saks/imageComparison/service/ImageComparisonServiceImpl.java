package saks.imageComparison.service;

import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

@Service
public class ImageComparisonServiceImpl implements ImageComparisonService{

//    @Override
//    public void compare(File actual, File expected, File diffOutput) throws IOException {
//        BufferedImage img1 = ImageIO.read(actual);
//        BufferedImage img2 = ImageIO.read(expected);
//
//        if (img1 == null || img2 == null) {
//            throw new IOException("Failed to read one or both images.");
//        }
//
//        int width = Math.max(img1.getWidth(), img2.getWidth());
//        int height = Math.max(img1.getHeight(), img2.getHeight());
//
//        BufferedImage diffImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
//
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                int rgb1 = (x < img1.getWidth() && y < img1.getHeight()) ? img1.getRGB(x, y) : Color.BLACK.getRGB();
//                int rgb2 = (x < img2.getWidth() && y < img2.getHeight()) ? img2.getRGB(x, y) : Color.BLACK.getRGB();
//
//                if (rgb1 != rgb2) {
//                    diffImage.setRGB(x, y, Color.RED.getRGB());
//                } else {
//                    diffImage.setRGB(x, y, rgb1);
//                }
//            }
//        }
//
//        ImageIO.write(diffImage, "PNG", diffOutput);
//    }
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

        String[] writers = ImageIO.getWriterFormatNames();
        System.out.println("Available writers:");
        for (String w : writers) {
            System.out.println(w);
        }
//        ImageIO.write(combined, "PNG", outputFile);
        boolean sideBySideWritten = ImageIO.write(combined, "png", outputFile);
        System.out.println("Side-by-side image written? " + sideBySideWritten);

        boolean writeSuccess = ImageIO.write(combined, "png", outputFile);
        System.out.println("Side-by-side write success: " + writeSuccess);
        System.out.println("Side-by-side exists after write? " + outputFile.exists());
        System.out.println("Side-by-side file size: " + outputFile.length());

        File file = new File(outputFile.getAbsolutePath());
        System.out.println("Side-by-side exists after write? " + file.exists());
        System.out.println("Side-by-side file size: " + file.length());
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
        // Round to 2 decimal places
        double roundedScore = Math.round(rawScore * 100.0) / 100.0;

        return roundedScore;
    }
    @Override
    public void compare(File actualFile, File expectedFile, File diffFile, File sideBySideFile) throws IOException {

        System.out.println("Reading actual image: " + actualFile.getAbsolutePath());
        System.out.println("Reading expected image: " + expectedFile.getAbsolutePath());

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

        // Compare pixel-by-pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb1 = (x < img1.getWidth() && y < img1.getHeight()) ? img1.getRGB(x, y) : Color.BLACK.getRGB();
                int rgb2 = (x < img2.getWidth() && y < img2.getHeight()) ? img2.getRGB(x, y) : Color.BLACK.getRGB();

                if (isPixelDifferent(rgb1, rgb2, 10)) {
                    diffImage.setRGB(x, y, Color.MAGENTA.getRGB());
                    diffMap[x][y] = true;
                    hasDifference = true;
                } else {
                    diffImage.setRGB(x, y, img2.getRGB(x, y));
                }
            }
        }

        // Draw red rectangles on differences
        if (hasDifference) {
            Graphics2D g = diffImage.createGraphics();
            g.setColor(Color.RED);
            g.setStroke(new BasicStroke(10));


            boolean[][] visited = new boolean[width][height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (diffMap[x][y] && !visited[x][y]) {
                        Rectangle box = floodFill(diffMap, visited, x, y, width, height);
                        if (box.width * box.height > 4) {
                            g.drawRect(box.x, box.y, box.width, box.height);
                        }
                    }
                }
            }
            g.dispose();
        }

        // Log available image writers
        String[] writers = ImageIO.getWriterFormatNames();
        System.out.println("Available writers:");
        for (String writer : writers) {
            System.out.println(writer);
        }

        // Normalize and write diff image
        System.out.println("Writing diff image to: " + diffFile.getAbsolutePath());
        diffImage = normalizeImage(diffImage);
        boolean diffWritten = ImageIO.write(diffImage, "png", diffFile);
        System.out.println("Diff written successfully? " + diffWritten);

        // Generate and write side-by-side image
        System.out.println("Writing side-by-side image to: " + sideBySideFile.getAbsolutePath());
        generateSideBySide(img1, img2, diffImage, sideBySideFile);

        // Verify side-by-side image
        System.out.println("Post-side-by-side generation check:");
        System.out.println("Side-by-side exists? " + sideBySideFile.exists());
        System.out.println("Side-by-side file size: " + sideBySideFile.length());
    }

}
