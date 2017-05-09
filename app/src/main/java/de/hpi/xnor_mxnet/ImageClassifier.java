package de.hpi.xnor_mxnet;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.os.Trace;

import org.dmlc.mxnet.Predictor;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ImageClassifier {
    private static Predictor mPredictor;
    private List<String> mLabels;
    private Map<String, Float> mMean;
    private MainActivity mActivity;
    private ProgressDialog mProgressDialog;

    private final int mImageWidth = 224;
    private final int mImageHeight = 224;
    private boolean modelNeedsMeanAdjust;

    public int getImageWidth() {
        return mImageWidth;
    }

    public int getImageHeight() {
        return mImageHeight;
    }

    ImageClassifier(MainActivity mainActivity) {
        mActivity = mainActivity;
        modelNeedsMeanAdjust = true;
        new ModelPreparationTask().execute();
    }

    float[] subtractMean(byte[] bytes) {
        float[] colors = new float[bytes.length / 4 * 3];

        float mean_b = mMean.get("b");
        float mean_g = mMean.get("g");
        float mean_r = mMean.get("r");

        int imageOffset = mImageWidth * mImageHeight;
        for (int i = 0; i < bytes.length; i += 4) {
            int j = i / 4;
            colors[0 * imageOffset + j] = (float)(((int)(bytes[i + 3])) & 0xFF) - mean_b;
            colors[1 * imageOffset + j] = (float)(((int)(bytes[i + 2])) & 0xFF) - mean_g;
            colors[2 * imageOffset + j] = (float)(((int)(bytes[i + 1])) & 0xFF) - mean_r;
        }
        return colors;
    }

    float[] extractRGBData(byte[] bytes) {
        float[] colors = new float[bytes.length / 4 * 3];

        int imageOffset = mImageWidth * mImageHeight;
        for (int i = 0; i < bytes.length; i += 4) {
            int j = i / 4;
            colors[0 * imageOffset + j] = (float)((int)(bytes[i + 3]) & 0xFF);
            colors[1 * imageOffset + j] = (float)((int)(bytes[i + 2]) & 0xFF);
            colors[2 * imageOffset + j] = (float)((int)(bytes[i + 1]) & 0xFF);
        }
        return colors;
    }


    Classification[] classifyImage(Bitmap bitmap) {
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
//        return new Classification("0", "Kekse", 1.0f);
    }

    private class ModelPreparationTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(mActivity);
            mProgressDialog.setTitle(R.string.loading_model);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                buildPredictor();
                mLabels = readRawTextFile(mActivity, R.raw.synset);
                if (modelNeedsMeanAdjust) {
                    loadMean(mActivity, R.raw.mean);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void voids) {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
            mActivity.runCameraLiveView();
        }
    }

    private void buildPredictor() {
        final byte[] symbol = readRawFile(mActivity, R.raw.binarized_resnet_18_binary_symbol);
        final byte[] params = readRawFile(mActivity, R.raw.binarized_resnet_18_binary_0033);
//        final byte[] symbol = readRawFile(mActivity, R.raw.binarized_32_cifar10_binary_symbol);
//        final byte[] params = readRawFile(mActivity, R.raw.binarized_32_cifar10_binary_0000);
        final Predictor.Device device = new Predictor.Device(Predictor.Device.Type.CPU, 0);
        final int[] shape = {1, 3, mImageHeight, mImageWidth};
        final String key = "data";
        final Predictor.InputNode node = new Predictor.InputNode(key, shape);

        mPredictor = new Predictor(symbol, params, device, new Predictor.InputNode[]{node});
    }

    private void loadMean(Context ctx, int resId) {
        mMean = new HashMap<>();
        mMean.put("b", (float) 103.939);
        mMean.put("g", (float) 116.779);
        mMean.put("r", (float) 123.68);
    }

    private static byte[] readRawFile(Context ctx, int resId)
    {
        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
        int size = 0;
        byte[] buffer = new byte[1024];
        try (InputStream ins = ctx.getResources().openRawResource(resId)) {
            while((size=ins.read(buffer,0,1024))>=0){
                outputStream.write(buffer,0,size);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }

    private static List<String> readRawTextFile(Context ctx, int resId)
    {
        List<String> result = new ArrayList<>();
        InputStream inputStream = ctx.getResources().openRawResource(resId);

        InputStreamReader inputreader = new InputStreamReader(inputStream);
        BufferedReader buffreader = new BufferedReader(inputreader);
        String line;

        try {
            while (( line = buffreader.readLine()) != null) {
                result.add(line);
            }
        } catch (IOException e) {
            return null;
        }
        return result;
    }

    private Classification[] getTopKresults(float[] input_matrix, int k) {
        float[] sorted_values = input_matrix.clone();
        Arrays.sort(sorted_values);

        Classification[] topK = new Classification[k];
        List<Float> input_list = new ArrayList<>();
        for (float f: input_matrix) {
            input_list.add(f);
        }

        if (BuildConfig.DEBUG && sorted_values.length < k) {
            throw new RuntimeException("Too few predicted values for getting topK results!");
        }

        for (int i = 0; i < topK.length; ++i) {
            int classId = input_list.indexOf(sorted_values[sorted_values.length - i - 1]);
            String tag = mLabels.get(classId);
            String [] tagInfo = tag.split(" ", 2);
            topK[i] = new Classification(classId, tagInfo[1], input_matrix[classId]);
        }
        return topK;
    }
}
