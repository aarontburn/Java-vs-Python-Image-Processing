from utils_custom_types import AWSFunctionOutput, AWSContextObject, AWSRequestObject, ImageType, OptionalImage
from utils_constants import BUCKET_KEY, FILE_NAME_KEY, ERROR_KEY, IMAGE_FILE_KEY, IMAGE_URL_KEY, IMAGE_URL_EXPIRES_IN_KEY, \
    IMAGE_URL_EXPIRATION_SECONDS
from utils_helpers import get_image_from_s3_and_record_time, validate_event, save_image_to_s3, get_downloadable_image_url
from io import BytesIO
from typing import Any
from PIL import Image

SUPPORTED_FORMATS: set[str] = {"JPEG", "PNG", "BMP", "GIF", "TIFF"}


def handle_request(event: AWSRequestObject,
                   context: AWSContextObject = None,
                   batch_image: OptionalImage = None) -> AWSFunctionOutput:
    output_dict: AWSFunctionOutput = {}

    is_batch: bool = batch_image is not None

    validate_message: str = validate_event(event, BUCKET_KEY, FILE_NAME_KEY)
    if validate_message:
        return {ERROR_KEY: validate_message}

    try:
        bucket_name: str = str(event[BUCKET_KEY])
        file_name: str = str(event[FILE_NAME_KEY])
        output_format: str = str(event.get('output_format', 'JPEG')).upper()  # Default to JPEG
        compress_quality: int = int(
            event.get('compress_quality', 85))  # Default to 85% quality (for lossy formats like JPEG)
        preserve_metadata: bool = bool(event.get('preserve_metadata', True))  # Default to preserving metadata
        output_file_name: str = "transformed_" + file_name

        if output_format not in SUPPORTED_FORMATS:
            return {
                ERROR_KEY: f"Unsupported output format: {output_format}. Supported formats: {', '.join(SUPPORTED_FORMATS)}"}
        if not (1 <= compress_quality <= 100):
            return {ERROR_KEY: "Compression quality must be between 1 and 100."}

        image: ImageType = batch_image if is_batch else get_image_from_s3_and_record_time(bucket_name, file_name,
                                                                                          output_dict)
        # Convert image to the desired format
        output_buffer: BytesIO = BytesIO()
        save_kwargs: dict[str, Any] = {}

        # Handle metadata
        if not preserve_metadata:
            save_kwargs["exif"] = None

        # Handle compression for lossy formats
        if output_format == "JPEG":
            save_kwargs["quality"] = compress_quality

        try:
            image = image.convert("RGB") if output_format == "JPEG" else image
            image.save(output_buffer, format=output_format, **save_kwargs)
        except Exception as e:
            return {ERROR_KEY: f"Error finalizing image: {str(e)}"}

        image = Image.open(output_buffer)

        if not is_batch:
            successful_write_to_s3: bool = save_image_to_s3(bucket_name, output_file_name, image, "png")
            if not successful_write_to_s3:
                raise RuntimeError("Could not write image to S3.")

        output_dict["output_format"] = output_format
        output_dict["message"] = "Image finalized successfully."

        if is_batch:
            output_dict[IMAGE_FILE_KEY] = image
        else:
            output_dict[IMAGE_URL_KEY] = get_downloadable_image_url(bucket_name, output_file_name)
            output_dict[IMAGE_URL_EXPIRES_IN_KEY] = IMAGE_URL_EXPIRATION_SECONDS

        return output_dict

    except Exception as e:
        return {ERROR_KEY: f"Unexpected error: {str(e)}"}
