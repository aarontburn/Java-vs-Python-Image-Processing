package functions;

import com.amazonaws.services.lambda.runtime.Context;
import saaf.Inspector;
import utils.Constants;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import static utils.Constants.*;
import static utils.Constants.estimateCost;

public class F3ImageResize {

    /***
     *  Function 3: Image Resize
     *
     *  @param request  The image arguments.
     *  @param context  The AWS Lambda context.
     *  @return A response object.
     */
    public static HashMap<String, Object> handleRequest(final HashMap<String, Object> request, final Context context) {
        return imageResize(null, request, context);
    }

    /***
     *  Function #3: Image Resize Batch Method.
     *      This function should only be called by the batch handler, which passes in a buffered image to use.
     *
     *  @param image    The image to modify.
     *  @param request  The request arguments.
     *  @param context  The AWS Lambda Context
     *  @return A response object.
     */
    private static HashMap<String, Object> imageResize(final BufferedImage image, final HashMap<String, Object> request, final Context context) {
        final long functionStartTime = System.currentTimeMillis();

        final boolean isBatch = image != null;

        Inspector inspector = new Inspector();
        inspector.addAttribute(COLD_START_KEY, isColdStart ? 1 : 0);
        isColdStart = false; // Reset the cold start flag for subsequent invocations

        final String validateMessage = Constants.validateRequestMap(request, BUCKET_KEY, FILE_NAME_KEY, "target_width", "target_height");

        if (validateMessage != null) {
            inspector = new Inspector();
            inspector.addAttribute(ERROR_KEY, validateMessage);
            return inspector.finish();
        }

        try {
            // Extract inputs from the request
            final String bucketName = (String) request.get(BUCKET_KEY);
            final String fileName = (String) request.get(FILE_NAME_KEY);
            final Integer targetWidth = (Integer) request.get("target_width");
            final Integer targetHeight = (Integer) request.get("target_height");
            final String outputFileName = "resized_" + fileName;


            final long processingStartTime = System.currentTimeMillis();


            if (targetWidth <= 0 || targetHeight <= 0) {
                throw new IllegalArgumentException("Target dimensions must be positive integers.");
            }

            final BufferedImage originalImage = isBatch ? image : ImageIO.read(Constants.getImageFromS3AndRecordLatency(bucketName, fileName, inspector));

            // Resize the image
            final Image resizedImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
            final BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType());
            outputImage.getGraphics().drawImage(resizedImage, 0, 0, null);

            if (!isBatch) {
                final boolean successfulWriteToS3 = Constants.saveImageToS3(bucketName, outputFileName, "png", outputImage);
                if (!successfulWriteToS3) {
                    throw new RuntimeException("Could not write image to S3");
                }
            }


            // Add resized image details to the response
            inspector.addAttribute("message", "Image resized successfully.");
            inspector.addAttribute("original_width", originalImage.getWidth());
            inspector.addAttribute("original_height", originalImage.getHeight());
            inspector.addAttribute("target_width", targetWidth);
            inspector.addAttribute("target_height", targetHeight);
            inspector.addAttribute(LANGUAGE_KEY, "Java");
            inspector.addAttribute(VERSION_KEY, 0.5);
            inspector.addAttribute(START_TIME_KEY, functionStartTime);
            inspector.addAttribute(END_TIME_KEY, System.currentTimeMillis());

            if (isBatch) {
                inspector.addAttribute(IMAGE_FILE_KEY, outputImage);
            } else {
                inspector.addAttribute(IMAGE_URL_KEY, getDownloadableImageURL(bucketName, outputFileName));
                inspector.addAttribute(IMAGE_URL_EXPIRES_IN, IMAGE_URL_EXPIRATION_SECONDS);
            }

            final long functionRunTime = System.currentTimeMillis() - processingStartTime;
            inspector.addAttribute(ROUND_TRIP_TIME_KEY, functionRunTime + (long) inspector.getAttribute(IMAGE_ACCESS_LATENCY_KEY));
            inspector.addAttribute(FUNCTION_RUN_TIME_KEY, functionRunTime);
            inspector.addAttribute(ESTIMATED_COST_KEY, estimateCost(functionRunTime));

        } catch (Exception e) {
            e.printStackTrace();
            inspector = new Inspector();
            inspector.addAttribute(ERROR_KEY, e.getMessage());
        }

        return inspector.finish();
    }
}