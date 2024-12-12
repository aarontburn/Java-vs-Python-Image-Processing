"""
TCSS 462 Image Transformation
Group 7

Rotates an image 90, 180, or 270 degrees.
"""

from utils_custom_types import AWSFunctionOutput, AWSContextObject, AWSRequestObject, ImageType, OptionalImage
from utils_constants import BUCKET_KEY, FILE_NAME_KEY, ERROR_KEY, IMAGE_FILE_KEY, SUCCESS_KEY, GET_DOWNLOAD_KEY
from utils_helpers import get_image_from_s3_and_record_time, validate_event, save_image_to_s3, add_image_url_to_dict, get_file_extension

ROTATION_ANGLE_KEY: str = 'rotation_angle'

def handle_request(event: AWSRequestObject,
                   context: AWSContextObject = None,
                   batch_image: OptionalImage = None) -> AWSFunctionOutput:

    is_batch: bool = batch_image is not None

    output_dict: AWSFunctionOutput = {}

    validate_message: str = validate_event(
        event, BUCKET_KEY, FILE_NAME_KEY, ROTATION_ANGLE_KEY)
    if validate_message:
        return {ERROR_KEY: validate_message}

    try:
        bucket_name: str = str(event[BUCKET_KEY])
        file_name: str = str(event[FILE_NAME_KEY])
        rotation_angle: int = int(event[ROTATION_ANGLE_KEY])
        output_file_name: str = "rotated_" + file_name

        img: ImageType = batch_image \
            if is_batch \
            else get_image_from_s3_and_record_time(bucket_name, file_name, output_dict)

        if img is None:
            return {ERROR_KEY: "Could not access image from S3."}

        if rotation_angle not in [90, 180, 270]:
            return {ERROR_KEY: "Invalid rotation angle. Only 90, 180, or 270 degrees are supported."}

        # Perform the rotation
        rotated_img: ImageType = img.rotate(-rotation_angle, expand=True)

        if not is_batch:
            successful_write_to_s3: bool = \
                save_image_to_s3(bucket_name,
                                 output_file_name,
                                 rotated_img,
                                 get_file_extension(output_file_name))

            if not successful_write_to_s3:
                return {ERROR_KEY: "Could not write image to S3."}

            if event[GET_DOWNLOAD_KEY]:
                add_image_url_to_dict(output_dict,
                                      bucket_name,
                                      output_file_name)

        else:
            output_dict[IMAGE_FILE_KEY] = rotated_img

        output_dict[SUCCESS_KEY] = "Image rotated successfully"
        output_dict['rotation_angle'] = rotation_angle

        return output_dict

    except Exception as e:
        return {ERROR_KEY: str(e)}
