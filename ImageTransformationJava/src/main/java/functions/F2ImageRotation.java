package functions;

import com.amazonaws.services.lambda.runtime.Context;
import saaf.Inspector;
import utils.Constants;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;

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
    
        long roundTripStart = System.currentTimeMillis();
    
        try {
            // Validate request parameters
            if (!request.containsKey(Constants.BUCKET_KEY) || !request.containsKey(Constants.FILE_NAME_KEY) || !request.containsKey("rotation_angle")) {
                inspector.addAttribute("Error", "Missing required parameters: bucketName, fileName, or rotation_angle.");
                return inspector.finish();
            }
    
            final String bucketName = request.get(Constants.BUCKET_KEY).toString();
            final String fileName = request.get(Constants.FILE_NAME_KEY).toString();
            final Integer rotationAngle = (Integer) request.get("rotation_angle");
            final String outputFileName = "rotated_" + fileName;
    
            // Validate rotation angle
            if (!(rotationAngle == 90 || rotationAngle == 180 || rotationAngle == 270)) {
                inspector.addAttribute("Error", "Invalid rotation_angle. Only 90, 180, or 270 degrees are supported.");
                return inspector.finish();
            }
    
            // Download image
            final BufferedImage originalImage = isBatch
                    ? image
                    : ImageIO.read(Constants.getImageFromS3AndRecordLatency(bucketName, fileName, inspector));
    
            if (originalImage == null) {
                throw new IllegalArgumentException("Failed to decode image data.");
            }
    
            // Rotate image
            BufferedImage rotatedImage = rotateImage(originalImage, rotationAngle);
    
            // Upload rotated image to S3
            if (!isBatch) {
                boolean uploadSuccess = Constants.saveImageToS3(bucketName, outputFileName, "png", rotatedImage);
                if (!uploadSuccess) {
                    throw new RuntimeException("Failed to save rotated image to S3");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            inspector.addAttribute("Error", e.getMessage());
        }
    
        // Collect metrics
        inspector.inspectMetrics(isBatch, roundTripStart);
    
        // Return all metrics
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
