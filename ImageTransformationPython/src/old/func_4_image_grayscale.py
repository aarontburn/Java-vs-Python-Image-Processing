import base64
import io
import os
from PIL import Image

IMAGE_FILE_KEY = 'image_file'
ERROR_KEY = 'error'

def handle_request(event, context = None):
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
        print(f"Decoded image data length: {len(image_data)} bytes")

        # Try to open the image
        with Image.open(io.BytesIO(image_data)) as img:
            print(f"Image opened successfully: {img.format}, {img.size}")

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


# Testing code with debugging
if __name__ == '__main__':
    print("\nPWD: " + os.getcwd())
    image_name = 'sample image.jpg'
    image_path = os.path.abspath('../sample images/' + image_name)

    print(f"Image path: {image_path}")

    if not os.path.exists(image_path):
        print(f"File not found: {image_path}")
    else:
        try:
            with open(image_path, 'rb') as f:
                # Check if the file is non-empty
                image_content = f.read()
                print(f"Image file size: {len(image_content)} bytes")

                if len(image_content) == 0:
                    print("Image file is empty.")
                else:
                    # Encode the image in Base64
                    image_base64 = base64.b64encode(image_content).decode('utf-8')
                    print(f"Base64-encoded image length: {len(image_base64)}")

                    # Create the event object
                    event_obj = {
                        IMAGE_FILE_KEY: image_base64
                    }

                    # Call the handle_request function
                    result = handle_request(event_obj)

                    # Save the grayscale image to a file for verification
                    if IMAGE_FILE_KEY in result:
                        output_file = 'grayscale_imagePy.png'
                        with open(output_file, 'wb') as out_f:
                            out_f.write(base64.b64decode(result[IMAGE_FILE_KEY]))
                        print(f"Grayscale image saved as {output_file}")
        except Exception as e:
            print(f"Error reading file: {str(e)}")