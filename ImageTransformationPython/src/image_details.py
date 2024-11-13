import base64
import io
import os

from PIL import Image


def handle_request(event, context):
    try:
        with Image.open(io.BytesIO(base64.decodebytes(event['image_file']))) as img:
            return {
                'region': os.environ['AWS_REGION'],
                'height': img.height,
                'width': img.width,
                'mode': img.mode,
                'dpi': img.info.get("dpi", (-1, -1)),
                'has_transparency_data': img.has_transparency_data  # This could be a 0 or 1 for cross compat i think
            }
    except Exception as e:
        return {'error': str(e)}

    # This might be unreachable
    return {'error': "Error processing file"}


if __name__ == '__main__':
    image_path = 'sample image 2.jpeg'
    with open(image_path, 'rb') as f:
        event_obj = {'image_file': base64.b64encode(f.read())}
        print(handle_request(event_obj, None))
