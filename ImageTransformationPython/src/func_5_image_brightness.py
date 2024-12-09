from utils_custom_types import AWSFunctionOutput, AWSContextObject, AWSRequestObject, ImageType, OptionalImage
from utils_constants import BUCKET_KEY, FILE_NAME_KEY, ERROR_KEY, IMAGE_FILE_KEY, IMAGE_URL_KEY, IMAGE_URL_EXPIRES_IN_KEY, \
    IMAGE_URL_EXPIRATION_SECONDS, SUCCESS_KEY
from utils_helpers import get_image_from_s3_and_record_time, validate_event, save_image_to_s3, get_downloadable_image_url
from PIL import ImageEnhance

BRIGHTNESS_KEY: str = 'brightness_delta'
BRIGHTNESS_BOUNDS: tuple[int, int] = (0, 100)  # I found that 100 is the maximum value before errors happen.


def handle_request(event: AWSRequestObject,
                   context: AWSContextObject = None,
                   batch_image: OptionalImage = None) -> AWSFunctionOutput:
    """
    Function #5: Image brightness modification
    """

    output_dict: AWSFunctionOutput = {}

    is_batch: bool = batch_image is not None

    validate_message: str = validate_event(event, BUCKET_KEY, FILE_NAME_KEY, BRIGHTNESS_KEY)
    if validate_message:
        return {ERROR_KEY: validate_message}

    try:
        brightness_delta = float(event[BRIGHTNESS_KEY])
        if brightness_delta < BRIGHTNESS_BOUNDS[0] or brightness_delta > BRIGHTNESS_BOUNDS[1]:
            return {ERROR_KEY: f"'{BRIGHTNESS_KEY}' is out-of-bounds \
                    ({BRIGHTNESS_BOUNDS[0]}-{BRIGHTNESS_BOUNDS[1]}): {brightness_delta}"}

    except Exception:
        return {ERROR_KEY: f"'{BRIGHTNESS_KEY}' is not parsable as a float."}

    # Modify and return image.
    try:
        bucket_name: str = str(event[BUCKET_KEY])
        file_name: str = str(event[FILE_NAME_KEY])
        output_file_name: str = "brightness_" + file_name

        img: ImageType = batch_image if is_batch else get_image_from_s3_and_record_time(bucket_name, file_name,
                                                                                        output_dict)
        modified_img: ImageType = ImageEnhance.Brightness(img).enhance(brightness_delta)

        if not is_batch:
            successful_write_to_s3: bool = save_image_to_s3(bucket_name, output_file_name, modified_img, "png")
            if not successful_write_to_s3:
                raise RuntimeError("Could not write image to S3.")

        output_dict[SUCCESS_KEY] = "Successfully changed image brightness."
        output_dict[BRIGHTNESS_KEY] = brightness_delta
        output_dict["brightness_factor"] = "Figure out implementation or remove from java" # ?
        
        if is_batch:
            output_dict[IMAGE_FILE_KEY] = modified_img
        else:
            output_dict[IMAGE_URL_KEY] = get_downloadable_image_url(bucket_name, output_file_name)
            output_dict[IMAGE_URL_EXPIRES_IN_KEY] = IMAGE_URL_EXPIRATION_SECONDS

        return output_dict
    except Exception as e:
        return {ERROR_KEY: str(e)}
