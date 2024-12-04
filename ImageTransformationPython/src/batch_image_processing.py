import base64
import os
import io
import boto3
from typing import Callable, Any
from PIL import ImageEnhance, Image
from PIL.Image import Image as ImageType
import boto3.resources
import boto3.s3
import time

# Custom typing
AWSFunctionOutput = dict[str, Any]
AWSRequestObject = Any
AWSContextObject = Any

# Event and response keys
IMAGE_FILE_KEY: str = 'image_file'
ERROR_KEY: str = 'error'
BUCKET_KEY: str = "bucketname"
FILE_NAME_KEY: str = "filename"
COLD_START_KEY: str = "cold_start"
IMAGE_URL_KEY: str = "url"
IMAGE_URL_EXPIRES_IN_KEY: str = "url_expires_in_seconds"
IMAGE_ACCESS_LATENCY_KEY: str = "network_latency_s"
FUNCTION_RUN_TIME_KEY: str = "function_runtime_s"
ESTIMATED_COST_KEY: str = "estimated_cost_usd"

# Default URL expiration time, in seconds
IMAGE_URL_EXPIRATION_SECONDS = 3600

cold_start: bool = True


def estimate_cost(run_time: int) -> float:
    memory_size_gb: float = 0.512; # Lambda memory size in GB
    price_per_gb_second: float = 0.00001667; # Pricing for Lambda
    return (run_time / 1000.0) * memory_size_gb * price_per_gb_second;


def validate_event(event: AWSRequestObject, *keys: tuple[str]) -> None | str:
    """
    Validates an event dictionary. If the event is valid, returns None.
    Otherwise, return an error string containing what keys are missing.
    """
    out: str = ''
    for key in keys:
        if key not in event:
            out += key + ", "

    if len(out) == 0:
        return None
    return "Missing request parameters: " + out[:-2]


def get_image_from_s3_and_record_time(bucket_name: str,
                                      file_name: str,
                                      output_dict: AWSFunctionOutput,
                                      region_name: str = "us-east-1") -> ImageType:
    """
    Retrieves an image from S3 from a provided bucket name and file name.
    """
    s3_start_time: float = time.time()
    
    s3 = boto3.resource('s3', region_name)
    obj = s3.Bucket(bucket_name).Object(file_name)
    file_stream: io.BytesIO = obj.get()["Body"]
    image: ImageType = Image.open(file_stream)
    
    output_dict[IMAGE_ACCESS_LATENCY_KEY] = time.time() - s3_start_time
    return image


def save_image_to_s3(bucket_name: str,
                     file_name: str,
                     image: ImageType,
                     file_format: str = "png",
                     region_name: str = "us-east-1") -> bool:
    """
    Saves an image to a given S3 storage bucket.
    """
    try:
        s3 = boto3.resource('s3', region_name)
        obj = s3.Bucket(bucket_name).Object(file_name)
        file_stream: io.BytesIO = io.BytesIO()
        image.save(file_stream, format=file_format)
        obj.put(Body=file_stream.getvalue())
        return True
    except Exception as e:
        print(e)
    return False

def get_downloadable_image_url(bucket_name: str, file_name: str) -> str | None:
    """
    Returns a url to download an image in a provided S3 bucket.
    """
    try:
        s3_client = boto3.client('s3')
        return s3_client.generate_presigned_url(
            'get_object',
            Params={'Bucket': bucket_name, 'Key': file_name},
            ExpiresIn=IMAGE_URL_EXPIRATION_SECONDS)
    except Exception as e:
        print("Error getting image URL: " + str(e))

    return None



