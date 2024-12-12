package functions;

import com.amazonaws.services.lambda.runtime.Context;
import utils.Constants;
import utils.FileValidator;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import static utils.Constants.GET_DOWNLOAD_KEY;
import static utils.Constants.IMAGE_URL_EXPIRATION_SECONDS;
import static utils.Constants.IMAGE_URL_EXPIRES_IN;
import static utils.Constants.IMAGE_URL_KEY;
import static utils.Constants.SUCCESS_KEY;

/**
 * TCSS 462 Image Transformation
 * Group 7
 * <p>
 * Rotates an image 90, 180, or 270 degrees.
 */
public class F2ImageRotation {

    /**
     * Function 2: Image Rotation
     *
     * @param request The image arguments.
     * @param context The AWS Lambda context.
     * @return A response object.
     */
    public static HashMap<String, Object> handleRequest(final HashMap<String, Object> request, final Context context) {
        return imageRotate(null, request, context);
    }

    /**
     * Function #2: Rotation Batch Method.
     * This function should only be called by the batch handler, which passes in a buffered image to use.
     *
     * @param image   The image to rotate.
     * @param request The request arguments.
     * @param context The AWS Lambda Context
     * @return A response object.
     */
    public static HashMap<String, Object> imageRotate(final BufferedImage image, final HashMap<String, Object> request, final Context context) {
        final boolean isBatch = image != null;

        final HashMap<String, Object> inspector = new HashMap<>();

        try {
            // Validate request parameters
            if (!request.containsKey(Constants.BUCKET_KEY) || !request.containsKey(Constants.FILE_NAME_KEY) || !request.containsKey("rotation_angle")) {
                return Constants.getErrorObject("Missing required parameters: bucketName, fileName, or rotation_angle.");
            }

            final String bucketName = request.get(Constants.BUCKET_KEY).toString();
            final String fileName = request.get(Constants.FILE_NAME_KEY).toString();
            final Integer rotationAngle = (Integer) request.get("rotation_angle");
            final String outputFileName = "rotated_" + fileName;

            // Validate file format
            String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
            if (!(fileExtension.equals("jpeg") || fileExtension.equals("jpg") || fileExtension.equals("png"))) {
                return Constants.getErrorObject("Unsupported file format. Only JPEG and PNG are allowed.");
            }

            // Validate rotation angle
            if (!(rotationAngle == 90 || rotationAngle == 180 || rotationAngle == 270)) {
                return Constants.getErrorObject("Invalid rotation_angle. Only 90, 180, or 270 degrees are supported.");
            }

            final BufferedImage originalImage = isBatch ? image : Constants.getImageFromS3AndRecordLatency(bucketName, fileName, inspector);
            if (originalImage == null) {
                return Constants.getErrorObject("Could not access image from S3.");
            }

            // Rotate image
            BufferedImage rotatedImage = rotateImage(originalImage, rotationAngle);

            // Upload rotated image to S3
            if (!isBatch) {
                final boolean uploadSuccess = Constants.saveImageToS3(bucketName, outputFileName, FileValidator.getFileExtension(outputFileName), rotatedImage);
                if (!uploadSuccess) {
                    return Constants.getErrorObject("Failed to save image to S3");
                }

                if ((boolean) request.get(GET_DOWNLOAD_KEY)) {
                    inspector.put(IMAGE_URL_KEY, Constants.getDownloadableImageURL(bucketName, outputFileName));
                    inspector.put(IMAGE_URL_EXPIRES_IN, IMAGE_URL_EXPIRATION_SECONDS);
                }

            } else {
                inspector.put(Constants.IMAGE_FILE_KEY, rotatedImage); // This needs to stay to work with the batch implementation
            }
            inspector.put(SUCCESS_KEY, "Image rotated successfully.");
            inspector.put("rotation_angle", rotationAngle);

        } catch (Exception e) {
            e.printStackTrace();
            return Constants.getErrorObject(e.toString());
        }

        // Return all metrics
        return inspector;
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
