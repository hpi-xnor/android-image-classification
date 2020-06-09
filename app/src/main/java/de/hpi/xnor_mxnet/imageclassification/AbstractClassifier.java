package de.hpi.xnor_mxnet.imageclassification;


import android.content.Context;

import org.dmlc.mxnet.Predictor;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import de.hpi.xnor_mxnet.BuildConfig;
import de.hpi.xnor_mxnet.MainActivity;

abstract class AbstractClassifier implements ImageClassifier {
    final MainActivity mActivity;
    Predictor mPredictor;
    List<String> mLabels;
    Map<String, Float> mMean;
    Map<String, Float> mStdDev;

    int mImageWidth;
    int mImageHeight;

    protected final boolean modelNeedsMeanAdjust = true;
    protected final boolean modelNeedsStdAdjust = true;

    @Override
    public int getImageWidth() {
        return mImageWidth;
    }

    @Override
    public int getImageHeight() {
        return mImageHeight;
    }

    AbstractClassifier(MainActivity activity) {
        mActivity = activity;
        ModelPreparationTask preparationTask = new ModelPreparationTask();
        preparationTask.setContext(activity);
        preparationTask.execute(this);
    }

    static byte[] readRawFile(Context ctx, int resId)
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

    static List<String> readRawTextFile(Context ctx, int resId)
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

    Classification[] getTopKresults(float[] input_matrix, int k) {
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

    float[] prepareInputImage(byte[] bytes) {
        float[] colors = new float[bytes.length / 4 * 3];

        // the R,G,B order has been tested (by HJ, 19.10.17), the R->G->B (1,2,3) got better results from prediction
        int imageOffset = mImageWidth * mImageHeight;
        for (int i = 0; i < bytes.length; i += 4) {
            int j = i / 4;

            int indexR = j;
            int indexG = imageOffset + j;
            int indexB = 2 * imageOffset + j;

            colors[indexR] = (float)(((int)(bytes[i + 1])) & 0xFF);
            colors[indexG] = (float)(((int)(bytes[i + 2])) & 0xFF);
            colors[indexB] = (float)(((int)(bytes[i + 3])) & 0xFF);

            if (modelNeedsMeanAdjust) {
                colors[indexR] -= mMean.get("r");
                colors[indexG] -= mMean.get("g");
                colors[indexB] -= mMean.get("b");
            }

            if (modelNeedsStdAdjust) {
                colors[indexR] /= mStdDev.get("r");
                colors[indexG] /= mStdDev.get("g");
                colors[indexB] /= mStdDev.get("b");
            }
        }
        return colors;
    }
}
