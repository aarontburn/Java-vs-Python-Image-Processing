package functions;

import com.amazonaws.services.lambda.runtime.Context;
import utils.Constants;

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
        final boolean isBatch = image != null;
        final HashMap<String, Object> inspector = new HashMap<>();

        try {
            // Validate request parameters
            String validateMessage = Constants.validateRequestMap(request, BUCKET_KEY, FILE_NAME_KEY);
            if (validateMessage != null) {
                return Constants.getErrorObject(validateMessage);
            }

            final String bucketName = (String) request.get(BUCKET_KEY);
            final String fileName = (String) request.get(FILE_NAME_KEY);

            // Check if file format is supported
            if (!fileName.endsWith(".jpeg") && !fileName.endsWith(".jpg") && !fileName.endsWith(".png")) {
                return Constants.getErrorObject("Unsupported file format. Only JPEG and PNG are allowed.");
            }

            final String outputFileName = "grayscaled_" + fileName;

            final BufferedImage originalImage = isBatch ? image : Constants.getImageFromS3AndRecordLatency(bucketName, fileName, inspector);
            if (originalImage == null) {
                return Constants.getErrorObject("Could not access image from S3.");
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
                boolean uploadSuccess = Constants.saveImageToS3(bucketName, outputFileName, Constants.getFileExtension(outputFileName), grayscaleImage);
                if (!uploadSuccess) {
                    return Constants.getErrorObject("Failed to save image to S3");
                }
                if ((boolean) request.get(GET_DOWNLOAD_KEY)) {
                    inspector.put(IMAGE_URL_KEY, Constants.getDownloadableImageURL(bucketName, outputFileName));
                    inspector.put(IMAGE_URL_EXPIRES_IN, IMAGE_URL_EXPIRATION_SECONDS);
                }
            } else {
                inspector.put(IMAGE_FILE_KEY, grayscaleImage);
            }

            inspector.put(SUCCESS_KEY, "Image successfully converted to grayscale.");

        } catch (Exception e) {
            e.printStackTrace();
            return Constants.getErrorObject(e.toString());
        }

        return inspector;
    }
}
