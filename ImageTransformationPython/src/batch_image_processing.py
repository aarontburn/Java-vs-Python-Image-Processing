import base64
import os
import io
import boto3
from typing import Callable, Any
from PIL import Image, ImageEnhance
import boto3.s3


IMAGE_FILE_KEY: str = 'image_file'
ERROR_KEY: str = 'error'
BUCKET_KEY: str = "bucketname"
FILE_NAME_KEY: str = "filename"

cold_start: bool = True

def validate_event(event, *keys):
    out = ''
    for key in keys:
        if key not in event:
            out += key + ", "
            
    if len(out) == 0:
        return None
    return "Missing request parameters: " + out[:-2]
    
    
def get_image_from_s3(bucket_name: str,
                      file_name: str, 
                      region_name: str = "us-east-1") -> Image:
    
    s3 = boto3.resource('s3', region_name)
    obj = s3.Bucket(bucket_name).Object(file_name)
    file_stream = obj.get()["Body"]
    return Image.open(file_stream)

def save_image_to_s3(bucket_name: str,
                     file_name: str,
                     image: Image, 
                     format: str = "png",
                     region_name: str = "us-east-1") -> bool:
    
    try:
        s3 = boto3.resource('s3', region_name)
        obj = s3.Bucket(bucket_name).Object(file_name)
        
        file_stream = io.BytesIO()
        image.save(file_stream, format=format)
        obj.put(Body=file_stream.getvalue())
    except Exception as e:
        print(e)
        return False
    return True
        


def func_1_image_details(event, context = None, batch_image: Image = None):
    
    """
    Function #1: Image upload and validation

    Request Body {
        'image_file': The image encoded in base64.
    }
    
    Pass in a Base64 encoded image in a dictionary under key 'image_file'. Will return a
        dictionary following one of these schemas:
        
            <key>: <type> -> <description>
        Successful Response {
            'region': str -> I think when this is ran within lambda, this should be populated. Otherwise, return "NO_REGION_DATA".
            'height': int -> The height (in px i think) of the image.
            'width': str -> The width (in px i think) of the image.
            'mode': str -> The mode of the image. See https://pillow.readthedocs.io/en/stable/handbook/concepts.html
            'has_transparency_data: int -> 1 if the image has transparency data, 0 otherwise.
        }
        
        Error Response {
            'error': str -> The error converted to a string form.
        }
    
    :param event:   A dictionary containing request data (must have the key, 'image_file', with value as the image in base64 encoding.).
    :param context: 
    :returns:       A dictionary containing the response data.
    """
    is_batch: bool = batch_image != None
    
    validate_message: str = validate_event(event, BUCKET_KEY, FILE_NAME_KEY)
    if validate_message:
        return {ERROR_KEY: validate_message}

    try:
        bucket_name: str = str(event[BUCKET_KEY])
        file_name: str = str(event[FILE_NAME_KEY])
        
        img: Image = batch_image if is_batch else get_image_from_s3(bucket_name, file_name)
        
        global cold_start
        local_cold_start = cold_start
        cold_start = False
        
        output_dict: dict[str, Any] = {
            'region': os.environ['AWS_REGION'] if 'AWS_REGION' in os.environ else 'NO_REGION_DATA',
            'height': img.height,
            'width': img.width,
            'mode': img.mode,
            'has_transparency_data': 1 if img.has_transparency_data else 0,
            "cold_start": 1 if local_cold_start else 0
        }
        
        if is_batch:
            output_dict[IMAGE_FILE_KEY] = img
        
        return output_dict
    except Exception as e:
        return {ERROR_KEY: str(e)}

    # This might be unreachable
    return {ERROR_KEY: "Unknown error processing file."}



