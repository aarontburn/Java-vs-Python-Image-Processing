package functions;

import com.amazonaws.services.lambda.runtime.Context;
import saaf.Inspector;
import utils.Constants;

import javax.imageio.ImageIO;
import java.awt.*;
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
            final String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

            if (!fileExtension.equals("jpeg") && !fileExtension.equals("jpg") && !fileExtension.equals("png")) {
                return Constants.getErrorObject("Only JPEG and PNG formats are supported.");
            }

            final String targetFormat = ((String) request.getOrDefault("target_format", "JPEG")).toUpperCase();
            if (!targetFormat.equals("JPEG") && !targetFormat.equals("PNG")) {
                return Constants.getErrorObject("Target format must be JPEG or PNG.");
            }


            // Function should still work so other things dont break.
//            if (fileExtension.equals(targetFormat.toLowerCase())) {
//                return Constants.getErrorObject("Source and target formats are the same. No transformation needed.");
//            }

            final String outputFileName = "transformed_" + fileName.substring(0, fileName.lastIndexOf('.')) + "." + targetFormat.toLowerCase();

            // Read the original image
            BufferedImage originalImage = isBatch ? image : Constants.getImageFromS3AndRecordLatency(bucketName, fileName, inspector);
            if (originalImage == null) {
                return Constants.getErrorObject("Could not access image from S3.");
            }

            if (fileExtension.equalsIgnoreCase("png") && targetFormat.equalsIgnoreCase("jpeg")) {
                originalImage = removeAlphaChannel(originalImage);
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
                    return Constants.getErrorObject("Failed to save image to S3");
                }
                if ((boolean) request.get(GET_DOWNLOAD_KEY)) {
                    inspector.put(IMAGE_URL_KEY, Constants.getDownloadableImageURL(bucketName, outputFileName));
                    inspector.put(IMAGE_URL_EXPIRES_IN, IMAGE_URL_EXPIRATION_SECONDS);
                }
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



    private static BufferedImage removeAlphaChannel(final BufferedImage img) {
        if (!img.getColorModel().hasAlpha()) {
            return img;
        }

        final BufferedImage target = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = target.createGraphics();
        // g.setColor(new Color(color, false));
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.drawImage(img, 0, 0, null);
        g.dispose();

        return target;
    }

}
