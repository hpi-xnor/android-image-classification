package de.hpi.xnor_mxnet;

import android.graphics.Bitmap;
import android.widget.TextView;

class ImageClassificationTask implements Runnable {

    private final Bitmap mImage;
    private final ImageClassifier mClassifier;
    private final MainActivity mActivity;

    ImageClassificationTask(Bitmap image, MainActivity activity) {
        mImage = image;
        mClassifier = activity.mImageClassifier;
        mActivity = activity;
    }

    @Override
    public void run() {
        final Classification result = mClassifier.classifyImage(mImage);
        final String resultString = String.format("%s, %.3f", result.get_label(), result.get_probability());

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView textView = (TextView) mActivity.findViewById(R.id.classDisplay);
                if (textView != null) {
                    textView.setText(resultString);
                }
                System.out.println(resultString);
                mActivity.setComputing(false);
            }
            });
    }
}
