package com.moneyforward.cameracompat.camera;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.io.IOException;
import java.util.List;

/**
 * Camera Preview
 * <p/>
 * Created by kuroda02 on 2015/12/18.
 */
public class Camera1Preview extends ViewGroup implements SurfaceHolder.Callback {

    private final String TAG = Camera1Preview.class.getSimpleName();

    private Camera.Size previewSize;
    private List<Camera.Size> supportedPreviewSizes;
    private Context context;
    private SurfaceView surfaceView;
    private SurfaceHolder holder;
    private List<String> supportedFlashModes;
    private Camera camera;

    public Camera1Preview(Context context, Camera camera) {
        super(context);
        this.context = context;
        setCamera(camera);

        this.surfaceView = new SurfaceView(context);
        addView(surfaceView, 0);
        this.holder = this.surfaceView.getHolder();
        this.holder.addCallback(this);
        this.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        this.holder.setKeepScreenOn(true);
    }

    public SurfaceHolder getHolder() {
        return holder;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
        if (this.camera != null) {
            this.supportedPreviewSizes = this.camera.getParameters().getSupportedPreviewSizes();
            this.supportedFlashModes = this.camera.getParameters().getSupportedFlashModes();
            // Set the camera to Auto Flash mode.
            if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                Camera.Parameters parameters = this.camera.getParameters();
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                this.camera.setParameters(parameters);
            }
        }
        requestLayout();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (this.camera != null) {
            this.camera.stopPreview();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (this.holder.getSurface() == null)
            return;
        try {
            this.camera.stopPreview();
        } catch (Exception ignored) {
        }

        try {
            Camera.Parameters parameters = this.camera.getParameters();
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            if (this.previewSize != null) {
                Camera.Size previewSize = this.previewSize;
                parameters.setPreviewSize(previewSize.width, previewSize.height);
            }

            this.camera.setParameters(parameters);
            this.camera.setPreviewDisplay(this.holder);
            this.camera.startPreview();
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            if (this.camera != null) {
                this.camera.setPreviewDisplay(holder);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (this.supportedPreviewSizes != null) {
            this.previewSize = getOptimalPreviewSize(this.supportedPreviewSizes, width, height);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (!changed) {
            return;
        }
        final View cameraView = getChildAt(0);

        final int width = right - left;
        final int height = bottom - top;

        int previewWidth = width;
        int previewHeight = height;
        if (this.previewSize != null) {
            Display display = ((WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

            switch (display.getRotation()) {
                case Surface.ROTATION_0:
                    previewWidth = this.previewSize.height;
                    previewHeight = this.previewSize.width;
                    this.camera.setDisplayOrientation(90);
                    break;
                case Surface.ROTATION_90:
                    previewWidth = this.previewSize.width;
                    previewHeight = this.previewSize.height;
                    break;
                case Surface.ROTATION_180:
                    previewWidth = this.previewSize.height;
                    previewHeight = this.previewSize.width;
                    break;
                case Surface.ROTATION_270:
                    previewWidth = this.previewSize.width;
                    previewHeight = this.previewSize.height;
                    this.camera.setDisplayOrientation(180);
                    break;
            }
        }

        final int scaledChildHeight = previewHeight * width / previewWidth;
        cameraView.layout(0, height - scaledChildHeight, width, height);
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int width, int height) {

        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) height / width;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = height;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
}