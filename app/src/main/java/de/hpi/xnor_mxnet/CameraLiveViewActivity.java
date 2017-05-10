package de.hpi.xnor_mxnet;


import android.app.Activity;

public abstract class CameraLiveViewActivity extends Activity {
    protected boolean computing = false;

    abstract public void runCameraLiveView();

    public void setComputing(boolean computing) {
        this.computing = computing;
    }
}
