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

    public HashMap<String, Object> handleRequest(final HashMap<String, Object> request, final Context context) {

        // This could be replaced with a hashmap, especially if we don't need info from the inspector
        Inspector inspector = new Inspector();

        final String encodedImage = (String) request.get("image_file");

        final byte[] imageByte = Base64.getDecoder().decode(encodedImage);

        final ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);

        try {
            final BufferedImage image = ImageIO.read(bis);

            final ColorSpace colorSpace = image.getColorModel().getColorSpace();

            inspector.addAttribute("region", System.getenv().getOrDefault("AWS_REGION", "NO_REGION_DATA"));
            inspector.addAttribute("width", image.getWidth());
            inspector.addAttribute("height", image.getHeight());
            inspector.addAttribute("mode", getColorType(colorSpace.getType()));
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
                return "Grayscale";
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
                return "Lab";
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
