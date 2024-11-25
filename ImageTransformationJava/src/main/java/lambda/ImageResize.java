package lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;

public class ImageResize implements RequestHandler<HashMap<String, Object>, HashMap<String, Object>> {

    @Override
    public HashMap<String, Object> handleRequest(HashMap<String, Object> request, Context context) {
        HashMap<String, Object> response = new HashMap<>();

        try {
            // Extract inputs from the request
            String base64Image = (String) request.get("image_file");
            Object widthObj = request.get("target_width");
            Object heightObj = request.get("target_height");

            // Validate inputs
            if (base64Image == null || widthObj == null || heightObj == null) {
                response.put("error", "Missing required inputs: image_file, target_width, or target_height.");
                return response;
            }

            int targetWidth;
            int targetHeight;

            try {
                targetWidth = Integer.parseInt(widthObj.toString());
                targetHeight = Integer.parseInt(heightObj.toString());
                if (targetWidth <= 0 || targetHeight <= 0) {
                    throw new IllegalArgumentException("Target dimensions must be positive integers.");
                }
            } catch (NumberFormatException e) {
                response.put("error", "Target width and height must be valid integers.");
                return response;
            }

            // Decode the Base64 image
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));

            if (originalImage == null) {
                response.put("error", "Invalid image format or decoding failed.");
                return response;
            }

            // Resize the image
            Image resizedImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
            BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight,
                    originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType());
            outputImage.getGraphics().drawImage(resizedImage, 0, 0, null);

            // Convert the resized image back to Base64
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(outputImage, "png", outputStream);  // Save as PNG
            String resizedImageBase64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());

            // Add resized image details to the response
            response.put("resized_image", resizedImageBase64);
            response.put("message", "Image resized successfully.");
            response.put("target_width", targetWidth);
            response.put("target_height", targetHeight);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "An error occurred: " + e.getMessage());
        }

        return response;
    }
}
