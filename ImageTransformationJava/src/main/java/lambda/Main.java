package lambda;

import com.amazonaws.services.lambda.runtime.Context;
import functions.*;
import utils.Constants;
import utils.Constants.ImageProcessFunction;

import java.util.HashMap;

import static utils.Constants.*;

public class Main {

    private boolean coldStart = true;

    private HashMap<String, Object> handleCall(final HashMap<String, Object> input,
                                               final Context context,
                                               final ImageProcessFunction function) {

        final boolean localColdStart = this.coldStart;
        this.coldStart = false;


        final long processStartTime = System.currentTimeMillis();
        final HashMap<String, Object> functionOutput = function.process(input, context);
        final long processEndTime = System.currentTimeMillis();
        final long functionRunTime = processEndTime - processStartTime;


        functionOutput.put(START_TIME_KEY, processStartTime);
        functionOutput.put(END_TIME_KEY, processEndTime);
        functionOutput.put(FUNCTION_RUN_TIME_KEY, functionRunTime);
        functionOutput.put(ROUND_TRIP_TIME_KEY, functionRunTime + (long) functionOutput.get(IMAGE_ACCESS_LATENCY_KEY));
        functionOutput.put(COLD_START_KEY, localColdStart ? 1 : 0);
        functionOutput.put(ESTIMATED_COST_KEY, estimateCost(functionRunTime));

        functionOutput.put(LANGUAGE_KEY, "Java");
        functionOutput.put(VERSION_KEY, 0.5);
        functionOutput.put("region", System.getenv().getOrDefault("AWS_REGION", "NO_REGION_DATA"));


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
