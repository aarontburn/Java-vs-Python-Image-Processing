package lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import saaf.Inspector;

import java.util.HashMap;

/**
 * uwt.lambda_test::handleRequest
 *
 * @author Wes Lloyd
 * @author Robert Cordingly
 */
public class ImageDetails implements RequestHandler<HashMap<String, Object>, HashMap<String, Object>> {

    /**
     * Lambda Function Handler
     * 
     * @param request Hashmap containing request JSON attributes.
     * @param context 
     * @return HashMap that Lambda will automatically convert into JSON.
     */
    public HashMap<String, Object> handleRequest(final HashMap<String, Object> request, final Context context) {
        
        final Inspector inspector = new Inspector();

        final String encodedImage = (String) request.get("image_file");


        return inspector.finish();
    }
    
    public static void main (String[] args) {
        final ImageDetails hm = new ImageDetails();
        
        final HashMap<String, Object> req = new HashMap<>();
        final String encodedImage = "placeholder";
        req.put("image_file", encodedImage);

        final HashMap<String, Object> resp = hm.handleRequest(req, null);
        
        // Print out function result
        System.out.println("function result:" + resp.toString());
        
        
        
    }
}
