package com.zjb.test.videoshottest.view.widget;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.LinearLayout;

import com.zjb.test.videoshottest.R;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by khb on 2016/12/7.
 */
public class VideoRecorderView extends LinearLayout implements MediaRecorder.OnErrorListener {

    private Context context;
    private int mWidth;
    private int mHeight;
    private boolean mIsCameraOpened;
    private int mMaxRecordTime;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private long sizePicture;
    private File recordFile;
    private MediaRecorder mediaRecorder;
    private int timeCount;
    private Timer timer;

    public VideoRecorderView(Context context) {
        this(context, null);
    }

    public VideoRecorderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoRecorderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        TypedArray typedArray = context.obtainStyledAttributes(attrs,
                R.styleable.VideoRecorderView, defStyleAttr, 0);
        mWidth = typedArray.getInteger(R.styleable.VideoRecorderView_recorder_width, 640);
        mHeight = typedArray.getInteger(R.styleable.VideoRecorderView_recorder_height, 360);
        mIsCameraOpened = typedArray.getBoolean(R.styleable.VideoRecorderView_is_camera_opened, true);
        mMaxRecordTime = typedArray.getInteger(R.styleable.VideoRecorderView_max_record_time, 10);
        LayoutInflater.from(context).inflate(R.layout.view_video_record, this);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new MySurfaceViewCallBack());
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        typedArray.recycle();
    }

    private class MySurfaceViewCallBack implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (!mIsCameraOpened){
                return ;
            }
            try {
                initCamera();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (!mIsCameraOpened){
                return ;
            }
            freeResource();
        }
    }

    private void initCamera() throws IOException {
        if (camera!=null){
            freeResource();
        }
//        优先使用前置摄像头，没有就使用后置摄像头
        try {
            if (checkCameraFacing(Camera.CameraInfo.CAMERA_FACING_BACK)) {
                camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            }else if (checkCameraFacing(Camera.CameraInfo.CAMERA_FACING_FRONT)){
                camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            }
        }catch (Exception e){
            e.printStackTrace();
            freeResource();
            ((Activity)context).finish();
        }
        if (camera == null){
            return ;
        }
        setCameraParams();
        camera.setDisplayOrientation(90);   //摄像头翻转90度
        camera.setPreviewDisplay(surfaceHolder);
        camera.startPreview();
        camera.unlock();

    }

    private void setCameraParams() {
        if (camera != null){
            Camera.Parameters params = camera.getParameters();
            params.set("orientation", "portrait");
            List<Camera.Size> supportedPictureSizes = params.getSupportedPictureSizes();
            for (Camera.Size size : supportedPictureSizes){
                sizePicture = (size.width*size.height) > sizePicture
                        ? size.width*size.height
                        : sizePicture;
            }
            Log.i("VRV", "手机支持的最大像素："+sizePicture);
            setPreviewSize(params);
            camera.setParameters(params);
        }
    }

    //根据手机支持的分辨率，设置预览尺寸
    private void setPreviewSize(Camera.Parameters params) {
        if (camera == null){
            return;
        }
//        float tmp;
//        float radio = 3.0f/4.0f;
        Camera.Size bestSize = null;
//        float minDiff = 100f;

        bestSize = setSize(params);
        Log.i("VRV", "bestSize w:"+ bestSize.width
                + "   h:"+bestSize.height);
        params.setPreviewSize(bestSize.width, bestSize.height);
        if (params.getSupportedVideoSizes() == null
                || params. getSupportedVideoSizes().size() == 0){
            mWidth = bestSize.width;
            mHeight = bestSize.height;
        }else {
//            如果有手机获取不到录制尺寸，就把预览尺寸用在录制尺寸上
            bestSize = setSize(params);
            mWidth = bestSize.width;
            mHeight = bestSize.height;
        }
    }

    private Camera.Size setSize(Camera.Parameters params) {
        float tmp;
        float radio = 3.0f/4.0f;
        Camera.Size bestSize = null;
        float minDiff = 100f;

        List<Camera.Size> supportedPreviewSizes = params.getSupportedPreviewSizes();
        Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size l, Camera.Size r) {
                if (l.width > r.width) {
                    return -1;
                } else if (l.width == r.width) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });
        for (Camera.Size supportedPreiview:
                supportedPreviewSizes) {
            Log.i("VRV", "supportedPreiview w:"+ supportedPreiview.width
                    + "   h:"+supportedPreiview.height);
            tmp = Math.abs(((float)supportedPreiview.height/(float)supportedPreiview.width)
                    - radio);
            if (tmp < minDiff){
                minDiff = tmp;
                bestSize = supportedPreiview;
            }
        }
        Log.i("VRV", "bestSize w:"+ bestSize.width
                + "   h:"+bestSize.height);
        return bestSize;
    }


    //释放摄像头资源
    private void freeResource() {
        try {
            if (camera != null) {
                camera.setPreviewCallback(null);
                camera.lock();
                camera.release();
                camera = null;
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            camera = null;
        }
    }

    //检查是否有指定的摄像头
    private boolean checkCameraFacing(int facing){
        int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i<cameraCount; i++){
            Camera.getCameraInfo(i, info);
            if (info.facing == facing){
                return true;
            }
        }
        return false;
    }

