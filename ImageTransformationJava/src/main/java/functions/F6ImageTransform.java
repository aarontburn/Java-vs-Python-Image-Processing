package functions;

import com.amazonaws.services.lambda.runtime.Context;
import saaf.Inspector;
import utils.Constants;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

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
        final boolean isBatch = image != null;

        Inspector inspector = new Inspector();

        final String validateMessage = Constants.validateRequestMap(request, BUCKET_KEY, FILE_NAME_KEY);
        if (validateMessage != null) {
            inspector = new Inspector();
            inspector.addAttribute(ERROR_KEY, validateMessage);
            return inspector.finish();
        }

        try {
            // Extract input parameters
            final String bucketName = (String) request.get(BUCKET_KEY);
            final String fileName = (String) request.get(FILE_NAME_KEY);
            final String targetFormat = ((String) request.getOrDefault("target_format", "JPEG")).toUpperCase();
            final String outputFileName = "transformed_" + fileName;


            final BufferedImage originalImage = ImageIO.read(Constants.getImageFromS3AndRecordLatency(bucketName, fileName, inspector));

            // Transform image to the target format
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            if (!ImageIO.write(originalImage, targetFormat, outputStream)) {
                throw new IllegalArgumentException("Unsupported target format: " + targetFormat);
            }

            final BufferedImage transformedImage = ImageIO.read(new ByteArrayInputStream(outputStream.toByteArray()));

            if (!isBatch) {
                final boolean successfulWriteToS3 = Constants.saveImageToS3(bucketName, outputFileName, "png", transformedImage);
                if (!successfulWriteToS3) {
                    throw new RuntimeException("Could not write image to S3");
                }
            }

            inspector.addAttribute(SUCCESS_KEY, "Successfully transformed image.");
            inspector.addAttribute("original_width", originalImage.getWidth());
            inspector.addAttribute("original_height", originalImage.getHeight());
            inspector.addAttribute("target_format", targetFormat);

            if (isBatch) {
                inspector.addAttribute(IMAGE_FILE_KEY, transformedImage);
            } else {
                inspector.addAttribute(IMAGE_URL_KEY, getDownloadableImageURL(bucketName, outputFileName));
                inspector.addAttribute(IMAGE_URL_EXPIRES_IN, IMAGE_URL_EXPIRATION_SECONDS);
            }


        } catch (Exception e) {
            e.printStackTrace();
            inspector = new Inspector();
            inspector.addAttribute(ERROR_KEY, e.getMessage());
        }

        return inspector.finish();
    }

}
