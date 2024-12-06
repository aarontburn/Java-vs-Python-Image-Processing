from utils_custom_types import AWSFunctionOutput, AWSContextObject, AWSRequestObject, ImageType, OptionalImage
from utils_constants import BUCKET_KEY, FILE_NAME_KEY, ERROR_KEY, IMAGE_FILE_KEY, IMAGE_URL_KEY, IMAGE_URL_EXPIRES_IN_KEY, \
    IMAGE_URL_EXPIRATION_SECONDS
from utils_helpers import get_image_from_s3_and_record_time, validate_event, save_image_to_s3, get_downloadable_image_url


def handle_request(event: AWSRequestObject,
                   context: AWSContextObject = None,
                   batch_image: OptionalImage = None) -> AWSFunctionOutput:
    """
    Function 4: Image Grayscale Conversion
    """

    output_dict: AWSFunctionOutput = {}

    is_batch: bool = batch_image is not None

    validate_message: str = validate_event(event, BUCKET_KEY, FILE_NAME_KEY)
    if validate_message:
        return {ERROR_KEY: validate_message}

    try:
        bucket_name: str = str(event[BUCKET_KEY])
        file_name: str = str(event[FILE_NAME_KEY])
        output_file_name: str = "grayscaled_" + file_name

        img: ImageType = batch_image if is_batch else get_image_from_s3_and_record_time(bucket_name, file_name,
                                                                                        output_dict)

        # Save original dimensions
        original_width, original_height = img.width, img.height

        # Convert the image to grayscale
        grayscale_img: ImageType = img.convert("L")

        if not is_batch:
            successful_write_to_s3: bool = save_image_to_s3(bucket_name, output_file_name, grayscale_img, "png")
            if not successful_write_to_s3:
                raise RuntimeError("Could not write image to S3.")

        output_dict['original_width'] = original_width
        output_dict['original_height'] = original_height
        output_dict['grayscale_width'] = grayscale_img.width
        output_dict['grayscale_height'] = grayscale_img.height

        if is_batch:
            output_dict[IMAGE_FILE_KEY] = grayscale_img
        else:
            output_dict[IMAGE_URL_KEY] = get_downloadable_image_url(bucket_name, output_file_name)
            output_dict[IMAGE_URL_EXPIRES_IN_KEY] = IMAGE_URL_EXPIRATION_SECONDS

        return output_dict

    except Exception as e:
        return {ERROR_KEY: str(e)}
