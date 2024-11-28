package lambda;

import com.amazonaws.services.lambda.runtime.Context;

import java.util.HashMap;
import java.util.Map;

public class Constants {

    public static final String IMAGE_FILE_KEY = "image_file";
    public static final String ERROR_KEY = "error";


    public static HashMap<String, Object> hashmapFromArray(final Object[][] inputArr) {
        final HashMap<String, Object> output = new HashMap<>();
        for (final Object[] arr: inputArr) {
            output.put((String) arr[0], arr[1]);
        }
        return output;
    }


    public interface Function {
        Map<String, Object> call(final HashMap<String, Object> arguments, final Context context);

    }


}
