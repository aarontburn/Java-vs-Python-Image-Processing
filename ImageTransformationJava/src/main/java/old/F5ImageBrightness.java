package old;

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
import java.awt.image.RescaleOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;

public class F5ImageBrightness implements RequestHandler<HashMap<String, Object>, HashMap<String, Object>> {

    private static final int MIN_BRIGHTNESS = 1;
    private static final int MAX_BRIGHTNESS = 100;
    private static boolean isColdStart = true;

    @Override
    public HashMap<String, Object> handleRequest(HashMap<String, Object> request, Context context) {
        Inspector inspector = new Inspector();
        long invocationStartTime = System.currentTimeMillis();

        try {
            // Log cold start
            boolean coldStart = isColdStart;
            isColdStart = false;
            inspector.addAttribute("cold_start", coldStart ? 1 : 0);

            // Extract input parameters
            String bucketName = (String) request.get("bucketname");
            String fileName = (String) request.get("filename");
            Integer brightnessDelta = (Integer) request.get("brightness_delta");

            if (bucketName == null || fileName == null || brightnessDelta == null) {
                throw new IllegalArgumentException("Missing required parameters: 'bucketname', 'filename', or 'brightness_delta'");
            }

            if (brightnessDelta < MIN_BRIGHTNESS || brightnessDelta > MAX_BRIGHTNESS) {
                throw new IllegalArgumentException(
                        String.format("Invalid brightness_delta. Must be between %d and %d.", MIN_BRIGHTNESS, MAX_BRIGHTNESS));
            }

            float brightnessFactor = mapBrightness(brightnessDelta);

            // Fetch the image from S3
            long s3StartTime = System.currentTimeMillis();
            AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
            S3Object s3Object = s3Client.getObject(bucketName, fileName);
            ByteArrayInputStream objectData = new ByteArrayInputStream(s3Object.getObjectContent().readAllBytes());
            long s3EndTime = System.currentTimeMillis();
            inspector.addAttribute("network_latency_ms", s3EndTime - s3StartTime);

            // Decode the image
            long decodeStartTime = System.currentTimeMillis();
            BufferedImage originalImage = ImageIO.read(objectData);
            long decodeEndTime = System.currentTimeMillis();
            if (originalImage == null) {
                throw new IllegalArgumentException("Failed to decode image data");
            }
            inspector.addAttribute("decode_time_ms", decodeEndTime - decodeStartTime);

            // Adjust brightness
            long processingStartTime = System.currentTimeMillis();
            BufferedImage brightenedImage = adjustBrightness(originalImage, brightnessFactor);
            long processingEndTime = System.currentTimeMillis();
            inspector.addAttribute("processing_time_ms", processingEndTime - processingStartTime);

            // Encode and upload the brightened image to S3
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(brightenedImage, "png", outputStream);
            byte[] brightenedImageBytes = outputStream.toByteArray();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(brightenedImageBytes.length);
            metadata.setContentType("image/png");

            String transformedFileName = "brightened_" + fileName;
            long saveStartTime = System.currentTimeMillis();
            s3Client.putObject(bucketName, transformedFileName, new ByteArrayInputStream(brightenedImageBytes), metadata);
            long saveEndTime = System.currentTimeMillis();
            inspector.addAttribute("save_latency_ms", saveEndTime - saveStartTime);

            // Populate response
            inspector.addAttribute("original_width", originalImage.getWidth());
            inspector.addAttribute("original_height", originalImage.getHeight());
            inspector.addAttribute("brightness_delta", brightnessDelta);
            inspector.addAttribute("brightness_factor", brightnessFactor);
            inspector.addAttribute("transformed_file_key", transformedFileName);

            // Estimate cost
            double memorySizeGB = 0.512;
            double pricePerGBSecond = 0.00001667;
            double functionRuntime = (processingEndTime - invocationStartTime) / 1000.0;
            double cost = functionRuntime * memorySizeGB * pricePerGBSecond;
            inspector.addAttribute("estimated_cost_usd", cost);

        } catch (Exception e) {
            e.printStackTrace();
            inspector.addAttribute("error", e.getMessage());
        }

        // Calculate round trip time
        long invocationEndTime = System.currentTimeMillis();
        inspector.addAttribute("round_trip_time_ms", invocationEndTime - invocationStartTime);

        return inspector.finish();
    }

    private float mapBrightness(int brightnessDelta) {
        return brightnessDelta / 50.0f;
    }

    private BufferedImage adjustBrightness(BufferedImage image, float brightnessFactor) {
        RescaleOp rescaleOp = new RescaleOp(brightnessFactor, 0, null);
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Graphics2D g = result.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        rescaleOp.filter(result, result);
        return result;
    }
}
