from custom_types import AWSFunctionOutput, AWSContextObject, AWSRequestObject, ImageType, AWSFunction
from constants import BUCKET_KEY, FILE_NAME_KEY, ERROR_KEY, IMAGE_FILE_KEY, IMAGE_URL_EXPIRATION_SECONDS, IMAGE_URL_KEY
from typing import Any
from helpers import get_image_from_s3_and_record_time, validate_event, save_image_to_s3, get_downloadable_image_url
from functions.func_1_image_details import handle_request as f1
from functions.func_2_image_rotate import handle_request as f2
from functions.func_3_image_resize import handle_request as f3
from functions.func_4_image_grayscale import handle_request as f4
from functions.func_5_image_brightness import handle_request as f5
from functions.func_6_image_transform import handle_request as f6


OPERATIONS_KEY: str = 'operations'
FUNCTIONS: dict[str, AWSFunction] = {
    "details": f1,
    "rotate": f2,
    "resize": f3,
    "grayscale": f4,
    "brightness": f5,
    "transform": f6
}

def handle_request(output_dict: AWSFunctionOutput,
                   event: AWSRequestObject, 
                   context: AWSContextObject = None) -> None:
    

    validate_message: str = validate_event(event, BUCKET_KEY, FILE_NAME_KEY, OPERATIONS_KEY)
    if validate_message:
        return {ERROR_KEY: validate_message}

    try:
        bucket_name: str = str(event[BUCKET_KEY])
        file_name: str = str(event[FILE_NAME_KEY])
        output_file_name: str = "batch_" + file_name

        image: ImageType = get_image_from_s3_and_record_time(bucket_name, file_name, output_dict)

        operations: list[list[str, dict]] = event[OPERATIONS_KEY]
        operation_outputs: list[dict[str, Any]] = []
        for i in range(len(operations)):
            operation_name: str = safe_list_access(operations[i], 0, '')
            operation_args: dict | None = safe_list_access(operations[i], 1)

            if operation_name not in FUNCTIONS:
                print(f"Pipeline error: invalid operation name at index {i}: {operation_name}")
                continue

            if operation_args is None:
                print(f"Pipeline error: Error retrieving input argument at index {i}")
                continue

            print(f"Executing function {i}: {operation_name}")
            operation_function: AWSFunction = FUNCTIONS[operation_name]
            response_object: AWSFunctionOutput = operation_function(operation_args, context, image)

            if ERROR_KEY in response_object:
                temp = operation_args.copy()
                temp.pop(IMAGE_FILE_KEY, None)
                print(f"Pipeline error: Error executing function at index {i} with args {temp}")
                print(response_object[ERROR_KEY])

            image = response_object[IMAGE_FILE_KEY] if IMAGE_FILE_KEY in response_object else image

            appended_output: dict[str, Any] = response_object.copy()
            appended_output.pop(IMAGE_FILE_KEY, None)
            operation_outputs.append(appended_output)

        successful_write_to_s3: bool = save_image_to_s3(bucket_name, output_file_name, image, "png")
        if not successful_write_to_s3:
            raise RuntimeError("Could not write image to S3.")

        
        output_dict ['operation_outputs'] = operation_outputs
        output_dict [IMAGE_URL_KEY] = get_downloadable_image_url(bucket_name, output_file_name)
        output_dict [IMAGE_URL_EXPIRATION_SECONDS] = IMAGE_URL_EXPIRATION_SECONDS
        
    except Exception as e:
        return {ERROR_KEY: str(e)}


def safe_list_access(l: list, index: int, fallback=None) -> Any:
    try:
        return l[index]
    except Exception:
        return fallback


