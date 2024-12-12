const fs = require("fs");

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

// Change this for the number of times we are running each test
const NUMBER_OF_TESTS = 10
// const IMAGE_NAMES = ["small"]
const IMAGE_NAMES = ["small", "medium", "large"]


const getGenericBody = ({ fileName = "sample image.jpg", returnOnlyMetrics = true, getDownloadURL = false } = {}) => {
    return {
        ...{
            "bucketname": "imagetransformation462",
            "filename": fileName,
            "return_only_metrics": returnOnlyMetrics,
            "get_download": getDownloadURL
        }
    }
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


function getMetricsFromFunction(functionOutput) {
    const output = [[], []]
    for (const key in functionOutput) {
        if (key === "function_output") {
            if (functionOutput[key]["success"] !== undefined) {
                // console.log("Success")
            } else {
                console.error("\tERROR")
            }
            continue;
        }
        output[0].push(key);
        output[1].push(functionOutput[key]);
    }
    return output
}





async function runTestsFor(imageName, functionURL, requestBody) {
    console.log("Running tests for " + getFunctionName(functionURL))


    const csv = [];
    const javaOrPythonStr = functionURL.includes("java") ? 'java' : "python"
    const fileName = `${getFunctionName(functionURL)}_${imageName}_${javaOrPythonStr}`;

    for (let i = 0; i < NUMBER_OF_TESTS; i++) {
        const functionOutput = await callFunction(functionURL, { ...requestBody, filename: `${imageName}.jpg` });

        const metrics = getMetricsFromFunction(functionOutput);
        if (i === 0) {
            csv.push([fileName])
            csv.push(metrics[0]);
        }
        csv.push(metrics[1]);
    }
    csv.push([''])
    csv.push([''])
    csv.push([''])
    csv.push([''])
    csv.push([''])

    return csv
}


async function testAll() {

    for (const imageName of IMAGE_NAMES) {
        let csv = []
        const outputs = await Promise.all([
            runTestsFor(imageName, PYTHON_FUNCTION_1_IMAGE_DETAILS_URL, { ...getGenericBody() }),
            runTestsFor(imageName, PYTHON_FUNCTION_2_IMAGE_ROTATE_URL, { ...getGenericBody(), "rotation_angle": 90, }),
            runTestsFor(imageName, PYTHON_FUNCTION_3_IMAGE_RESIZE_URL, { ...getGenericBody(), "target_width": 250, "target_height": 500 }),
            runTestsFor(imageName, PYTHON_FUNCTION_4_IMAGE_GRAYSCALE_URL, { ...getGenericBody() }),
            runTestsFor(imageName, PYTHON_FUNCTION_5_IMAGE_BRIGHTNESS_URL, { ...getGenericBody(), "brightness_delta": 90 }),
            runTestsFor(imageName, PYTHON_FUNCTION_6_IMAGE_TRANSFORM_URL, { ...getGenericBody(), "target_format": "png" }),
            runTestsFor(imageName, PYTHON_BATCH_FUNCTION_URL, {
                ...getGenericBody(), "operations": [
                    ["details"],
                    ["rotate", { "rotation_angle": 90 }],
                    ["resize", { "target_height": 50, "target_width": 150 }],
                    ["grayscale"],
                    ["brightness", { "brightness_delta": 50 }],
                    ["transform", { "target_format": "png" }]
                ]
            }),

            runTestsFor(imageName, JAVA_FUNCTION_1_IMAGE_DETAILS_URL, { ...getGenericBody() }),
            runTestsFor(imageName, JAVA_FUNCTION_2_IMAGE_ROTATE_URL, { ...getGenericBody(), "rotation_angle": 90, }),
            runTestsFor(imageName, JAVA_FUNCTION_3_IMAGE_RESIZE_URL, { ...getGenericBody(), "target_width": 250, "target_height": 500 }),
            runTestsFor(imageName, JAVA_FUNCTION_4_IMAGE_GRAYSCALE_URL, { ...getGenericBody() }),
            runTestsFor(imageName, JAVA_FUNCTION_5_IMAGE_BRIGHTNESS_URL, { ...getGenericBody(), "brightness_delta": 90 }),
            runTestsFor(imageName, JAVA_FUNCTION_6_IMAGE_TRANSFORM_URL, { ...getGenericBody(), "target_format": "png" }),
            runTestsFor(imageName, JAVA_BATCH_FUNCTION_URL, {
                ...getGenericBody(), "operations": [
                    ["details"],
                    ["rotate", { "rotation_angle": 90 }],
                    ["resize", { "target_height": 50, "target_width": 150 }],
                    ["grayscale"],
                    ["brightness", { "brightness_delta": 50 }],
                    ["transform", { "target_format": "png" }]
                ]
            }),
        ]);

        for (const output of outputs) {
            csv = csv.concat(output)
        }
        arrayToCSV(`tests/${imageName}.csv`, csv);
    }


}


function arrayToCSV(outputName, rows) {
    const csvContent = rows.map(e => e.join(",")).join("\n");

    fs.writeFileSync(outputName, csvContent, "utf8")
}


testAll()