def func_1_image_details(event: AWSRequestObject, 
                         context: AWSContextObject = None, 
                         batch_image: ImageType = None) -> AWSFunctionOutput:
    
    is_batch: bool = batch_image is not None
    global cold_start; 
    local_cold_start: bool = cold_start; 
    cold_start = False
    
    output_dict: AWSFunctionOutput = {} 

    validate_message: str = validate_event(event, BUCKET_KEY, FILE_NAME_KEY)
    if validate_message:
        return {ERROR_KEY: validate_message}

    try:
        bucket_name: str = str(event[BUCKET_KEY])
        file_name: str = str(event[FILE_NAME_KEY])
        
        processing_start_time: float = time.time()

        img: ImageType = batch_image if is_batch else get_image_from_s3_and_record_time(bucket_name, file_name, output_dict)

        output_dict['region'] = os.environ['AWS_REGION'] if 'AWS_REGION' in os.environ else 'NO_REGION_DATA'
        output_dict['height'] = img.height
        output_dict['width'] = img.width
        output_dict['mode'] = img.mode
        output_dict['has_transparency_data'] = 1 if img.has_transparency_data else 0
        output_dict[COLD_START_KEY] = 1 if local_cold_start else 0
        
        print(img.height)
        
        function_run_time: float = time.time() - processing_start_time
        output_dict[FUNCTION_RUN_TIME_KEY] = function_run_time
        output_dict[ESTIMATED_COST_KEY] = function_run_time

        if is_batch:
            output_dict[IMAGE_FILE_KEY] = img

        return output_dict
    except Exception as e:
        return {ERROR_KEY: str(e)}


ROTATION_ANGLE_KEY: str = 'rotation_angle'


def func_2_image_rotate(event: AWSRequestObject, 
                        context: AWSContextObject = None, 
                        batch_image: ImageType = None) -> AWSFunctionOutput:
    """
    Function #2: Image Rotation
    
    Pass in a Base64 encoded image and a rotation angle in a dictionary. 
    Will return a dictionary with the rotated image and metadata or an error message.
    
    Input:
    - 'image_file': Base64-encoded image string
    - 'rotation_angle': Rotation angle in degrees (90, 180, 270)
    
    Successful Response:
    {
        'rotated_image': str -> Base64-encoded rotated image,
        'original_width': int -> Width of the original image,
        'original_height': int -> Height of the original image,
        'rotated_width': int -> Width of the rotated image,
        'rotated_height': int -> Height of the rotated image,
        'rotation_angle': int -> The angle the image was rotated.
    }
    
    Error Response:
    {
        'error': str -> Error message
    }
    """
    is_batch: bool = batch_image is not None
    global cold_start
    local_cold_start: bool = cold_start
    cold_start = False
    
    output_dict: AWSFunctionOutput = {} 

    validate_message: str = validate_event(event, BUCKET_KEY, FILE_NAME_KEY, ROTATION_ANGLE_KEY)
    if validate_message:
        return {ERROR_KEY: validate_message}

    try:
        bucket_name: str = str(event[BUCKET_KEY])
        file_name: str = str(event[FILE_NAME_KEY])
        rotation_angle: int = int(event[ROTATION_ANGLE_KEY])
        output_file_name: str = "rotated_" + file_name
        
        processing_start_time: float = time.time()

        img: ImageType = batch_image if is_batch else get_image_from_s3_and_record_time(bucket_name, file_name, output_dict)

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
        output_dict[COLD_START_KEY] = 1 if local_cold_start else 0
        
        function_run_time: float = time.time() - processing_start_time
        output_dict[FUNCTION_RUN_TIME_KEY] = function_run_time
        output_dict[ESTIMATED_COST_KEY] = estimate_cost(function_run_time)

        if is_batch:
            output_dict[IMAGE_FILE_KEY] = rotated_img
        else:
            output_dict[IMAGE_URL_KEY] = get_downloadable_image_url(bucket_name, output_file_name)
            output_dict[IMAGE_URL_EXPIRES_IN_KEY] = IMAGE_URL_EXPIRATION_SECONDS
            
        return output_dict

    except Exception as e:
        return {ERROR_KEY: str(e)}


TARGET_WIDTH_KEY: str = 'target_width'
TARGET_HEIGHT_KEY: str = 'target_height'


