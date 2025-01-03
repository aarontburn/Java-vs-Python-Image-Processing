package functions;

import com.amazonaws.services.lambda.runtime.Context;
import utils.Constants;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import static utils.Constants.BUCKET_KEY;
import static utils.Constants.FILE_NAME_KEY;
import static utils.Constants.GET_DOWNLOAD_KEY;
import static utils.Constants.IMAGE_FILE_KEY;
import static utils.Constants.IMAGE_URL_EXPIRATION_SECONDS;
import static utils.Constants.IMAGE_URL_EXPIRES_IN;
import static utils.Constants.IMAGE_URL_KEY;
import static utils.Constants.SUCCESS_KEY;

/**
 * TCSS 462 Image Transformation
 * Group 7
 * <p>
 * Retrieves image details.
 */
public class F1ImageDetails {

    /**
     * Function #1: Image upload and validation
     *
     * @param request The image arguments.
     * @param context The AWS Lambda context.
     * @return A response object.
     */
    public static HashMap<String, Object> handleRequest(final HashMap<String, Object> request, final Context context) {
        return imageDetails(null, request, context);
    }

    /**
     * Function #1: Image Details Batch Method.
     * This function should only be called by the batch handler, which passes in a buffered image to use.
     *
     * @param image   The image to get the details of.
     * @param request The request arguments.
     * @param context The AWS Lambda Context.
     * @return A response object.
     */
    public static HashMap<String, Object> imageDetails(final BufferedImage image, final HashMap<String, Object> request, final Context context) {
        final boolean isBatch = image != null;

        // This could be replaced with a hashmap, especially if we don't need info from the inspector
        final HashMap<String, Object> inspector = new HashMap<>();

        final String validateMessage = Constants.validateRequestMap(request, BUCKET_KEY, FILE_NAME_KEY);
        if (validateMessage != null) {
            return Constants.getErrorObject(validateMessage);
        }


        try {
            final String bucketName = (String) request.get(BUCKET_KEY);
            final String fileName = (String) request.get(FILE_NAME_KEY);


            // Validate file extension
            if (!fileName.toLowerCase().endsWith(".jpeg") && !fileName.toLowerCase().endsWith(".jpg") && !fileName.toLowerCase().endsWith(".png")) {
                return Constants.getErrorObject("Unsupported file format. Only JPEG and PNG are allowed.");
            }

            final BufferedImage imageObject = isBatch ? image : Constants.getImageFromS3AndRecordLatency(bucketName, fileName, inspector);
            if (imageObject == null) {
                return Constants.getErrorObject("Could not access image from S3.");
            }

            inspector.put(SUCCESS_KEY, "Successfully retrieved image details.");
            inspector.put("width", imageObject.getWidth());
            inspector.put("height", imageObject.getHeight());
            inspector.put("mode", getColorType(imageObject.getColorModel().getColorSpace().getType()));
            inspector.put("has_transparency_data", imageObject.getColorModel().hasAlpha() ? 1 : 0);
            if (isBatch) {
                inspector.put(IMAGE_FILE_KEY, imageObject);
            } else {
                if ((boolean) request.get(GET_DOWNLOAD_KEY)) {
                    inspector.put(IMAGE_URL_KEY, Constants.getDownloadableImageURL(bucketName, fileName));
                    inspector.put(IMAGE_URL_EXPIRES_IN, IMAGE_URL_EXPIRATION_SECONDS);
                }
            }

        } catch (final Exception e) {
            e.printStackTrace();
            return Constants.getErrorObject(e.toString());
        }

        return inspector;
    }

    /**
     * Converts an integer to color type String.
     *
     * @param type The color type as an integer.
     * @return The color type as a String.
     */
    private static String getColorType(final int type) {
        switch (type) {
            case ColorSpace.TYPE_RGB:
                return "RGB";
            case ColorSpace.TYPE_GRAY:
                return "L";
            case ColorSpace.TYPE_CMYK:
                return "CMYK";
            case ColorSpace.TYPE_YCbCr:
                return "YCbCr";
            case ColorSpace.TYPE_CMY:
                return "CMY";
            case ColorSpace.TYPE_HLS:
                return "HLS";
            case ColorSpace.TYPE_HSV:
                return "HSV";
            case ColorSpace.TYPE_Lab:
                return "LAB";
            case ColorSpace.TYPE_Luv:
                return "Luv";
            case ColorSpace.TYPE_XYZ:
                return "XYZ";
            default:
                return "Unknown";
        }
    }

}
