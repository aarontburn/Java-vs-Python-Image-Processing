"""
Main entry point.
"""

from utils_custom_types import AWSContextObject, AWSFunctionOutput, AWSRequestObject, AWSFunction
import utils_constants as constants
import utils_helpers as helpers
from func_1_image_details import handle_request as f1
from func_2_image_rotate import handle_request as f2
from func_3_image_resize import handle_request as f3
from func_4_image_grayscale import handle_request as f4
from func_5_image_brightness import handle_request as f5
from func_6_image_transform import handle_request as f6
from batch_image_processing import handle_request as batch
from SAAF import Inspector


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



def _handle_call(event: AWSRequestObject,
                 context: AWSContextObject,
                 function: AWSFunction) -> AWSFunctionOutput:


    return_only_metrics: bool = bool(event[constants.ONLY_METRICS_KEY]) if constants.ONLY_METRICS_KEY in event else False 

    round_trip_start: int = helpers.current_time_millis()

    function_output: AWSFunctionOutput = function(event, context)

    inspector: Inspector = Inspector(return_only_metrics) 
    inspector.addAttribute(constants.NETWORK_LATENCY_KEY, function_output.get(constants.NETWORK_LATENCY_KEY))
    function_output.pop(constants.NETWORK_LATENCY_KEY)
    
    inspector.addAttribute("function_output", function_output)

    inspector.inspectMetrics(round_trip_start)

    return inspector.finish()
