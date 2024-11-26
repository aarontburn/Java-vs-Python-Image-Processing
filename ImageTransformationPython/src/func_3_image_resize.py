import json
import base64
from PIL import Image
import io


IMAGE_FILE_KEY: str = 'image_file'
WIDTH_KEY: str = 'target_width'
HEIGHT_KEY: str = 'target_height'
ERROR_KEY: str = 'error'


def handle_request(event, context = None):
    try:
        # Extract inputs
        image_base64 = event.get(IMAGE_FILE_KEY)
        target_width = event.get(WIDTH_KEY)
        target_height = event.get(HEIGHT_KEY)

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
            WIDTH_KEY: target_width,
            HEIGHT_KEY: target_height
        }
    except Exception as e:
        return {ERROR_KEY: str(e)}


# Local testing code
if __name__ == '__main__':
    import os
    print("\nPWD: " + os.getcwd())
    image_name = 'sample image.jpg'

    with open('../sample images/' + image_name, 'rb') as f:
        event_obj = {
            IMAGE_FILE_KEY: base64.b64encode(f.read()).decode('utf-8'),
            WIDTH_KEY: 200,  # Specify desired width
            HEIGHT_KEY: 200  # Specify desired height
        }
        
        response_body = handle_request(event_obj)
        with open('test output.jpg', "wb") as w:
            w.write(base64.b64decode(response_body[IMAGE_FILE_KEY]))