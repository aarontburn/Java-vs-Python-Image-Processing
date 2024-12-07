package lambda;

import com.amazonaws.services.lambda.runtime.Context;
import functions.*;
import saaf.Inspector;
import utils.Constants.ImageProcessFunction;

import java.util.HashMap;

public class Main {

    private HashMap<String, Object> handleCall(final HashMap<String, Object> input,
                                               final Context context,
                                               final ImageProcessFunction function) {

        // Record function runtime
        final long roundTripStart = System.currentTimeMillis();
        final HashMap<String, Object> functionOutput = function.process(input, context);

        // Use Inspector for metrics collection
        Inspector inspector = new Inspector();
        inspector.inspectMetrics(false, roundTripStart);

        // Merge Inspector metrics into the function output
        functionOutput.putAll(inspector.finish());

        return functionOutput;
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
