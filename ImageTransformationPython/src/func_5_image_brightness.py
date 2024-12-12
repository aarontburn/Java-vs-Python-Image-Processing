"""
TCSS 462 Image Transformation
Group 7

Modifies the brightness of an image.
"""

from utils_custom_types import AWSFunctionOutput, AWSContextObject, AWSRequestObject, ImageType, OptionalImage
from utils_constants import BUCKET_KEY, FILE_NAME_KEY, ERROR_KEY, GET_DOWNLOAD_KEY, IMAGE_FILE_KEY, SUCCESS_KEY
from utils_helpers import add_image_url_to_dict, get_file_extension, get_image_from_s3_and_record_time, validate_event, save_image_to_s3
from PIL import ImageEnhance

BRIGHTNESS_KEY: str = 'brightness_delta'
# I found that 100 is the maximum value before errors happen.
BRIGHTNESS_BOUNDS: tuple[int, int] = (0, 100)


def handle_request(event: AWSRequestObject,
                   context: AWSContextObject = None,
                   batch_image: OptionalImage = None) -> AWSFunctionOutput:
    """
    Function #5: Image brightness modification
    """

    output_dict: AWSFunctionOutput = {}

    is_batch: bool = batch_image is not None

    validate_message: str = validate_event(
        event, BUCKET_KEY, FILE_NAME_KEY, BRIGHTNESS_KEY)
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

        img: ImageType = batch_image \
            if is_batch \
            else get_image_from_s3_and_record_time(bucket_name, file_name, output_dict)

        if img is None:
            return {ERROR_KEY: "Could not access image from S3."}

        modified_img: ImageType = ImageEnhance.Brightness(img) \
            .enhance(brightness_delta)

        if not is_batch:
            successful_write_to_s3: bool = \
                save_image_to_s3(bucket_name,
                                 output_file_name,
                                 modified_img,
                                 get_file_extension(output_file_name))

            if not successful_write_to_s3:
                return {ERROR_KEY: "Could not write image to S3."}

            if event[GET_DOWNLOAD_KEY]:
                add_image_url_to_dict(output_dict,
                                      bucket_name,
                                      output_file_name)

        else:
            output_dict[IMAGE_FILE_KEY] = modified_img

        output_dict[SUCCESS_KEY] = "Successfully changed image brightness."
        output_dict[BRIGHTNESS_KEY] = brightness_delta

        return output_dict
    except Exception as e:
        return {ERROR_KEY: str(e)}