def func_3_image_resize(event: AWSRequestObject, 
                        context: AWSContextObject = None, 
                        batch_image: ImageType = None) -> AWSFunctionOutput:
    
    is_batch: bool = batch_image is not None
    global cold_start
    local_cold_start: bool = cold_start
    cold_start = False
    
    output_dict: AWSFunctionOutput = {} 

    validate_message: str = validate_event(event, BUCKET_KEY, FILE_NAME_KEY, TARGET_WIDTH_KEY, TARGET_HEIGHT_KEY)
    if validate_message:
        return {ERROR_KEY: validate_message}

    try:
        bucket_name: str = str(event[BUCKET_KEY])
        file_name: str = str(event[FILE_NAME_KEY])
        target_width = int(event[TARGET_WIDTH_KEY])
        target_height = int(event[TARGET_HEIGHT_KEY])
        output_file_name: str = "resized_" + file_name
        
        processing_start_time: float = time.time()

        if target_width <= 0 or target_height <= 0:
            return {ERROR_KEY: f"Target dimensions must be positive integers."}

        image: ImageType = batch_image if is_batch else get_image_from_s3_and_record_time(bucket_name, file_name, output_dict)

        # Resize the image
        resized_image: ImageType = image.resize((target_width, target_height))

        if not is_batch:
            successful_write_to_s3: bool = save_image_to_s3(bucket_name, output_file_name, resized_image, "png")
            if not successful_write_to_s3:
                raise RuntimeError("Could not write image to S3.")

        output_dict["message"] = "Image resized successfully."
        output_dict[TARGET_WIDTH_KEY] = target_width
        output_dict[TARGET_HEIGHT_KEY] = target_height
        output_dict[COLD_START_KEY] = 1 if local_cold_start else 0
            
        function_run_time: float = time.time() - processing_start_time
        output_dict[FUNCTION_RUN_TIME_KEY] = function_run_time
        output_dict[ESTIMATED_COST_KEY] = estimate_cost(function_run_time)

        if is_batch:
            output_dict[IMAGE_FILE_KEY] = resized_image
        else:
            output_dict[IMAGE_URL_KEY] = get_downloadable_image_url(bucket_name, output_file_name)
            output_dict[IMAGE_URL_EXPIRES_IN_KEY] = IMAGE_URL_EXPIRATION_SECONDS
            

        return output_dict
    except Exception as e:
        return {ERROR_KEY: str(e)}


def func_4_image_grayscale(event: AWSRequestObject, 
                           context: AWSContextObject = None, 
                           batch_image: ImageType = None) -> AWSFunctionOutput:
    """
    Function 4: Image Grayscale Conversion
    """
    is_batch: bool = batch_image is not None
    global cold_start
    local_cold_start: bool = cold_start
    cold_start = False
    
    output_dict: AWSFunctionOutput = {} 

    validate_message: str = validate_event(event, BUCKET_KEY, FILE_NAME_KEY)
    if validate_message:
        return {ERROR_KEY: validate_message}

    try:
        bucket_name: str = str(event[BUCKET_KEY])
        file_name: str = str(event[FILE_NAME_KEY])
        output_file_name: str = "grayscaled_" + file_name
        
        processing_start_time: float = time.time()
        
        img: ImageType = batch_image if is_batch else get_image_from_s3_and_record_time(bucket_name, file_name, output_dict)

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
        output_dict[COLD_START_KEY] = 1 if local_cold_start else 0
        
        function_run_time: float = time.time() - processing_start_time
        output_dict[FUNCTION_RUN_TIME_KEY] = function_run_time
        output_dict[ESTIMATED_COST_KEY] = estimate_cost(function_run_time)

        if is_batch:
            output_dict[IMAGE_FILE_KEY] = grayscale_img
        else:
            output_dict[IMAGE_URL_KEY] = get_downloadable_image_url(bucket_name, output_file_name)
            output_dict[IMAGE_URL_EXPIRES_IN_KEY] = IMAGE_URL_EXPIRATION_SECONDS
            

        return output_dict

    except Exception as e:
        return {ERROR_KEY: str(e)}


BRIGHTNESS_KEY: str = 'brightness_delta'
BRIGHTNESS_BOUNDS: tuple[int, int] = (0, 100)  # I found that 100 is the maximum value before errors happen.


