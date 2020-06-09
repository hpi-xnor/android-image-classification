Android Image Classification
============================

This is an example project capable of performing image classification on a live camera feed, using a binarized neural network on Android.

Requirements
------------

* android device (minimum required Android version is 4.2 (API level 21))

Usage
-----

Clone the repository, open and compile the project with Android Studio. Tested to work with Android Studio 4.0 and SDK 29.
If you want to use the library on an device that does not have a armv7 or armv8 processor, you have to recompile the BMXNet library as described [here](https://github.com/hpi-xnor/BMXNet-v2/tree/master/amalgamation), using the correct toolchain.

Please take care when recompiling the library to set the definition of `BINARY_WORD_32` or `BINARY_WORD_64` according to your target architecture and also to convert the model to the right `BINARY_WORD` using the model converter supplied with BMXNet.

If you want to use any model other than the two provided (`DenseNet-28` and `ResNetE-18`), you'll have to make sure that
the `symbol` file contains a `softmax` as output op. And that the id of the output layer is set correctly in the symbol json file.
You can find the symbol and model files in `app/src/main/res/raw`.

If you want to change the used default model (from `DensetNet-28` to `ResNetE-18` or vice versa), you'll have to
change [these two](https://github.com/hpi-xnor/android-image-classification/blob/master/app/src/main/java/de/hpi/xnor_mxnet/imageclassification/ImageNetClassifier.java#L56-L57)
lines.
