Android Image Classification
============================

This is an example project capable of performing image classification on a live camera fedd, using a binarized neural network on Android.

Requirements
------------

* android device (minimum required Android version is 4.2 (API level 21))

Usage
-----

Clone the repository, open and compile the project with Android Studio.
If you want to use the library on an ARM64 device you have to recompile the BMXNet library as described (here)[https://github.com/hpi-xnor/BMXNet/tree/master/amalgamation], using the correct toolchain.

Please take care when recompiling the library to set the definition of `BINARY_WORD_32` or `BINARY_WORD_64` according to your target architecture and also to convert the model to the right `BINARY_WORD` using the model converter supplied with BMXNet.
