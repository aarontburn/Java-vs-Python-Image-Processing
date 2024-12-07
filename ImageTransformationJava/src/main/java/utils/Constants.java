package utils;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import saaf.Inspector;

import javax.imageio.ImageIO;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.Buffer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Constants {

    public static final String IMAGE_FILE_KEY = "image_file";
    public static final String ERROR_KEY = "error";
    public static final String BUCKET_KEY = "bucketname";
    public static final String FILE_NAME_KEY = "filename";
    public static final String IMAGE_URL_KEY = "url";
    public static final String IMAGE_URL_EXPIRES_IN = "url_expires_in_seconds";
    public static final String SUCCESS_KEY = "success";

    public static final String LANGUAGE_KEY = "language";
    public static final String NETWORK_LATENCY_KEY = "network_latency_ms";
    public static final String FUNCTION_RUN_TIME_KEY = "function_runtime_ms";
    public static final String ESTIMATED_COST_KEY = "cost_usd";
    public static final String COLD_START_KEY = "cold_start";
    public static final String START_TIME_KEY = "start_time";
    public static final String END_TIME_KEY = "end_time";
    public static final String PROCESSING_THROUGHPUT_KEY = "processing_throughput";
    public static final String MEMORY_USED_MB_KEY = "memory_used_mb";

    public static final int IMAGE_URL_EXPIRATION_SECONDS = 3600;



    public static boolean saveImageToS3(
            final String bucketName,
            final String fileName,
            final String imageExtension, // Maybe we can default this to PNG?
            final BufferedImage image) {

        try {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, imageExtension, outputStream);
            final byte[] imageBytes = outputStream.toByteArray();

            // Save the rotated image back to S3
            final ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(imageBytes.length);
            metadata.setContentType("image/" + imageExtension);
            AmazonS3ClientBuilder
                    .defaultClient()
                    .putObject(bucketName, fileName, new ByteArrayInputStream(imageBytes), metadata);

        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }

    public static String validateRequestMap(final Map<String, Object> request, final String... keys) {
        final StringBuilder sb = new StringBuilder();
        for (final String key : keys) {
            if (!request.containsKey(key)) {
                sb.append(key).append(", ");
            }
        }
        if (sb.length() == 0) {
            return null;
        }
        return "Missing request parameters: " + sb.subSequence(0, sb.length() - 1).toString();
    }

    public static InputStream getImageFromS3AndRecordLatency(final String bucketName,
                                                             final String fileName,
                                                             final Inspector inspector) {
        // Fetch the image from S3
        final long s3StartTime = System.currentTimeMillis();
        final S3Object s3Object = AmazonS3ClientBuilder.defaultClient().getObject(bucketName, fileName);
        final InputStream objectData = s3Object.getObjectContent();

        inspector.addAttribute(NETWORK_LATENCY_KEY, System.currentTimeMillis() - s3StartTime);
        return objectData;
    }

    public static String getDownloadableImageURL(final String bucketName, final String fileName) {
        final Date expiration = new Date();
        final long expTimeMillis = expiration.getTime() + 1000 * IMAGE_URL_EXPIRATION_SECONDS;
        expiration.setTime(expTimeMillis);

        return AmazonS3ClientBuilder.defaultClient().generatePresignedUrl(bucketName, fileName, expiration).toString();
    }

    public static double estimateCost(final long runTime) {
        final double memorySizeGB = 0.512; // Lambda memory size in GB
        final double pricePerGBSecond = 0.00001667; // Pricing for Lambda
        return (runTime / 1000.0) * memorySizeGB * pricePerGBSecond;
    }




    @FunctionalInterface
    public interface ImageBatchFunction {
        HashMap<String, Object> process(
                final BufferedImage image,
                final HashMap<String, Object> request,
                final Context context);
    }


    @FunctionalInterface
    public interface ImageProcessFunction {
        HashMap<String, Object> process(
                final HashMap<String, Object> request,
                final Context context);
    }


}