def func_5_image_brightness(event: AWSRequestObject, 
                            context: AWSContextObject = None, 
                            batch_image: ImageType = None) -> AWSFunctionOutput:
    """
    Function #5: Image brightness modification
    """
    is_batch: bool = batch_image is not None
    global cold_start
    local_cold_start: bool = cold_start
    cold_start = False
    
    output_dict: AWSFunctionOutput = {} 

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
        
        processing_start_time: float = time.time()

        img: ImageType = batch_image if is_batch else get_image_from_s3_and_record_time(bucket_name, file_name, output_dict)
        modified_img: ImageType = ImageEnhance.Brightness(img).enhance(brightness_delta)

        if not is_batch:
            successful_write_to_s3: bool = save_image_to_s3(bucket_name, output_file_name, modified_img, "png")
            if not successful_write_to_s3:
                raise RuntimeError("Could not write image to S3.")

        output_dict["args"] = {BRIGHTNESS_KEY: event[BRIGHTNESS_KEY]}
        output_dict[COLD_START_KEY] = 1 if local_cold_start else 0
        
        function_run_time: float = time.time() - processing_start_time
        output_dict[FUNCTION_RUN_TIME_KEY] = function_run_time
        output_dict[ESTIMATED_COST_KEY] = estimate_cost(function_run_time)
        
        if is_batch:
            output_dict[IMAGE_FILE_KEY] = modified_img
        else:
            output_dict[IMAGE_URL_KEY] = get_downloadable_image_url(bucket_name, output_file_name)
            output_dict[IMAGE_URL_EXPIRES_IN_KEY] = IMAGE_URL_EXPIRATION_SECONDS

        return output_dict


    except Exception as e:
        return {ERROR_KEY: str(e)}


SUPPORTED_FORMATS: set[str] = {"JPEG", "PNG", "BMP", "GIF", "TIFF"}


def func_6_image_transform(event: AWSRequestObject, 
                           context: AWSContextObject = None, 
                           batch_image: ImageType = None) -> AWSFunctionOutput:
    
    is_batch: bool = batch_image is not None
    global cold_start
    local_cold_start: bool = cold_start
    cold_start = False
    
    output_dict: AWSFunctionOutput = {} 
    
    validate_message: str = validate_event(event, BUCKET_KEY, FILE_NAME_KEY)
    if validate_message:
        return {ERROR_KEY: validate_message}

    try:
        bucket_name: str = str(event[BUCKET_KEY])
        file_name: str = str(event[FILE_NAME_KEY])
        output_format: str = str(event.get('output_format', 'JPEG')).upper()  # Default to JPEG
        compress_quality: int = int(event.get('compress_quality', 85))  # Default to 85% quality (for lossy formats like JPEG)
        preserve_metadata: bool = bool(event.get('preserve_metadata', True))  # Default to preserving metadata
        output_file_name: str = "transformed_" + file_name
        
        processing_start_time: float = time.time()
        
        if output_format not in SUPPORTED_FORMATS:
            return {
                ERROR_KEY: f"Unsupported output format: {output_format}. Supported formats: {', '.join(SUPPORTED_FORMATS)}"}
        if not (1 <= compress_quality <= 100):
            return {ERROR_KEY: "Compression quality must be between 1 and 100."}

        image: ImageType = batch_image if is_batch else get_image_from_s3_and_record_time(bucket_name, file_name, output_dict)
        # Convert image to the desired format
        output_buffer: io.BytesIO = io.BytesIO()
        save_kwargs: dict[str, Any] = {}

        # Handle metadata
        if not preserve_metadata:
            save_kwargs["exif"] = None

        # Handle compression for lossy formats
        if output_format == "JPEG":
            save_kwargs["quality"] = compress_quality

        try:
            image = image.convert("RGB") if output_format == "JPEG" else image
            image.save(output_buffer, format=output_format, **save_kwargs)
        except Exception as e:
            return {ERROR_KEY: f"Error finalizing image: {str(e)}"}

        image = Image.open(output_buffer)

        if not is_batch:
            successful_write_to_s3: bool = save_image_to_s3(bucket_name, output_file_name, image, "png")
            if not successful_write_to_s3:
                raise RuntimeError("Could not write image to S3.")

        output_dict["output_format"] = output_format
        output_dict["message"] = "Image finalized successfully."
        output_dict[COLD_START_KEY] = 1 if local_cold_start else 0
        
        function_run_time: float = time.time() - processing_start_time
        output_dict[FUNCTION_RUN_TIME_KEY] = function_run_time
        output_dict[ESTIMATED_COST_KEY] = estimate_cost(function_run_time)

        if is_batch:
            output_dict[IMAGE_FILE_KEY] = image
        else:
            output_dict[IMAGE_URL_KEY] = get_downloadable_image_url(bucket_name, output_file_name)
            output_dict[IMAGE_URL_EXPIRES_IN_KEY] = IMAGE_URL_EXPIRATION_SECONDS
            

        return output_dict

    except Exception as e:
        return {ERROR_KEY: f"Unexpected error: {str(e)}"}


