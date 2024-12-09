package functions;

import com.amazonaws.services.lambda.runtime.Context;
import saaf.Inspector;
import utils.Constants;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;

import static utils.Constants.*;

public class F6ImageTransform {

    /***
     *  Function 6: Image Transform
     *
     *  @param request  The image arguments.
     *  @param context  The AWS Lambda context.
     *  @return A response object.
     */
    public static HashMap<String, Object> handleRequest(final HashMap<String, Object> request, final Context context) {
        return imageTransform(null, request, context);
    }

    /***
     *  Function #6: Transform Batch Method.
     *      This function should only be called by the batch handler, which passes in a buffered image to use.
     *
     *  @param image    The image to modify.
     *  @param request  The request arguments.
     *  @param context  The AWS Lambda Context
     *  @return A response object.
     */
    public static HashMap<String, Object> imageTransform(final BufferedImage image, final HashMap<String, Object> request, final Context context) {
        final HashMap<String, Object> inspector = new HashMap<>();

        final boolean isBatch = image != null;

        try {
            // Validate request parameters
            String validateMessage = Constants.validateRequestMap(request, BUCKET_KEY, FILE_NAME_KEY);
            if (validateMessage != null) {
                return Constants.getErrorObject(validateMessage);
            }

            // Extract input parameters
            final String bucketName = (String) request.get(BUCKET_KEY);
            final String fileName = (String) request.get(FILE_NAME_KEY);
            final String targetFormat = ((String) request.getOrDefault("target_format", "JPEG")).toUpperCase();
            final String outputFileName = "transformed_" + (fileName.split("\\.")[0]) + "." + targetFormat.toLowerCase();

            // Read the original image
            final BufferedImage originalImage = isBatch
                    ? image
                    : ImageIO.read(Constants.getImageFromS3AndRecordLatency(bucketName, fileName, inspector));

            if (originalImage == null) {
                throw new IllegalArgumentException("Failed to decode image data.");
            }

            // Transform the image to the target format
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            if (!ImageIO.write(originalImage, targetFormat, outputStream)) {
                throw new IllegalArgumentException("Unsupported target format: " + targetFormat);
            }
            BufferedImage transformedImage = ImageIO.read(new ByteArrayInputStream(outputStream.toByteArray()));

            // Upload transformed image to S3 (if not in batch mode)
            if (!isBatch) {
                boolean uploadSuccess = Constants.saveImageToS3(bucketName, outputFileName, targetFormat.toLowerCase(), transformedImage);
                if (!uploadSuccess) {
                    throw new RuntimeException("Failed to save transformed image to S3");
                }
//                inspector.put(IMAGE_URL_KEY, Constants.getDownloadableImageURL(bucketName, outputFileName));
//                inspector.put(IMAGE_URL_EXPIRES_IN, IMAGE_URL_EXPIRATION_SECONDS);
            } else {
                inspector.put(IMAGE_FILE_KEY, transformedImage);
            }

            // Collect success attributes
            inspector.put(SUCCESS_KEY, "Successfully transformed image.");
            inspector.put("target_format", targetFormat);

        } catch (Exception e) {
            e.printStackTrace();
            return Constants.getErrorObject(e.toString());
        }

        return inspector;
    }
}
