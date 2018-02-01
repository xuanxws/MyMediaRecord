package huainan.kidyn.cn.mymediarecord;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 录制视频页面，封装一些页面操作
 * Created by Vison on 2018/1/29.
 */

public class MediaRecordActivity extends Activity {

    private String currentInputVideoPath = "";//录制的视频路径

    MyProgressbar mMyProgressbar;//进度条展示

    RelativeLayout mRelativeLayout;

    ImageView mIvRight;

    ImageView mIvError;

    ImageView mIvPhotoChange;

    RelativeLayout mRlCamera;

    ImageButton mFlashButton;

    TextView mTvSp;

    private int photo_direction = 2;//摄像头方向，默认是后置摄像头

    public static final int FRONT_CAMERA = 1;//前置摄像头状态

    public static final int BEHIND_CAMERA = 2;//后置摄像头状态

    private int flash_state = 0;//闪光灯状态

    public static final int FLASH_OPEN = 1;//闪光灯开启

    public static final int FLASH_CLOSE = 0;//闪关灯关闭

    private Camera mCamera;
    LinearLayout mLlSurface;
    CameraPreView mCameraPreView; //预览SurfaceView

    private MediaRecorder mMediaRecorder;//录制视频

    private int videoWidth, videoHeight;//屏幕分辨率

    private boolean isRecording;//判断是否正在录制

    private File mTargetFile;//段视频保存的目录

    private int quality = CamcorderProfile.QUALITY_480P;

