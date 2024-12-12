package lambda;

import com.amazonaws.services.lambda.runtime.Context;
import functions.F1ImageDetails;
import functions.F2ImageRotation;
import functions.F3ImageResize;
import functions.F4ImageGrayscale;
import functions.F5ImageBrightness;
import functions.F6ImageTransform;
import functions.ImageBatchProcessing;
import saaf.Inspector;
import utils.Constants;
import utils.Constants.ImageProcessFunction;
import utils.FileValidator;

import java.util.HashMap;

/***
 *  TCSS 462 Image Transformation
 *  Group 7
 *
 *  Main entry point.
 */
public class Main {

    /***
     *  Handles the request.
     *
     *  @param request  The request parameters.
     *  @param context  AWS Lambda context.
     *  @param function The function to execute.
     *  @return         The function output alongside metrics.
     */
    private HashMap<String, Object> handleCall(final HashMap<String, Object> request,
                                               final Context context,
                                               final ImageProcessFunction function) {


        // Validate file type
        final String fileName = (String) request.get(Constants.FILE_NAME_KEY);
        if (!FileValidator.isValidFileType(fileName)) {
            return Constants.getErrorObject("Unsupported file format. Only JPEG, JPG and PNG are allowed.");
        }

        // To return only metrics, add "return_only_metrics": true to request body
        final boolean returnOnlyMetrics = (boolean) request.getOrDefault(Constants.ONLY_METRICS_KEY, false);

        // To get a download URL, add "get_download": true to request body. Defaults to false.
        final boolean getDownloadURL = (boolean) request.getOrDefault(Constants.GET_DOWNLOAD_KEY, false);
        request.put(Constants.GET_DOWNLOAD_KEY, getDownloadURL);

        // Record function start time
        final long roundTripStart = System.currentTimeMillis();

        // Use Inspector for metrics collection
        final Inspector inspector = new Inspector(returnOnlyMetrics);

        // Execute function
        final HashMap<String, Object> functionOutput = function.process(request, context);

        // Move network latency to top-level inspector and remove from function output
        inspector.addAttribute(Constants.NETWORK_LATENCY_KEY, functionOutput.get(Constants.NETWORK_LATENCY_KEY));
        functionOutput.remove(Constants.NETWORK_LATENCY_KEY);

        // Append function output to inspector
        inspector.addAttribute("function_output", functionOutput);

        // Inspect metrics
        inspector.inspectMetrics(roundTripStart);

        return inspector.finish();
    }

    /**
     * AWS Lambda entry point for Function 1.
     */
    public HashMap<String, Object> imageDetails(final HashMap<String, Object> request, final Context context) {
        return handleCall(request, context, F1ImageDetails::handleRequest);
    }

    /**
     * AWS Lambda entry point for Function 2.
     */
    public HashMap<String, Object> imageRotate(final HashMap<String, Object> request, final Context context) {
        return handleCall(request, context, F2ImageRotation::handleRequest);
    }

    /**
     * AWS Lambda entry point for Function 3.
     */
    public HashMap<String, Object> imageResize(final HashMap<String, Object> request, final Context context) {
        return handleCall(request, context, F3ImageResize::handleRequest);
    }

    /**
     * AWS Lambda entry point for Function 4.
     */
    public HashMap<String, Object> imageGrayscale(final HashMap<String, Object> request, final Context context) {
        return handleCall(request, context, F4ImageGrayscale::handleRequest);
    }

    /**
     * AWS Lambda entry point for Function 5.
     */
    public HashMap<String, Object> imageBrightness(final HashMap<String, Object> request, final Context context) {
        return handleCall(request, context, F5ImageBrightness::handleRequest);
    }

    /**
     * AWS Lambda entry point for Function 6.
     */
    public HashMap<String, Object> imageTransform(final HashMap<String, Object> request, final Context context) {
        return handleCall(request, context, F6ImageTransform::handleRequest);
    }

    /**
     * AWS Lambda entry point for the batch pipeline.
     */
    public HashMap<String, Object> imageBatch(final HashMap<String, Object> request, final Context context) {
        return handleCall(request, context, ImageBatchProcessing::handleRequest);
    }
}
