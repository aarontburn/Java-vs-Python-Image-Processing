import base64
import os
from PIL import Image, ImageEnhance, UnidentifiedImageError
import io
from typing import Callable, Any

IMAGE_FILE_KEY: str = 'image_file'
ERROR_KEY: str = 'error'

def func_1_image_details(event, context = None):
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
    if IMAGE_FILE_KEY not in event:
        return {ERROR_KEY: "Missing key-value pair associated with " + IMAGE_FILE_KEY }


    try:
        with Image.open(io.BytesIO(base64.b64decode(event[IMAGE_FILE_KEY]))) as img:
            return {
                'region': os.environ['AWS_REGION'] if 'AWS_REGION' in os.environ else 'NO_REGION_DATA',
                'height': img.height,
                'width': img.width,
                'mode': img.mode,
                'has_transparency_data': 1 if img.has_transparency_data else 0
            }
    except Exception as e:
        return {ERROR_KEY: str(e)}

    # This might be unreachable
    return {ERROR_KEY: "Unknown error processing file."}



ROTATION_ANGLE_KEY = 'rotation_angle'
def func_2_image_rotate(event, context = None):
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
    if IMAGE_FILE_KEY not in event or ROTATION_ANGLE_KEY not in event:
        return {ERROR_KEY: f"Missing key-value pair associated with '{IMAGE_FILE_KEY}' or '{ROTATION_ANGLE_KEY}'"}

    try:
        # Decode the Base64-encoded image
        image_data = base64.b64decode(event[IMAGE_FILE_KEY])
        rotation_angle = event[ROTATION_ANGLE_KEY]

        if rotation_angle not in [90, 180, 270]:
            return {ERROR_KEY: "Invalid rotation angle. Only 90, 180, or 270 degrees are supported."}

        with Image.open(io.BytesIO(image_data)) as img:
            # Save original dimensions
            original_width, original_height = img.width, img.height

            # Perform the rotation
            rotated_img = img.rotate(-rotation_angle, expand=True)

            # Save rotated image to Base64
            buffer = io.BytesIO()
            rotated_img.save(buffer, format="PNG")
            rotated_image_base64 = base64.b64encode(buffer.getvalue()).decode('utf-8')

            # Save metadata and response
            return {
                IMAGE_FILE_KEY: rotated_image_base64,
                'original_width': original_width,
                'original_height': original_height,
                'rotated_width': rotated_img.width,
                'rotated_height': rotated_img.height,
                'rotation_angle': rotation_angle,
            }

    except Exception as e:
        return {ERROR_KEY: str(e)}

    # This might be unreachable
    return {ERROR_KEY: "Unknown error processing file."}


TARGET_WIDTH_KEY: str = 'target_width'
TARGET_HEIGHT_KEY: str = 'target_height'
def func_3_image_resize(event, context = None):
    try:
        # Extract inputs
        image_base64 = event.get(IMAGE_FILE_KEY)
        target_width = event.get(TARGET_WIDTH_KEY)
        target_height = event.get(TARGET_HEIGHT_KEY)

        # Validate inputs
        if not image_base64 or not target_width or not target_height:
            return {ERROR_KEY: "Missing required inputs: image_file, target_width, or target_height."}

        target_width = int(target_width)
        target_height = int(target_height)

        if target_width <= 0 or target_height <= 0:
            return {ERROR_KEY: f"Target dimensions must be positive integers."}

        # Decode and open the image
        image_bytes = base64.b64decode(image_base64)
        image = Image.open(io.BytesIO(image_bytes))

        # Resize the image
        resized_image = image.resize((target_width, target_height))

        # Convert back to Base64
        output_buffer = io.BytesIO()
        resized_image.save(output_buffer, format=image.format)
        resized_image_base64 = base64.b64encode(output_buffer.getvalue()).decode('utf-8')

        return {
            IMAGE_FILE_KEY: resized_image_base64,
            "message": "Image resized successfully.",
            TARGET_WIDTH_KEY: target_width,
            TARGET_HEIGHT_KEY: target_height
        }
    except Exception as e:
        return {ERROR_KEY: str(e)}
    

def func_4_image_grayscale(event, context = None):
    """
    Function: Image Grayscale Conversion

    Pass in a Base64-encoded image in a dictionary.
    Will return a dictionary with the grayscale image and metadata or an error message.
    """
    if IMAGE_FILE_KEY not in event:
        return {ERROR_KEY: f"Missing key-value pair associated with '{IMAGE_FILE_KEY}'"}

    try:
        # Decode the Base64-encoded image
        image_data = base64.b64decode(event[IMAGE_FILE_KEY])
        # print(f"Decoded image data length: {len(image_data)} bytes")

        # Try to open the image
        with Image.open(io.BytesIO(image_data)) as img:
            # print(f"Image opened successfully: {img.format}, {img.size}")

            # Save original dimensions
            original_width, original_height = img.width, img.height

            # Convert the image to grayscale
            grayscale_img = img.convert("L")

            # Save grayscale image to Base64
            buffer = io.BytesIO()
            grayscale_img.save(buffer, format="PNG")
            grayscale_image_base64 = base64.b64encode(buffer.getvalue()).decode('utf-8')

            # Save metadata and response
            return {
                IMAGE_FILE_KEY: grayscale_image_base64,
                'original_width': original_width,
                'original_height': original_height,
                'grayscale_width': grayscale_img.width,
                'grayscale_height': grayscale_img.height,
            }

    except Exception as e:
        return {ERROR_KEY: str(e)}

    return {ERROR_KEY: "Unknown error processing file."}


