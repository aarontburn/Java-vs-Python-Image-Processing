const PYTHON_BATCH_FUNCTION_URL = "https://alq28v8tc5.execute-api.us-east-1.amazonaws.com/image_transformation_batch";
const PYTHON_FUNCTION_1_IMAGE_DETAILS_URL = "https://851eqoqxj4.execute-api.us-east-1.amazonaws.com/image_transformation_details";
const PYTHON_FUNCTION_2_IMAGE_ROTATE_URL = "https://4w90llitq9.execute-api.us-east-1.amazonaws.com/image_transformation_rotate";
const PYTHON_FUNCTION_3_IMAGE_RESIZE_URL = "https://rxhhsgc28h.execute-api.us-east-1.amazonaws.com/image_transformation_resize";
const PYTHON_FUNCTION_4_IMAGE_GRAYSCALE_URL = "https://yg20xgk7ae.execute-api.us-east-1.amazonaws.com/image_transformation_grayscale";
const PYTHON_FUNCTION_5_IMAGE_BRIGHTNESS_URL = "https://j47ytbbtg1.execute-api.us-east-1.amazonaws.com/image_transformation_brightness";
const PYTHON_FUNCTION_6_IMAGE_TRANSFORM_URL = "https://n01so0hl66.execute-api.us-east-1.amazonaws.com/image_transformation_transform";


const JAVA_BATCH_FUNCTION_URL = "https://kllmszc7z5.execute-api.us-east-1.amazonaws.com/image_transformation_java_batch"
const JAVA_FUNCTION_1_IMAGE_DETAILS_URL = "https://cmc3y2rfbb.execute-api.us-east-1.amazonaws.com/image_transformation_java_details"
const JAVA_FUNCTION_2_IMAGE_ROTATE_URL = "https://jyoel0khkc.execute-api.us-east-1.amazonaws.com/image_transformation_java_rotate"
const JAVA_FUNCTION_3_IMAGE_RESIZE_URL = "https://nwq7okd0o2.execute-api.us-east-1.amazonaws.com/image_transformation_java_resize"
const JAVA_FUNCTION_4_IMAGE_GRAYSCALE_URL = "https://32jae2yac8.execute-api.us-east-1.amazonaws.com/image_transformation_java_grayscale"
const JAVA_FUNCTION_5_IMAGE_BRIGHTNESS_URL = "https://m6pjeyck6h.execute-api.us-east-1.amazonaws.com/image_transformation_java_brightness"
const JAVA_FUNCTION_6_IMAGE_TRANSFORM_URL = "https://iw3re9dw77.execute-api.us-east-1.amazonaws.com/image_transformation_java_transform"

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
    try {
        const response = await callFunction(functionUrl, requestBody)
        if (response["cold_start"] !== undefined) {
            console.log(`PASS: ${getFunctionName(functionUrl)}`)
            return true
        }

    } catch (err){
        console.error(err)
    }
    console.error(`FAIL: ${getFunctionName(functionUrl)}`)
    return false
}



async function runTests() {
    testFunction(PYTHON_FUNCTION_1_IMAGE_DETAILS_URL, {...getGenericBody()})
    testFunction(PYTHON_FUNCTION_2_IMAGE_ROTATE_URL, {...getGenericBody(), "rotation_angle": 90,})
    testFunction(PYTHON_FUNCTION_3_IMAGE_RESIZE_URL, {...getGenericBody(), "target_width": 250, "target_height": 500})
    testFunction(PYTHON_FUNCTION_4_IMAGE_GRAYSCALE_URL, {...getGenericBody()})
    testFunction(PYTHON_FUNCTION_5_IMAGE_BRIGHTNESS_URL, {...getGenericBody(), "brightness_delta": 90})
    testFunction(PYTHON_FUNCTION_6_IMAGE_TRANSFORM_URL, {...getGenericBody()})
}

runTests()