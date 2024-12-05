import base64
import os
import io

from PIL import Image, ImageEnhance

ERROR_KEY: str = 'error'
IMAGE_FILE_KEY: str = 'image_file'
BRIGHTNESS_KEY: float = 'brightness_delta'
BRIGHTNESS_BOUNDS: tuple[int, int] = (0, 100) # I found that 100 is the maximum value before errors happen.

def handle_request(event, context = None):
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


# Some testing code.
if __name__ == '__main__':
    print("\nPWD: " + os.getcwd())
    image_name = 'sample image 2.jpeg'

    for i in range(102): # 102 since 101 will fail
        with open('./sample images/' + image_name, 'rb') as f:
            event_obj = {IMAGE_FILE_KEY: base64.b64encode(f.read()), BRIGHTNESS_KEY: i}
            with open('test output.jpg', "wb") as w:
                try:
                    w.write(base64.b64decode(handle_request(event_obj)[IMAGE_FILE_KEY]))
                    print("Success", i)
                except Exception as e:
                    print("\tError", i)


    # with open('./sample images/' + image_name, 'rb') as f:

    #     event_obj = {IMAGE_FILE_KEY: base64.b64encode(f.read()), BRIGHTNESS_KEY: 1}

    #     with open('test output.jpg', "wb") as w:
    #         w.write(base64.b64decode(handle_request(event_obj)[IMAGE_FILE_KEY]))