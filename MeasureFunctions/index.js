const BATCH_FUNCTION_URL = "";
const FUNCTION_1_IMAGE_DETAILS_URL = "https://851eqoqxj4.execute-api.us-east-1.amazonaws.com/image_transformation_details";
const FUNCTION_2_IMAGE_ROTATE_URL = "https://4w90llitq9.execute-api.us-east-1.amazonaws.com/image_transformation_rotate";
const FUNCTION_3_IMAGE_RESIZE_URL = "";
const FUNCTION_4_IMAGE_GRAYSCALE_URL = "";
const FUNCTION_5_IMAGE_BRIGHTNESS_URL = "";
const FUNCTION_6_IMAGE_TRANSFORM_URL = "";


REQUEST_BODY = {
    "bucketname": "imagetransformation462",
    "rotation_angle": 90,
    "filename": "github-logo.png"
}


async function testFunction2() {
    fetch(FUNCTION_2_IMAGE_ROTATE_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" }, // Set content type
        body: JSON.stringify(REQUEST_BODY), // Serialize the body
    })
    .then(response => response.json())
    .then(console.log)
    .catch(console.error);
}

testFunction2();
