import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ImageGenerator {
    public static void main(String[] args) throws Exception {
        String busId = args[0];
        int numImages = Integer.parseInt(args[1]);

        File folder = new File(busId);
        folder.mkdirs();
        
        // Initialize Robot for screen capturing and get the main display size
        Robot robot = new Robot();
        Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

        for (int i = 0; i < numImages; i++) {
            // Take the screenshot
            BufferedImage screenCapture = robot.createScreenCapture(screenRect);

            // Save the screenshot to the folder
            ImageIO.write(screenCapture, "jpg", new File(folder, "image_" + i + ".jpg"));
            System.out.println("Captured screenshot " + (i + 1) + " of " + numImages);

            // Wait 1 second before the next screenshot (skip delay on the last image)
            if (i < numImages - 1) {
                Thread.sleep(1000);
            }
        }
    }
}