import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;

public class ImageGenerator {
    public static void main(String[] args) throws Exception {
        String busId = args[0];
        int numImages = Integer.parseInt(args[1]);

        File folder = new File(busId);
        folder.mkdirs();
        Random random = new Random();

        for (int i = 0; i < numImages; i++) {
            BufferedImage image = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();

            // Draw random rectangles so the images aren't all the same
            for (int j = 0; j < 500; j++) {
                graphics.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
                graphics.fillRect(random.nextInt(1920), random.nextInt(1080), 50 + random.nextInt(200), 50 + random.nextInt(200));
            }

            graphics.dispose();
            ImageIO.write(image, "jpg", new File(folder, "image_" + i + ".jpg"));
        }
    }
}