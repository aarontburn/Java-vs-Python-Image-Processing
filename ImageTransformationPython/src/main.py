from custom_types import AWSContextObject, AWSFunctionOutput, AWSRequestObject, AWSFunction
import helpers
from time import time
import os
import ImageTransformationPython.src.constants as constants
from functions.func_1_image_details import handle_request as f1
from functions.func_2_image_rotate import handle_request as f2
from functions.func_3_image_resize import handle_request as f3
from functions.func_4_image_grayscale import handle_request as f4
from functions.func_5_image_brightness import handle_request as f5
from functions.func_6_image_transform import handle_request as f6
from functions.batch_image_processing import handle_request as batch


def image_details(event: AWSRequestObject, context: AWSContextObject) -> AWSFunctionOutput:
    return _handle_call(event, context, f1)


def image_rotate(event: AWSRequestObject, context: AWSContextObject) -> AWSFunctionOutput:
    return _handle_call(event, context, f2)


def image_resize(event: AWSRequestObject, context: AWSContextObject) -> AWSFunctionOutput:
    return _handle_call(event, context, f3)


def image_grayscale(event: AWSRequestObject, context: AWSContextObject) -> AWSFunctionOutput:
    return _handle_call(event, context, f4)


def image_brightness(event: AWSRequestObject, context: AWSContextObject) -> AWSFunctionOutput:
    return _handle_call(event, context, f5)


def image_transform(event: AWSRequestObject, context: AWSContextObject) -> AWSFunctionOutput:
    return _handle_call(event, context, f6)


def image_batch(event: AWSRequestObject, context: AWSContextObject) -> AWSFunctionOutput:
    return _handle_call(event, context, batch)

_cold_start: bool = True

def _handle_call(event: AWSRequestObject, 
                 context: AWSContextObject,
                 function: AWSFunction) -> AWSFunctionOutput:
    global _cold_start; 
    local_cold_start: bool = _cold_start; 
    _cold_start = False

    output_dict: AWSFunctionOutput = {}
    
    function_start_time: float = time()
    function(event, context, output_dict)
    function_run_time: float = time() - function_start_time

    # Attach common metrics
    output_dict['region'] = os.environ['AWS_REGION'] if 'AWS_REGION' in os.environ else 'NO_REGION_DATA'
    output_dict[constants.COLD_START_KEY] = 1 if local_cold_start else 0
    output_dict[constants.FUNCTION_RUN_TIME_KEY] = function_run_time
    output_dict[constants.ESTIMATED_COST_KEY] = helpers.estimate_cost(function_run_time)
    output_dict["language"] = "Python"
    output_dict["version"] = 0.5 # ?

    return output_dict
