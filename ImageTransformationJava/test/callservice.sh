#!/bin/bash
# JSON object to pass to Lambda Function
json={"\"bucketname\"":"\"imagetransformation462\"","\"rotation_angle\"":90,"\"filename\"":"\"sample image.jpg\""}
echo "Invoking Lambda function using AWS CLI (Boto3)"
time output=`aws lambda invoke --invocation-type RequestResponse --cli-binary-format raw-in-base64-out --function-name image_rotate --region us-east-1 --payload $json /dev/stdout | head -n 1 | head -c -2 ; echo`
echo ""
echo "JSON RESULT:"
echo $output | jq
echo ""
