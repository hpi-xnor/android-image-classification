package de.hpi.xnor_mxnet.imageclassification;

import android.graphics.Bitmap;

public interface ImageClassifier {
    int getImageWidth();
    int getImageHeight();
    Classification[] classifyImage(Bitmap bitmap);
    void loadModel();
    void loadLabels();
    void loadMean();
    void loadStdDev();
}
