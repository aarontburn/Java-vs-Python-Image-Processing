package utils;

import java.util.Set;


/***
 *  TCSS 462 Image Transformation
 *  Group 7
 *
 *  Holds helper functions to validate input/output files.
 */
public class FileValidator {

    /**
     * All allowed file extensions.
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpeg", "jpg");

    /**
     * Validate the file type for both input and output files.
     *
     * @param fileName The name of the file.
     * @return True if the file type is valid, false otherwise.
     */
    public static boolean isValidFileType(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        String fileExtension = getFileExtension(fileName);
        return ALLOWED_EXTENSIONS.contains(fileExtension.toLowerCase());
    }


    /**
     * Extract the file extension from the file name
     *
     * @param fileName The name of the file.
     * @return The file extension, or an empty string if there is no extension.
     */
    public static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return ""; // No extension
        }
        return fileName.substring(lastDotIndex + 1);
    }


    /**
     * Validate the output file type explicitly.
     *
     * @param fileName The name of the file.
     * @return True if the output file is valid, false otherwise.
     */
    public static boolean isValidOutputFile(String fileName) {
        if (!isValidFileType(fileName)) {
            System.err.println("Invalid output file type: " + getFileExtension(fileName) +
                    ". Only JPEG, JPG, and PNG are allowed.");
            return false;
        }
        return true;
    }
}
