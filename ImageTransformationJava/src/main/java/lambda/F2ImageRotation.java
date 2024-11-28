package lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import saaf.Inspector;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;
import java.util.HashMap;

import static lambda.Constants.ERROR_KEY;
import static lambda.Constants.IMAGE_FILE_KEY;

public class F2ImageRotation implements RequestHandler<HashMap<String, Object>, HashMap<String, Object>> {

    @Override
    public HashMap<String, Object> handleRequest(HashMap<String, Object> request, Context context) {
        Inspector inspector = new Inspector();

        try {
            // Extract input parameters
            String encodedImage = (String) request.get(IMAGE_FILE_KEY);
            Integer rotationAngle = (Integer) request.get("rotation_angle");

            if (encodedImage == null || rotationAngle == null) {
                throw new IllegalArgumentException("Missing required parameters: 'image_file' or 'rotation_angle'");
            }

            // Decode the Base64-encoded image
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(encodedImage)));
            if (originalImage == null) {
                throw new IllegalArgumentException("Failed to decode image data");
            }

            // Validate rotation angle
            if (!isValidRotationAngle(rotationAngle)) {
                throw new IllegalArgumentException("Invalid rotation angle. Only 90, 180, or 270 degrees are supported.");
            }

            // Perform rotation
            BufferedImage rotatedImage = rotateImage(originalImage, rotationAngle);

            // Encode the rotated image back to Base64
            String rotatedImageBase64 = encodeImageToBase64(rotatedImage);

            // Save the rotated image to a file
            saveImageToFile(rotatedImage, "rotated_image.png");

            // Populate response attributes
            inspector.addAttribute(IMAGE_FILE_KEY, rotatedImageBase64);
            inspector.addAttribute("original_width", originalImage.getWidth());
            inspector.addAttribute("original_height", originalImage.getHeight());
            inspector.addAttribute("rotated_width", rotatedImage.getWidth());
            inspector.addAttribute("rotated_height", rotatedImage.getHeight());
            inspector.addAttribute("rotation_angle", rotationAngle);

        } catch (Exception e) {
            e.printStackTrace();
            inspector = new Inspector();
            inspector.addAttribute(ERROR_KEY, e.getMessage());
        }

        return inspector.finish();
    }

    private boolean isValidRotationAngle(int angle) {
        return angle == 90 || angle == 180 || angle == 270;
    }

    private BufferedImage rotateImage(BufferedImage image, int rotationAngle) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Create a new BufferedImage with appropriate dimensions and type
        BufferedImage rotatedImage = new BufferedImage(
                (rotationAngle == 90 || rotationAngle == 270) ? height : width,
                (rotationAngle == 90 || rotationAngle == 270) ? width : height,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D graphics = rotatedImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Perform rotation with proper translation
        if (rotationAngle == 90) {
            graphics.translate(height, 0);
        } else if (rotationAngle == 180) {
            graphics.translate(width, height);
        } else if (rotationAngle == 270) {
            graphics.translate(0, width);
        }

        graphics.rotate(Math.toRadians(rotationAngle));
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();

        return rotatedImage;
    }



    private String encodeImageToBase64(BufferedImage image) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    /**
     * Saves the rotated image to a file.
     *
     * @param image The rotated image to save.
     * @param fileName The name of the output file.
     * @throws IOException If an I/O error occurs.
     */
    private void saveImageToFile(BufferedImage image, String fileName) throws IOException {
        File outputFile = new File(fileName);
        ImageIO.write(image, "png", outputFile);
        System.out.println("Image saved to: " + outputFile.getAbsolutePath());
    }

    public static void main(String[] args) {
        final String testImageName = "github-logo.png";

        System.out.println("PWD: " + System.getProperty("user.dir") + "\n");
        final HashMap<String, Object> req = new HashMap<>();

        final File file = new File("../../sample images/" + testImageName);
        try (FileInputStream fileInputStreamReader = new FileInputStream(file)) {
            final byte[] bytes = new byte[(int) file.length()];
            fileInputStreamReader.read(bytes);
            req.put("image_file", Base64.getEncoder().encodeToString(bytes));
            req.put("rotation_angle", 90); // Set the rotation angle here

            System.out.println(new F2ImageRotation().handleRequest(req, null).toString());

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}