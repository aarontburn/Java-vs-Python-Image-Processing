from utils_custom_types import AWSFunctionOutput, AWSContextObject, AWSRequestObject, ImageType, OptionalImage
from utils_constants import BUCKET_KEY, FILE_NAME_KEY, ERROR_KEY, IMAGE_FILE_KEY, IMAGE_URL_KEY, IMAGE_URL_EXPIRES_IN_KEY, \
    IMAGE_URL_EXPIRATION_SECONDS
from utils_helpers import get_image_from_s3_and_record_time, validate_event, save_image_to_s3, get_downloadable_image_url

ROTATION_ANGLE_KEY: str = 'rotation_angle'


def handle_request(event: AWSRequestObject,
                   context: AWSContextObject = None,
                   batch_image: OptionalImage = None) -> AWSFunctionOutput:

    output_dict: AWSFunctionOutput = {}

    is_batch: bool = batch_image is not None

    validate_message: str = validate_event(event, BUCKET_KEY, FILE_NAME_KEY, ROTATION_ANGLE_KEY)
    if validate_message:
        return {ERROR_KEY: validate_message}

    try:
        bucket_name: str = str(event[BUCKET_KEY])
        file_name: str = str(event[FILE_NAME_KEY])
        rotation_angle: int = int(event[ROTATION_ANGLE_KEY])
        output_file_name: str = "rotated_" + file_name

        img: ImageType = batch_image if is_batch else get_image_from_s3_and_record_time(bucket_name, file_name,
                                                                                        output_dict)

        if rotation_angle not in [90, 180, 270]:
            return {ERROR_KEY: "Invalid rotation angle. Only 90, 180, or 270 degrees are supported."}

        # Save original dimensions
        original_width, original_height = img.width, img.height

        # Perform the rotation
        rotated_img: ImageType = img.rotate(-rotation_angle, expand=True)

        if not is_batch:
            successful_write_to_s3: bool = save_image_to_s3(bucket_name, output_file_name, rotated_img, "png")
            if not successful_write_to_s3:
                raise RuntimeError("Could not write image to S3.")

        output_dict['original_width'] = original_width
        output_dict['original_height'] = original_height
        output_dict['rotated_width'] = rotated_img.width
        output_dict['rotated_height'] = rotated_img.height
        output_dict['rotation_angle'] = rotation_angle

        if is_batch:
            output_dict[IMAGE_FILE_KEY] = rotated_img
        else:
            output_dict[IMAGE_URL_KEY] = get_downloadable_image_url(bucket_name, output_file_name)
            output_dict[IMAGE_URL_EXPIRES_IN_KEY] = IMAGE_URL_EXPIRATION_SECONDS

        return output_dict

    except Exception as e:
        return {ERROR_KEY: str(e)}
