const fs = require('fs');
const path = require("path")
const archiver = require('archiver')("zip");


console.log("PWD: " + __dirname)


const PWD = __dirname;
const pythonPath = path.join(__dirname, "../", "ImageTransformationPython", "src")

// ()

const outputStream = fs.createWriteStream("src.zip")
archiver.pipe(outputStream)

archiver.directory(pythonPath, false)

archiver.finalize();







