package de.hpi.xnor_mxnet;

import android.app.Activity;
import android.graphics.Bitmap;
import android.widget.TextView;

import de.hpi.xnor_mxnet.imageclassification.Classification;
import de.hpi.xnor_mxnet.imageclassification.ImageClassifier;
import de.hpi.xnor_mxnet.imageclassification.ImageNetClassifier;

class ImageClassificationTask implements Runnable {

    private final Bitmap mImage;
    private final ImageClassifier mClassifier;
    private final CameraLiveViewActivity mActivity;

    ImageClassificationTask(Bitmap image, CameraLiveViewActivity activity, ImageClassifier classifier) {
        mImage = image;
        mClassifier = classifier;
        mActivity = activity;
    }

    private String join(String delimiter, String[] s) {
        StringBuilder out = new StringBuilder();
        out.append(s[0]);
        for (int i = 1; i < s.length; ++i) {
            out.append(delimiter).append(s[i]);
        }
        return out.toString();
    }

    @Override
    public void run() {
        final Classification[] results = mClassifier.classifyImage(mImage);
        final String[] resultStrings = new String[results.length];

        for (int i = 0; i < results.length; ++i) {
            resultStrings[i] = String.format("%s, %.3f", results[i].get_label(), results[i].get_probability());
        }

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView textView = (TextView) mActivity.findViewById(R.id.classDisplay);
                String text = join("\n", resultStrings);
                if (textView != null) {
                    textView.setText(text);
                }
                System.out.println(text);
                mActivity.setComputing(false);
            }
            });
    }
}
