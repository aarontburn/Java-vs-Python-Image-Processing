import base64
import json
from PIL import Image, UnidentifiedImageError
import io

SUPPORTED_FORMATS = {"JPEG", "PNG", "BMP", "GIF", "TIFF"}

def lambda_handler(event, context):
    try:
        # Extract inputs
        image_base64 = event.get('image_file')
        output_format = event.get('output_format', 'JPEG').upper()  # Default to JPEG
        compress_quality = event.get('compress_quality', 85)  # Default to 85% quality (for lossy formats like JPEG)
        preserve_metadata = event.get('preserve_metadata', True)  # Default to preserving metadata

        # Validate inputs
        if not image_base64:
            return {"statusCode": 400, "body": "Missing required input: image_file"}
        if output_format not in SUPPORTED_FORMATS:
            return {"statusCode": 400, "body": f"Unsupported output format: {output_format}. Supported formats: {', '.join(SUPPORTED_FORMATS)}"}
        if not (1 <= compress_quality <= 100):
            return {"statusCode": 400, "body": "Compression quality must be between 1 and 100."}

        # Decode the Base64 image
        try:
            image_bytes = base64.b64decode(image_base64)
            image = Image.open(io.BytesIO(image_bytes))
        except (base64.binascii.Error, UnidentifiedImageError) as e:
            return {"statusCode": 400, "body": f"Invalid image data: {str(e)}"}

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
            return {"statusCode": 500, "body": f"Error finalizing image: {str(e)}"}

        # Encode the finalized image to Base64
        finalized_image_base64 = base64.b64encode(output_buffer.getvalue()).decode('utf-8')

        # Prepare response
        return {
            "statusCode": 200,
            "body": json.dumps({
                "finalized_image": finalized_image_base64,
                "output_format": output_format,
                "message": "Image finalized successfully."
            })
        }

    except Exception as e:
        return {"statusCode": 500, "body": f"Unexpected error: {str(e)}"}



# Some testing code.
if __name__ == '__main__':
    print("\nPWD: " + os.getcwd())
    image_name = 'github-logo.png'
    with open('../sample images/' + image_name, 'rb') as f:
        event_obj = {'image_file': base64.b64encode(f.read())}
        print(handle_request(event_obj))