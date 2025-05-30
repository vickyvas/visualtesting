package saks.imageComparison;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@SpringBootApplication
public class ImageComparisonApplication {

	public static void main(String[] args) {
		SpringApplication.run(ImageComparisonApplication.class, args);
//		try {
//			BufferedImage dummyImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
//			File testFile = new File("/Users/nmvv412/Documents/imageComparison/uploads/test.png");
//			boolean success = ImageIO.write(dummyImage, "png", testFile);
//			System.out.println("Manual write test: " + success + ", Exists? " + testFile.exists() + ", File size: " + testFile.length());
//		} catch (IOException e) {
//			System.err.println("Error writing test image: " + e.getMessage());
//			e.printStackTrace();
//		}

	}

}
