from typing import Callable, Any
from PIL.Image import Image


# Custom typing
AWSFunctionOutput = dict[str, Any]
AWSRequestObject = Any
AWSContextObject = Any
ImageType = Image
OptionalImage = ImageType | None
AWSFunction = Callable[[AWSFunctionOutput, AWSRequestObject, AWSContextObject, OptionalImage], None]
