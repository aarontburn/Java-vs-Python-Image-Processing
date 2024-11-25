import json
import base64
from PIL import Image
import io

def lambda_handler(event, context):
    try:
        # Extract inputs
        image_base64 = event.get('image_file')
        target_width = event.get('target_width')
        target_height = event.get('target_height')

        # Validate inputs
        if not image_base64 or not target_width or not target_height:
            return {"statusCode": 400, "body": "Missing required inputs: image_file, target_width, or target_height."}

        target_width = int(target_width)
        target_height = int(target_height)

        if target_width <= 0 or target_height <= 0:
            return {"statusCode": 400, "body": "Target dimensions must be positive integers."}

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
            "statusCode": 200,
            "body": json.dumps({
                "resized_image": resized_image_base64,
                "message": "Image resized successfully.",
                "target_width": target_width,
                "target_height": target_height
            })
        }
    except Exception as e:
        return {"statusCode": 500, "body": f"Error: {str(e)}"}


# Local testing code
if __name__ == '__main__':
    import os
    print("\nPWD: " + os.getcwd())
    image_name = 'github-logo.png'

    with open('../sample images/' + image_name, 'rb') as f:
        event_obj = {
            'image_file': base64.b64encode(f.read()).decode('utf-8'),
            'target_width': 200,  # Specify desired width
            'target_height': 200  # Specify desired height
        }
        print(handle_request(event_obj))
