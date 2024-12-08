package functions;

import com.amazonaws.services.lambda.runtime.Context;
import saaf.Inspector;
import utils.Constants;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.HashMap;

import static utils.Constants.*;

public class F5ImageBrightness {

    /***
     *  The minimum brightness value.
     */
    private static final int MIN_BRIGHTNESS = 1;

    /***
     *  The maximum brightness value.
     */
    private static final int MAX_BRIGHTNESS = 100;


    /***
     *  Function 5: Image Brightness
     *
     *  @param request  The image arguments.
     *  @param context  The AWS Lambda context.
     *  @return A response object.
     */
    public static HashMap<String, Object> handleRequest(final HashMap<String, Object> request, final Context context) {
        return imageBrightness(null, request, context);
    }

    /***
     *  Function #5: Brightness Batch Method.
     *      This function should only be called by the batch handler, which passes in a buffered image to use.
     *
     *  @param image    The image to modify.
     *  @param request  The request arguments.
     *  @param context  The AWS Lambda Context
     *  @return A response object.
     */
    public static HashMap<String, Object> imageBrightness(final BufferedImage image, final HashMap<String, Object> request, final Context context) {
        final boolean isBatch = image != null;

        Inspector inspector = new Inspector();

        // Record the round-trip start time
        final long roundTripStart = System.currentTimeMillis();


        final String validateMessage = Constants.validateRequestMap(request, BUCKET_KEY, FILE_NAME_KEY, "brightness_delta");
        if (validateMessage != null) {
            inspector = new Inspector();
            inspector.addAttribute(ERROR_KEY, validateMessage);
            return inspector.finish();
        }


        try {
            // Extract input parameters
            final String bucketName = (String) request.get(BUCKET_KEY);
            final String fileName = (String) request.get(FILE_NAME_KEY);
            final Integer brightnessDelta = (Integer) request.get("brightness_delta");
            final String outputFileName = "brightness_" + fileName;


            // Validate brightness_delta
            if (brightnessDelta < MIN_BRIGHTNESS || brightnessDelta > MAX_BRIGHTNESS) {
                throw new IllegalArgumentException(String.format("Invalid brightness_delta. Must be between %d and %d.", MIN_BRIGHTNESS, MAX_BRIGHTNESS));
            }

            // Map brightness_delta (1–100) to RescaleOp factor (0.0–2.0)
            final float brightnessFactor = brightnessDelta / 50.0f;

            // Decode the Base64-encoded image
            final BufferedImage originalImage = isBatch ? image : ImageIO.read(Constants.getImageFromS3AndRecordLatency(bucketName, fileName, inspector));

            // Adjust brightness
            final BufferedImage brightenedImage = adjustBrightness(originalImage, brightnessFactor);

            if (!isBatch) {
                final boolean successfulWriteToS3 = Constants.saveImageToS3(bucketName, outputFileName, "png", brightenedImage);
                if (!successfulWriteToS3) {
                    throw new RuntimeException("Could not write image to S3");

                }
            }

            // Populate response attributes
            inspector.addAttribute(SUCCESS_KEY, "Successfully changed image brightness.");
            inspector.addAttribute("original_width", originalImage.getWidth());
            inspector.addAttribute("original_height", originalImage.getHeight());
            inspector.addAttribute("brightness_delta", brightnessDelta);
            inspector.addAttribute("brightness_factor", brightnessFactor);
            if (isBatch) {
                inspector.addAttribute(IMAGE_FILE_KEY, brightenedImage);
            } else {
                inspector.addAttribute(IMAGE_URL_KEY, getDownloadableImageURL(bucketName, outputFileName));
                inspector.addAttribute(IMAGE_URL_EXPIRES_IN, IMAGE_URL_EXPIRATION_SECONDS);
            }

        } catch (Exception e) {
            e.printStackTrace();
            inspector = new Inspector();
            inspector.addAttribute(ERROR_KEY, e.getMessage());
        }

        // Collect metrics
        inspector.inspectMetrics(isBatch, roundTripStart);


        return inspector.finish();
    }


    /**
     * Helper function to adjust brightness.
     *
     * @param image            The original image to modify.
     * @param brightnessFactor The factor to adjust the brightness (1.0 = original, < 1.0 = darker, > 1.0 = brighter).
     * @return The modified image with adjusted brightness.
     */
    private static BufferedImage adjustBrightness(final BufferedImage image, final float brightnessFactor) {
        final RescaleOp rescaleOp = new RescaleOp(brightnessFactor, 0, null);
        final BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        final Graphics2D g = result.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        rescaleOp.filter(result, result);
        return result;
    }
}