//    视频文件存放目录
    private void createRecordDir(){
        File sampleDir = new File(Environment.getExternalStorageDirectory()
                + File.separator + "TestSnapShots");
        if (!sampleDir.exists()){
            sampleDir.mkdirs();
        }
        try {
            recordFile = new File(sampleDir, "snapshots_" + System.currentTimeMillis() + ".mp4");
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initRecord() throws IOException {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.reset();
        if (camera == null){
            return ;
        }
        mediaRecorder.setCamera(camera);
        mediaRecorder.setOnErrorListener(this);
        mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
        mediaRecorder.setVideoSize(mWidth, mHeight);
        if (sizePicture < 3000000) {//设置分辨率，控制清晰度
            mediaRecorder.setVideoEncodingBitRate(3*1920*1080);
        } else if (sizePicture <= 5000000){
            mediaRecorder.setVideoEncodingBitRate(2 * 1920*1080);
        } else {
            mediaRecorder.setVideoEncodingBitRate(1 * 1920*1080);
        }
        mediaRecorder.setOrientationHint(90);  //输出旋转90度，保持竖屏录制
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);     //视频录制格式
        mediaRecorder.setOutputFile(recordFile.getAbsolutePath());
        mediaRecorder.prepare();
        mediaRecorder.start();
    }

    public void record(final OnRecordFinishListener onRecordFinishListener){
        this.onRecordFinishListener = onRecordFinishListener;
        createRecordDir();
        try {
            if (!mIsCameraOpened) {
                initCamera();
            }
            initRecord();
            timeCount = 0;  //时间计数器重设
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    timeCount++;
                    if (onRecordProgressListener != null){
                        onRecordProgressListener.onProgressChanged(mMaxRecordTime, timeCount);
                    }
                    if (timeCount == mMaxRecordTime){
                        stop();
                        if (VideoRecorderView.this.onRecordFinishListener != null){
                            VideoRecorderView.this.onRecordFinishListener.onRecordFinish();
                        }
                    }
                }
            }, 0, 1000);
        } catch (IOException e) {
            e.printStackTrace();
            if (mediaRecorder != null){
                mediaRecorder.release();
            }
            freeResource();
        }
    }

    public void stop() {
        stopRecord();
        releaseRecord();
        freeResource();
    }

    private void stopRecord() {
        if (timer != null){
            timer.cancel();
        }
        if (mediaRecorder != null){
            mediaRecorder.setOnErrorListener(null);
            mediaRecorder.setPreviewDisplay(null);
            try {
                mediaRecorder.stop();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void releaseRecord() {
        if (mediaRecorder != null){
            mediaRecorder.setOnErrorListener(null);
            try {
                mediaRecorder.release();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        mediaRecorder = null;
    }

    public int getTimeCount(){
        return timeCount;
    }

    public void setRecordMaxTime(int recordMaxTime){
        this.mMaxRecordTime = recordMaxTime;
    }

    public File getRecordFile(){
        return recordFile;
    }



    @Override
    public void onError(MediaRecorder mr, int what, int extra) {

    }
        
        
    private OnRecordFinishListener onRecordFinishListener;
    
    public interface OnRecordFinishListener {
        void onRecordFinish();
    }

    private OnRecordProgressListener onRecordProgressListener;

    public interface OnRecordProgressListener {
        void onProgressChanged(int maxTime, int currentTime);
    }

    public void setOnRecordProgressListener(OnRecordProgressListener onRecordProgressListener){
        this.onRecordProgressListener = onRecordProgressListener;
    }
        
}
