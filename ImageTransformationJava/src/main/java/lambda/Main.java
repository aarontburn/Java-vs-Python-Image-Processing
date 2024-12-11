package lambda;

import com.amazonaws.services.lambda.runtime.Context;
import functions.*;
import saaf.Inspector;
import utils.Constants;
import utils.Constants.ImageProcessFunction;
import utils.FileValidator;

import java.util.HashMap;

public class Main {




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


    public HashMap<String, Object> imageDetails(final HashMap<String, Object> input, final Context context) {
        return handleCall(input, context, F1ImageDetails::handleRequest);
    }

    public HashMap<String, Object> imageRotate(final HashMap<String, Object> input, final Context context) {
        return handleCall(input, context, F2ImageRotation::handleRequest);
    }

    public HashMap<String, Object> imageResize(final HashMap<String, Object> input, final Context context) {
        return handleCall(input, context, F3ImageResize::handleRequest);
    }

    public HashMap<String, Object> imageGrayscale(final HashMap<String, Object> input, final Context context) {
        return handleCall(input, context, F4ImageGrayscale::handleRequest);
    }

    public HashMap<String, Object> imageBrightness(final HashMap<String, Object> input, final Context context) {
        return handleCall(input, context, F5ImageBrightness::handleRequest);
    }

    public HashMap<String, Object> imageTransform(final HashMap<String, Object> input, final Context context) {
        return handleCall(input, context, F6ImageTransform::handleRequest);
    }

    public HashMap<String, Object> imageBatch(final HashMap<String, Object> input, final Context context) {
        return handleCall(input, context, ImageBatchProcessing::handleRequest);
    }
}
