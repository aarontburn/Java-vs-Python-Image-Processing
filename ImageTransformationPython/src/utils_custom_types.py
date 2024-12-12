"""
TCSS 462 Image Transformation
Group 7

This file holds custom typings.
"""

from typing import Callable, Any
from PIL.Image import Image

AWSFunctionOutput = dict[str, Any]
AWSRequestObject = Any
AWSContextObject = Any
ImageType = Image
OptionalImage = ImageType | None
AWSFunction = Callable[[AWSRequestObject, AWSContextObject, OptionalImage], AWSFunctionOutput]

