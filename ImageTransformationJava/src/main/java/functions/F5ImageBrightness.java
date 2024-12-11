package functions;

import com.amazonaws.services.lambda.runtime.Context;
import saaf.Inspector;
import utils.Constants;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
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

        final HashMap<String, Object> inspector = new HashMap<>();

        final String validateMessage = Constants.validateRequestMap(request, BUCKET_KEY, FILE_NAME_KEY, "brightness_delta");
        if (validateMessage != null) {
            return Constants.getErrorObject(validateMessage);
        }

        try {
            // Extract input parameters
            final String bucketName = (String) request.get(BUCKET_KEY);
            final String fileName = (String) request.get(FILE_NAME_KEY);
            final Integer brightnessDelta = (Integer) request.get("brightness_delta");
            final String outputFileName = "brightness_" + fileName;

//            if (fileName.split("\\.")[1].equalsIgnoreCase("png")) {
//                return Constants.getErrorObject("Cannot modify brightness of a png file.");
//            }

            // Validate brightness_delta
            if (brightnessDelta < MIN_BRIGHTNESS || brightnessDelta > MAX_BRIGHTNESS) {
                throw new IllegalArgumentException(String.format("Invalid brightness_delta. Must be between %d and %d.", MIN_BRIGHTNESS, MAX_BRIGHTNESS));
            }

            // Map brightness_delta (1–100) to RescaleOp factor (0.0–2.0)
            final float brightnessFactor = brightnessDelta / 50.0f;

            final BufferedImage originalImage = isBatch ? image : Constants.getImageFromS3AndRecordLatency(bucketName, fileName, inspector);
            if (originalImage == null) {
                return Constants.getErrorObject("Could not access image from S3.");
            }

            // Adjust brightness
            final BufferedImage brightenedImage = adjustBrightness(originalImage, brightnessFactor);

            if (!isBatch) {
                final boolean successfulWriteToS3 = Constants.saveImageToS3(bucketName, outputFileName, Constants.getFileExtension(outputFileName), brightenedImage);
                if (!successfulWriteToS3) {
                    return Constants.getErrorObject("Failed to save image to S3");
                }
                if ((boolean) request.get(GET_DOWNLOAD_KEY)) {
                    inspector.put(IMAGE_URL_KEY, Constants.getDownloadableImageURL(bucketName, outputFileName));
                    inspector.put(IMAGE_URL_EXPIRES_IN, IMAGE_URL_EXPIRATION_SECONDS);
                }
            } else {
                inspector.put(IMAGE_FILE_KEY, brightenedImage);
            }

            // Populate response attributes
            inspector.put(SUCCESS_KEY, "Successfully changed image brightness.");
            inspector.put("brightness_delta", brightnessDelta);
            inspector.put("brightness_factor", brightnessFactor);

        } catch (Exception e) {
            e.printStackTrace();
            return Constants.getErrorObject(e.toString());
        }


        return inspector;
    }


    /**
     * Helper function to adjust brightness.
     *
     * @param image            The original image to modify.
     * @param brightnessFactor The factor to adjust the brightness (1.0 = original, < 1.0 = darker, > 1.0 = brighter).
     * @return The modified image with adjusted brightness.
     */
    private static BufferedImage adjustBrightness(final BufferedImage image, final float brightnessFactor) {
        final BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = result.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        final RescaleOp rescaleOp = new RescaleOp(brightnessFactor, 0, null);
        rescaleOp.filter(result, result);
        return result;
    }
}
