package lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import saaf.Inspector;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.*;
import java.util.*;
import java.util.List;


public class ImageBatchProcessing implements RequestHandler<HashMap<String, Object>, HashMap<String, Object>> {

    private static final String ERROR_KEY = "error";
    private static final String IMAGE_FILE_KEY = "image_file";
    private static final String OPERATIONS_KEY = "operations";

    private final Map<String, Constants.Function> FUNCTIONS = new HashMap<>();


    public static void main(final String[] args) {
        System.out.println("PWD: " + System.getProperty("user.dir") + "\n");

        final String testImageName = "sample image.jpg";
        final HashMap<String, Object> req = new HashMap<>();


        final File file = new File("../sample images/" + testImageName);
        try (FileInputStream fileInputStreamReader = new FileInputStream(file)) {
            final byte[] bytes = new byte[(int) file.length()];
            fileInputStreamReader.read(bytes);
            req.put(IMAGE_FILE_KEY, Base64.getEncoder().encodeToString(bytes));


            final Object[][] operations = new Object[][]{
                    new Object[]{"details", new HashMap<>()},
                    new Object[]{"brightness", Constants.hashmapFromArray(new Object[][]{
                            new Object[]{"brightness_delta", 50}
                    })},
                    new Object[]{"rotate", Constants.hashmapFromArray(new Object[][]{
                            new Object[]{"rotation_angle", 180}
                    })},
                    new Object[]{"resize", Constants.hashmapFromArray(new Object[][]{
                            new Object[]{"target_width", 250},
                            new Object[]{"target_height", 500}
                    })},
                    new Object[]{"grayscale", new HashMap<>()},
                    new Object[]{"details", new HashMap<>()},
                    new Object[]{"transform", Constants.hashmapFromArray(new Object[][]{
                            new Object[]{"output_format", "PNG"}
                    })},
            };
            req.put(OPERATIONS_KEY, operations);

            final HashMap<String, Object> responseBody = new ImageBatchProcessing().handleRequest(req, null);

            if (responseBody.containsKey(ERROR_KEY)) {
                System.out.println(responseBody);
            } else {
                System.out.println(responseBody.get("operation_outputs"));
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode((String) responseBody.get(IMAGE_FILE_KEY))));

                File outputFile = new File("test output.png");
                ImageIO.write(image, "png", outputFile);
                System.out.println("Image saved to: " + outputFile.getAbsolutePath());
            }


        } catch (final Exception e) {
            e.printStackTrace();
        }

    }


    public ImageBatchProcessing() {
        FUNCTIONS.put("details", this::imageDetails);
        FUNCTIONS.put("rotate", this::imageRotate);
        FUNCTIONS.put("resize", this::imageResize);
        FUNCTIONS.put("grayscale", this::imageGrayscale);
        FUNCTIONS.put("brightness", this::imageBrightness);
        FUNCTIONS.put("transform", this::imageTransform);
    }


    public HashMap<String, Object> handleRequest(final HashMap<String, Object> request, final Context context) {
        Inspector inspector = new Inspector();

        if (!request.containsKey(IMAGE_FILE_KEY)) {
            inspector = new Inspector();
            inspector.addAttribute(ERROR_KEY, "Missing key-value pair associated with " + IMAGE_FILE_KEY);
            return inspector.finish();
        }
        if (!request.containsKey(OPERATIONS_KEY)) {
            inspector = new Inspector();
            inspector.addAttribute(ERROR_KEY, "Missing key-value pair associated with " + OPERATIONS_KEY);
            return inspector.finish();
        }


        try {
            String img_bytes = (String) request.get(IMAGE_FILE_KEY);
            final Object[][] operations = (Object[][]) request.get(OPERATIONS_KEY);

            final List<HashMap<String, Object>> operationsOutput = new ArrayList<>();

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
                final Constants.Function operationFunction = FUNCTIONS.get(operationName);
                operationArgs.put(IMAGE_FILE_KEY, img_bytes);

                final Map<String, Object> responseObject = operationFunction.call(operationArgs, context);

                if (responseObject.containsKey(ERROR_KEY)) {
                    System.out.println("Pipeline error: Error executing function at index " + i);
                }

                img_bytes = responseObject.containsKey(IMAGE_FILE_KEY) ? (String) responseObject.get(IMAGE_FILE_KEY) : img_bytes;
                final HashMap<String, Object> appendedOutput = new HashMap<>(responseObject);
                appendedOutput.remove(IMAGE_FILE_KEY);
                operationsOutput.add(appendedOutput);
            }

            inspector.addAttribute(IMAGE_FILE_KEY, img_bytes);
            inspector.addAttribute("operation_outputs", operationsOutput);

        } catch (final Exception e) {
            e.printStackTrace();
            inspector = new Inspector();
            inspector.addAttribute(ERROR_KEY, e.toString());
        }
        return inspector.finish();
    }


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
    public HashMap<String, Object> imageDetails(final HashMap<String, Object> request, final Context context) {

        // This could be replaced with a hashmap, especially if we don't need info from the inspector
        Inspector inspector = new Inspector();

        final String encodedImage = (String) request.get(IMAGE_FILE_KEY);

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
            inspector.addAttribute(ERROR_KEY, e.toString());
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


    public HashMap<String, Object> imageRotate(final HashMap<String, Object> request, final Context context) {
        Inspector inspector = new Inspector();

        try {
            // Extract input parameters
            final String encodedImage = (String) request.get(IMAGE_FILE_KEY);
            final Integer rotationAngle = (Integer) request.get("rotation_angle");

            if (encodedImage == null || rotationAngle == null) {
                throw new IllegalArgumentException("Missing required parameters: 'image_file' or 'rotation_angle'");
            }

            // Decode the Base64-encoded image
            final BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(encodedImage)));
            if (originalImage == null) {

                throw new IllegalArgumentException("Failed to decode image data");
            }

            // Validate rotation angle
            if (!(rotationAngle == 90 || rotationAngle == 180 || rotationAngle == 270)) {
                throw new IllegalArgumentException("Invalid rotation angle. Only 90, 180, or 270 degrees are supported.");
            }

            // Perform rotation
            final BufferedImage rotatedImage = rotateImage(originalImage, rotationAngle);

            // Encode the rotated image back to Base64
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(rotatedImage, "png", outputStream);
            final String rotatedImageBase64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());


            // Populate response attributes
            inspector.addAttribute(IMAGE_FILE_KEY, rotatedImageBase64);
            inspector.addAttribute("original_width", originalImage.getWidth());
            inspector.addAttribute("original_height", originalImage.getHeight());
            inspector.addAttribute("rotated_width", rotatedImage.getWidth());
            inspector.addAttribute("rotated_height", rotatedImage.getHeight());
            inspector.addAttribute("rotation_angle", rotationAngle);

        } catch (final Exception e) {
            e.printStackTrace();
            inspector = new Inspector();
            inspector.addAttribute(ERROR_KEY, e.getMessage());
        }

        return inspector.finish();
    }


    private BufferedImage rotateImage(final BufferedImage image, final int rotationAngle) {
        final int width = image.getWidth();
        final int height = image.getHeight();

        // Create a new BufferedImage with appropriate dimensions and type
        final BufferedImage rotatedImage = new BufferedImage(
                (rotationAngle == 90 || rotationAngle == 270) ? height : width,
                (rotationAngle == 90 || rotationAngle == 270) ? width : height,
                BufferedImage.TYPE_INT_ARGB
        );

        final Graphics2D graphics = rotatedImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Perform rotation with proper translation
        if (rotationAngle == 90) {
            graphics.translate(height, 0);
        } else if (rotationAngle == 180) {
            graphics.translate(width, height);
        } else if (rotationAngle == 270) {
            graphics.translate(0, width);
        }

        graphics.rotate(Math.toRadians(rotationAngle));
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();

        return rotatedImage;
    }


    public HashMap<String, Object> imageResize(HashMap<String, Object> request, Context context) {
        Inspector inspector = new Inspector();

        try {
            // Extract inputs from the request
            final String base64Image = (String) request.get(IMAGE_FILE_KEY);
            final Object widthObj = request.get("target_width");
            final Object heightObj = request.get("target_height");

            // Validate inputs
            if (base64Image == null || widthObj == null || heightObj == null) {
                inspector.addAttribute("error", "Missing required inputs: image_file, target_width, or target_height.");
                return inspector.finish();
            }

            int targetWidth;
            int targetHeight;

            try {
                targetWidth = Integer.parseInt(widthObj.toString());
                targetHeight = Integer.parseInt(heightObj.toString());
                if (targetWidth <= 0 || targetHeight <= 0) {
                    throw new IllegalArgumentException("Target dimensions must be positive integers.");
                }
            } catch (NumberFormatException e) {
                inspector = new Inspector();
                inspector.addAttribute(ERROR_KEY, "Target width and height must be valid integers.");
                return inspector.finish();
            }

            // Decode the Base64 image
            final byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            final BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));

            if (originalImage == null) {
                inspector = new Inspector();
                inspector.addAttribute(ERROR_KEY, "Invalid image format or decoding failed.");
                return inspector.finish();
            }

            // Resize the image
            final Image resizedImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
            final BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight,
                    originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType());
            outputImage.getGraphics().drawImage(resizedImage, 0, 0, null);

            // Convert the resized image back to Base64
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(outputImage, "png", outputStream);  // Save as PNG
            final String resizedImageBase64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());

            // Add resized image details to the response
            inspector.addAttribute(IMAGE_FILE_KEY, resizedImageBase64);
            inspector.addAttribute("message", "Image resized successfully.");
            inspector.addAttribute("target_width", targetWidth);
            inspector.addAttribute("target_height", targetHeight);

        } catch (Exception e) {
            e.printStackTrace();
            inspector = new Inspector();
            inspector.addAttribute(ERROR_KEY, e.getMessage());
        }

        return inspector.finish();
    }


    public HashMap<String, Object> imageGrayscale(final HashMap<String, Object> request, final Context context) {
        Inspector inspector = new Inspector();

        try {
            // Extract input parameter
            final String encodedImage = (String) request.get(IMAGE_FILE_KEY);

            if (encodedImage == null) {
                throw new IllegalArgumentException("Missing required parameter: " + IMAGE_FILE_KEY);
            }

            // Decode the Base64-encoded image
            final BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(encodedImage)));
            if (originalImage == null) {
                throw new IllegalArgumentException("Failed to decode image data");
            }

            // Convert the image to grayscale
            final BufferedImage grayscaleImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            grayscaleImage.getGraphics().drawImage(originalImage, 0, 0, null);


            // Encode the grayscale image back to Base64
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(grayscaleImage, "png", outputStream);
            final String grayscaleImageBase64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());


            // Populate response attributes
            inspector.addAttribute("original_width", originalImage.getWidth());
            inspector.addAttribute("original_height", originalImage.getHeight());
            inspector.addAttribute(IMAGE_FILE_KEY, grayscaleImageBase64);

        } catch (final Exception e) {
            e.printStackTrace();
            inspector = new Inspector();
            inspector.addAttribute(ERROR_KEY, e.getMessage());
        }

        return inspector.finish();
    }

    private static final int MIN_BRIGHTNESS = 1;  // Minimum input value for brightness
    private static final int MAX_BRIGHTNESS = 100; // Maximum input value for brightness


    public HashMap<String, Object> imageBrightness(final HashMap<String, Object> request, final Context context) {
        Inspector inspector = new Inspector();

        try {
            // Extract input parameters
            final String encodedImage = (String) request.get(IMAGE_FILE_KEY);
            final Integer brightnessDelta = (Integer) request.get("brightness_delta");

            if (encodedImage == null || brightnessDelta == null) {
                throw new IllegalArgumentException("Missing required parameters: 'image_file' or 'brightness_delta'");
            }

            // Validate brightness_delta
            if (brightnessDelta < MIN_BRIGHTNESS || brightnessDelta > MAX_BRIGHTNESS) {
                throw new IllegalArgumentException(
                        String.format("Invalid brightness_delta. Must be between %d and %d.", MIN_BRIGHTNESS, MAX_BRIGHTNESS));
            }

            // Map brightness_delta (1–100) to RescaleOp factor (0.0–2.0)
            final float brightnessFactor = brightnessDelta / 50.0f;


            // Decode the Base64-encoded image
            final BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(encodedImage)));
            if (originalImage == null) {
                throw new IllegalArgumentException("Failed to decode image data");
            }

            // Adjust brightness
            final BufferedImage brightenedImage = adjustBrightness(originalImage, brightnessFactor);

            // Encode the brightened image back to Base64
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(brightenedImage, "png", outputStream);
            final String brightenedImageBase64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());

            // Populate response attributes
            inspector.addAttribute("original_width", originalImage.getWidth());
            inspector.addAttribute("original_height", originalImage.getHeight());
            inspector.addAttribute("brightness_delta", brightnessDelta);
            inspector.addAttribute("brightness_factor", brightnessFactor);
            inspector.addAttribute(IMAGE_FILE_KEY, brightenedImageBase64);

        } catch (Exception e) {
            e.printStackTrace();
            inspector = new Inspector();
            inspector.addAttribute(ERROR_KEY, e.getMessage());
        }

        return inspector.finish();
    }


    /**
     * Adjusts the brightness of a BufferedImage.
     *
     * @param image            The original image to modify.
     * @param brightnessFactor The factor to adjust the brightness (1.0 = original, < 1.0 = darker, > 1.0 = brighter).
     * @return The modified image with adjusted brightness.
     */
    private BufferedImage adjustBrightness(final BufferedImage image, final float brightnessFactor) {
        final RescaleOp rescaleOp = new RescaleOp(brightnessFactor, 0, null);
        final BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        final Graphics2D g = result.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        rescaleOp.filter(result, result);
        return result;
    }


    public HashMap<String, Object> imageTransform(final HashMap<String, Object> request, final Context context) {
        Inspector inspector = new Inspector();

        try {
            // Extract input parameter
            final String encodedImage = (String) request.get(IMAGE_FILE_KEY);
            final String targetFormat = (String) request.getOrDefault("target_format", "jpeg");

            if (encodedImage == null) {
                throw new IllegalArgumentException("Missing required parameter: 'image_file'");
            }

            // Decode the Base64-encoded image
            final BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(encodedImage)));
            if (originalImage == null) {
                throw new IllegalArgumentException("Failed to decode image data");
            }

            // Transform image to the target format
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            if (!ImageIO.write(originalImage, targetFormat, outputStream)) {
                throw new IllegalArgumentException("Unsupported target format: " + targetFormat);
            }

            final String transformedImageBase64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());

            // Populate response attributes
            inspector.addAttribute("original_width", originalImage.getWidth());
            inspector.addAttribute("original_height", originalImage.getHeight());
            inspector.addAttribute(IMAGE_FILE_KEY, transformedImageBase64);
            inspector.addAttribute("target_format", targetFormat);

        } catch (Exception e) {
            e.printStackTrace();
            inspector = new Inspector();
            inspector.addAttribute(ERROR_KEY, e.getMessage());
        }

        return inspector.finish();
    }


}
