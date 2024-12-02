package lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import saaf.Inspector;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;

public class F2ImageRotation implements RequestHandler<HashMap<String, Object>, HashMap<String, Object>> {

    private static boolean isColdStart = true; // Tracks whether it's a cold start

    @Override
    public HashMap<String, Object> handleRequest(HashMap<String, Object> request, Context context) {
        Inspector inspector = new Inspector(); // Initialize Inspector
        long invocationStartTime = System.currentTimeMillis(); // Start time in ms

        try {
            // Extract input parameters
            String bucketName = (String) request.get("bucketname");
            String fileName = (String) request.get("filename");
            Integer rotationAngle = (Integer) request.get("rotation_angle");

            if (bucketName == null || fileName == null || rotationAngle == null) {
                throw new IllegalArgumentException("Missing required parameters: 'bucketname', 'filename', or 'rotation_angle'");
            }

            // Log whether this is a cold start
            boolean coldStart = isColdStart;
            isColdStart = false; // Reset the cold start flag for subsequent invocations
            inspector.addAttribute("cold_start", coldStart ? 1 : 0);

            // Fetch the image from S3
            long s3StartTime = System.currentTimeMillis();
            AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
            S3Object s3Object = s3Client.getObject(bucketName, fileName);
            InputStream objectData = s3Object.getObjectContent();
            long s3EndTime = System.currentTimeMillis();
            long networkLatency = s3EndTime - s3StartTime;
            inspector.addAttribute("network_latency_ms", networkLatency);

            // Process the image
            long processingStartTime = System.currentTimeMillis();
            BufferedImage originalImage = ImageIO.read(objectData);
            if (originalImage == null) {
                throw new IllegalArgumentException("Failed to decode image from S3");
            }

            // Validate rotation angle
            if (!isValidRotationAngle(rotationAngle)) {
                throw new IllegalArgumentException("Invalid rotation angle. Only 90, 180, or 270 degrees are supported.");
            }

            // Perform rotation
            BufferedImage rotatedImage = rotateImage(originalImage, rotationAngle);

            // Encode rotated image back to bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(rotatedImage, "png", outputStream);
            byte[] rotatedImageBytes = outputStream.toByteArray();

            // Save the rotated image back to S3
            String rotatedFilename = "rotated_" + fileName;
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(rotatedImageBytes.length);
            metadata.setContentType("image/png");
            s3Client.putObject(bucketName, rotatedFilename, new ByteArrayInputStream(rotatedImageBytes), metadata);

            long processingEndTime = System.currentTimeMillis();
            long functionRuntime = processingEndTime - processingStartTime;
            inspector.addAttribute("function_runtime_ms", functionRuntime);

            // Populate response attributes
            inspector.addAttribute("original_width", originalImage.getWidth());
            inspector.addAttribute("original_height", originalImage.getHeight());
            inspector.addAttribute("rotated_width", rotatedImage.getWidth());
            inspector.addAttribute("rotated_height", rotatedImage.getHeight());
            inspector.addAttribute("rotation_angle", rotationAngle);
            inspector.addAttribute("rotated_image_key", rotatedFilename);

            // Estimate cost (in USD)
            double memorySizeGB = 0.512; // Lambda memory size in GB
            double pricePerGBSecond = 0.00001667; // Pricing for Lambda
            double cost = (functionRuntime / 1000.0) * memorySizeGB * pricePerGBSecond;
            inspector.addAttribute("estimated_cost_usd", cost);

        } catch (Exception e) {
            e.printStackTrace();
            inspector.addAttribute("error", e.getMessage());
        }

        // Calculate total round trip time
        long invocationEndTime = System.currentTimeMillis();
        long roundTripTime = invocationEndTime - invocationStartTime;
        inspector.addAttribute("round_trip_time_ms", roundTripTime);

        // Output metrics to CloudWatch Logs
        System.out.println(inspector.finish());

        return inspector.finish();
    }

    /**
     * Validates if the rotation angle is one of the supported values (90, 180, 270).
     *
     * @param angle The rotation angle to validate.
     * @return true if the angle is valid; false otherwise.
     */
    private boolean isValidRotationAngle(Integer angle) {
        return angle == 90 || angle == 180 || angle == 270;
    }

    /**
     * Rotates the given image by the specified angle.
     *
     * @param image         The image to rotate.
     * @param rotationAngle The angle to rotate the image (90, 180, or 270 degrees).
     * @return The rotated image.
     */
    private BufferedImage rotateImage(BufferedImage image, int rotationAngle) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Create a new BufferedImage with appropriate dimensions and type
        BufferedImage rotatedImage = new BufferedImage(
                (rotationAngle == 90 || rotationAngle == 270) ? height : width,
                (rotationAngle == 90 || rotationAngle == 270) ? width : height,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D graphics = rotatedImage.createGraphics();
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