ROTATION_ANGLE_KEY = 'rotation_angle'
def func_2_image_rotate(event, context = None, batch_image: Image = None):
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
    is_batch: bool = batch_image != None
    
    validate_message: str = validate_event(event, BUCKET_KEY, FILE_NAME_KEY, ROTATION_ANGLE_KEY)
    if validate_message:
        return {ERROR_KEY: validate_message}

    try:
        bucket_name: str = str(event[BUCKET_KEY])
        file_name: str = str(event[FILE_NAME_KEY])
        rotation_angle: int = int(event[ROTATION_ANGLE_KEY])
        
        img: Image = batch_image if is_batch else get_image_from_s3(bucket_name, file_name)

        if rotation_angle not in [90, 180, 270]:
            return {ERROR_KEY: "Invalid rotation angle. Only 90, 180, or 270 degrees are supported."}

        global cold_start
        local_cold_start = cold_start
        cold_start = False

        # Save original dimensions
        original_width, original_height= img.width, img.height

        # Perform the rotation
        rotated_img: Image = img.rotate(-rotation_angle, expand=True)

        if not is_batch:
            successful_write_to_s3: bool = save_image_to_s3(bucket_name, "rotated_" + file_name, rotated_img, "png")
            if not successful_write_to_s3:
                raise RuntimeError("Could not write image to S3.")


        output_dict: dict[str, Any] = {
            'original_width': original_width,
            'original_height': original_height,
            'rotated_width': rotated_img.width,
            'rotated_height': rotated_img.height,
            'rotation_angle': rotation_angle,
            "cold_start": 1 if local_cold_start else 0
        }
        
        if is_batch:
            output_dict[IMAGE_FILE_KEY] = rotated_img

        return output_dict

    except Exception as e:
        return {ERROR_KEY: str(e)}

    # This might be unreachable
    return {ERROR_KEY: "Unknown error processing file."}


TARGET_WIDTH_KEY: str = 'target_width'
TARGET_HEIGHT_KEY: str = 'target_height'
def func_3_image_resize(event, context = None, batch_image: Image = None):
    is_batch: bool = batch_image != None
    
    validate_message: str = validate_event(event, BUCKET_KEY, FILE_NAME_KEY, TARGET_WIDTH_KEY, TARGET_HEIGHT_KEY)
    if validate_message:
        return {ERROR_KEY: validate_message}
    
    global cold_start
    local_cold_start = cold_start
    cold_start = False
    
    try:
        bucket_name: str = str(event[BUCKET_KEY])
        file_name: str = str(event[FILE_NAME_KEY])
        target_width = int(event[TARGET_WIDTH_KEY])
        target_height = int(event[TARGET_HEIGHT_KEY])

        if target_width <= 0 or target_height <= 0:
            return {ERROR_KEY: f"Target dimensions must be positive integers."}

        image: Image = batch_image if is_batch else get_image_from_s3(bucket_name, file_name)

        # Resize the image
        resized_image: Image = image.resize((target_width, target_height))
        
        if not is_batch:
            successful_write_to_s3: bool = save_image_to_s3(bucket_name, "resized_" + file_name, resized_image, "png")
            if not successful_write_to_s3:
                raise RuntimeError("Could not write image to S3.")

        output_dict: dict[str, Any] = {
            "message": "Image resized successfully.",
            TARGET_WIDTH_KEY: target_width,
            TARGET_HEIGHT_KEY: target_height,
            "cold_start": 1 if local_cold_start else 0
        }
        
        if is_batch:
            output_dict[IMAGE_FILE_KEY] = resized_image

        return output_dict
    except Exception as e:
        return {ERROR_KEY: str(e)}
    

def func_4_image_grayscale(event, context = None, batch_image: Image = None):
    """
    Function: Image Grayscale Conversion

    Pass in a Base64-encoded image in a dictionary.
    Will return a dictionary with the grayscale image and metadata or an error message.
    """
    is_batch: bool = batch_image != None
    
    validate_message: str = validate_event(event, BUCKET_KEY, FILE_NAME_KEY)
    if validate_message:
        return {ERROR_KEY: validate_message}
    
    global cold_start
    local_cold_start = cold_start
    cold_start = False
    
    try:
        bucket_name: str = str(event[BUCKET_KEY])
        file_name: str = str(event[FILE_NAME_KEY])
        img: Image = batch_image if is_batch else get_image_from_s3(bucket_name, file_name)

        # Save original dimensions
        original_width, original_height = img.width, img.height

        # Convert the image to grayscale
        grayscale_img = img.convert("L")
        
        if not is_batch:
            successful_write_to_s3: bool = save_image_to_s3(bucket_name, "grayscaled_" + file_name, grayscale_img, "png")
            if not successful_write_to_s3:
                raise RuntimeError("Could not write image to S3.")
        
        output_dict: dict[str, Any] = {
            'original_width': original_width,
            'original_height': original_height,
            'grayscale_width': grayscale_img.width,
            'grayscale_height': grayscale_img.height,
            "cold_start": 1 if local_cold_start else 0
        }
        
        if is_batch:
            output_dict[IMAGE_FILE_KEY] = grayscale_img

        
        return output_dict

    except Exception as e:
        return {ERROR_KEY: str(e)}

    return {ERROR_KEY: "Unknown error processing file."}


