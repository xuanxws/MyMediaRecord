package huainan.kidyn.cn.mymediarecord;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity implements MediaRecordActivity.onReturnVideoFile{

    ImageButton mIbRecord;
    ImageButton mIbCompress;
    TextView mTvFilePath;
    TextView mTvCompressState;
    private Compressor mCompressor;

    public static final int REQUEST_FILE = 1001;
    private String videoFile = "";//录制的视频路径
    private String currentOutputVideoPath = "/storage/emulated/0/Movies/out.mp4";
    String cmd = "-y -i " + videoFile + " -strict -2 -vcodec libx264 -preset ultrafast " +
            "-crf 24 -acodec aac -ar 44100 -ac 2 -b:a 96k -s 640x480 -aspect 16:9 " + currentOutputVideoPath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView(){
        mIbRecord = findViewById(R.id.ib_record);
        mIbCompress = findViewById(R.id.ib_compress);
        mTvFilePath = findViewById(R.id.tv_filepath);
        mTvCompressState = findViewById(R.id.tv_compress_state);
        mCompressor = new Compressor(this);
        mCompressor.loadBinary(new InitListener() {
            @Override
            public void onLoadSuccess() {
                Log.v("test", "load library succeed");
            }

            @Override
            public void onLoadFail(String reason) {
                Log.i("test", "load library fail:" + reason);
            }
        });
        mIbRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,MediaRecordActivity.class);
                startActivityForResult(intent,REQUEST_FILE);
            }
        });
        mIbCompress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (null == videoFile || "".equals(videoFile)) {
                    Toast.makeText(MainActivity.this, "没有视频可以压缩！！！", Toast.LENGTH_SHORT).show();
                } else {
                    refreshCurrentPath();
                    if (TextUtils.isEmpty(cmd)) {
                        Toast.makeText(MainActivity.this, getString(R.string.compree_please_input_command)
                                , Toast.LENGTH_SHORT).show();
                    } else if (TextUtils.isEmpty(videoFile)) {
                        Toast.makeText(MainActivity.this, R.string.no_video_tips, Toast.LENGTH_SHORT).show();
                    } else {
                        File file = new File(currentOutputVideoPath);
                        if (file.exists()) {
                            file.delete();
                        }
                        execCommand(cmd);
                    }
                }
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode){
                case REQUEST_FILE:
                    videoFile = data.getStringExtra("file");
                    if (null != videoFile && !"".equals(videoFile)) {
                        mTvFilePath.setText("当前路径为：" + videoFile);
                    }
                    Log.i("test","videoFile===" + videoFile);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onReturn(String file) {

    }

    private void refreshCurrentPath() {
        cmd = "-y -i " + videoFile + " -strict -2 -vcodec libx264 -preset ultrafast " +
                "-crf 24 -acodec aac -ar 44100 -ac 2 -b:a 96k -s 480x320 -aspect 16:9 " + currentOutputVideoPath;
    }

    private void execCommand(String cmd) {
        Log.i("test", "开始压缩...");
        File mFile = new File(currentOutputVideoPath);
        if (mFile.exists()) {
            mFile.delete();
        }
        mCompressor.execCommand(cmd, new CompressListener() {
            @Override
            public void onExecSuccess(String message) {
                Log.i("test", "success " + message);
                mTvCompressState.setText("压缩成功~~~");
            }

            @Override
            public void onExecFail(String reason) {
                Log.i("test", "fail " + reason);
                mTvCompressState.setText("压缩失败！！！");
            }

            @Override
            public void onExecProgress(String message) {
                Log.i("test", "progress " + message);
                mTvCompressState.setText("压缩中...");
            }
        });
    }
}
