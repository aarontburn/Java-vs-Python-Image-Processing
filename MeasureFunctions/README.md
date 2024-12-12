# Function Validation and Faster Pushing
This package allows you to update all the AWS functions at once.


# This is Windows only, I think. 

## Prereqs:
1. Have the AWS CLI installed and configured with the credentials sent in the Discord server
2. (JAVA) Have Maven part of the environment variables (verify by typing mvn in the console)
3. Have a Node environment setup

# To update Python:
1. cd into the 'MeasureFunctions' directory
2. Into a command prompt window (powershell was giving me issues), do 'npm install' to install any necessary Javascript packages
3. Run 'npm run python' and it should zip up the appropriate files and upload them to AWS

# To update Java
1. cd into the 'MeasureFunctions' directory
2. Into a command prompt window (powershell was giving me issues), do 'npm install' to install any necessary Javascript packages
3. Run 'npm run java' and it should build the project and upload the jar file to AWS

# To update both:
1. cd into the 'MeasureFunctions' directory
2. Into a command prompt window (powershell was giving me issues), do 'npm install' to install any necessary Javascript packages
3. Run 'npm run updateall' and it should update both.
