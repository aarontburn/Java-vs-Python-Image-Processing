package functions;

import com.amazonaws.services.lambda.runtime.Context;
import utils.Constants;
import utils.Constants.ImageBatchFunction;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        final HashMap<String, Object> inspector = new HashMap<>();

        // Validate input
        final String validateMessage = Constants.validateRequestMap(request, BUCKET_KEY, FILE_NAME_KEY, OPERATIONS_KEY);
        if (validateMessage != null) {
            return Constants.getErrorObject(validateMessage);
        }

        try {
            final String bucketName = (String) request.get(BUCKET_KEY);
            final String fileName = (String) request.get(FILE_NAME_KEY);
            final ArrayList<ArrayList<Object>> operations = (ArrayList<ArrayList<Object>>) request.get(OPERATIONS_KEY);
            String outputFileName = "batch_" + fileName;
            final List<HashMap<String, Object>> operationsOutput = new ArrayList<>();

            // Fetch the initial image from S3
            BufferedImage image = Constants.getImageFromS3AndRecordLatency(bucketName, fileName, inspector);
            if (image == null) {
                return Constants.getErrorObject("Could not access image from S3.");
            }

            // Process all operations
            for (int i = 0; i < operations.size(); i++) {
                final String operationName = (String) safeListAccess(operations.get(i), 0, "");
                final HashMap<String, Object> operationArgs = (HashMap<String, Object>) safeListAccess(operations.get(i), 1, new HashMap<String, Object>());

                if (!FUNCTIONS.containsKey(operationName)) {
                    System.out.println("Pipeline error: Invalid operation name at index " + i + ": " + operationName);
                    continue;
                }

                // Add required params to the operation arguments
                operationArgs.put(BUCKET_KEY, bucketName);
                operationArgs.put(FILE_NAME_KEY, fileName);

                // Execute the operation
                final ImageBatchFunction operationFunction = FUNCTIONS.get(operationName);
                final Map<String, Object> responseObject = operationFunction.process(image, operationArgs, context);


                if (responseObject.containsKey(ERROR_KEY)) {
                    System.out.println("Pipeline error: Error executing function at index " + i);
                } else {
                    if (operationName.equals("transform")) {
                        outputFileName = "batch_" + fileName.split("\\.")[0] + "." + ((String) operationArgs.get("target_format")).toLowerCase();
                    }
                }

                image = responseObject.containsKey(IMAGE_FILE_KEY) ? (BufferedImage) responseObject.get(IMAGE_FILE_KEY) : image;
                final HashMap<String, Object> appendedOutput = new HashMap<>(responseObject);
                appendedOutput.remove(IMAGE_FILE_KEY);
                operationsOutput.add(appendedOutput);

            }

            // Save the final processed image to S3
            final boolean successfulWriteToS3 = Constants.saveImageToS3(bucketName, outputFileName, Constants.getFileExtension(outputFileName), image);
            if (!successfulWriteToS3) {
                return Constants.getErrorObject("Failed to save image to S3");
            }

            inspector.put(SUCCESS_KEY, "Successfully processed image.");
            inspector.put("batch_operations_count", operations.size());
            inspector.put("operation_outputs", operationsOutput);


            if ((boolean) request.get(GET_DOWNLOAD_KEY)) {
                inspector.put(IMAGE_URL_KEY, Constants.getDownloadableImageURL(bucketName, outputFileName));
                inspector.put(IMAGE_URL_EXPIRES_IN, IMAGE_URL_EXPIRATION_SECONDS);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Constants.getErrorObject(e.toString());
        }

        return inspector;
    }

    private static Object safeListAccess(final ArrayList<Object> arrayList, final int index, final Object fallback) {
        try {
            return arrayList.get(index);
        } catch (final Exception ignored) {
            return fallback;
        }
    }


}

