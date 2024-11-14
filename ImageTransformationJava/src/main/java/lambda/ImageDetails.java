package lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import saaf.Inspector;

import javax.imageio.ImageIO;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Base64;
import java.util.HashMap;

public class ImageDetails implements RequestHandler<HashMap<String, Object>, HashMap<String, Object>> {

    /***
     *  Function #1: Image upload and validation
     *  Pass in a Base64 encoded image in a HashMap under key 'image_file'.
     *  Will return a HashMap following one of these schemas:
     *          <key>: <type> -> <description>
     *      Successful Response {
     *          'region': String -> I think when this is ran within lambda, this should be populated. Otherwise, return "NO_REGION_DATA".
     *          'height': int -> The height (in px i think) of the image.
     *          'width': int -> The width (in px i think) of the image.
     *          'mode': String -> The mode of the image.
     *          'has_transparency_data: int -> 1 if the image has transparency data, 0 otherwise.
     *      }
     *      Error Response {
     *          'error': String -> The error converted to a string form.
     *      }
     *
     *  @param request  A HashMap containing request data (must have the key, 'image_file', with value as the image in base64 encoding.).
     *  @param context
     *  @return A HashMap containing the response data.
     */
    public HashMap<String, Object> handleRequest(final HashMap<String, Object> request, final Context context) {

        // This could be replaced with a hashmap, especially if we don't need info from the inspector
        Inspector inspector = new Inspector();

        final String encodedImage = (String) request.get("image_file");

        try {
            final BufferedImage image = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(encodedImage)));

            inspector.addAttribute("region", System.getenv().getOrDefault("AWS_REGION", "NO_REGION_DATA"));
            inspector.addAttribute("width", image.getWidth());
            inspector.addAttribute("height", image.getHeight());
            inspector.addAttribute("mode", getColorType(image.getColorModel().getColorSpace().getType()));
            inspector.addAttribute("has_transparency_data", image.getColorModel().hasAlpha() ? 1 : 0);

        } catch (final Exception e) {
            e.printStackTrace();
            inspector = new Inspector();
            inspector.addAttribute("error", e.toString());
        }

        return inspector.finish();
    }

    private String getColorType(final int type) {
        switch (type) {
            case ColorSpace.TYPE_RGB:
                return "RGB";
            case ColorSpace.TYPE_GRAY:
                return "L";
            case ColorSpace.TYPE_CMYK:
                return "CMYK";
            case ColorSpace.TYPE_YCbCr:
                return "YCbCr";
            case ColorSpace.TYPE_CMY:
                return "CMY";
            case ColorSpace.TYPE_HLS:
                return "HLS";
            case ColorSpace.TYPE_HSV:
                return "HSV";
            case ColorSpace.TYPE_Lab:
                return "LAB";
            case ColorSpace.TYPE_Luv:
                return "Luv";
            case ColorSpace.TYPE_XYZ:
                return "XYZ";
            default:
                return "Unknown";
        }
    }

    public static void main(String[] args) {

        final String testImageName = "sample image.jpg";


        System.out.println("PWD: " + System.getProperty("user.dir") + "\n");
        final HashMap<String, Object> req = new HashMap<>();

        final File file = new File("../sample images/" + testImageName);
        try (FileInputStream fileInputStreamReader = new FileInputStream(file)) {
            final byte[] bytes = new byte[(int) file.length()];
            fileInputStreamReader.read(bytes);
            req.put("image_file", Base64.getEncoder().encodeToString(bytes));

            System.out.println(new ImageDetails().handleRequest(req, null).toString());

        } catch (final Exception e) {
            e.printStackTrace();
        }


    }
}
