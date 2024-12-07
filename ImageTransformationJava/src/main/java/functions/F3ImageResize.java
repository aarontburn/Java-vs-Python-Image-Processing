package functions;

import com.amazonaws.services.lambda.runtime.Context;
import saaf.Inspector;
import utils.Constants;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import static utils.Constants.*;

public class F3ImageResize {

    private static boolean isColdStart = true; // Global variable to track cold/warm starts

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
        Inspector inspector = new Inspector(); // Initialize inspector
        long roundTripStart = System.currentTimeMillis(); // Record round trip start time
    
        // Cold start tracking
        if (isColdStart) {
            inspector.addAttribute(COLD_START_KEY, 1);
            isColdStart = false;
        } else {
            inspector.addAttribute(COLD_START_KEY, 0);
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
    
            // Validate image format
            if (!fileName.endsWith(".png") && !fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg")) {
                inspector.addAttribute(ERROR_KEY, "Unsupported image format. Only PNG and JPEG are supported.");
                return inspector.finish();
            }
    
            // Validate dimensions
            if (targetWidth == null || targetHeight == null || targetWidth <= 0 || targetHeight <= 0) {
                inspector.addAttribute(ERROR_KEY, "Target dimensions must be positive integers.");
                return inspector.finish();
            }
    
            // Fetch the image from S3 and measure network latency
            BufferedImage originalImage;
            long s3FetchStartTime = System.currentTimeMillis();
            try {
                originalImage = ImageIO.read(Constants.getImageFromS3AndRecordLatency(bucketName, fileName, inspector));
                if (originalImage == null) {
                    throw new IllegalArgumentException("Image could not be decoded. Check the file format.");
                }
                inspector.addTimeStamp("s3_fetch_latency", s3FetchStartTime);
            } catch (Exception e) {
                inspector.addAttribute(ERROR_KEY, "Error fetching image from S3: " + e.getMessage());
                return inspector.finish();
            }
    
            // Record original dimensions
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            inspector.addAttribute("original_width", originalWidth);
            inspector.addAttribute("original_height", originalHeight);
    
            // Track memory usage before resizing
            inspector.inspectMemory();
    
            // Resize the image
            long resizeStartTime = System.currentTimeMillis();
            if (targetWidth > originalWidth || targetHeight > originalHeight) {
                inspector.addAttribute(ERROR_KEY, "Target dimensions exceed original dimensions.");
                return inspector.finish();
            }
            Image resizedImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
            BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight,
                    originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType());
            Graphics2D graphics = outputImage.createGraphics();
            graphics.drawImage(resizedImage, 0, 0, null);
            graphics.dispose();
            inspector.addTimeStamp("resize_time", resizeStartTime);
    
            // Track memory usage after resizing
            inspector.inspectMemory();
    
            // Save resized image back to S3
            long s3SaveStartTime = System.currentTimeMillis();
            String resizedFileName = "resized_" + fileName;
            boolean savedSuccessfully = Constants.saveImageToS3(bucketName, resizedFileName, "png", outputImage);
            if (!savedSuccessfully) {
                inspector.addAttribute(ERROR_KEY, "Failed to save resized image to S3.");
                return inspector.finish();
            }
            inspector.addTimeStamp("s3_save_latency", s3SaveStartTime);
    
            // Generate presigned URL for the resized image
            String presignedURL = Constants.getDownloadableImageURL(bucketName, resizedFileName);
            inspector.addAttribute(IMAGE_URL_KEY, presignedURL);
            inspector.addAttribute(IMAGE_URL_EXPIRES_IN, IMAGE_URL_EXPIRATION_SECONDS);
    
            // Add success message
            inspector.addAttribute(SUCCESS_KEY, "Image resized successfully.");
            inspector.addAttribute("target_width", targetWidth);
            inspector.addAttribute("target_height", targetHeight);
    
            // Calculate throughput
            double throughput = 1000.0 / (System.currentTimeMillis() - roundTripStart);
            inspector.addAttribute("processing_throughput", throughput);
    
        } catch (Exception e) {
            // Handle unexpected errors
            inspector.addAttribute(ERROR_KEY, "An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    
        // Record metrics
        inspector.addTimeStamp(FUNCTION_RUN_TIME_KEY, roundTripStart); // Measure function runtime
        inspector.inspectMetrics(false, roundTripStart); // Inspect essential metrics
    
        return inspector.finish(); // Return collected metrics
    }
    

    /**
     * Secondary entry point: Processes an already fetched BufferedImage and resizes it.
     *
     * @param image The original BufferedImage to resize.
     * @param args  A map containing resize parameters:
     *              - "target_width" (Integer): Desired width of the resized image.
     *              - "target_height" (Integer): Desired height of the resized image.
     *              - Other metadata (e.g., bucket name, cold start tracking).
     * @param context The Lambda execution context.
     * @return A response map containing the resized image and any associated metadata.
     */
    public static HashMap<String, Object> imageResize(BufferedImage image, HashMap<String, Object> args, Context context) {
        HashMap<String, Object> response = new HashMap<>();
        Inspector inspector = new Inspector(); // Initialize Inspector for logging and metrics

        try {
            // Extract target dimensions
            Integer targetWidth = (Integer) args.get("target_width");
            Integer targetHeight = (Integer) args.get("target_height");

            // Validate dimensions
            if (targetWidth == null || targetHeight == null || targetWidth <= 0 || targetHeight <= 0) {
                response.put(ERROR_KEY, "Target dimensions must be positive integers.");
                inspector.addAttribute(ERROR_KEY, "Invalid dimensions.");
                return response;
            }

            // Record original dimensions
            inspector.addAttribute("original_width", image.getWidth());
            inspector.addAttribute("original_height", image.getHeight());

            // Resize the image
            if (targetWidth > image.getWidth() || targetHeight > image.getHeight()) {
                response.put(ERROR_KEY, "Target dimensions exceed original dimensions.");
                inspector.addAttribute(ERROR_KEY, "Invalid resize dimensions.");
                return response;
            }
            Image resizedImage = image.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
            BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight,
                    image.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : image.getType());
            Graphics2D graphics = outputImage.createGraphics();
            graphics.drawImage(resizedImage, 0, 0, null);
            graphics.dispose();

            // Add resized image and success attributes to the response
            response.put(IMAGE_FILE_KEY, outputImage);
            response.put(SUCCESS_KEY, "Image resized successfully.");
            response.put("target_width", targetWidth);
            response.put("target_height", targetHeight);

            // Log success to the inspector
            inspector.addAttribute(SUCCESS_KEY, "Image resized successfully.");
            inspector.addAttribute("target_width", targetWidth);
            inspector.addAttribute("target_height", targetHeight);

        } catch (Exception e) {
            response.put(ERROR_KEY, "Unexpected error during resizing: " + e.getMessage());
            inspector.addAttribute(ERROR_KEY, "Unexpected error during resizing: " + e.getMessage());
            e.printStackTrace();
        }

        // Finalize and return metrics collected by the Inspector
        response.put("inspector_attributes", inspector.finish());
        return response;
    }
}
