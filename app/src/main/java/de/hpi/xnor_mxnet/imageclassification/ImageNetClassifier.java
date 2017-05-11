package de.hpi.xnor_mxnet.imageclassification;

import android.graphics.Bitmap;
import android.os.SystemClock;
import android.os.Trace;

import org.dmlc.mxnet.Predictor;

import java.nio.ByteBuffer;
import java.util.HashMap;

import de.hpi.xnor_mxnet.MainActivity;
import de.hpi.xnor_mxnet.R;

public class ImageNetClassifier extends AbstractClassifier {
    private final boolean modelNeedsMeanAdjust = true;

    public ImageNetClassifier(MainActivity activity) {
        super(activity);
        mImageWidth = 224;
        mImageHeight = 224;
    }

   public Classification[] classifyImage(Bitmap bitmap) {
        Trace.beginSection("create Image Buffer");
        ByteBuffer byteBuffer = ByteBuffer.allocate(bitmap.getByteCount());
        bitmap.copyPixelsToBuffer(byteBuffer);
        byte[] bytes = byteBuffer.array();
        Trace.endSection();

        Trace.beginSection("color adaption");
        float[] colors;
        if (modelNeedsMeanAdjust) {
            colors = subtractMean(bytes);
        } else {
            colors = extractRGBData(bytes);
        }
        Trace.endSection();
        Trace.beginSection("Model execution");

        final long startTime = SystemClock.uptimeMillis();
        mPredictor.forward("data", colors);
        mActivity.setLasProcessingTimeMs(SystemClock.uptimeMillis() - startTime);

        final float[] result = mPredictor.getOutput(0);
        Trace.endSection();

        Trace.beginSection("gather top results");
        Classification[] results = getTopKresults(result, 5);
        Trace.endSection();

        mActivity.requestRender();
        return results;
    }

    @Override
    public void loadModel() {
        final byte[] symbol = readRawFile(mActivity, R.raw.binarized_resnet_18_symbol);
        final byte[] params = readRawFile(mActivity, R.raw.binarized_resnet_18_params);
        final Predictor.Device device = new Predictor.Device(Predictor.Device.Type.CPU, 0);
        final int[] shape = {1, 3, mImageHeight, mImageWidth};
        final String key = "data";
        final Predictor.InputNode node = new Predictor.InputNode(key, shape);

        mPredictor = new Predictor(symbol, params, device, new Predictor.InputNode[]{node});
    }

    @Override
    public void loadLabels() {
        mLabels = readRawTextFile(mActivity, R.raw.synset);
    }

    @Override
    public void loadMean() {
        mMean = new HashMap<>();
        if (modelNeedsMeanAdjust) {
            mMean.put("b", (float) 103.939);
            mMean.put("g", (float) 116.779);
            mMean.put("r", (float) 123.68);
        }

    }
}