BRIGHTNESS_KEY: float = 'brightness_delta'
BRIGHTNESS_BOUNDS: tuple[int, int] = (0, 100) # I found that 100 is the maximum value before errors happen.
def func_5_image_brightness(event, context = None):
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

    # Input validations
    if IMAGE_FILE_KEY not in event:
        return {ERROR_KEY: "Missing key-value pair associated with " + IMAGE_FILE_KEY }
    
    brightness_delta: float = 1.
    if BRIGHTNESS_KEY not in event:
        return {ERROR_KEY: "Missing key-value pair associated with " + BRIGHTNESS_KEY }
    else:
        try:
            brightness_delta = float(event[BRIGHTNESS_KEY])
            if brightness_delta < BRIGHTNESS_BOUNDS[0] or brightness_delta > BRIGHTNESS_BOUNDS[1]:
                return {ERROR_KEY: f"'{BRIGHTNESS_KEY}' is out-of-bounds \
                        ({BRIGHTNESS_BOUNDS[0]}-{BRIGHTNESS_BOUNDS[1]}): {brightness_delta}"}

        except:
            return {ERROR_KEY: f"'{BRIGHTNESS_KEY}' is not parsable as a float."}

    # Modify and return image.
    try:
        with Image.open(io.BytesIO(base64.decodebytes(event[IMAGE_FILE_KEY]))) as img:
            modified_img: Image = ImageEnhance.Brightness(img).enhance(brightness_delta)
            
            buffer: io.BytesIO = io.BytesIO()
            modified_img.save(buffer, format="PNG")
            encoded_image: bytes = base64.b64encode(buffer.getvalue()).decode('utf-8')

            return {
                IMAGE_FILE_KEY: encoded_image,
                "args": {BRIGHTNESS_KEY: event[BRIGHTNESS_KEY]},
            }


    except Exception as e:
        return {ERROR_KEY: str(e)}
    
SUPPORTED_FORMATS = {"JPEG", "PNG", "BMP", "GIF", "TIFF"}
def func_6_image_transform(event, context = None):
    try:
        # Extract inputs
        image_base64 = event.get(IMAGE_FILE_KEY)
        output_format = event.get('output_format', 'JPEG').upper()  # Default to JPEG
        compress_quality = event.get('compress_quality', 85)  # Default to 85% quality (for lossy formats like JPEG)
        preserve_metadata = event.get('preserve_metadata', True)  # Default to preserving metadata

        # Validate inputs
        if not image_base64:
            return {ERROR_KEY: "Missing required input: " + IMAGE_FILE_KEY}
        if output_format not in SUPPORTED_FORMATS:
            return {ERROR_KEY: f"Unsupported output format: {output_format}. Supported formats: {', '.join(SUPPORTED_FORMATS)}"}
        if not (1 <= compress_quality <= 100):
            return {ERROR_KEY: "Compression quality must be between 1 and 100."}

        # Decode the Base64 image
        try:
            image_bytes = base64.b64decode(image_base64)
            image = Image.open(io.BytesIO(image_bytes))
        except (base64.binascii.Error, UnidentifiedImageError) as e:
            return {ERROR_KEY: f"Invalid image data: {str(e)}"}

        # Convert image to the desired format
        output_buffer = io.BytesIO()
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

        # Encode the finalized image to Base64
        finalized_image_base64 = base64.b64encode(output_buffer.getvalue()).decode('utf-8')

        # Prepare response
        return {
            IMAGE_FILE_KEY: finalized_image_base64,
            "output_format": output_format,
            "message": "Image finalized successfully."
        }

    except Exception as e:
        return {ERROR_KEY: f"Unexpected error: {str(e)}"}



OPERATIONS_KEY: str = 'operations'
FUNCTIONS: dict[str, Callable[[dict, Any], dict]] = {
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
        'image_file': The image encoded in base64.
        'operations': [ ["<operation name>": { args } ] ]
    }
    """
    if IMAGE_FILE_KEY not in event:
        return {ERROR_KEY: "Missing key-value pair associated with " + IMAGE_FILE_KEY}

    if OPERATIONS_KEY not in event:
        return {ERROR_KEY: "Missing key-value pair associated with " + OPERATIONS_KEY}
    
    try:
        
        img_bytes = event[IMAGE_FILE_KEY]
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
            operation_args[IMAGE_FILE_KEY] = img_bytes # Replace the argument image bytes with the modified one
            response_object = operation_function(operation_args, context)
            
            if ERROR_KEY in response_object:
                temp = operation_args.copy()
                temp.pop(IMAGE_FILE_KEY, None)
                print(f"Pipeline error: Error executing function at index {i} with args {temp}")
                print(response_object[ERROR_KEY])
                
                
            img_bytes = response_object[IMAGE_FILE_KEY] if IMAGE_FILE_KEY in response_object else img_bytes
            
            appended_output = response_object.copy()
            appended_output.pop(IMAGE_FILE_KEY, None)
            # print(list(appended_output.keys()))
            operation_outputs.append(appended_output)
        
        return {
            IMAGE_FILE_KEY: img_bytes,
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