BRIGHTNESS_KEY: float = 'brightness_delta'
BRIGHTNESS_BOUNDS: tuple[int, int] = (0, 100) # I found that 100 is the maximum value before errors happen.
def func_5_image_brightness(event, context = None, batch_image: Image = None):
    """
    Function #5: Image brightness modification
    
    Request Body {
        'image_file': str -> The image to modify encoded in base64.
        'brightness_delta': float[0 - 100]: The brightness modifier.
    }

    Pass in a Base64 encoded image in a dictionary under key 'image_file' and 
    a brightness modifier from 0 - 100. 0 Will return a black image, 1 will return the
    original image, anything higher will be an image with its brightness modified. 
    
    Will return a dictionary following one of these schemas:
        
            <key>: <type> -> <description>
        Successful Response {
            'args': {
                'brightness_delta': float -> Brightness modifier
            } -> Input arguments. Not really needed, but might be useful.
            'image_file': str -> The modified image encoded in base64.
        }
        
        Error Response {
            'error': str -> The error converted to a string form.
        }
    
    :param event:   A dictionary containing request data.
    :param context: 
    :returns:       A dictionary containing the response data.
    """
    is_batch: bool = batch_image != None
    
    
    validate_message: str = validate_event(event, BUCKET_KEY, FILE_NAME_KEY, BRIGHTNESS_KEY)
    if validate_message:
        return {ERROR_KEY: validate_message}
    
    global cold_start
    local_cold_start = cold_start
    cold_start = False
    
    brightness_delta: float = 1.
    try:
        brightness_delta = float(event[BRIGHTNESS_KEY])
        if brightness_delta < BRIGHTNESS_BOUNDS[0] or brightness_delta > BRIGHTNESS_BOUNDS[1]:
            return {ERROR_KEY: f"'{BRIGHTNESS_KEY}' is out-of-bounds \
                    ({BRIGHTNESS_BOUNDS[0]}-{BRIGHTNESS_BOUNDS[1]}): {brightness_delta}"}

    except:
        return {ERROR_KEY: f"'{BRIGHTNESS_KEY}' is not parsable as a float."}

    # Modify and return image.
    try:
        bucket_name: str = str(event[BUCKET_KEY])
        file_name: str = str(event[FILE_NAME_KEY])
        
        img: Image = batch_image if is_batch else get_image_from_s3(bucket_name, file_name)
        modified_img: Image = ImageEnhance.Brightness(img).enhance(brightness_delta)
        
        if not is_batch:
            successful_write_to_s3: bool = save_image_to_s3(bucket_name, "brightness_" + file_name, modified_img, "png")
            if not successful_write_to_s3:
                raise RuntimeError("Could not write image to S3.")

        output_dict: dict[str, Any] = {
            "args": {BRIGHTNESS_KEY: event[BRIGHTNESS_KEY]},
            "cold_start": 1 if local_cold_start else 0
        }
        
        if is_batch:
            output_dict[IMAGE_FILE_KEY] = modified_img
        
        return output_dict


    except Exception as e:
        return {ERROR_KEY: str(e)}
    
