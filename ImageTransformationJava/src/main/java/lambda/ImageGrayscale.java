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
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;

public class ImageGrayscale implements RequestHandler<HashMap<String, Object>, HashMap<String, Object>> {

    @Override
    public HashMap<String, Object> handleRequest(HashMap<String, Object> request, Context context) {
        Inspector inspector = new Inspector();

        try {
            // Extract input parameter
            String encodedImage = (String) request.get("image_file");

            if (encodedImage == null) {
                throw new IllegalArgumentException("Missing required parameter: 'image_file'");
            }

            // Decode the Base64-encoded image
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(encodedImage)));
            if (originalImage == null) {
                throw new IllegalArgumentException("Failed to decode image data");
            }

            // Convert the image to grayscale
            BufferedImage grayscaleImage = convertToGrayscale(originalImage);

            // Encode the grayscale image back to Base64
            String grayscaleImageBase64 = encodeImageToBase64(grayscaleImage);

            // Save the grayscale image to a file (optional)
            saveImageToFile(grayscaleImage, "grayscale_image.png");

            // Populate response attributes
            inspector.addAttribute("original_width", originalImage.getWidth());
            inspector.addAttribute("original_height", originalImage.getHeight());
            inspector.addAttribute("grayscale_image", grayscaleImageBase64);

        } catch (Exception e) {
            e.printStackTrace();
            inspector.addAttribute("error", e.getMessage());
        }

        return inspector.finish();
    }

    /**
     * Converts a BufferedImage to grayscale.
     *
     * @param image The original image to convert.
     * @return The grayscale version of the image.
     */
    private BufferedImage convertToGrayscale(BufferedImage image) {
        BufferedImage grayscaleImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        grayscaleImage.getGraphics().drawImage(image, 0, 0, null);
        return grayscaleImage;
    }

    private String encodeImageToBase64(BufferedImage image) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    /**
     * Saves the grayscale image to a file.
     *
     * @param image The grayscale image to save.
     * @param fileName The name of the output file.
     * @throws IOException If an I/O error occurs.
     */
    private void saveImageToFile(BufferedImage image, String fileName) throws IOException {
        File outputFile = new File(fileName);
        ImageIO.write(image, "png", outputFile);
        System.out.println("Image saved to: " + outputFile.getAbsolutePath());
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

            // Call the grayscale handler and print the result
            HashMap<String, Object> response = new ImageGrayscale().handleRequest(req, null);
            System.out.println("Response: " + response);

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

}
