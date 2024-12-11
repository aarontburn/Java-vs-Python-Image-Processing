package utils;

import java.util.Set;

public class FileValidator {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpeg", "jpg");

    // Validate the file type for both input and output files
    public static boolean isValidFileType(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        String fileExtension = getFileExtension(fileName);
        return ALLOWED_EXTENSIONS.contains(fileExtension.toLowerCase());
    }

    // Extract the file extension from the file name
    public static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return ""; // No extension
        }
        return fileName.substring(lastDotIndex + 1);
    }

    // Validate the output file type explicitly
    public static boolean isValidOutputFile(String fileName) {
        if (!isValidFileType(fileName)) {
            System.err.println("Invalid output file type: " + getFileExtension(fileName) +
                               ". Only JPEG, JPG, and PNG are allowed.");
            return false;
        }
        return true;
    }
}
