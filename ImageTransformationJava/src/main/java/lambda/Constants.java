package lambda;

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
import java.util.HashMap;
import java.util.Map;

public class Constants {

    public static final String IMAGE_FILE_KEY = "image_file";
    public static final String ERROR_KEY = "error";
    public static final String BUCKET_KEY = "bucketname";
    public static final String FILE_NAME_KEY = "filename";


    public static HashMap<String, Object> hashmapFromArray(final Object[][] inputArr) {
        final HashMap<String, Object> output = new HashMap<>();
        for (final Object[] arr : inputArr) {
            output.put((String) arr[0], arr[1]);
        }
        return output;
    }

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

        inspector.addAttribute("network_latency_ms", System.currentTimeMillis() - s3StartTime);
        return objectData;
    }

    public static double estimateCost(final long runTime) {
        double memorySizeGB = 0.512; // Lambda memory size in GB
        double pricePerGBSecond = 0.00001667; // Pricing for Lambda
        return (runTime / 1000.0) * memorySizeGB * pricePerGBSecond;
    }

    public static String getColorType(final int type) {
        switch (type) {
            case ColorSpace.TYPE_RGB:
                return "RGB";
            case ColorSpace.TYPE_GRAY:
                return "L";
            case ColorSpace.TYPE_CMYK:
                return "CMYK";
            case ColorSpace.TYPE_YCbCr:
                return "YCbCr";
            case ColorSpace.TYPE_CMY:
                return "CMY";
            case ColorSpace.TYPE_HLS:
                return "HLS";
            case ColorSpace.TYPE_HSV:
                return "HSV";
            case ColorSpace.TYPE_Lab:
                return "LAB";
            case ColorSpace.TYPE_Luv:
                return "Luv";
            case ColorSpace.TYPE_XYZ:
                return "XYZ";
            default:
                return "Unknown";
        }
    }


    @FunctionalInterface
    public interface ImageProcessFunction {
        Map<String, Object> Process(
                final BufferedImage image,
                final HashMap<String, Object> request,
                final Context context);
    }


}
