package saks.imageComparison.controller;

import jakarta.annotation.PostConstruct;
import saks.imageComparison.service.ImageComparisonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ui.Model;
import saks.imageComparison.service.ImageComparisonServiceImpl;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class ImageController {

    @Autowired
    private ImageComparisonService imageComparisonService;

    @GetMapping("/upload")
    public String showUploadForm() {
        return "upload";
    }



    @PostMapping("/compare")
    public String compareImages(@RequestParam("actual") MultipartFile actual,
                                @RequestParam("expected") MultipartFile expected,
                                Model model) {
        try {
            String uploadDir = "/Users/nmvv412/Documents/imageComparison/uploads";
            // Save uploaded files
            Path actualPath = Paths.get(uploadDir, "actual.jpg");
            Path expectedPath = Paths.get(uploadDir, "expected.jpg");
            actual.transferTo(actualPath);
            expected.transferTo(expectedPath);

            // Output files
            File diffFile = new File(uploadDir, "difference.png");
            File sideBySideFile = new File(uploadDir, "side_by_side.png");
            System.out.println("Diff exists: " + diffFile.exists());
            System.out.println("Side-by-side exists: " + sideBySideFile.exists());

            // Perform comparison
            imageComparisonService.compare(actualPath.toFile(), expectedPath.toFile(), diffFile, sideBySideFile);

            // Calculate match score
            BufferedImage img1 = ImageIO.read(actualPath.toFile());
            BufferedImage img2 = ImageIO.read(expectedPath.toFile());
            double score = imageComparisonService.calculateMatchScore(img1, img2, 30);

            model.addAttribute("matchScore", score); // For logic
            model.addAttribute("matchScoreText", String.format("%.2f", score) + "%"); // For display
            model.addAttribute("sideBySideImagePath", "/uploads/side_by_side.png");
            model.addAttribute("actualImagePath", "/uploads/actual.jpg");
            model.addAttribute("expectedImagePath", "/uploads/expected.jpg");
            model.addAttribute("differenceImagePath", "/uploads/difference.png");
            model.addAttribute("differenceFound", true);

            return "upload";

        } catch (Exception e) {
            model.addAttribute("error", "Comparison failed: " + e.getMessage());
            return "upload";
        }
    }

//    @PostConstruct
//    public void testManualImageWrite() {
//        try {
//            BufferedImage testImg = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
//            File testFile = new File("/Users/nmvv412/Documents/imageComparison/uploads/test_write.png");
//            boolean result = ImageIO.write(testImg, "png", testFile);
//            System.out.println("Test write result: " + result);
//            System.out.println("File exists after write: " + testFile.exists());
//            System.out.println("File size: " + testFile.length());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

}
