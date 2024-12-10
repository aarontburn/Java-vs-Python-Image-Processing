from utils_custom_types import AWSFunctionOutput, AWSContextObject, AWSRequestObject, ImageType, OptionalImage
from utils_constants import BUCKET_KEY, FILE_NAME_KEY, ERROR_KEY, GET_DOWNLOAD_KEY, IMAGE_FILE_KEY, IMAGE_URL_KEY, IMAGE_URL_EXPIRES_IN_KEY, \
    IMAGE_URL_EXPIRATION_SECONDS, SUCCESS_KEY
from utils_helpers import add_image_url_to_dict, get_file_extension, get_image_from_s3_and_record_time, validate_event, save_image_to_s3, get_downloadable_image_url


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

        if img is None:
            return {ERROR_KEY: "Could not access image from S3."}

        # Convert the image to grayscale
        grayscale_img: ImageType = img.convert("L")

        if not is_batch:
            successful_write_to_s3: bool = \
                save_image_to_s3(bucket_name,
                                 output_file_name,
                                 grayscale_img,
                                 get_file_extension(output_file_name))

            if not successful_write_to_s3:
                return {ERROR_KEY: "Could not write image to S3."}

            if event[GET_DOWNLOAD_KEY]:
                add_image_url_to_dict(
                    output_dict, bucket_name, output_file_name)
        else:
            output_dict[IMAGE_FILE_KEY] = grayscale_img

        output_dict[SUCCESS_KEY] = "Image successfully converted to grayscale"

        return output_dict

    except Exception as e:
        return {ERROR_KEY: str(e)}
