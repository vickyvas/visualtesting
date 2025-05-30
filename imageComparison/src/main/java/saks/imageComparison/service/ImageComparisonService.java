package saks.imageComparison.service;

import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public interface ImageComparisonService {

//    void compare(MultipartFile actualFile, MultipartFile expectedFile, String uploadDir) throws IOException;

    void compare(File actualFile, File expectedFile, File diffFile, File sideBySideFile) throws IOException;

    double calculateMatchScore(BufferedImage img1, BufferedImage img2, int tolerance);
}
