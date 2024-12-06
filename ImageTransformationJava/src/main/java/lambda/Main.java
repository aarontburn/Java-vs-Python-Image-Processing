package lambda;

import com.amazonaws.services.lambda.runtime.Context;
import functions.*;

import java.util.HashMap;

public class Main {


    public HashMap<String, Object> imageDetails(final HashMap<String, Object> input, final Context context) {
        return F1ImageDetails.handleRequest(input, context);
    }
    public HashMap<String, Object> imageRotate(final HashMap<String, Object> input, final Context context) {
        return F2ImageRotation.handleRequest(input, context);
    }
    public HashMap<String, Object> imageResize(final HashMap<String, Object> input, final Context context) {
        return F3ImageResize.handleRequest(input, context);
    }
    public HashMap<String, Object> imageGrayscale(final HashMap<String, Object> input, final Context context) {
        return F4ImageGrayscale.handleRequest(input, context);
    }
    public HashMap<String, Object> imageBrightness(final HashMap<String, Object> input, final Context context) {
        return F5ImageBrightness.handleRequest(input, context);
    }
    public HashMap<String, Object> imageTransform(final HashMap<String, Object> input, final Context context) {
        return F6ImageTransform.handleRequest(input, context);
    }
//    public HashMap<String, Object> imageBatch(final HashMap<String, Object> input, final Context context) {
//        return F1ImageDetails.handleRequest(input, context);
//    }

    private HashMap<String, Object> handleCall(final InputProcessFunction function, final HashMap<String, Object> input, final Context context) {
        return null;
    }
}
