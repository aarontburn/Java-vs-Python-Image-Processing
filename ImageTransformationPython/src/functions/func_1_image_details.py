from ImageTransformationPython.src.custom_types import AWSFunctionOutput, AWSContextObject, AWSRequestObject, ImageType
from constants import BUCKET_KEY, FILE_NAME_KEY, ERROR_KEY, IMAGE_FILE_KEY
from helpers import get_image_from_s3_and_record_time, validate_event

def handle_request(output_dict: AWSFunctionOutput, 
                    event: AWSRequestObject, 
                    context: AWSContextObject = None, 
                    batch_image: ImageType = None) -> None:
    
    is_batch: bool = batch_image is not None
    
    validate_message: str = validate_event(event, BUCKET_KEY, FILE_NAME_KEY)
    if validate_message:
        return {ERROR_KEY: validate_message}

    try:
        bucket_name: str = str(event[BUCKET_KEY])
        file_name: str = str(event[FILE_NAME_KEY])
        
        img: ImageType = batch_image if is_batch else get_image_from_s3_and_record_time(bucket_name, file_name, output_dict)

        output_dict['height'] = img.height
        output_dict['width'] = img.width
        output_dict['mode'] = img.mode
        output_dict['has_transparency_data'] = 1 if img.has_transparency_data else 0

        if is_batch:
            output_dict[IMAGE_FILE_KEY] = img

    except Exception as e:
        return {ERROR_KEY: str(e)}
