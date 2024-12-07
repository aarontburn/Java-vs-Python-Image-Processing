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
        Inspector inspector = new Inspector();
        long roundTripStart = System.currentTimeMillis();
        final boolean isBatch = image != null;

        try {
            // Validate request parameters
            String validateMessage = Constants.validateRequestMap(request, BUCKET_KEY, FILE_NAME_KEY);
            if (validateMessage != null) {
                inspector.addAttribute("Error", validateMessage);
                return inspector.finish();
            }

            final String bucketName = (String) request.get(BUCKET_KEY);
            final String fileName = (String) request.get(FILE_NAME_KEY);
            final String outputFileName = "grayscaled_" + fileName;

            // Download image
            final BufferedImage originalImage = isBatch
                    ? image
                    : ImageIO.read(Constants.getImageFromS3AndRecordLatency(bucketName, fileName, inspector));

            if (originalImage == null) {
                throw new IllegalArgumentException("Failed to decode image data.");
            }

            // Convert image to grayscale
            BufferedImage grayscaleImage = new BufferedImage(
                    originalImage.getWidth(),
                    originalImage.getHeight(),
                    BufferedImage.TYPE_BYTE_GRAY
            );
            grayscaleImage.getGraphics().drawImage(originalImage, 0, 0, null);

            // Upload grayscale image to S3
            if (!isBatch) {
                boolean uploadSuccess = Constants.saveImageToS3(bucketName, outputFileName, "png", grayscaleImage);
                if (!uploadSuccess) {
                    throw new RuntimeException("Failed to save grayscaled image to S3");
                }
                inspector.addAttribute("ImageURL", Constants.getDownloadableImageURL(bucketName, outputFileName));
            }

            // Collect additional metrics
            inspector.addAttribute("OriginalWidth", originalImage.getWidth());
            inspector.addAttribute("OriginalHeight", originalImage.getHeight());
            inspector.addAttribute("Success", "Image successfully converted to grayscale.");

        } catch (Exception e) {
            e.printStackTrace();
            inspector.addAttribute("Error", e.getMessage());
        }

        // Collect round-trip metrics
        inspector.inspectMetrics(isBatch, roundTripStart);

        // Return all metrics
        return inspector.finish();
    }
}
