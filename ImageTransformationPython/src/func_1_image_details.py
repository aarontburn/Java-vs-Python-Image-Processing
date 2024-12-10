from utils_custom_types \
    import AWSFunctionOutput, AWSContextObject, AWSRequestObject, ImageType, OptionalImage

from utils_constants \
    import BUCKET_KEY, FILE_NAME_KEY, ERROR_KEY, IMAGE_FILE_KEY, SUCCESS_KEY, GET_DOWNLOAD_KEY


from utils_helpers \
    import get_image_from_s3_and_record_time, validate_event, add_image_url_to_dict


def handle_request(event: AWSRequestObject,
                   context: AWSContextObject = None,
                   batch_image: OptionalImage = None) -> AWSFunctionOutput:

    is_batch: bool = batch_image is not None

    output_dict: AWSFunctionOutput = {}

    validate_message: str = validate_event(event, BUCKET_KEY, FILE_NAME_KEY)
    if validate_message:
        return {ERROR_KEY: validate_message}

    try:
        bucket_name: str = str(event[BUCKET_KEY])
        file_name: str = str(event[FILE_NAME_KEY])

        img: ImageType = batch_image \
            if is_batch \
            else get_image_from_s3_and_record_time(bucket_name, file_name, output_dict)
            
        if img is None:
            return {ERROR_KEY: "Could not access image from S3."}

        output_dict[SUCCESS_KEY] = "Successfully retrieved image details"
        output_dict['height'] = img.height
        output_dict['width'] = img.width
        output_dict['mode'] = img.mode
        output_dict['has_transparency_data'] = 1 if img.has_transparency_data else 0

        if is_batch:
            output_dict[IMAGE_FILE_KEY] = img
        else:
            if event[GET_DOWNLOAD_KEY]:
                add_image_url_to_dict(output_dict, bucket_name, file_name)

        return output_dict

    except Exception as e:
        return {ERROR_KEY: str(e)}
