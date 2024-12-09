package functions;

import com.amazonaws.services.lambda.runtime.Context;
import utils.Constants;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import static utils.Constants.*;

public class F3ImageResize {


    /**
     * Lambda entry point for Function 3: Image Resize.
     * Handles requests that include fetching an image from S3 and resizing it.
     *
     * @param request The input request map, containing:
     *                - "bucketname" (String): S3 bucket name.
     *                - "filename" (String): S3 file key.
     *                - "target_width" (Integer): Desired width of the resized image.
     *                - "target_height" (Integer): Desired height of the resized image.
     * @param context The Lambda execution context.
     * @return A response map containing metrics and result details.
     */
    public static HashMap<String, Object> handleRequest(final HashMap<String, Object> request, final Context context) {
        return imageResize(null, request, context);
    }

    /**
     * Secondary entry point: Processes an already fetched BufferedImage and resizes it.
     *
     * @param image   The original BufferedImage to resize.
     * @param request A map containing resize parameters:
     *                - "target_width" (Integer): Desired width of the resized image.
     *                - "target_height" (Integer): Desired height of the resized image.
     *                - Other metadata (e.g., bucket name, cold start tracking).
     * @param context The Lambda execution context.
     * @return A response map containing the resized image and any associated metadata.
     */
    public static HashMap<String, Object> imageResize(BufferedImage image, HashMap<String, Object> request, Context context) {
        final boolean isBatch = image != null;
        final HashMap<String, Object> inspector = new HashMap<>();

        try {
            // Validate input request
            String validationError = Constants.validateRequestMap(request, BUCKET_KEY, FILE_NAME_KEY, "target_width", "target_height");
            if (validationError != null) {
                return Constants.getErrorObject(validationError);
            }

            // Extract inputs
            String bucketName = (String) request.get(BUCKET_KEY);
            String fileName = (String) request.get(FILE_NAME_KEY);
            Integer targetWidth = (Integer) request.get("target_width");
            Integer targetHeight = (Integer) request.get("target_height");

            // Validate image format
            if (!fileName.endsWith(".png") && !fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg")) {
                return Constants.getErrorObject("Unsupported image format. Only PNG and JPEG are supported.");
            }

            // Validate dimensions
            if (targetWidth == null || targetHeight == null || targetWidth <= 0 || targetHeight <= 0) {
                return Constants.getErrorObject("Target dimensions must be positive integers.");
            }

            // Fetch the image from S3 and measure network latency
            BufferedImage originalImage;
            try {
                originalImage = isBatch ? image : ImageIO.read(Constants.getImageFromS3AndRecordLatency(bucketName, fileName, inspector));
                if (originalImage == null) {
                    throw new IllegalArgumentException("Image could not be decoded. Check the file format.");
                }
            } catch (Exception e) {
                return Constants.getErrorObject("Error fetching image from S3: " + e.getMessage());
            }

            // Record original dimensions
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();


            if (targetWidth > originalWidth || targetHeight > originalHeight) {
                return Constants.getErrorObject("Target dimensions exceed original dimensions.");
            }
            Image resizedImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
            BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType());
            Graphics2D graphics = outputImage.createGraphics();
            graphics.drawImage(resizedImage, 0, 0, null);
            graphics.dispose();

            if (!isBatch) {
                String resizedFileName = "resized_" + fileName;
                boolean savedSuccessfully = Constants.saveImageToS3(bucketName, resizedFileName, "png", outputImage);
                if (!savedSuccessfully) {
                    return Constants.getErrorObject("Failed to save resized image to S3.");
                }

                // Generate presigned URL for the resized image
//                inspector.put(IMAGE_URL_KEY, Constants.getDownloadableImageURL(bucketName, resizedFileName));
//                inspector.put(IMAGE_URL_EXPIRES_IN, IMAGE_URL_EXPIRATION_SECONDS);
            } else {
                inspector.put(IMAGE_FILE_KEY, outputImage);
            }

            // Add success message
            inspector.put(SUCCESS_KEY, "Image resized successfully.");
            inspector.put("original_width", originalWidth);
            inspector.put("original_height", originalHeight);
            inspector.put("target_width", targetWidth);
            inspector.put("target_height", targetHeight);

        } catch (Exception e) {
            // Handle unexpected errors
            e.printStackTrace();
            return Constants.getErrorObject(e.toString());
        }

        return inspector; // Return collected metrics
    }


}
