package de.hpi.xnor_mxnet.imageclassification;


import android.app.ProgressDialog;
import android.os.AsyncTask;

import de.hpi.xnor_mxnet.CameraLiveViewActivity;
import de.hpi.xnor_mxnet.R;


public class ModelPreparationTask extends AsyncTask<ImageClassifier, Void, Void> {
    private ProgressDialog mProgressDialog;
    private CameraLiveViewActivity context;

    public void setContext(CameraLiveViewActivity context) {
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setTitle(R.string.loading_model);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.show();
    }

    @Override
    protected Void doInBackground(ImageClassifier... imageClassifiers) {
        for (ImageClassifier classifier: imageClassifiers) {
            classifier.loadModel();
            classifier.loadLabels();
            classifier.loadMean();
            classifier.loadStdDev();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void voids) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        context.runCameraLiveView();
    }

}
