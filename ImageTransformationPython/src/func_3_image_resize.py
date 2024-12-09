from utils_custom_types import AWSFunctionOutput, AWSContextObject, AWSRequestObject, ImageType, OptionalImage
from utils_constants import BUCKET_KEY, FILE_NAME_KEY, ERROR_KEY, IMAGE_FILE_KEY, IMAGE_URL_KEY, IMAGE_URL_EXPIRES_IN_KEY, \
    IMAGE_URL_EXPIRATION_SECONDS, SUCCESS_KEY
from utils_helpers import get_image_from_s3_and_record_time, validate_event, save_image_to_s3, get_downloadable_image_url

TARGET_WIDTH_KEY: str = 'target_width'
TARGET_HEIGHT_KEY: str = 'target_height'


def handle_request(event: AWSRequestObject,
                   context: AWSContextObject = None,
                   batch_image: OptionalImage = None) -> AWSFunctionOutput:
    output_dict: AWSFunctionOutput = {}

    is_batch: bool = batch_image is not None

    validate_message: str = validate_event(event, BUCKET_KEY, FILE_NAME_KEY, TARGET_WIDTH_KEY, TARGET_HEIGHT_KEY)
    if validate_message:
        return {ERROR_KEY: validate_message}

    try:
        bucket_name: str = str(event[BUCKET_KEY])
        file_name: str = str(event[FILE_NAME_KEY])
        target_width = int(event[TARGET_WIDTH_KEY])
        target_height = int(event[TARGET_HEIGHT_KEY])
        output_file_name: str = "resized_" + file_name

        if target_width <= 0 or target_height <= 0:
            return {ERROR_KEY: f"Target dimensions must be positive integers."}

        image: ImageType = batch_image if is_batch else get_image_from_s3_and_record_time(bucket_name, file_name,
                                                                                          output_dict)

        # Resize the image
        resized_image: ImageType = image.resize((target_width, target_height))

        if not is_batch:
            successful_write_to_s3: bool = save_image_to_s3(bucket_name, output_file_name, resized_image, "png")
            if not successful_write_to_s3:
                raise RuntimeError("Could not write image to S3.")

        output_dict[SUCCESS_KEY] = "Image resized successfully."
        output_dict["original_width"] = image.width
        output_dict["original_height"] = image.height
        output_dict[TARGET_WIDTH_KEY] = target_width
        output_dict[TARGET_HEIGHT_KEY] = target_height


        if is_batch:
            output_dict[IMAGE_FILE_KEY] = resized_image
        else:
            output_dict[IMAGE_URL_KEY] = get_downloadable_image_url(bucket_name, output_file_name)
            output_dict[IMAGE_URL_EXPIRES_IN_KEY] = IMAGE_URL_EXPIRATION_SECONDS

        return output_dict
    except Exception as e:
        return {ERROR_KEY: str(e)}
