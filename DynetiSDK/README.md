# CatDogImageSDK

The CatDogImageSDK is a lightweight software development kit (SDK) designed to facilitate cat and dog image classification tasks within Android applications. It provides functionality to efficiently classify images as either containing a cat or a dog using TensorFlow Lite models.

## Features

- **Object Detection**: Detects cats and dogs within images with high accuracy.
- **Model Integration**: Seamlessly integrates TensorFlow Lite models for efficient classification.
- **Simple Interface**: Offers easy-to-use functions for image classification and result handling.
- **Unit Testing**: Includes unit tests to ensure the accuracy and reliability of the classification results.
- 
## Installation

To integrate the CatDogImageSDK into your Android application, follow these steps:

1. Add the SDK to your project dependencies.
2. Ensure that the required TensorFlow Lite model files are included in your project assets.

## Usage

### Initialization

Initialize the CatDogImageClassifier class with a context:

```
val classifier = CatDogImageClassifier(context)
```

### Image Classification
Classify a Bitmap image using the classify function:
```
val inputBuffer = classifier.convertBitmapToByteBuffer(bitmap)
val output = classifier.classify(inputBuffer)
val result = classifier.handleOutput(output)
```

### Result Handling
Handle the classification result using the DetectionResult class:
```
val className = result.className
val confidence = result.confidence
val boundingBox = result.boundingBox
```

### Saving Output
Save the classification output and image URI to a file:
```
classifier.saveOutputAndImage(output, imageUri, outputDirectory)
```

## Acknowledgments
This tensorflow model was developed by the Dyneti Technologies team.