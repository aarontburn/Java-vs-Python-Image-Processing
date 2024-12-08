package functions;

import com.amazonaws.services.lambda.runtime.Context;
import saaf.Inspector;
import utils.Constants;

import javax.imageio.ImageIO;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;

import static utils.Constants.*;

public class F1ImageDetails {

    /***
     *  Function #1: Image upload and validation
     *
     *  @param request  The image arguments.
     *  @param context  The AWS Lambda context.
     *  @return A response object.
     */
    public static HashMap<String, Object> handleRequest(final HashMap<String, Object> request, final Context context) {
        return imageDetails(null, request, context);
    }

    /***
     *  Function #1: Image Details Batch Method.
     *      This function should only be called by the batch handler, which passes in a buffered image to use.
     *
     *  @param image    The image to get the details of.
     *  @param request  The request arguments.
     *  @param context  The AWS Lambda Context.
     *  @return A response object.
     */
    public static HashMap<String, Object> imageDetails(final BufferedImage image, final HashMap<String, Object> request, final Context context) {
        final boolean isBatch = image != null;

        // This could be replaced with a hashmap, especially if we don't need info from the inspector
        Inspector inspector = new Inspector();

        // Record the round-trip start time
        final long roundTripStart = System.currentTimeMillis();


        final String validateMessage = Constants.validateRequestMap(request, BUCKET_KEY, FILE_NAME_KEY);
        if (validateMessage != null) {
            inspector = new Inspector();
            inspector.addAttribute(ERROR_KEY, validateMessage);
            return inspector.finish();
        }


        try {
            final String bucketName = (String) request.get(BUCKET_KEY);
            final String fileName = (String) request.get(FILE_NAME_KEY);

            final InputStream objectData = Constants.getImageFromS3AndRecordLatency(bucketName, fileName, inspector);
            final BufferedImage imageObject = isBatch ? image : ImageIO.read(objectData);

            inspector.addAttribute(SUCCESS_KEY, "Successfully retrieved image details.");
            inspector.addAttribute("width", imageObject.getWidth());
            inspector.addAttribute("height", imageObject.getHeight());
            inspector.addAttribute("mode", getColorType(imageObject.getColorModel().getColorSpace().getType()));
            inspector.addAttribute("has_transparency_data", imageObject.getColorModel().hasAlpha() ? 1 : 0);
            if (isBatch) {
                inspector.addAttribute(IMAGE_FILE_KEY, imageObject);
            }

        } catch (final Exception e) {
            e.printStackTrace();
            inspector = new Inspector();
            inspector.addAttribute(ERROR_KEY, e.toString());
        }
        // Collect metrics
        inspector.inspectMetrics(isBatch, roundTripStart);


        return inspector.finish();
    }

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
