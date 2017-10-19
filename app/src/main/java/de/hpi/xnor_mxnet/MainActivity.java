package de.hpi.xnor_mxnet;

import android.Manifest;
import android.app.Fragment;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.Vector;

import de.hpi.xnor_mxnet.imageclassification.ImageClassifier;
import de.hpi.xnor_mxnet.imageclassification.ImageNetClassifier;


public class MainActivity extends CameraLiveViewActivity implements ImageReader.OnImageAvailableListener {
    static {
        System.loadLibrary("native-image-utils");
    }
    private static final int PERMISSIONS_REQUEST = 1;

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String PERMISSION_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE;
    private static final float TEXT_SIZE_DIP = 10;
    private static String TAG;

    private Handler handler;
    private HandlerThread handlerThread;


    private ImageClassifier mImageClassifier;
    private byte[][] yuvBytes;
    private int[] rgbBytes;
    private int previewWidth;
    private int previewHeight;
    private Bitmap rgbFrameBitmap;
    private Bitmap croppedBitmap;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private boolean debug;
    private Bitmap cropCopyBitmap;
    private BorderedText borderedText;
    private long lasProcessingTimeMs;
    private boolean isFirstImage = true;

    private boolean hasPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(PERMISSION_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA) || shouldShowRequestPermissionRationale(PERMISSION_STORAGE)) {
                Toast.makeText(MainActivity.this, "Camera AND storage permission are required for this demo", Toast.LENGTH_LONG).show();
            }
            requestPermissions(new String[] {PERMISSION_CAMERA, PERMISSION_STORAGE}, PERMISSIONS_REQUEST);
        }
    }

    private void buildImageClassifier() {
        mImageClassifier = new ImageNetClassifier(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        TAG = getResources().getString(R.string.app_name);

        setContentView(R.layout.main_activity);

        if (hasPermission()) {
            buildImageClassifier();
        } else {
            requestPermission();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause() {
        Log.d(TAG, "Pausing");

        if (!isFinishing()) {
            finish();
        }

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            Log.e(TAG, e.getMessage());
        }

        super.onPause();
    }

    public void runCameraLiveView() {
        final Fragment cameraView = CameraConnectionFragment.newInstance(
                new CameraConnectionFragment.ConnectionCallback() {
                    @Override
                    public void onPreviewSizeChosen(Size size, int rotation) {
                        MainActivity.this.onPreviewSizeChosen(size);
                    }
                },
                this,
                R.layout.placerecognizer_ui,
                new Size(mImageClassifier.getImageWidth(), mImageClassifier.getImageHeight())
        );

        getFragmentManager().beginTransaction()
                        .replace(R.id.container, cameraView)
                        .commit();
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode) {
            case PERMISSIONS_REQUEST: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    buildImageClassifier();
                } else {
                    requestPermission();
                }
            }
        }
    }

    public void onPreviewSizeChosen(final Size size) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        Log.i(TAG, String.format("Initializing cameraPreview at size %dx%d", previewWidth, previewHeight));
        rgbBytes = new int[previewWidth * previewHeight];
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(mImageClassifier.getImageWidth(), mImageClassifier.getImageHeight(), Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        mImageClassifier.getImageWidth(), mImageClassifier.getImageHeight(),
                        90, true);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        yuvBytes = new byte[3][];

        addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        renderDebug(canvas);
                    }
                });
    }

    protected void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                Log.d(TAG, String.format("Initializing buffer %d at size %d", i, buffer.capacity()));
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    public void requestRender() {
        final OverlayView overlay = (OverlayView) findViewById(R.id.debug_overlay);
        if (overlay != null) {
            overlay.postInvalidate();
        }
    }

    public void addCallback(final OverlayView.DrawCallback callback) {
        final OverlayView overlay = (OverlayView) findViewById(R.id.debug_overlay);
        if (overlay != null) {
            overlay.addCallback(callback);
        }
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            debug = !debug;
            requestRender();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void renderDebug(final Canvas canvas) {
        if (!debug) {
            return;
        }
        final Bitmap copy = cropCopyBitmap;
        if (copy != null) {
            final Matrix matrix = new Matrix();
            final float scaleFactor = 2;
            matrix.postScale(scaleFactor, scaleFactor);
            matrix.postTranslate(
                    canvas.getWidth() - copy.getWidth() * scaleFactor,
                    canvas.getHeight() - copy.getHeight() * scaleFactor);
            canvas.drawBitmap(copy, matrix, new Paint());

            final Vector<String> lines = new Vector<>();

            lines.add("Frame: " + previewWidth + "x" + previewHeight);
            lines.add("Crop: " + copy.getWidth() + "x" + copy.getHeight());
            lines.add("View: " + canvas.getWidth() + "x" + canvas.getHeight());
            lines.add("Inference time: " + lasProcessingTimeMs + "ms");

            borderedText.drawLines(canvas, 10, canvas.getHeight() - 10, lines);
        }
    }

    @Override
    public void onImageAvailable(ImageReader imageReader) {
        Image image = null;

        try {
            image = imageReader.acquireLatestImage();

            if (image == null) {
                return;
            }

            if (computing || isFirstImage) {
                isFirstImage = false;
                image.close();
                return;
            }
            computing = true;

            Trace.beginSection("imageAvailable");

            final Image.Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);

            final int yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();
            ImageUtils.convertYUV420ToARGB8888(
                    yuvBytes[0],
                    yuvBytes[1],
                    yuvBytes[2],
                    rgbBytes,
                    previewWidth,
                    previewHeight,
                    yRowStride,
                    uvRowStride,
                    uvPixelStride,
                    false);
            image.close();
            Trace.endSection();
        } catch (final Exception e) {
            if (image != null) {
                image.close();
            }
            Log.e(TAG, String.format("Exception in onImageAvailable: %s", e));
            Trace.endSection();
            return;
        }

        rgbFrameBitmap.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight);
        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);

        if (handler != null) {
            handler.post(new ImageClassificationTask(croppedBitmap, this, mImageClassifier));
        }
    }

    public void setLasProcessingTimeMs(long lasProcessingTimeMs) {
        this.lasProcessingTimeMs = lasProcessingTimeMs;
    }
}
