package lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import saaf.Inspector;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Base64;
import java.util.HashMap;

public class ImageTransform implements RequestHandler<HashMap<String, Object>, HashMap<String, Object>> {

    @Override
    public HashMap<String, Object> handleRequest(HashMap<String, Object> request, Context context) {
        Inspector inspector = new Inspector();

        try {
            // Extract input parameter
            String encodedImage = (String) request.get("image_file");
            String targetFormat = (String) request.getOrDefault("target_format", "jpeg");

            if (encodedImage == null) {
                throw new IllegalArgumentException("Missing required parameter: 'image_file'");
            }

            // Decode the Base64-encoded image
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(encodedImage)));
            if (originalImage == null) {
                throw new IllegalArgumentException("Failed to decode image data");
            }

            // Transform image to the target format
            String transformedImageBase64 = transformImageToFormat(originalImage, targetFormat);

            // Populate response attributes
            inspector.addAttribute("original_width", originalImage.getWidth());
            inspector.addAttribute("original_height", originalImage.getHeight());
            inspector.addAttribute("transformed_image", transformedImageBase64);
            inspector.addAttribute("target_format", targetFormat);

        } catch (Exception e) {
            e.printStackTrace();
            inspector.addAttribute("error", e.getMessage());
        }

        return inspector.finish();
    }

    /**
     * Transforms the image to the specified file format and encodes it to Base64.
     *
     * @param image The original image to transform.
     * @param format The target file format (e.g., "jpeg", "png").
     * @return The Base64-encoded transformed image.
     * @throws Exception If an error occurs during transformation.
     */
    private String transformImageToFormat(BufferedImage image, String format) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        boolean success = ImageIO.write(image, format, outputStream);

        if (!success) {
            throw new IllegalArgumentException("Unsupported target format: " + format);
        }

        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    public static void main(String[] args) {
        final String testImageName = "sample image 2.jpeg";

        System.out.println("PWD: " + System.getProperty("user.dir") + "\n");
        final HashMap<String, Object> req = new HashMap<>();

        final File file = new File("./sample images/" + testImageName);
        System.out.println("Absolute path: " + file.getAbsolutePath());

        if (!file.exists() || file.length() == 0) {
            System.err.println("Error: File does not exist or is empty at path: " + file.getAbsolutePath());
            return;
        }

        try (FileInputStream fileInputStreamReader = new FileInputStream(file)) {
            final byte[] bytes = new byte[(int) file.length()];
            fileInputStreamReader.read(bytes);

            // Encode the image to Base64
            String encodedImage = Base64.getEncoder().encodeToString(bytes);
            System.out.println("Encoded Image (truncated): " + encodedImage.substring(0, 100) + "...");

            req.put("image_file", encodedImage);
            req.put("target_format", "jpeg");

            // Call the transform handler and print the result
            HashMap<String, Object> response = new ImageTransform().handleRequest(req, null);
            System.out.println("Response: " + response);

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
