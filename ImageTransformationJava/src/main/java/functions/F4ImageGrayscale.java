package functions;

import com.amazonaws.services.lambda.runtime.Context;
import saaf.Inspector;
import utils.Constants;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import static utils.Constants.*;

public class F4ImageGrayscale {

    /***
     *  Function 4: Image Grayscale
     *
     *  @param request  The image arguments.
     *  @param context  The AWS Lambda context.
     *  @return A response object.
     */
    public static HashMap<String, Object> handleRequest(final HashMap<String, Object> request, final Context context) {
        return imageGrayscale(null, request, context);
    }

    /***
     *  Function #4: Grayscale Batch Method.
     *      This function should only be called by the batch handler, which passes in a buffered image to use.
     *
     *  @param image    The image to modify.
     *  @param request  The request arguments.
     *  @param context  The AWS Lambda Context
     *  @return A response object.
     */
    public static HashMap<String, Object> imageGrayscale(final BufferedImage image, final HashMap<String, Object> request, final Context context) {
        final long functionStartTime = System.currentTimeMillis();
        final boolean isBatch = image != null;

        Inspector inspector = new Inspector();

        final String validateMessage = Constants.validateRequestMap(request, BUCKET_KEY, FILE_NAME_KEY);
        if (validateMessage != null) {
            inspector = new Inspector();
            inspector.addAttribute(ERROR_KEY, validateMessage);
            return inspector.finish();
        }

        try {
            final String bucketName = (String) request.get(BUCKET_KEY);
            final String fileName = (String) request.get(FILE_NAME_KEY);
            final String outputFileName = "grayscaled_" + fileName;

            final long processingStartTime = System.currentTimeMillis();

            final BufferedImage originalImage = isBatch
                    ? image
                    : ImageIO.read(Constants.getImageFromS3AndRecordLatency(bucketName, fileName, inspector));

            // Convert the image to grayscale
            final BufferedImage grayscaleImage = new BufferedImage(
                    originalImage.getWidth(),
                    originalImage.getHeight(),
                    BufferedImage.TYPE_BYTE_GRAY
            );
            grayscaleImage.getGraphics().drawImage(originalImage, 0, 0, null);

            if (!isBatch) {
                final boolean successfulWriteToS3 = Constants.saveImageToS3(bucketName, outputFileName, "png", grayscaleImage);
                if (!successfulWriteToS3) {
                    throw new RuntimeException("Could not write image to S3");
                }
            }

            // Add details to the response
            inspector.addAttribute(SUCCESS_KEY, "Successfully converted image to grayscale.");
            inspector.addAttribute("original_width", originalImage.getWidth());
            inspector.addAttribute("original_height", originalImage.getHeight());

            if (isBatch) {
                inspector.addAttribute(IMAGE_FILE_KEY, grayscaleImage);
            } else {
                inspector.addAttribute(IMAGE_URL_KEY, getDownloadableImageURL(bucketName, outputFileName));
                inspector.addAttribute(IMAGE_URL_EXPIRES_IN, IMAGE_URL_EXPIRATION_SECONDS);
            }

        } catch (final Exception e) {
            e.printStackTrace();
            inspector = new Inspector();
            inspector.addAttribute(ERROR_KEY, e.getMessage());
        }

        return inspector.finish();
    }
}
