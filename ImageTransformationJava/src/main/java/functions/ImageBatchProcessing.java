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


    /***
     *  The function to handle batch requests.
     *
     *  @param request The Lambda Function input
     *  @param context The Lambda execution environment context object.
     *  @return A HashMap containing request output.
     */
    public static HashMap<String, Object> handleRequest(final HashMap<String, Object> request, final Context context) {
        Inspector inspector = new Inspector();

        final String validateMessage = Constants.validateRequestMap(request, BUCKET_KEY, FILE_NAME_KEY, OPERATIONS_KEY);
        if (validateMessage != null) {
            inspector = new Inspector();
            inspector.addAttribute(ERROR_KEY, validateMessage);
            return inspector.finish();
        }

        try {
            final String bucketName = (String) request.get(BUCKET_KEY);
            final String fileName = (String) request.get(FILE_NAME_KEY);
            final Object[][] operations = (Object[][]) request.get(OPERATIONS_KEY);
            final String outputFileName = "batch_" + fileName;


            final List<HashMap<String, Object>> operationsOutput = new ArrayList<>();


            BufferedImage image = ImageIO.read(Constants.getImageFromS3AndRecordLatency(bucketName, fileName, inspector));
            if (image == null) {
                throw new IllegalArgumentException("Failed to decode image data.");
            }

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

                System.out.println("Executing function " + i + ": " + operationName);
                final ImageBatchFunction operationFunction = FUNCTIONS.get(operationName);
                final Map<String, Object> responseObject = operationFunction.process(image, operationArgs, context);

                if (responseObject.containsKey(ERROR_KEY)) {
                    System.out.println("Pipeline error: Error executing function at index " + i);
                }

                image = responseObject.containsKey(IMAGE_FILE_KEY) ? (BufferedImage) responseObject.get(IMAGE_FILE_KEY) : image;
                final HashMap<String, Object> appendedOutput = new HashMap<>(responseObject);
                appendedOutput.remove(IMAGE_FILE_KEY);
                operationsOutput.add(appendedOutput);
            }


            final boolean successfulWriteToS3 = Constants.saveImageToS3(bucketName, outputFileName, "png", image);
            if (!successfulWriteToS3) {
                throw new RuntimeException("Could not write image to S3");
            }

            inspector.addAttribute("operation_outputs", operationsOutput);
            inspector.addAttribute(IMAGE_URL_KEY, getDownloadableImageURL(bucketName, outputFileName));
            inspector.addAttribute(IMAGE_URL_EXPIRES_IN, IMAGE_URL_EXPIRATION_SECONDS);


        } catch (final Exception e) {
            e.printStackTrace();
            inspector = new Inspector();
            inspector.addAttribute(ERROR_KEY, e.toString());
        }
        return inspector.finish();
    }


}
