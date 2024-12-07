package functions;

import com.amazonaws.services.lambda.runtime.Context;
import saaf.Inspector;
import utils.Constants;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;

import static utils.Constants.*;

public class F3ImageResize {

    private static boolean isColdStart = true; // Global variable to track cold/warm starts

    /**
     * Lambda entry point for Function 3: Image Resize.
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
        Inspector inspector = new Inspector(); // Initialize inspector
        long roundTripStart = System.currentTimeMillis(); // Record round trip start time

        // Detect and record cold start
        if (isColdStart) {
            inspector.addAttribute(COLD_START_KEY, 1); // Cold start detected
            isColdStart = false; // Mark subsequent invocations as warm
        } else {
            inspector.addAttribute(COLD_START_KEY, 0); // Warm start
        }

        try {
            // Validate input request
            String validationError = Constants.validateRequestMap(request, BUCKET_KEY, FILE_NAME_KEY, "target_width", "target_height");
            if (validationError != null) {
                inspector.addAttribute(ERROR_KEY, validationError);
                return inspector.finish(); // Return validation error
            }

            // Extract inputs
            String bucketName = (String) request.get(BUCKET_KEY);
            String fileName = (String) request.get(FILE_NAME_KEY);
            Integer targetWidth = (Integer) request.get("target_width");
            Integer targetHeight = (Integer) request.get("target_height");

            // Validate dimensions
            if (targetWidth <= 0 || targetHeight <= 0) {
                inspector.addAttribute(ERROR_KEY, "Target dimensions must be positive integers.");
                return inspector.finish();
            }

            // Fetch the image from S3 and measure network latency
            BufferedImage originalImage = ImageIO.read(Constants.getImageFromS3AndRecordLatency(bucketName, fileName, inspector));
            if (originalImage == null) {
                inspector.addAttribute(ERROR_KEY, "Failed to fetch or decode image from S3.");
                return inspector.finish();
            }

            // Record original dimensions
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            inspector.addAttribute("original_width", originalWidth);
            inspector.addAttribute("original_height", originalHeight);

            // Resize the image
            Image resizedImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
            BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight,
                    originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType());
            Graphics2D graphics = outputImage.createGraphics();
            graphics.drawImage(resizedImage, 0, 0, null);
            graphics.dispose();

            // Save resized image back to S3
            String resizedFileName = "resized_" + fileName;
            boolean savedSuccessfully = Constants.saveImageToS3(bucketName, resizedFileName, "png", outputImage);
            if (!savedSuccessfully) {
                inspector.addAttribute(ERROR_KEY, "Failed to save resized image to S3.");
                return inspector.finish();
            }

            // Generate presigned URL for the resized image
            String presignedURL = Constants.getDownloadableImageURL(bucketName, resizedFileName);
            inspector.addAttribute(IMAGE_URL_KEY, presignedURL);
            inspector.addAttribute(IMAGE_URL_EXPIRES_IN, IMAGE_URL_EXPIRATION_SECONDS);

            // Add success message
            inspector.addAttribute(SUCCESS_KEY, "Image resized successfully.");
            inspector.addAttribute("target_width", targetWidth);
            inspector.addAttribute("target_height", targetHeight);

        } catch (Exception e) {
            // Handle unexpected errors
            inspector.addAttribute(ERROR_KEY, "An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }

        // Record metrics
        inspector.addTimeStamp(FUNCTION_RUN_TIME_KEY); // Measure function runtime
        inspector.inspectMetrics(false, roundTripStart); // Inspect essential metrics

        return inspector.finish(); // Return collected metrics
    }
}