    private int totalNum = 10;//视频录制时长，10s
    final ArrayList<String> mQualityList = new ArrayList<String>();
    public static final int MESSAGE_WHAT = 1;
    private float curProgress = 0;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_WHAT:
                    curProgress = curProgress + 7.2f;
                    mMyProgressbar.setCurrentProgress(curProgress);
                    Message message = new Message();
                    message.what = MESSAGE_WHAT;
                    if (curProgress < 360) {
                        mHandler.sendMessageDelayed(message, 200);
                    } else {
                        completeRecord();
                        mMyProgressbar.resetProgress();
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_record);
        initView();
    }

    private void initView() {
        videoWidth = 640;
        videoHeight = 480;
        mMyProgressbar = findViewById(R.id.myprogress);
        mRelativeLayout = findViewById(R.id.rl_choose);
        mFlashButton = findViewById(R.id.iv_flash);
        mIvRight = findViewById(R.id.iv_right);
        mIvError = findViewById(R.id.iv_error);
        mIvPhotoChange = findViewById(R.id.iv_photo_change);
        mRlCamera = findViewById(R.id.rl_camera);
        mTvSp = findViewById(R.id.tv_sp);
        mMediaRecorder = new MediaRecorder();
        mLlSurface = findViewById(R.id.ll_surface);
        mCameraPreView = new CameraPreView(MediaRecordActivity.this, mCamera);
        mLlSurface.addView(mCameraPreView);
        mFlashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isRecording && photo_direction != FRONT_CAMERA) {
                    if (flash_state == FLASH_CLOSE) {
                        flash_state = FLASH_OPEN;
                        mFlashButton.setBackgroundResource(R.drawable.flash_open);
                        setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    } else if (flash_state == FLASH_OPEN) {
                        flash_state = FLASH_CLOSE;
                        mFlashButton.setBackgroundResource(R.drawable.flash_close);
                        setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    }
                }
            }
        });

        mTvSp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (null != mQualityList && mQualityList.size() > 0) {
                    bottomwindow(mRlCamera);
                }
            }
        });
        mIvPhotoChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isRecording) {
                    int camerasNumber = Camera.getNumberOfCameras();
                    if (camerasNumber > 1) {
                        releaseCamera();
                        if (photo_direction == FRONT_CAMERA) {//当前是前置摄像头
                            photo_direction = BEHIND_CAMERA;
                            int cameraId = findBackFacingCamera();
                            if (cameraId >= 0) {
                                mCamera = Camera.open(cameraId);
                                mCameraPreView.refreshCamera(mCamera);
                                reloadQualities(cameraId);
                            }
                        } else if (photo_direction == BEHIND_CAMERA) {//当前是后置摄像头
                            int cameraId = findFrontFacingCamera();
                            if (cameraId >= 0) {
                                photo_direction = FRONT_CAMERA;
                                mCamera = Camera.open(cameraId);
                                if (flash_state == FLASH_OPEN) {
                                    flash_state = FLASH_CLOSE;
                                    mCameraPreView.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                                }
                                mCameraPreView.refreshCamera(mCamera);
                                reloadQualities(cameraId);
                            } else {//前置摄像头不存在
                                Toast.makeText(MediaRecordActivity.this, R.string.dont_have_front_camera, Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        //只有一个摄像头不允许切换
                        Toast.makeText(getApplicationContext(), R.string.only_have_one_camera
                                , Toast.LENGTH_SHORT).show();
                    }
                }

            }

        });

        mIvRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                Log.i("test","currentInputVideoPath===" + currentInputVideoPath);
                intent.putExtra("file",currentInputVideoPath);
                setResult(RESULT_OK,intent);
                finish();
            }
        });
        mIvError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRecordUnSave();
                mRelativeLayout.setVisibility(View.GONE);
                mRlCamera.setVisibility(View.VISIBLE);
            }
        });
        mMyProgressbar.setOnActionListener(new MyProgressbar.OnActionListener() {
            @Override
            public void onPress() {
                startRecord();
                Message message = new Message();
                message.what = MESSAGE_WHAT;
                if (curProgress < 360) {
                    mHandler.sendMessageDelayed(message, 0);
                }
            }

            @Override
            public void onLift() {
                mMyProgressbar.resetProgress();
                curProgress = 0;
                mHandler.removeMessages(MESSAGE_WHAT);
                completeRecord();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!hasCamera(getApplicationContext())) {
            //这台设备没有发现摄像头
            Toast.makeText(getApplicationContext(), R.string.dont_have_camera_error
                    , Toast.LENGTH_SHORT).show();
            releaseCamera();
            releaseMediaRecorder();
            finish();
        }
        if (mCamera == null) {
            releaseCamera();
            int cameraId = findFrontFacingCamera();
            if (cameraId < 0) {
                //尝试寻找后置摄像头
                cameraId = findBackFacingCamera();
                if (flash_state == FLASH_OPEN) {
                    mCameraPreView.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                }
            } else if (photo_direction != FRONT_CAMERA) {
                cameraId = findBackFacingCamera();
                if (flash_state == FLASH_OPEN) {
                    mCameraPreView.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                }
            }
            Log.i("test","cameraId===" + cameraId);
            mCamera = Camera.open(cameraId);
            mCameraPreView.refreshCamera(mCamera);
            reloadQualities(cameraId);
        }

    }

    /**
     * 找前置摄像头,没有则返回-1
     *
     * @return cameraId
     */
    private int findFrontFacingCamera() {
        int cameraId = -1;
        //获取摄像头个数
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    /**
     * 找后置摄像头,没有则返回-1
     *
     * @return cameraId
     */
    private int findBackFacingCamera() {
        int cameraId = -1;
        //获取摄像头个数
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    //检查设备是否有摄像头
    private boolean hasCamera(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }


    //闪光灯
    public void setFlashMode(String mode) {
        try {
            if (getPackageManager().hasSystemFeature(
                    PackageManager.FEATURE_CAMERA_FLASH)
                    && mCamera != null
                    && photo_direction != FRONT_CAMERA) {

                mCameraPreView.setFlashMode(mode);
                mCameraPreView.refreshCamera(mCamera);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), R.string.changing_flashLight_mode,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void completeRecord() {
        mRelativeLayout.setVisibility(View.VISIBLE);
        mRlCamera.setVisibility(View.GONE);
        stopRecord();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mMediaRecorder != null) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    /**
     * 开始录制
     */
    private void startRecord() {
        mRelativeLayout.setVisibility(View.GONE);
        mRlCamera.setVisibility(View.VISIBLE);
        if (mMediaRecorder != null) {
            //没有外置存储, 直接停止录制
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                return;
            }
            try {
                //mMediaRecorder.reset();
                mCamera.unlock();
                mMediaRecorder.setCamera(mCamera);
                //从相机采集视频
                mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                // 从麦克采集音频信息
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                // TODO: 2016/10/20  设置视频格式
                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mMediaRecorder.setVideoSize(videoWidth, videoHeight);
                //每秒的帧数
                mMediaRecorder.setVideoFrameRate(24);
                //编码格式
                mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                // 设置帧频率，然后就清晰了
                mMediaRecorder.setVideoEncodingBitRate(1 * 1024 * 1024 * 100);
                // TODO: 2016/10/20 临时写个文件地址, 稍候该!!!
                File targetDir = Environment.
                        getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
                mTargetFile = new File(targetDir,
                        SystemClock.currentThreadTimeMillis() + ".mp4");
                currentInputVideoPath = mTargetFile.getAbsolutePath();
                mMediaRecorder.setOutputFile(mTargetFile.getAbsolutePath());
                Log.i("test","quqlity==" + quality);
                //mMediaRecorder.setProfile(CamcorderProfile.get(quality));
                //mMediaRecorder.setCamera(mCamera);
                if (null != mCameraPreView.getSurfaceHolder()) {
                    mMediaRecorder.setPreviewDisplay(mCameraPreView.getSurfaceHolder().getSurface());
                }
                //解决录制视频, 播放器横向问题
                //mMediaRecorder.setOrientationHint(90);
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    if (photo_direction == FRONT_CAMERA) {
                        mMediaRecorder.setOrientationHint(270);
                    } else {
                        mMediaRecorder.setOrientationHint(90);
                    }
                }

                mMediaRecorder.prepare();
                //正式录制
                mMediaRecorder.start();
                isRecording = true;
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 停止录制 并且保存
     */
    private void stopRecord() {
        if (isRecording) {
            try {
                mMediaRecorder.stop();
            } catch (IllegalStateException e) {
                showRecordMedia();
                Log.i("test", "stop+++>exception--->" + e.toString());
            } catch (RuntimeException e) {
                showRecordMedia();
                Log.i("test", "stop***>exception--->" + e.toString());
            } catch (Exception e) {
                showRecordMedia();
                Log.i("test", "stop--->exception--->" + e.toString());
            }
            isRecording = false;
            Log.i("test", "file==" + mTargetFile.getAbsolutePath());
        }
    }

    /**
     * 不保存录音文件
     */
    private void stopRecordUnSave() {
        if (mTargetFile.exists()) {
            //不保存直接删掉
            mTargetFile.delete();
        }
    }

    private void showRecordMedia() {
        mRelativeLayout.setVisibility(View.GONE);
        mRlCamera.setVisibility(View.VISIBLE);
        mMyProgressbar.resetProgress();
        if (mTargetFile.exists()) {
            mTargetFile.delete();
        }
    }

    /**
     * 展示分辨率的popwoindow
     */
    PopupWindow popupWindow;
    void bottomwindow(final View view) {
        if (popupWindow != null && popupWindow.isShowing()) {
            return;
        }
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.pop_list_layout, null);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.removeAllViews();
        for (int i = 0; i < mQualityList.size(); i++) {
            TextView textView = new TextView(this);
            textView.setText(mQualityList.get(i));
            textView.setTextSize(16);
            textView.setGravity(Gravity.CENTER);
            textView.setBackgroundResource(R.color.white);
            textView.setPadding(20,5,20,5);
            final int finalI = i;
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final String item = mQualityList.get(finalI);
                    mTvSp.setText(item);
                    if (item.equals("480p")) {
                        changeVideoQuality(CamcorderProfile.QUALITY_480P);
                    } else if (item.equals("720p")) {
                        changeVideoQuality(CamcorderProfile.QUALITY_720P);
                    } else if (item.equals("1080p")) {
                        changeVideoQuality(CamcorderProfile.QUALITY_1080P);
                    } else if (item.equals("2160p")) {
                        changeVideoQuality(CamcorderProfile.QUALITY_2160P);
                    }
                    if (popupWindow.isShowing()) {
                        popupWindow.dismiss();
                    }
                }
            });
            layout.addView(textView);
            TextView line = new TextView(this);
            line.setHeight(1);
            line.setBackgroundResource(R.color.colorPrimaryDark);
            layout.addView(line);
        }
        popupWindow = new PopupWindow(layout,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        //点击空白处时，隐藏掉pop窗口
        popupWindow.setFocusable(true);
        popupWindow.setBackgroundDrawable(new BitmapDrawable());
        //添加弹出、弹入的动画
        popupWindow.setAnimationStyle(R.style.Popupwindow);

        final int[] location = new int[2];
        view.getLocationOnScreen(location);
        View contentView = popupWindow.getContentView();
        //需要先测量，PopupWindow还未弹出时，宽高为0
        contentView.measure(makeDropDownMeasureSpec(popupWindow.getWidth()),
                makeDropDownMeasureSpec(popupWindow.getHeight()));
        //popupWindow.showAsDropDown(view, (view.getWidth() - contentView.getMeasuredWidth()) / 2, -(view.getHeight() + contentView.getMeasuredHeight()));
          popupWindow.showAtLocation(view,Gravity.NO_GRAVITY,(view.getWidth() - contentView.getMeasuredWidth()) / 2,location[1] - contentView.getMeasuredHeight());
        /*int[] location = new int[2];
        view.getLocationOnScreen(location);
        popupWindow.showAtLocation(view, Gravity.NO_GRAVITY, 0, location[1]);*/
    }

    @SuppressWarnings("ResourceType")
    private static int makeDropDownMeasureSpec(int measureSpec) {
        int mode;
        if (measureSpec == ViewGroup.LayoutParams.WRAP_CONTENT) {
            mode = View.MeasureSpec.UNSPECIFIED;
        } else {
            mode = View.MeasureSpec.EXACTLY;
        }
        return View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(measureSpec), mode);
    }

    //reload成像质量
    private void reloadQualities(int idCamera) {
        if (null != mQualityList) {
            mQualityList.clear();
        }
        SharedPreferences prefs = getSharedPreferences("RECORDING", Context.MODE_PRIVATE);

        quality = prefs.getInt("QUALITY", CamcorderProfile.QUALITY_480P);

        changeVideoQuality(quality);

        int maxQualitySupported = CamcorderProfile.QUALITY_480P;

        if (CamcorderProfile.hasProfile(idCamera, CamcorderProfile.QUALITY_480P)) {
            mQualityList.add("480p");
            maxQualitySupported = CamcorderProfile.QUALITY_480P;
        }
        if (CamcorderProfile.hasProfile(idCamera, CamcorderProfile.QUALITY_720P)) {
            mQualityList.add("720p");
            maxQualitySupported = CamcorderProfile.QUALITY_720P;
        }
        if (CamcorderProfile.hasProfile(idCamera, CamcorderProfile.QUALITY_1080P)) {
            mQualityList.add("1080p");
            maxQualitySupported = CamcorderProfile.QUALITY_1080P;
        }
        if (CamcorderProfile.hasProfile(idCamera, CamcorderProfile.QUALITY_2160P)) {
            mQualityList.add("2160p");
            maxQualitySupported = CamcorderProfile.QUALITY_2160P;
        }

        if (!CamcorderProfile.hasProfile(idCamera, quality)) {
            quality = maxQualitySupported;
            updateButtonText(maxQualitySupported);
        }

    }

    //修改录像质量
    private void changeVideoQuality(int quality) {
        SharedPreferences prefs = getSharedPreferences("RECORDING", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("QUALITY", quality);
        editor.commit();
        this.quality = quality;
        updateButtonText(quality);
    }

    private void updateButtonText(int quality) {
        if (quality == CamcorderProfile.QUALITY_480P)
            mTvSp.setText("480p");
        if (quality == CamcorderProfile.QUALITY_720P)
            mTvSp.setText("720p");
        if (quality == CamcorderProfile.QUALITY_1080P)
            mTvSp.setText("1080p");
        if (quality == CamcorderProfile.QUALITY_2160P)
            mTvSp.setText("2160p");
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            mCamera.lock();
        }
    }

    /**
     * 返回视频接口
     */
    public interface onReturnVideoFile{
        void onReturn(String file);
    }

    onReturnVideoFile mOnReturnVideoFile;

    public void setOnReturnVideoFile(onReturnVideoFile onReturnVideoFile){
        this.mOnReturnVideoFile = onReturnVideoFile;
    }
}