SUPPORTED_FORMATS = {"JPEG", "PNG", "BMP", "GIF", "TIFF"}
def func_6_image_transform(event, context = None, batch_image: Image = None):
    is_batch: bool = batch_image != None
    
    try:
        bucket_name: str = str(event[BUCKET_KEY])
        file_name: str = str(event[FILE_NAME_KEY])
        
        output_format: str = str(event.get('output_format', 'JPEG')).upper()  # Default to JPEG
        compress_quality: int = int(event.get('compress_quality', 85))  # Default to 85% quality (for lossy formats like JPEG)
        preserve_metadata: bool = bool(event.get('preserve_metadata', True))  # Default to preserving metadata

        global cold_start
        local_cold_start = cold_start
        cold_start = False

        if output_format not in SUPPORTED_FORMATS:
            return {ERROR_KEY: f"Unsupported output format: {output_format}. Supported formats: {', '.join(SUPPORTED_FORMATS)}"}
        if not (1 <= compress_quality <= 100):
            return {ERROR_KEY: "Compression quality must be between 1 and 100."}

        image: Image = batch_image if is_batch else get_image_from_s3(bucket_name, file_name)
        # Convert image to the desired format
        output_buffer: io.BytesIO = io.BytesIO()
        save_kwargs = {}

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
            successful_write_to_s3: bool = save_image_to_s3(bucket_name, "transformed_" + file_name, image, "png")
            if not successful_write_to_s3:
                raise RuntimeError("Could not write image to S3.")


        output_dict: dict[str, Any] = {
            "output_format": output_format,
            "message": "Image finalized successfully.",
            "cold_start": 1 if local_cold_start else 0
        }
        
        if is_batch:
            output_dict[IMAGE_FILE_KEY] = image
            
        
        return output_dict

    except Exception as e:
        return {ERROR_KEY: f"Unexpected error: {str(e)}"}



OPERATIONS_KEY: str = 'operations'
FUNCTIONS: dict[str, Callable[[dict, Any, Any], dict]] = {
    "details": func_1_image_details,
    "rotate": func_2_image_rotate,
    "resize": func_3_image_resize,
    "grayscale": func_4_image_grayscale,
    "brightness": func_5_image_brightness,
    "transform": func_6_image_transform
}

def handle_request(event, context = None):
    """
    {
        'operations': [ ["<operation name>": { args } ] ]
    }
    """
    validate_message: str = validate_event(event, BUCKET_KEY, FILE_NAME_KEY, OPERATIONS_KEY)
    if validate_message:
        return {ERROR_KEY: validate_message}

    try:
        bucket_name: str = str(event[BUCKET_KEY])
        file_name: str = str(event[FILE_NAME_KEY])
        
        image: Image = get_image_from_s3(bucket_name, file_name)
        
        operations: list[list[str, dict]] = event[OPERATIONS_KEY]
        operation_outputs: list[dict[str, Any]] = []
        for i in range(len(operations)):
            operation_name: str = safe_list_access(operations[i], 0, '')
            operation_args: dict | None = safe_list_access(operations[i], 1)

            if operation_name not in FUNCTIONS:
                print(f"Pipeline error: invalid operation name at index {i}: {operation_name}")
                continue

            if operation_args == None:
                print(f"Pipeline error: Error retrieving input argument at index {i}")
                continue

            print(f"Executing function {i}: {operation_name}")
            operation_function = FUNCTIONS[operation_name]
            response_object = operation_function(operation_args, context, image)
            
            if ERROR_KEY in response_object:
                temp = operation_args.copy()
                temp.pop(IMAGE_FILE_KEY, None)
                print(f"Pipeline error: Error executing function at index {i} with args {temp}")
                print(response_object[ERROR_KEY])
                
                
            image = response_object[IMAGE_FILE_KEY] if IMAGE_FILE_KEY in response_object else image
            
            appended_output = response_object.copy()
            appended_output.pop(IMAGE_FILE_KEY, None)
            # print(list(appended_output.keys()))
            operation_outputs.append(appended_output)
        
        
        successful_write_to_s3: bool = save_image_to_s3(bucket_name, "batch_" + file_name, image, "png")
        if not successful_write_to_s3:
            raise RuntimeError("Could not write image to S3.")

        return {
            'operation_outputs': operation_outputs
        }
                

    except Exception as e:
        return {ERROR_KEY: str(e)}




def safe_list_access(list: list, index: int, fallback = None):
    try:
        return list[index]
    except:
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