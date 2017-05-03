package de.hpi.xnor_mxnet;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.widget.TextView;

import java.nio.ByteBuffer;


class ImageClassificationTask implements Runnable {

    private final Image mImage;
    private final ImageClassifier mClassifier;
    private final MainActivity mActivity;

    ImageClassificationTask(Image image, MainActivity activity) {
        mImage = image;
        mClassifier = activity.mImageClassifier;
        mActivity = activity;
    }

    @Override
    public void run() {
        try {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            final Classification result = mClassifier.classifyImage(bitmap);
            final String resultString = String.format("%s, %.3f", result.get_label(), result.get_probability());

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView textView = (TextView) mActivity.findViewById(R.id.classDisplay);
                    textView.setText(resultString);
                    System.out.println(resultString);
                }
            });
        } finally {
            mImage.close();
        }
    }
}
