const PYTHON_BATCH_FUNCTION_URL = "https://der5m94jo5.execute-api.us-east-1.amazonaws.com/image_transform/python/batch";

const PYTHON_FUNCTION_1_IMAGE_DETAILS_URL = "https://der5m94jo5.execute-api.us-east-1.amazonaws.com/image_transform/python/details";
const PYTHON_FUNCTION_2_IMAGE_ROTATE_URL = "https://der5m94jo5.execute-api.us-east-1.amazonaws.com/image_transform/python/rotate";
const PYTHON_FUNCTION_3_IMAGE_RESIZE_URL = "https://der5m94jo5.execute-api.us-east-1.amazonaws.com/image_transform/python/resize";
const PYTHON_FUNCTION_4_IMAGE_GRAYSCALE_URL = "https://der5m94jo5.execute-api.us-east-1.amazonaws.com/image_transform/python/grayscale";
const PYTHON_FUNCTION_5_IMAGE_BRIGHTNESS_URL = "https://der5m94jo5.execute-api.us-east-1.amazonaws.com/image_transform/python/brightness";
const PYTHON_FUNCTION_6_IMAGE_TRANSFORM_URL = "https://der5m94jo5.execute-api.us-east-1.amazonaws.com/image_transform/python/transform";



const JAVA_BATCH_FUNCTION_URL = "https://der5m94jo5.execute-api.us-east-1.amazonaws.com/image_transform/java/batch"
const JAVA_FUNCTION_1_IMAGE_DETAILS_URL = "https://der5m94jo5.execute-api.us-east-1.amazonaws.com/image_transform/java/details";
const JAVA_FUNCTION_2_IMAGE_ROTATE_URL = "https://der5m94jo5.execute-api.us-east-1.amazonaws.com/image_transform/java/rotate";
const JAVA_FUNCTION_3_IMAGE_RESIZE_URL = "https://der5m94jo5.execute-api.us-east-1.amazonaws.com/image_transform/java/resize";
const JAVA_FUNCTION_4_IMAGE_GRAYSCALE_URL = "https://der5m94jo5.execute-api.us-east-1.amazonaws.com/image_transform/java/grayscale";
const JAVA_FUNCTION_5_IMAGE_BRIGHTNESS_URL = "https://der5m94jo5.execute-api.us-east-1.amazonaws.com/image_transform/java/brightness";
const JAVA_FUNCTION_6_IMAGE_TRANSFORM_URL = "https://der5m94jo5.execute-api.us-east-1.amazonaws.com/image_transform/java/transform";

const BUCKET_NAME = "imagetransformation462"

const getGenericBody = (fileName = "sample image.jpg") => {
    return { ...{ "bucketname": BUCKET_NAME, "filename": fileName } }
}

REQUEST_BODY = {
    "bucketname": "imagetransformation462",
    "rotation_angle": 90,
    "filename": "github-logo.png"
}



async function callFunction(functionUrl, requestBody) {
    return fetch(functionUrl, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(requestBody),
    }).then(r => r.json())
}

function getFunctionName(functionUrl) {
    return functionUrl.split("/").at(-1)
}


async function testFunction(functionUrl, requestBody) {
    let response = undefined;
    try {
        response = await callFunction(functionUrl, requestBody)
        if (response["cold_start"] !== undefined) {
            console.log(`PASS: ${getFunctionName(functionUrl)}`)
            console.log(response)
            return true
        }

    } catch (err) {
        console.error(err)
    }
    console.error(`FAIL: ${getFunctionName(functionUrl)}`)
    if (response) {
        console.log(response)
    }
    return false
}



async function runTests() {
    testFunction(PYTHON_FUNCTION_1_IMAGE_DETAILS_URL, {...getGenericBody()})
    testFunction(PYTHON_FUNCTION_2_IMAGE_ROTATE_URL, {...getGenericBody(), "rotation_angle": 90,})
    testFunction(PYTHON_FUNCTION_3_IMAGE_RESIZE_URL, {...getGenericBody(), "target_width": 250, "target_height": 500})
    testFunction(PYTHON_FUNCTION_4_IMAGE_GRAYSCALE_URL, {...getGenericBody()})
    testFunction(PYTHON_FUNCTION_5_IMAGE_BRIGHTNESS_URL, {...getGenericBody(), "brightness_delta": 90})
    testFunction(PYTHON_FUNCTION_6_IMAGE_TRANSFORM_URL, {...getGenericBody()})

    testFunction(JAVA_FUNCTION_1_IMAGE_DETAILS_URL, {...getGenericBody()})
    testFunction(JAVA_FUNCTION_2_IMAGE_ROTATE_URL, {...getGenericBody(), "rotation_angle": 90,})
    testFunction(JAVA_FUNCTION_3_IMAGE_RESIZE_URL, {...getGenericBody(), "target_width": 250, "target_height": 500})
    testFunction(JAVA_FUNCTION_4_IMAGE_GRAYSCALE_URL, {...getGenericBody()})
    testFunction(JAVA_FUNCTION_5_IMAGE_BRIGHTNESS_URL, {...getGenericBody(), "brightness_delta": 90})
    testFunction(JAVA_FUNCTION_6_IMAGE_TRANSFORM_URL, {...getGenericBody()})
}

runTests()