# android_uvc_tensorflow_kt
TensorFlow Lite Object Detection Android UVC Demo

# Hardware Provider(Innocomm SB35/SC51)
https://www.innocomm.com/

### Overview

This is a uvc camera app that continuously detects the objects (bounding boxes and
classes) in the frames seen by your device's back camera, with the option to use
a quantized
[MobileNet SSD](https://tfhub.dev/tensorflow/lite-model/ssd_mobilenet_v1/1/metadata/2),
[EfficientDet Lite 0](https://tfhub.dev/tensorflow/lite-model/efficientdet/lite0/detection/metadata/1),
[EfficientDet Lite1](https://tfhub.dev/tensorflow/lite-model/efficientdet/lite1/detection/metadata/1),
or
[EfficientDet Lite2](https://tfhub.dev/tensorflow/lite-model/efficientdet/lite2/detection/metadata/1)
model trained on the [COCO dataset](http://cocodataset.org/). These instructions
walk you through building and running the demo on an Android device.

The model files are downloaded via Gradle scripts when you build and run the
app. You don't need to do any steps to download TFLite models into the project
explicitly.

This application should be run on a physical Android device with UVC enabled device.

![App example showing Highlights a some objects](images/image001.png)
![App example showing UI controls.](images/image002.png)
![App example showing UI controls.](images/image003.png) 


## Build the demo using Android Studio

### Prerequisites

*   The **[Android Studio](https://developer.android.com/studio/index.html)**
    IDE. This sample has been tested on Android Studio Koala.

*   A physical Android device with a minimum OS version of SDK 29 (Android 10 -
    Quince Tart) with developer mode enabled. The process of enabling developer mode
    may vary by device.

### Building

*   Open Android Studio. From the Welcome screen, select Open an existing
    Android Studio project.

*   From the Open File or Project window that appears, navigate to and select
    the android_uvc_tensorflow_kt/ directory. Click OK.

*   If it asks you to do a Gradle Sync, click OK.

*   With your Android device connected to your computer and developer mode
    enabled, click on the green Run arrow in Android Studio.

## Reference
*   https://github.com/tensorflow/examples/blob/master/lite/examples/object_detection/android/README.md
*   https://github.com/jiangdongguo/AndroidUSBCamera




