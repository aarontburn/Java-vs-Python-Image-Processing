import base64
import os
from PIL import Image
import io


def func_1_image_details(event, context = None):
    pass

def func_2_image_rotate(event, context = None):
    pass

def func_3_image_resize(event, context = None):
    pass

def func_4_image_grayscale(event, context = None):
    pass

def func_5_image_brightness(event, context = None):
    pass

def func_6_image_transform(event, context = None):
    pass


OPERATIONS_KEY: str = 'operations'
ERROR_KEY: str = 'error'
IMAGE_FILE_KEY: str = 'image_file'
FUNCTIONS = {
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

        for i in range(len(operations)):
            operation = operations[i]
            operation_name: str | None = safe_list_access(operation, 0, '')
            operation_args: dict | None = safe_list_access(operation, 1)

            if operation_name not in FUNCTIONS:
                print(f"Pipeline error: invalid operation name at index {i}: {operation_name}")
                continue

            if operation_args == None:
                print(f"Pipeline error: Error retrieving input argument at index {i}")
                continue

            operation_function = FUNCTIONS[operation_name]
            img_bytes = operation_function[IMAGE_FILE_KEY]

        return {
            IMAGE_FILE_KEY: img_bytes
        }
                

    except Exception as e:
        return {ERROR_KEY: str(e)}




def safe_list_access(list: list, index: int, fallback = None):
    try:
        return list[index]
    except:
        return fallback

# Some testing code.
if __name__ == '__main__':
    print("\nPWD: " + os.getcwd())
    image_name = 'github-logo.png'
    with open('../sample images/' + image_name, 'rb') as f:
        event_obj = {'image_file': base64.b64encode(f.read())}
        print(handle_request(event_obj))