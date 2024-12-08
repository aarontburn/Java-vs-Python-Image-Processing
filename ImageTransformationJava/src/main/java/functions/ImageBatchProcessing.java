package functions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import saaf.Inspector;
import utils.Constants;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.*;
import java.util.*;
import java.util.List;

import static utils.Constants.*;


public class ImageBatchProcessing {

    private static final String OPERATIONS_KEY = "operations";

    private static final Map<String, ImageBatchFunction> FUNCTIONS = new HashMap<>();

    static {
        FUNCTIONS.put("details", F1ImageDetails::imageDetails);
        FUNCTIONS.put("rotate", F2ImageRotation::imageRotate);
        FUNCTIONS.put("resize", F3ImageResize::imageResize);
        FUNCTIONS.put("grayscale", F4ImageGrayscale::imageGrayscale);
        FUNCTIONS.put("brightness", F5ImageBrightness::imageBrightness);
        FUNCTIONS.put("transform", F6ImageTransform::imageTransform);
    }

    public static HashMap<String, Object> handleRequest(final HashMap<String, Object> request, final Context context) {
        Inspector inspector = new Inspector();

        // Validate input
        final String validateMessage = Constants.validateRequestMap(request, BUCKET_KEY, FILE_NAME_KEY, OPERATIONS_KEY);
        if (validateMessage != null) {
            inspector.addAttribute(ERROR_KEY, validateMessage);
            return inspector.finish();
        }

        try {
            final String bucketName = (String) request.get(BUCKET_KEY);
            final String fileName = (String) request.get(FILE_NAME_KEY);
            final Object[][] operations = (Object[][]) request.get(OPERATIONS_KEY);
            final String outputFileName = "batch_" + fileName;

            // Track start time and memory usage
            long batchStartTime = System.currentTimeMillis();
            inspector.inspectMemory(); // Record initial memory usage
            long totalNetworkLatency = 0;

            // Fetch the initial image from S3
            BufferedImage image = ImageIO.read(Constants.getImageFromS3AndRecordLatency(bucketName, fileName, inspector));
            if (image == null) {
                throw new IllegalArgumentException("Failed to decode image data.");
            }

            // Process all operations
            for (int i = 0; i < operations.length; i++) {
                final String operationName = (String) operations[i][0];
                final HashMap<String, Object> operationArgs = (HashMap<String, Object>) operations[i][1];

                if (!FUNCTIONS.containsKey(operationName)) {
                    System.out.println("Pipeline error: Invalid operation name at index " + i + ": " + operationName);
                    continue;
                }
                if (operationArgs == null) {
                    System.out.println("Pipeline error: Error retrieving input arguments at index " + i);
                    continue;
                }

                // Execute the operation
                final ImageBatchFunction operationFunction = FUNCTIONS.get(operationName);
                final Map<String, Object> responseObject = operationFunction.process(image, operationArgs, context);

                // Update the image for the next operation
                image = responseObject.containsKey(IMAGE_FILE_KEY) ? (BufferedImage) responseObject.get(IMAGE_FILE_KEY) : image;

                // Track network latency from individual operations (if applicable)
                totalNetworkLatency += (long) responseObject.getOrDefault("s3_latency", 0);
            }

            // Save the final processed image to S3
            boolean successfulWriteToS3 = Constants.saveImageToS3(bucketName, outputFileName, "png", image);
            if (!successfulWriteToS3) {
                throw new RuntimeException("Failed to save processed image to S3.");
            }

            // Calculate batch-level metrics
            long batchEndTime = System.currentTimeMillis();
            long batchRuntime = batchEndTime - batchStartTime;
            double batchThroughput = operations.length / (batchRuntime / 1000.0);

            // Add aggregated metrics to the inspector
            inspector.addAttribute("batch_runtime_ms", batchRuntime);
            inspector.addAttribute("batch_operations_count", operations.length);
            inspector.addAttribute("batch_network_latency_ms", totalNetworkLatency);
            inspector.addAttribute("batch_throughput_ops_per_sec", batchThroughput);
            inspector.addAttribute("batch_cost_usd", Constants.estimateCost(batchRuntime));
            inspector.inspectMemory(); // Record final memory usage

            // Add final processed image URL
            inspector.addAttribute(IMAGE_URL_KEY, Constants.getDownloadableImageURL(bucketName, outputFileName));
            inspector.addAttribute(IMAGE_URL_EXPIRES_IN, IMAGE_URL_EXPIRATION_SECONDS);

        } catch (Exception e) {
            // Capture and log any errors during processing
            e.printStackTrace();
            inspector.addAttribute(ERROR_KEY, e.toString());
        }

        // Return only the aggregated metrics for the batch
        return inspector.finish();
    }
}

