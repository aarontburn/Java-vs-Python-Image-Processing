import base64
import os
import io

from PIL import Image, ImageEnhance

ERROR_KEY: str = 'error'
IMAGE_FILE_KEY: str = 'image_file'
BRIGHTNESS_KEY: float = 'brightness_delta'
BRIGHTNESS_BOUNDS: tuple[int, int] = (0, 5) # Maximum value is 5 times the original brightness? Can be changed.

def handle_request(event, context = None):
    if IMAGE_FILE_KEY not in event:
        return {ERROR_KEY: "Missing key-value pair associated with " + IMAGE_FILE_KEY }
    
    brightness_delta = 1
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


    try:
        with Image.open(io.BytesIO(base64.decodebytes(event[IMAGE_FILE_KEY]))) as img:
            enhancer = ImageEnhance.Brightness(img)
            img = enhancer.enhance(brightness_delta)

            

            return {
                "args": {BRIGHTNESS_KEY: event[BRIGHTNESS_KEY]},
                IMAGE_FILE_KEY: base64.encodebytes(img.tobytes())
            }



    except Exception as e:
        return {ERROR_KEY: str(e)}


# Some testing code.
if __name__ == '__main__':
    print("\nPWD: " + os.getcwd())
    image_name = 'sample image.jpg'
    with open('./sample images/' + image_name, 'rb') as f:
        event_obj = {IMAGE_FILE_KEY: base64.b64encode(f.read()), BRIGHTNESS_KEY: 1}
        print(handle_request(event_obj))


        with open('test output.jpg', "wb") as w:
            w.write(handle_request(event_obj)[IMAGE_FILE_KEY])