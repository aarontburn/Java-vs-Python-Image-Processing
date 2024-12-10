from utils_custom_types import AWSFunctionOutput, AWSContextObject, AWSRequestObject, ImageType, OptionalImage
from utils_constants import BUCKET_KEY, FILE_NAME_KEY, ERROR_KEY, GET_DOWNLOAD_KEY, IMAGE_FILE_KEY, IMAGE_URL_KEY, IMAGE_URL_EXPIRES_IN_KEY, \
    IMAGE_URL_EXPIRATION_SECONDS, SUCCESS_KEY
from utils_helpers import add_image_url_to_dict, get_file_extension, get_image_from_s3_and_record_time, validate_event, save_image_to_s3, get_downloadable_image_url
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
        
        target_format: str = str(
            event.get('target_format', 'JPEG')).upper()  # Default to JPEG
        
        output_file_name: str = "transformed_" + \
            file_name.split(".")[0] + "." + target_format.lower()

        if target_format not in SUPPORTED_FORMATS:
            return {
                ERROR_KEY: f"Unsupported output format: {target_format}. Supported formats: {', '.join(SUPPORTED_FORMATS)}"}

        image: ImageType = batch_image \
            if is_batch \
            else get_image_from_s3_and_record_time(bucket_name, file_name, output_dict)

        if image is None:
            return {ERROR_KEY: "Could not access image from S3."}

        # Convert image to the desired format
        output_buffer: BytesIO = BytesIO()

        try:
            image = image.convert("RGB") if target_format == "JPEG" else image
            image.save(output_buffer, format=target_format)
        except Exception as e:
            return {ERROR_KEY: f"Error finalizing image: {str(e)}"}

        transformed_image: ImageType = Image.open(output_buffer)

        if not is_batch:
            successful_write_to_s3: bool = \
                save_image_to_s3(bucket_name,
                                 output_file_name,
                                 transformed_image,
                                 get_file_extension(output_file_name))

            if not successful_write_to_s3:
                return {ERROR_KEY: "Could not write image to S3."}
            
            if event[GET_DOWNLOAD_KEY]:
                add_image_url_to_dict(output_dict, 
                                      bucket_name, 
                                      output_file_name)
        else:
            output_dict[IMAGE_FILE_KEY] = transformed_image

        output_dict[SUCCESS_KEY] = "Successfully transformed image."
        output_dict["target_format"] = target_format

        return output_dict

    except Exception as e:
        return {ERROR_KEY: f"Unexpected error: {str(e)}"}