OPERATIONS_KEY: str = 'operations'
FUNCTIONS: dict[str, Callable[[dict, Any, ImageType], AWSFunctionOutput]] = {
    "details": func_1_image_details,
    "rotate": func_2_image_rotate,
    "resize": func_3_image_resize,
    "grayscale": func_4_image_grayscale,
    "brightness": func_5_image_brightness,
    "transform": func_6_image_transform
}


def handle_request(event: AWSRequestObject, context: AWSContextObject = None) -> AWSFunctionOutput:
    
    global cold_start
    local_cold_start: bool = cold_start
    cold_start = False
    
    output_dict: AWSFunctionOutput = {} 


    validate_message: str = validate_event(event, BUCKET_KEY, FILE_NAME_KEY, OPERATIONS_KEY)
    if validate_message:
        return {ERROR_KEY: validate_message}

    try:
        bucket_name: str = str(event[BUCKET_KEY])
        file_name: str = str(event[FILE_NAME_KEY])
        output_file_name: str = "batch_" + file_name

        processing_start_time: float = time.time()

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
            operation_function: Callable[[dict, Any, ImageType], AWSFunctionOutput] = FUNCTIONS[operation_name]
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
        output_dict [COLD_START_KEY]= 1 if local_cold_start else 0
        
        function_run_time: float = time.time() - processing_start_time
        output_dict[FUNCTION_RUN_TIME_KEY] = function_run_time
        output_dict[ESTIMATED_COST_KEY] = estimate_cost(function_run_time)
        
        output_dict [IMAGE_URL_KEY] = get_downloadable_image_url(bucket_name, output_file_name)
        output_dict [IMAGE_URL_EXPIRATION_SECONDS] = IMAGE_URL_EXPIRATION_SECONDS
        
        
        return output_dict


    except Exception as e:
        return {ERROR_KEY: str(e)}


def safe_list_access(l: list, index: int, fallback=None) -> Any:
    try:
        return l[index]
    except Exception:
        return fallback


# FUNCTIONS: dict[str, Callable[[dict, Any], dict]] = {
#     "details": func_1_image_details,
#     "rotate": func_2_image_rotate,
#     "resize": func_3_image_resize,
#     "grayscale": func_4_image_grayscale,
#     "brightness": func_5_image_brightness,
#     "transform": func_6_image_transform
# }

# Some testing code.
if __name__ == '__main__':
    print("\nPWD: " + os.getcwd() + "\n")
    image_name = 'sample image.jpg'

    with open('../sample images/' + image_name, 'rb') as f:
        encoded_image = base64.b64encode(f.read())
        event_obj = {
            IMAGE_FILE_KEY: encoded_image,
            OPERATIONS_KEY: [
                ['details', {}],
                ['brightness', {BRIGHTNESS_KEY: '100'}],
                ['rotate', {ROTATION_ANGLE_KEY: 180}],
                ['resize', {TARGET_WIDTH_KEY: 250, TARGET_HEIGHT_KEY: 500}],
                ['grayscale', {}],
                ['details', {}],
                ['transform', {'output_format': 'PNG'}]
            ]
        }

        response_body = handle_request(event_obj)

        if ERROR_KEY not in response_body:
            print(response_body['operation_outputs'])
            with open('test output.png', "wb") as w:
                w.write(base64.b64decode(response_body[IMAGE_FILE_KEY]))
        else:
            print(response_body)
