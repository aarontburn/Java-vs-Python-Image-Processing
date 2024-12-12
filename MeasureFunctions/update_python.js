/**
 *  TCSS 462 Image Transformation
 *  Group 7
 * 
 *  This file automatically updates all AWS Java Lambda Functions.
 *      To run, cd into the parent directory and run 'npm run java'
 * 
 *  This was only tested on a Windows machine.
 * 
 *  @author Aaron Burnham
 */

const fs = require('fs');
const path = require("path")
const archiver = require('archiver')("zip");
const { execSync } = require('child_process');

// Change this if you wanna see the results of running the CMD commands
const DEBUG_OUTPUT = false

const PYTHON_LAMBDA_NAMES = [
    "image_batch",
    "image_details",
    "image_rotate",
    "image_resize",
    "image_brightness",
    "image_grayscale",
    "image_transform"
];

(async () => {
    const command = `aws lambda update-function-code --function-name <name> --zip-file "fileb://src.zip"`;

    console.log("Archiving Python code to src.zip");
    const pythonPath = path.join(__dirname, "../", "ImageTransformationPython", "src");
    const outputStream = fs.createWriteStream("src.zip");
    archiver.pipe(outputStream);
    archiver.directory(pythonPath, false);
    await archiver.finalize();
    await new Promise(resolve => setTimeout(resolve, 1000));

    console.log("Uploading code to AWS");

    for (const lambdaName of PYTHON_LAMBDA_NAMES) {
        execSync(command.replace("<name>", lambdaName), DEBUG_OUTPUT ? { stdio: 'inherit' } : undefined);
        console.log("Finished updating " + lambdaName);
    };

})();






