#!/bin/bash

json=$(cat <<EOF
{
  "bucketname": "imagetransformation462",
  "rotation_angle": 90,
  "filename": "sample image.jpg"
}
EOF
)

echo "Invoking Lambda function using AWS CLI"
time output=$(aws lambda invoke \
  --invocation-type RequestResponse \
  --cli-binary-format raw-in-base64-out \
  --function-name image_rotate \
  --region us-east-1 \
  --payload "$json" \
  /dev/stdout)

# Print output
echo ""
echo "JSON RESULT:"
echo "$output" | jq
echo ""
