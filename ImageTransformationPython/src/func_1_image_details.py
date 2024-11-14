import base64
import io
import os

from PIL import Image


def handle_request(event, context = None):
    """
    Function #1: Image upload and validation
    
    Pass in a Base64 encoded image in a dictionary under key 'image_file'. Will return a
        dictionary following one of these schemas:
        
            <key>: <type> -> <description>
        Successful Response {
            'region': str -> I think when this is ran within lambda, this should be populated. Otherwise, return "NO_REGION_DATA".
            'height': int -> The height (in px i think) of the image.
            'width': str -> The width (in px i think) of the image.
            'mode': str -> The mode of the image. See https://pillow.readthedocs.io/en/stable/handbook/concepts.html
            'has_transparency_data: bool -> True if the image has transparency data, False otherwise.
        }
        
        Error Response {
            'error': str -> The error converted to a string form.
        }
    
    :param event: A dictionary containing at LEAST a key-value pair where the key is 'image_file' and the value is a Base64 encoded image
    :param context: AWS Lambda stuff. No clue what this is. For testing purposes, I've made this 'None' by default.
    :returns: A response body containing information about the image, or an error object otherwise.
    """
    try:
        with Image.open(io.BytesIO(base64.decodebytes(event['image_file']))) as img:
            return {
                'region': os.environ['AWS_REGION'] if 'AWS_REGION' in os.environ else 'NO_REGION_DATA',
                'height': img.height,
                'width': img.width,
                'mode': img.mode,
                'has_transparency_data': 1 if img.has_transparency_data else 0
            }
    except Exception as e:
        return {'error': str(e)}

    # This might be unreachable
    return {'error': "Unknown error processing file."}


# Some testing code.
if __name__ == '__main__':
    print("\nPWD: " + os.getcwd())
    image_name = 'sample image.jpg'
    with open('../sample images/' + image_name, 'rb') as f:
        event_obj = {'image_file': base64.b64encode(f.read())}
        print(handle_request(event_obj))
