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

public class F4ImageGrayscale implements RequestHandler<HashMap<String, Object>, HashMap<String, Object>> {

    private static boolean isColdStart = true;

    @Override
    public HashMap<String, Object> handleRequest(HashMap<String, Object> request, Context context) {
        Inspector inspector = new Inspector();
        long invocationStartTime = System.currentTimeMillis();

        try {
            // Extract input parameters
            String bucketName = (String) request.get("bucketname");
            String fileName = (String) request.get("filename");

            if (bucketName == null || fileName == null) {
                throw new IllegalArgumentException("Missing required parameters: 'bucketname' or 'filename'");
            }

            // Log whether this is a cold start
            boolean coldStart = isColdStart;
            isColdStart = false; // Reset the cold start flag for subsequent invocations
            inspector.addAttribute("cold_start", coldStart ? 1 : 0);

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

            // Convert the image to grayscale
            long processingStartTime = System.currentTimeMillis();
            BufferedImage grayscaleImage = convertToGrayscale(originalImage);
            long processingEndTime = System.currentTimeMillis();
            inspector.addAttribute("processing_time_ms", processingEndTime - processingStartTime);

            // Encode the grayscale image back to bytes
            long encodeStartTime = System.currentTimeMillis();
            String grayscaleImageBase64 = encodeImageToBase64(grayscaleImage);
            long encodeEndTime = System.currentTimeMillis();
            inspector.addAttribute("encode_time_ms", encodeEndTime - encodeStartTime);

            // Save the grayscale image back to S3
            String grayscaleFileName = "grayscale_" + fileName;
            byte[] grayscaleImageBytes = Base64.getDecoder().decode(grayscaleImageBase64);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(grayscaleImageBytes.length);
            metadata.setContentType("image/png");
            s3Client.putObject(bucketName, grayscaleFileName, new ByteArrayInputStream(grayscaleImageBytes), metadata);

            // Populate response attributes
            inspector.addAttribute("original_width", originalImage.getWidth());
            inspector.addAttribute("original_height", originalImage.getHeight());
            inspector.addAttribute("grayscale_width", grayscaleImage.getWidth());
            inspector.addAttribute("grayscale_height", grayscaleImage.getHeight());
            inspector.addAttribute("grayscale_image_key", grayscaleFileName);

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

    private BufferedImage convertToGrayscale(BufferedImage image) {
        BufferedImage grayscaleImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        grayscaleImage.getGraphics().drawImage(image, 0, 0, null);
        return grayscaleImage;
    }

    private String encodeImageToBase64(BufferedImage image) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }
}
