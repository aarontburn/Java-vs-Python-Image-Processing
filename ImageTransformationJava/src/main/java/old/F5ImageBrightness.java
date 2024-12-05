package old;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import saaf.Inspector;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;

import static lambda.Constants.ERROR_KEY;
import static lambda.Constants.IMAGE_FILE_KEY;

public class F5ImageBrightness implements RequestHandler<HashMap<String, Object>, HashMap<String, Object>> {

    private static final int MIN_BRIGHTNESS = 1;  // Minimum input value for brightness
    private static final int MAX_BRIGHTNESS = 100; // Maximum input value for brightness

    @Override
    public HashMap<String, Object> handleRequest(HashMap<String, Object> request, Context context) {
        Inspector inspector = new Inspector();

        try {
            // Extract input parameters
            String encodedImage = (String) request.get(IMAGE_FILE_KEY);
            Integer brightnessDelta = (Integer) request.get("brightness_delta");

            if (encodedImage == null || brightnessDelta == null) {
                throw new IllegalArgumentException("Missing required parameters: 'image_file' or 'brightness_delta'");
            }

            // Validate brightness_delta
            if (brightnessDelta < MIN_BRIGHTNESS || brightnessDelta > MAX_BRIGHTNESS) {
                throw new IllegalArgumentException(
                        String.format("Invalid brightness_delta. Must be between %d and %d.", MIN_BRIGHTNESS, MAX_BRIGHTNESS));
            }

            // Map brightness_delta (1–100) to RescaleOp factor (0.0–2.0)
            float brightnessFactor = mapBrightness(brightnessDelta);

            // Decode the Base64-encoded image
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(encodedImage)));
            if (originalImage == null) {
                throw new IllegalArgumentException("Failed to decode image data");
            }

            // Adjust brightness
            BufferedImage brightenedImage = adjustBrightness(originalImage, brightnessFactor);

            // Encode the brightened image back to Base64
            String brightenedImageBase64 = encodeImageToBase64(brightenedImage);

            // Save the brightened image to a file
//            saveImageToFile(brightenedImage, "brightened_image.png");

            // Populate response attributes
            inspector.addAttribute("original_width", originalImage.getWidth());
            inspector.addAttribute("original_height", originalImage.getHeight());
            inspector.addAttribute("brightness_delta", brightnessDelta);
            inspector.addAttribute("brightness_factor", brightnessFactor);
            inspector.addAttribute(IMAGE_FILE_KEY, brightenedImageBase64);

        } catch (Exception e) {
            e.printStackTrace();
            inspector = new Inspector();
            inspector.addAttribute(ERROR_KEY, e.getMessage());
        }

        return inspector.finish();
    }

    /**
     * Maps brightness_delta (1–100) to a brightness factor (0.0–2.0).
     *
     * @param brightnessDelta The brightness adjustment in the range 1–100.
     * @return A brightness factor suitable for RescaleOp.
     */
    private float mapBrightness(int brightnessDelta) {
        // Map 1–100 to 0.0–2.0. 50 maps to 1.0 (no change).
        return brightnessDelta / 50.0f;
    }

    /**
     * Adjusts the brightness of a BufferedImage.
     *
     * @param image            The original image to modify.
     * @param brightnessFactor The factor to adjust the brightness (1.0 = original, < 1.0 = darker, > 1.0 = brighter).
     * @return The modified image with adjusted brightness.
     */
    private BufferedImage adjustBrightness(BufferedImage image, float brightnessFactor) {
        RescaleOp rescaleOp = new RescaleOp(brightnessFactor, 0, null);
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Graphics2D g = result.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        rescaleOp.filter(result, result);
        return result;
    }

    private String encodeImageToBase64(BufferedImage image) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    /**
     * Saves the brightened image to a file.
     *
     * @param image The brightened image to save.
     * @param fileName The name of the output file.
     * @throws IOException If an I/O error occurs.
     */
    private void saveImageToFile(BufferedImage image, String fileName) throws IOException {
        File outputFile = new File(fileName);
        ImageIO.write(image, "png", outputFile);
        System.out.println("Image saved to: " + outputFile.getAbsolutePath());
    }

    public static void main(String[] args) {
        final String testImageName = "sample image.jpg";

        System.out.println("PWD: " + System.getProperty("user.dir") + "\n");
        final HashMap<String, Object> req = new HashMap<>();

        final File file = new File("../../sample images/" + testImageName);
        try (FileInputStream fileInputStreamReader = new FileInputStream(file)) {
            final byte[] bytes = new byte[(int) file.length()];
            fileInputStreamReader.read(bytes);

            // Encode the image to Base64
            req.put("image_file", Base64.getEncoder().encodeToString(bytes));
            
            // Set brightness_delta (1–100)
            req.put("brightness_delta", 100); // Example input to increase brightness significantly

            // Call the brightness handler and print the result
            System.out.println(new F5ImageBrightness().handleRequest(req, null).toString());

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
