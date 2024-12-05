package old;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import saaf.Inspector;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;

public class F6ImageTransform implements RequestHandler<HashMap<String, Object>, HashMap<String, Object>> {

    private static boolean isColdStart = true;

    @Override
    public HashMap<String, Object> handleRequest(HashMap<String, Object> request, Context context) {
        Inspector inspector = new Inspector();
        long invocationStartTime = System.currentTimeMillis(); // Track function start time

        try {
            // Log cold start
            boolean coldStart = isColdStart;
            isColdStart = false; // Reset the flag for subsequent invocations
            inspector.addAttribute("cold_start", coldStart ? 1 : 0);

            // Extract input parameters
            String bucketName = (String) request.get("bucketname");
            String fileName = (String) request.get("filename");
            String targetFormat = (String) request.getOrDefault("target_format", "jpeg");

            if (bucketName == null || fileName == null) {
                throw new IllegalArgumentException("Missing required parameters: 'bucketname' or 'filename'");
            }

            // Fetch the image from S3
            long s3StartTime = System.currentTimeMillis();
            AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
            S3Object s3Object = s3Client.getObject(bucketName, fileName);
            ByteArrayInputStream objectData = new ByteArrayInputStream(s3Object.getObjectContent().readAllBytes());
            long s3EndTime = System.currentTimeMillis();
            long networkLatency = s3EndTime - s3StartTime;
            inspector.addAttribute("network_latency_ms", networkLatency);

            // Decode the image from S3
            long decodeStartTime = System.currentTimeMillis();
            BufferedImage originalImage = ImageIO.read(objectData);
            long decodeEndTime = System.currentTimeMillis();
            if (originalImage == null) {
                throw new IllegalArgumentException("Failed to decode image data");
            }
            inspector.addAttribute("decode_time_ms", decodeEndTime - decodeStartTime);

            // Transform the image to the target format
            long processingStartTime = System.currentTimeMillis();
            String transformedImageBase64 = transformImageToFormat(originalImage, targetFormat);
            long processingEndTime = System.currentTimeMillis();
            inspector.addAttribute("processing_time_ms", processingEndTime - processingStartTime);

            // Save the transformed image back to S3
            String transformedFileName = "transformed_" + fileName;
            byte[] transformedImageBytes = Base64.getDecoder().decode(transformedImageBase64);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(transformedImageBytes.length);
            metadata.setContentType("image/" + targetFormat);

            long saveStartTime = System.currentTimeMillis();
            s3Client.putObject(bucketName, transformedFileName, new ByteArrayInputStream(transformedImageBytes), metadata);
            long saveEndTime = System.currentTimeMillis();
            inspector.addAttribute("save_latency_ms", saveEndTime - saveStartTime);

            // Populate response attributes
            inspector.addAttribute("original_width", originalImage.getWidth());
            inspector.addAttribute("original_height", originalImage.getHeight());
            inspector.addAttribute("transformed_file_key", transformedFileName);
            inspector.addAttribute("target_format", targetFormat);

            // Estimate cost (in USD)
            double memorySizeGB = 0.512; // Lambda memory size in GB
            double pricePerGBSecond = 0.00001667; // Pricing for Lambda
            double functionRuntime = (processingEndTime - invocationStartTime) / 1000.0;
            double cost = functionRuntime * memorySizeGB * pricePerGBSecond;
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
     * Transforms the image to the specified file format and encodes it to Base64.
     *
     * @param image  The original image to transform.
     * @param format The target file format (e.g., "jpeg", "png").
     * @return The Base64-encoded transformed image.
     * @throws Exception If an error occurs during transformation.
     */
    private String transformImageToFormat(BufferedImage image, String format) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        boolean success = ImageIO.write(image, format, outputStream);

        if (!success) {
            throw new IllegalArgumentException("Unsupported target format: " + format);
        }

        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }
}
