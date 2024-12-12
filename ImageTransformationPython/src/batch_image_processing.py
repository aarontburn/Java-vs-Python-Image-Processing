"""
TCSS 462 Image Transformation
Group 7

Conducts multiple image transformations on a single image.
"""

from utils_custom_types import AWSFunctionOutput, AWSContextObject, AWSRequestObject, ImageType, AWSFunction
from utils_constants import BUCKET_KEY, FILE_NAME_KEY, ERROR_KEY, GET_DOWNLOAD_KEY, IMAGE_FILE_KEY, SUCCESS_KEY
from utils_helpers import add_image_url_to_dict, get_image_from_s3_and_record_time, validate_event, save_image_to_s3
from func_1_image_details import handle_request as f1
from func_2_image_rotate import handle_request as f2
from func_3_image_resize import handle_request as f3
from func_4_image_grayscale import handle_request as f4
from func_5_image_brightness import handle_request as f5
from func_6_image_transform import handle_request as f6

OPERATIONS_KEY: str = 'operations'
FUNCTIONS: dict[str, AWSFunction] = {
    "details": f1,
    "rotate": f2,
    "resize": f3,
    "grayscale": f4,
    "brightness": f5,
    "transform": f6
}


def handle_request(event: AWSRequestObject,
                   context: AWSContextObject = None) -> AWSFunctionOutput:
    output_dict: AWSFunctionOutput = {}

    validate_message: str = validate_event(
        event, BUCKET_KEY, FILE_NAME_KEY, OPERATIONS_KEY)
    if validate_message:
        return {ERROR_KEY: validate_message}

    try:
        bucket_name: str = str(event[BUCKET_KEY])
        file_name: str = str(event[FILE_NAME_KEY])
        operations: list[list[str, dict]] = event[OPERATIONS_KEY]
        output_file_name: str = "batch_" + file_name
        output_format: str = "png"
        operation_outputs: list[AWSFunctionOutput] = []

        image: ImageType | None = get_image_from_s3_and_record_time(
            bucket_name, file_name, output_dict)
        if image is None:
            return {ERROR_KEY: "Could not access image from S3."}

        for i in range(len(operations)):
            operation_name: str = safe_list_access(operations[i], 0, '')
            operation_args: dict | None = safe_list_access(
                operations[i], 1, {})

            if operation_name not in FUNCTIONS:
                print(f"Pipeline error: invalid operation name at index {i}: {operation_name}")
                continue

            operation_args[BUCKET_KEY] = bucket_name
            operation_args[FILE_NAME_KEY] = file_name

            print(f"Executing function {i}: {operation_name}")
            operation_function: AWSFunction = FUNCTIONS[operation_name]
            response_object: AWSFunctionOutput = operation_function(
                operation_args, context, image)

            if ERROR_KEY in response_object:
                temp = operation_args.copy()
                temp.pop(IMAGE_FILE_KEY, None)
                print(f"Pipeline error: Error executing function at index {i} with args {temp}")
                print(response_object[ERROR_KEY])
            else:
                if operation_name == "transform":
                    output_format = str(
                        operation_args.get("target_format")).lower()
                    output_file_name = "batch_" + \
                        file_name.split(".")[0] + "." + output_format

            image = response_object[IMAGE_FILE_KEY] \
                if IMAGE_FILE_KEY in response_object \
                else image

            appended_output: AWSFunctionOutput = response_object.copy()
            appended_output.pop(IMAGE_FILE_KEY, None)
            operation_outputs.append(appended_output)

        successful_write_to_s3: bool = \
            save_image_to_s3(bucket_name,
                             output_file_name,
                             image,
                             output_format)

        if not successful_write_to_s3:
            return {ERROR_KEY: "Could not write image to S3."}

        output_dict[SUCCESS_KEY] = "Successfully processed image."
        output_dict["batch_operations_count"] = len(operations)
        output_dict['operation_outputs'] = operation_outputs

        if event[GET_DOWNLOAD_KEY]:
            add_image_url_to_dict(output_dict,
                                  bucket_name,
                                  output_file_name)

    except Exception as e:
        return {ERROR_KEY: str(e)}

    return output_dict


def safe_list_access(list_to_access: list, index: int, fallback=None):
    try:
        return list_to_access[index]
    except Exception:
        return fallback
