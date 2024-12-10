"""
This file contains helper methods that are used by multiple functions.
"""

import boto3
from io import BytesIO
from PIL.Image import Image as ImageType
from PIL import Image
import utils_constants as constants
from utils_custom_types import AWSRequestObject, AWSFunctionOutput
from time import time


def validate_event(event: AWSRequestObject, *keys: str) -> None | str:
    """
    Validates an event dictionary. If the event is valid, returns None.
    Otherwise, return an error string containing what keys are missing.
    """
    out: str = ''
    for key in keys:
        if key not in event:
            out += key + ", "

    if len(out) == 0:
        return None
    return "Missing request parameters: " + out[:-2]


def estimate_cost(run_time: float) -> float:
    memory_size_gb: float = 0.512  # Lambda memory size in GB
    price_per_gb_second: float = 0.00001667  # Pricing for Lambda
    return (run_time / 1000.0) * memory_size_gb * price_per_gb_second


def get_image_from_s3_and_record_time(bucket_name: str,
                                      file_name: str,
                                      output_dict: AWSFunctionOutput,
                                      region_name: str = "us-east-1") -> ImageType | None:
    """
    Retrieves an image from S3 from a provided bucket name and file name.
    """
    try:
        s3_start_time: float = int(round(time() * 1000))

        s3 = boto3.resource('s3', region_name)
        obj = s3.Bucket(bucket_name).Object(file_name)
        file_stream: BytesIO = obj.get()["Body"]
        image: ImageType = Image.open(file_stream)

        output_dict[constants.NETWORK_LATENCY_KEY] = int(round(time() * 1000)) - s3_start_time
        return image

    except Exception:
        output_dict[constants.NETWORK_LATENCY_KEY] = -1
        return None


def save_image_to_s3(bucket_name: str,
                     file_name: str,
                     image: ImageType,
                     file_format: str = "png",
                     region_name: str = "us-east-1") -> bool:
    """
    Saves an image to a given S3 storage bucket.
    """
    print(f"Attempting to write to s3 with arguments:")
    print(f"\t bucket_name: {bucket_name}")
    print(f"\t file_name: {file_name}")
    print(f"\t file_format: {file_format}")
    print(f"\t region_name: {region_name}")
    if (file_format.lower() == "jpg"):
        file_format = "jpeg"
    
    try:
        s3 = boto3.resource('s3', region_name)
        obj = s3.Bucket(bucket_name).Object(file_name)
        file_stream: BytesIO = BytesIO()
        image.save(file_stream, format=file_format)
        obj.put(Body=file_stream.getvalue())
        return True
    except Exception as e:
        print(e)
    return False


def get_downloadable_image_url(bucket_name: str, file_name: str) -> str:
    """
    Returns a url to download an image in a provided S3 bucket.
    """
    try:
        s3_client = boto3.client('s3')
        return s3_client.generate_presigned_url(
            'get_object',
            Params={'Bucket': bucket_name, 'Key': file_name},
            ExpiresIn=constants.IMAGE_URL_EXPIRATION_SECONDS)
    except Exception as e:
        print("Error getting image URL: " + str(e))

    return ''


def add_image_url_to_dict(output_dict: AWSFunctionOutput, bucket_name: str, file_name: str):
    output_dict[constants.IMAGE_URL_KEY] = get_downloadable_image_url(bucket_name, file_name)
    output_dict[constants.IMAGE_URL_EXPIRES_IN_KEY] = constants.IMAGE_URL_EXPIRATION_SECONDS


def get_file_extension(file_name: str):
    return file_name.split(".")[1].lower()


def get_file_name(file_name: str):
    return file_name.split('.')[0]
