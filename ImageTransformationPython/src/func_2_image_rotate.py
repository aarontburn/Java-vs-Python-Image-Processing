import base64
import io
import os
from PIL import Image

IMAGE_FILE_KEY = 'image_file'
ROTATION_ANGLE_KEY = 'rotation_angle'
ERROR_KEY = 'error'

def handle_request(event, context=None):
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
                # 'rotated_image': rotated_image_base64,
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


# Some testing code.
if __name__ == '__main__':
    print("\nPWD: " + os.getcwd())
    image_name = 'github-logo.png'
    with open('../../sample images/' + image_name, 'rb') as f:
        event_obj = {
            IMAGE_FILE_KEY: base64.b64encode(f.read()).decode('utf-8'),  # Ensure the Base64 string is UTF-8 encoded
            ROTATION_ANGLE_KEY: 90  # Set the rotation angle here
        }
        result = handle_request(event_obj)
        print(result)

        # Save the rotated image to a file for verification
        if 'rotated_image' in result:
            output_file = 'rotated_image.png'
            with open(output_file, 'wb') as out_f:
                out_f.write(base64.b64decode(result['rotated_image']))
            print(f"Rotated image saved as {output_file}")
