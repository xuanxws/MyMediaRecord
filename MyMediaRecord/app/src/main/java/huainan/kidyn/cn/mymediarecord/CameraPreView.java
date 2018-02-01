package huainan.kidyn.cn.mymediarecord;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

/**
 * 相机预览界面
 * Created by Vison on 2018/1/31.
 */

public class CameraPreView extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder mHolder;
    private Camera mCamera;

    private Context mContext;

    private String flashMode = Camera.Parameters.FLASH_MODE_OFF;

    public CameraPreView(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        mContext = context;

        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mHolder = surfaceHolder;
        startPreView(mHolder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        destroyCamera();
    }

    /**
     * 开启预览
     *
     * @param holder
     */
    private void startPreView(SurfaceHolder holder) {
        if (mCamera == null) {
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        }

        if (mCamera != null) {
            mCamera.setDisplayOrientation(90);
            try {
                mCamera.setPreviewDisplay(holder);
                Camera.Parameters parameters = mCamera.getParameters();
                //实现Camera自动对焦
                List<String> focusModes = parameters.getSupportedFocusModes();
                if (focusModes != null) {
                    for (String mode : focusModes) {
                        mode.contains("continuous-video");
                        parameters.setFocusMode("continuous-video");
                    }
                }
                parameters.setFlashMode(flashMode);
                mCamera.setParameters(parameters);
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void refreshCamera(Camera camera) {
        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }
        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }
        // set preview size and make any resize, rotate or
        // reformatting changes here
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (camera != null) {
                camera.setDisplayOrientation(90);
            }
        }

        // start preview with new settings
        setCamera(camera);
        startPreView(mHolder);
    }

    public void setCamera(Camera camera) {
        //method to set a camera instance
        mCamera = camera;
    }

    public void setFlashMode(String mode) {
        flashMode = mode;
    }

    /**
     * 释放资源
     */
    public void destroyCamera(){
        if (mCamera != null) {
            //停止预览并释放摄像头资源
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public SurfaceHolder getSurfaceHolder(){
        return mHolder;
    }
}
