package de.hpi.xnor_mxnet;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

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

    ImageClassifier(MainActivity mainActivity) {
        mActivity = mainActivity;
        new ModelPreparationTask().execute();
    }

    Classification classifyImage(Bitmap bitmap) {
        bitmap = resizeImage(bitmap);
        ByteBuffer byteBuffer = ByteBuffer.allocate(bitmap.getByteCount());
        bitmap.copyPixelsToBuffer(byteBuffer);
        byte[] bytes = byteBuffer.array();
        float[] colors = new float[bytes.length / 4 * 3];

        float mean_b = mMean.get("b");
        float mean_g = mMean.get("g");
        float mean_r = mMean.get("r");
        for (int i = 0; i < bytes.length; i += 4) {
            int j = i / 4;
            colors[0 * 224 * 224 + j] = (float)(((int)(bytes[i + 0])) & 0xFF) - mean_r;
            colors[1 * 224 * 224 + j] = (float)(((int)(bytes[i + 1])) & 0xFF) - mean_g;
            colors[2 * 224 * 224 + j] = (float)(((int)(bytes[i + 2])) & 0xFF) - mean_b;
        }
        mPredictor.forward("data", colors);
        final float[] result = mPredictor.getOutput(0);

        int index = 0;
        for (int i = 0; i < result.length; ++i) {
            if (result[index] < result[i]) index = i;
        }
        String tag = mLabels.get(index);
        String [] arr = tag.split(" ", 2);
        return new Classification(arr[0], arr[1], result[index]);
    }

    private Bitmap resizeImage(Bitmap bitmap) {
        return Bitmap.createScaledBitmap(bitmap, mImageWidth, mImageHeight, true);
    }

    private class ModelPreparationTask extends AsyncTask<Void, Void, Predictor> {

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(mActivity);
            mProgressDialog.setTitle(R.string.loading_model);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.show();
        }

        @Override
        protected Predictor doInBackground(Void... voids) {
            try {
                buildPredictor();
                mLabels = readRawTextFile(mActivity, R.raw.synset);
                loadMean(mActivity, R.raw.mean);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return mPredictor;
        }

        @Override
        protected void onPostExecute(Predictor predictor) {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
            mActivity.findViewById(R.id.camera).setEnabled(true);
        }
    }

    private void buildPredictor() {
        final byte[] symbol = readRawFile(mActivity, R.raw.symbol);
        final byte[] params = readRawFile(mActivity, R.raw.params);
        final Predictor.Device device = new Predictor.Device(Predictor.Device.Type.CPU, 0);
        final int[] shape = {1, 3, 224, 224};
        final String key = "data";
        final Predictor.InputNode node = new Predictor.InputNode(key, shape);

        mPredictor = new Predictor(symbol, params, device, new Predictor.InputNode[]{node});
    }

    private void loadMean(Context ctx, int resId) {
        try {
            final StringBuilder sb = new StringBuilder();
            final List<String> lines = readRawTextFile(ctx, resId);
            for (final String line : lines) {
                sb.append(line);
            }
            final JSONObject meanJson = new JSONObject(sb.toString());
            mMean = new HashMap<>();
            mMean.put("b", (float) meanJson.optDouble("b"));
            mMean.put("g", (float) meanJson.optDouble("g"));
            mMean.put("r", (float) meanJson.optDouble("r"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
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

//    private Classification[] getTopKresults(float[] input_matrix, int k) {
//        float[] sorted_values = input_matrix.clone();
//        Arrays.sort(sorted_values);
//
//        Classification[] topK = new Classification[k];
//        List<Float> input_list = new ArrayList<>();
//        for (float f: input_matrix) {
//            input_list.add(f);
//        }
//
//        if (BuildConfig.DEBUG && sorted_values.length < k) {
//            throw new RuntimeException("Too few predicted values for getting topK results!");
//        }
//
//        for (int i = 0; i < topK.length; ++i) {
//            int classId = input_list.indexOf(sorted_values[sorted_values.length - i - 1]);
//            topK[i] = new Classification(classId, labels[classId], input_matrix[classId]);
//        }
//        return topK;
//    }
}
