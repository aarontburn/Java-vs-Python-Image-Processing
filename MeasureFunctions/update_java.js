/**
 *  TCSS 462 Image Transformation
 *  Group 7
 * 
 *  This file automatically updates all AWS Python Lambda Functions.
 *      To run, cd into the parent directory and run 'npm run python'
 * 
 *  This was only tested on a Windows machine.
 * 
 *  @author Aaron Burnham
 */


const { execSync } = require('child_process');

// Change this if you wanna see the results of running the CMD commands
const DEBUG_OUTPUT = false

const JAVA_LAMBDA_NAMES = [
    "image_batch_java",
    "image_details_java",
    "image_rotation_java",
    "image_resize_java",
    "image_brightness_java",
    "image_grayscale_java",
    "image_transform_java"
]

const jarName = "lambda_test-1.0-SNAPSHOT.jar"

const command = `aws lambda update-function-code --function-name <name> --zip-file "fileb://../ImageTransformationJava/target/${jarName}`

console.log("Rebuilding Java Project")
execSync("cd ../ImageTransformationJava/ && mvn clean -f pom.xml && mvn verify -f pom.xml", DEBUG_OUTPUT ? { stdio: 'inherit' } : undefined)

console.log("Uploading code to AWS")
for (const lambdaName of JAVA_LAMBDA_NAMES) {
    execSync(command.replace("<name>", lambdaName), DEBUG_OUTPUT ? { stdio: 'inherit' } : undefined)
    console.log("Finished updating " + lambdaName)
}
