"""
Main entry point.
"""

from utils_custom_types \
    import AWSContextObject, AWSFunctionOutput, AWSRequestObject, AWSFunction
import utils_constants as constants
from func_1_image_details import handle_request as f1
from func_2_image_rotate import handle_request as f2
from func_3_image_resize import handle_request as f3
from func_4_image_grayscale import handle_request as f4
from func_5_image_brightness import handle_request as f5
from func_6_image_transform import handle_request as f6
from batch_image_processing import handle_request as batch
from SAAF import Inspector
from time import time


def image_details(event: AWSRequestObject, context: AWSContextObject) -> AWSFunctionOutput:
    return __handle_call(event, context, f1)


def image_rotate(event: AWSRequestObject, context: AWSContextObject) -> AWSFunctionOutput:
    return __handle_call(event, context, f2)


def image_resize(event: AWSRequestObject, context: AWSContextObject) -> AWSFunctionOutput:
    return __handle_call(event, context, f3)


def image_grayscale(event: AWSRequestObject, context: AWSContextObject) -> AWSFunctionOutput:
    return __handle_call(event, context, f4)


def image_brightness(event: AWSRequestObject, context: AWSContextObject) -> AWSFunctionOutput:
    return __handle_call(event, context, f5)


def image_transform(event: AWSRequestObject, context: AWSContextObject) -> AWSFunctionOutput:
    return __handle_call(event, context, f6)


def image_batch(event: AWSRequestObject, context: AWSContextObject) -> AWSFunctionOutput:
    return __handle_call(event, context, batch)


def __handle_call(event: AWSRequestObject,
                  context: AWSContextObject,
                  function: AWSFunction) -> AWSFunctionOutput:

    # To return only metrics, add "return_only_metrics": true to request body. Defaults to false.
    return_only_metrics: bool = \
        bool(event[constants.ONLY_METRICS_KEY]) \
        if constants.ONLY_METRICS_KEY in event \
        else False

    # Use an Inspector for metrics collection
    inspector: Inspector = Inspector(return_only_metrics)

    # To get a download URL, add "get_download": true to request body. Defaults to false.
    get_download_url: bool = \
        bool(event[constants.GET_DOWNLOAD_KEY]) \
        if constants.GET_DOWNLOAD_KEY in event \
        else False

    event[constants.GET_DOWNLOAD_KEY] = get_download_url

    # Record function start time
    round_trip_start: int = int(round(time() * 1000))

    # Execute function
    function_output: AWSFunctionOutput = function(event, context)

    # Move network latency to top-level inspector and remove from function output
    inspector.addAttribute(
        constants.NETWORK_LATENCY_KEY,
        function_output.get(constants.NETWORK_LATENCY_KEY, -1))
    function_output.pop(constants.NETWORK_LATENCY_KEY, -1)

    # Append function output to inspector
    inspector.addAttribute("function_output", function_output)

    # Inspect metrics
    inspector.inspectMetrics(round_trip_start)

    return inspector.finish()
