package functions;

import com.amazonaws.services.lambda.runtime.Context;
import saaf.Inspector;
import utils.Constants;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import static utils.Constants.*;

public class F2ImageRotation {

    /***
     *  Function 2: Image Rotation
     *
     *  @param request  The image arguments.
     *  @param context  The AWS Lambda context.
     *  @return A response object.
     */
    public static HashMap<String, Object> handleRequest(final HashMap<String, Object> request, final Context context) {
        return imageRotate(null, request, context);
    }

    /***
     *  Function #2: Rotation Batch Method.
     *      This function should only be called by the batch handler, which passes in a buffered image to use.
     *
     *  @param image    The image to rotate.
     *  @param request  The request arguments.
     *  @param context  The AWS Lambda Context
     *  @return A response object.
     */
    public static HashMap<String, Object> imageRotate(final BufferedImage image, final HashMap<String, Object> request, final Context context) {
        final boolean isBatch = image != null;

        Inspector inspector = new Inspector();

        final String validateMessage = Constants.validateRequestMap(request, BUCKET_KEY, FILE_NAME_KEY, "rotation_angle");
        if (validateMessage != null) {
            inspector = new Inspector();
            inspector.addAttribute(ERROR_KEY, validateMessage);
            return inspector.finish();
        }

        try {
            // Extract input parameters
            final String bucketName = (String) request.get(BUCKET_KEY);
            final String fileName = (String) request.get(FILE_NAME_KEY);
            final Integer rotationAngle = (Integer) request.get("rotation_angle");
            final String outputFileName = "rotated_" + fileName;


            final BufferedImage originalImage = isBatch ? image : ImageIO.read(Constants.getImageFromS3AndRecordLatency(bucketName, fileName, inspector));
            if (originalImage == null) {
                throw new IllegalArgumentException("Failed to decode image data.");
            }

            // Validate rotation angle
            if (!(rotationAngle == 90 || rotationAngle == 180 || rotationAngle == 270)) {
                throw new IllegalArgumentException("Invalid rotation angle. Only 90, 180, or 270 degrees are supported.");
            }

            // Perform rotation
            final BufferedImage rotatedImage = rotateImage(originalImage, rotationAngle);

            if (!isBatch) {
                final boolean successfulWriteToS3 = Constants.saveImageToS3(bucketName, outputFileName, "png", rotatedImage);
                if (!successfulWriteToS3) {
                    throw new RuntimeException("Could not write image to S3");
                }
            }

            // Populate response attributes
            inspector.addAttribute(SUCCESS_KEY, "Successfully rotated image.");
            inspector.addAttribute("original_width", originalImage.getWidth());
            inspector.addAttribute("original_height", originalImage.getHeight());
            inspector.addAttribute("rotated_width", rotatedImage.getWidth());
            inspector.addAttribute("rotated_height", rotatedImage.getHeight());
            inspector.addAttribute("rotation_angle", rotationAngle);
            inspector.addAttribute("rotated_image_key", outputFileName);
            if (isBatch) {
                inspector.addAttribute(IMAGE_FILE_KEY, rotatedImage);
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

    /***
     *  Helper method for image rotation.
     *
     *  @param image            The image to rotate.
     *  @param rotationAngle    The rotation angle.
     *  @return The rotated angle.
     */
    private static BufferedImage rotateImage(final BufferedImage image, final int rotationAngle) {
        final int width = image.getWidth();
        final int height = image.getHeight();

        // Create a new BufferedImage with appropriate dimensions and type
        final BufferedImage rotatedImage = new BufferedImage((rotationAngle == 90 || rotationAngle == 270) ? height : width, (rotationAngle == 90 || rotationAngle == 270) ? width : height, BufferedImage.TYPE_INT_ARGB);

        final Graphics2D graphics = rotatedImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Perform rotation with proper translation
        if (rotationAngle == 90) {
            graphics.translate(height, 0);
        } else if (rotationAngle == 180) {
            graphics.translate(width, height);
        } else if (rotationAngle == 270) {
            graphics.translate(0, width);
        }

        graphics.rotate(Math.toRadians(rotationAngle));
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();

        return rotatedImage;
    }

}
