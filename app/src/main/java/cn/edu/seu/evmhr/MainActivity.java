package cn.edu.seu.evmhr;
import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.view.View.OnClickListener;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import cn.edu.seu.evmhr.Utils.CacheUtil;
import cn.edu.seu.evmhr.Utils.Utils;

public class MainActivity extends AppCompatActivity implements OnClickListener, SurfaceHolder.Callback, OnErrorListener,OnInfoListener{
    private final static String CLASS_LABEL = "MainActivity";
    private PowerManager.WakeLock mWakeLock;
    private ImageView btnStart;// 开始录制按钮
    private ImageView btnStop;// 停止录制按钮

    private MediaRecorder mediaRecorder;// 录制视频的类
    private SurfaceView mVideoView;// 显示视频的控件

    String localPath = "";// 录制的视频路径

    private Camera mCamera;

    // 预览的宽高
    private int previewWidth = 800;
    private int previewHeight = 480;

    private int frontCamera = 1;// 0是后置摄像头，1是前置摄像头

    private SurfaceHolder mSurfaceHolder;
    int defaultVideoFrameRate = -1;

    private int VIDEOID = 1;

    private MyCount myCount;
    TextView tv;//显示倒计时数字
    private FFmpeg ffmpeg;
    private ProgressDialog mProgressDialog;
    private String result;
    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE };
    /**
     * Checks if the app has permission to write to device storage
     * If the app does not has permission then the user will be prompted to
     * grant permissions
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
// Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
// We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏
        // 选择支持半透明模式，在有surfaceview的activity中使用
        verifyStoragePermissions(MainActivity.this);
        initViews();
        initFfmepg();
    }

    private void initFfmepg() {
        ffmpeg = FFmpeg.getInstance(MainActivity.this);
        mProgressDialog = new ProgressDialog(MainActivity.this);
        mProgressDialog.setTitle(getString(R.string.initing));
        mProgressDialog.show();
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                @Override
                public void onStart() {

                }

                @Override
                public void onFailure() {
                    new AlertDialog.Builder(MainActivity.this)
                    .setMessage(getString(R.string.initfail))
                            .setPositiveButton(R.string.ok,new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,int which) {
                                    finish();
                                }
                            })
                            .setCancelable(false).show();
                }

                @Override
                public void onSuccess() {
                    mProgressDialog.dismiss();
                }

                @Override
                public void onFinish() {}
            });
        } catch (FFmpegNotSupportedException e) {

            new AlertDialog.Builder(MainActivity.this)
                    .setMessage(getString(R.string.not_support))
                    .setPositiveButton(R.string.ok,new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,int which) {
                            finish();
                        }
                    })
                    .setCancelable(false).show();
        }
    }

    private void initViews() {
        mVideoView = (SurfaceView) findViewById(R.id.mVideoView);
        //mVideoView.setVisibility(View.INVISIBLE);

        btnStart = (ImageView) findViewById(R.id.recorder_start);
        btnStop = (ImageView) findViewById(R.id.recorder_stop);
        tv = (TextView) findViewById(R.id.tv);

        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);

        mSurfaceHolder = mVideoView.getHolder();
        mSurfaceHolder.addCallback(this);
        //mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);



    }
    @Override
    protected void onResume() {
        super.onResume();
        keepWakeLock();
        if (!initCamera()) {
            showFailDialog();
        }
    }

    private void keepWakeLock() {
        if (mWakeLock == null) {
            // 获取唤醒锁,保持屏幕常亮
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,CLASS_LABEL);
            mWakeLock.acquire();
        }
    }


    private boolean initCamera() {
        // check Android 6 permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i("TEST","Granted");
            //init(barcodeScannerView, getIntent(), null);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 1);//1 can be another integer
        }
        try {
            if (frontCamera == 0) {
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                mCamera.unlock();
            } else {
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                mCamera.unlock();
            }
            mCamera.lock();
            mSurfaceHolder = mVideoView.getHolder();
            mSurfaceHolder.addCallback(this);
            //mCamera.setDisplayOrientation(270);
        } catch (RuntimeException ex) {
            return false;
        }
        return true;
    }

    private void handleSurfaceChanged() {

        surfaceChanged(mSurfaceHolder,0,0,0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        myCount = new MyCount(10 * 1000, 1000);
    }

    @Override
    protected void onStop() {
        super.onStop();
        myCount.cancel();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.recorder_start:
                // 开始录制
                deletevideo();
                if (!startRecording())return;
                Toast.makeText(this, R.string.The_video_to_start,Toast.LENGTH_SHORT).show();
                btnStart.setVisibility(View.INVISIBLE);
                btnStop.setVisibility(View.VISIBLE);
                // 重置其他
                myCount.start();
                break;
            case R.id.recorder_stop:
                // 停止拍摄
                myCount.cancel();
                tv.setText("");
                stopRecording();
                btnStart.setVisibility(View.VISIBLE);
                btnStop.setVisibility(View.INVISIBLE);
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(getString(R.string.stopped))
                        .setPositiveButton(R.string.ok,new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,int which) {
                                dialog.dismiss();
                                if (mCamera == null) {
                                    initCamera();
                                }
                                try {
                                    mCamera.setPreviewDisplay(mSurfaceHolder);
                                    mCamera.startPreview();
                                    handleSurfaceChanged();
                                } catch (IOException e1) {

                                }
                            }
                        })
                        .setCancelable(false).show();
                deletevideo();
                break;

            default:
                break;
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {
        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        Camera.Size selectedSize  =getBestSupportPreviewSize(sizes, getScreenSize());

//设定摄像机预览界面尺寸
        params.setPreviewSize(selectedSize.width, selectedSize.height);
        mCamera.setParameters(params);

        mCamera.setDisplayOrientation(90);
        mCamera.startPreview();
    }
    public static Camera.Size getBestSupportPreviewSize(List<Camera.Size> previewSizes, Camera.Size screenSize) {
        double screenRatio = screenSize.width * 1.0 / screenSize.height;
        Camera.Size maxSize = previewSizes.get(0);
        for (Camera.Size size : previewSizes) {
            double sizeRatio = size.width * 1.0 / size.height;
            if (size.width < 2000 && sizeRatio > screenRatio - 0.1 && sizeRatio < screenRatio + 0.1)
                maxSize = (size.width > maxSize.width) ? size : maxSize;
        }
        return maxSize;
    }
    private Camera.Size getScreenSize() {
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        int width = metric.widthPixels;  // 宽度（PX）
        int height = metric.heightPixels;  // 高度（PX）

        return mCamera.new Size(height, width);
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mCamera == null) {
            if (!initCamera()) {
                showFailDialog();
                return;
            }
        }
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
            handleSurfaceChanged();
            if (mCamera == null) {
                finish();
                return;
            }
            //获取摄像头支持的帧率
            boolean hasSupportRate = false;
            List<Integer> supportedPreviewFrameRates = mCamera.getParameters().getSupportedPreviewFrameRates();
            if (supportedPreviewFrameRates != null&& supportedPreviewFrameRates.size() > 0) {
                Collections.sort(supportedPreviewFrameRates);
                for (int i = 0; i < supportedPreviewFrameRates.size(); i++) {
                    int supportRate = supportedPreviewFrameRates.get(i);

                    if (supportRate == 30) {
                        hasSupportRate = true;
                    }

                }
                if (hasSupportRate) {
                    defaultVideoFrameRate = 30;
                } else {
                    defaultVideoFrameRate = supportedPreviewFrameRates.get(0);
                }

            }
            // 获取摄像头的所有支持的分辨率
            List<Camera.Size> resolutionList = Utils.getResolutionList(mCamera);
            if (resolutionList != null && resolutionList.size() > 0) {
                Collections.sort(resolutionList, new Utils.ResolutionComparator());
                Camera.Size previewSize = null;
                boolean hasSize = false;
                // 如果摄像头支持800*480，那么强制设为800*480
                for (int i = 0; i < resolutionList.size(); i++) {
                    Camera.Size size = resolutionList.get(i);
                    if (size != null && size.width == 800 && size.height == 480) {
                        previewSize = size;
                        previewWidth = previewSize.width;
                        previewHeight = previewSize.height;
                        hasSize = true;
                        break;
                    }
                }
                // 如果不支持设为中间的那个
                if (!hasSize) {
                    int mediumResolution = resolutionList.size() / 2;
                    if (mediumResolution >= resolutionList.size())
                        mediumResolution = resolutionList.size() - 1;
                    previewSize = resolutionList.get(mediumResolution);
                    previewWidth = previewSize.width;
                    previewHeight = previewSize.height;

                }

            }
        } catch (Exception e1) {
            showFailDialog();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0) {
    }

    public boolean startRecording() {
        if (mediaRecorder == null) {
            if (!initRecorder())
                return false;
        }
        mediaRecorder.setOnInfoListener(this);
        mediaRecorder.setOnErrorListener(this);
        mediaRecorder.start();
        return true;
    }


    private boolean initRecorder() {
        if (!Utils.isExitsSdcard()) {
            showNoSDCardDialog();
            return false;
        }

//        if (mCamera == null) {
//            if (!initCamera()) {
//                showFailDialog();
//                return false;
//            }
//        }

        mediaRecorder = new MediaRecorder();
        //mCamera.setDisplayOrientation(90);
        mCamera.unlock();

        mediaRecorder.setCamera(mCamera);

        //mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        // 设置录制视频源为Camera（相机）
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        if (frontCamera == 1) {
            mediaRecorder.setOrientationHint(270);
        } else {
            mediaRecorder.setOrientationHint(90);
        }
        // 设置录制完成后视频的封装格式THREE_GPP为3gp.MPEG_4为mp4
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        //mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        // 设置录制的视频编码h263 h264
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
        // 设置视频录制的分辨率。必须放在设置编码和格式的后面，否则报错
        //mediaRecorder.setVideoSize(previewWidth, previewHeight);
        // 设置视频的比特率
        mediaRecorder.setVideoEncodingBitRate(5*previewHeight *previewWidth );
        // // 设置录制的视频帧率。必须放在设置编码和格式的后面，否则报错
        if (defaultVideoFrameRate != -1) {
            //mediaRecorder.setVideoFrameRate(defaultVideoFrameRate);
        }
        // 设置视频文件输出的路径
        localPath = CacheUtil.getSDCacheDir("video") + "/"+ VIDEOID + ".mp4";
        mediaRecorder.setOutputFile(localPath);
        //设置最大录制视频时间10秒
        mediaRecorder.setMaxDuration(10000);
        //mediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }

    //停止录制
    public void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.setOnErrorListener(null);
            mediaRecorder.setOnInfoListener(null);
            try {
                mediaRecorder.stop();
            } catch (IllegalStateException e) {
            }
        }
        releaseRecorder();
        if (mCamera != null) {
            mCamera.stopPreview();
            releaseCamera();
        }
    }
    //释放
    private void releaseRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }
    //释放摄像头
    protected void releaseCamera() {
        try {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        } catch (Exception e) {
        }
    }
    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            stopRecording();
            btnStart.setVisibility(View.VISIBLE);
            btnStop.setVisibility(View.INVISIBLE);
            if (localPath == null) {
                return;
            }

        }

    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        stopRecording();
        Toast.makeText(this,"Recording error has occurred. Stopping the recording",Toast.LENGTH_SHORT).show();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();

        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
        deletevideo();
    }

    @Override
    public void onBackPressed() {
        releaseRecorder();
        releaseCamera();
        finish();
    }

    private void showFailDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.prompt)
                .setMessage(R.string.Open_the_equipment_failure)
                .setPositiveButton(R.string.ok,new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,int which) {
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void showNoSDCardDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.prompt)
                .setMessage("No sd card!")
                .setPositiveButton(R.string.ok,new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog,int which) {
                        finish();

                    }
                })
                .setCancelable(false)
                .show();
    }
    //倒计时
    class MyCount extends CountDownTimer {
        public MyCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            stopRecording();
            tv.setText("");
            String st3 = getResources().getString(R.string.complete);
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage(st3)
                    .setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,int which) {
                                        dialog.dismiss();
                                        if (mCamera == null) {
                                            initCamera();
                                        }
                                        try {
                                            mCamera.setPreviewDisplay(mSurfaceHolder);
                                            mCamera.startPreview();
                                            handleSurfaceChanged();
                                        } catch (IOException e1) {
                                        }
                                        btnStart.setVisibility(View.VISIBLE);
                                        btnStop.setVisibility(View.INVISIBLE);
                                        deletevideo();
                                    }

                            })
                    .setCancelable(false).show();
            mProgressDialog.setTitle("计算中，请稍候。。。");
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
            reformatvideo(1);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            tv.setText((millisUntilFinished / 1000) + "");
        }
    }

    private void deletevideo(){
        File file=new File(CacheUtil.getSDCacheDir("video")+"/1.mp4");
        if(file.exists()){
            file.delete();
        }
        file=new File(CacheUtil.getSDCacheDir("video")+"/1.avi");
        if(file.exists()){
            file.delete();
        }
    }
    private void reformatvideo(final int id){
        try {
            //-c:v mjpeg -an
            String cmd="-i "+CacheUtil.getSDCacheDir("video") + "/"+ id + ".mp4"+" -c:v mjpeg -an "+CacheUtil.getSDCacheDir("video") + "/"+ id + ".avi";
            String[] command = cmd.split(" ");
            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {
                }

                @Override
                public void onProgress(String message) {
                }

                @Override
                public void onFailure(String message) {
                    Log.e("e","fail:"+message);
                }

                @Override
                public void onSuccess(String message) {
                    Log.i("i","success:"+message);
                    Thread thread=new Thread(new Runnable() {
                        @Override
                        public void run() {
                            result = stringFromJNI(CacheUtil.getSDCacheDir("video") + "/"+ id + ".avi");
                            Message msg = new Message();
                            msg.what = 1;
                            mHandler.sendMessage(msg);
                        }
                    });
                    thread.start();
                }

                @Override
                public void onFinish() {
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            Log.e("e",e.toString());
            // Handle if FFmpeg is already running
        }
    }
    private Handler mHandler = new Handler(){
        // 覆写这个方法，接收并处理消息。
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                    tv.setText("心率："+ result);
                    mProgressDialog.dismiss();
                    break;
            }
        }
    };

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI(String a);

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
}
