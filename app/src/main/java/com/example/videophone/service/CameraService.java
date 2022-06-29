package com.example.videophone.service;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraService {
    public static final String LOG_TAG = CameraService.class.getName();

    public static TextureView view;

    private MediaCodec mCodec = null; // кодер
    Surface mEncoderSurface; // Surface как вход данных для кодера

    private Handler mBackgroundHandler = null;

    private ByteBuffer outPutByteBuffer;


    private String cameraID;

    private CameraDevice camera;

    private CameraCaptureSession mSession;

    private CaptureRequest.Builder mPreviewBuilder;

    private CameraManager manager;

    public CameraService(CameraManager manager, String cameraID) {
        this.manager = manager;
        this.cameraID = cameraID;
    }

    private CameraDevice.StateCallback mCameraCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            CameraService.this.camera = camera;
            Log.i(LOG_TAG, "Open camera  with id:" + CameraService.this.camera.getId());

            startCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            CameraService.this.camera.close();

            Log.i(LOG_TAG, "disconnect camera  with id:" + CameraService.this.camera.getId());
            CameraService.this.camera = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.i(LOG_TAG, "error! camera id:" + camera.getId() + " error:" + error);
        }
    };

    private void startCameraPreviewSession() {
        SurfaceTexture texture = view.getSurfaceTexture();
        texture.setDefaultBufferSize(320, 240);
        Surface surface = new Surface(texture);
        try {
            mPreviewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuilder.addTarget(surface);
            mPreviewBuilder.addTarget(mEncoderSurface);
            camera.createCaptureSession(Arrays.asList(surface, mEncoderSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            mSession = session;
                            try {
                                mSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                        }
                    }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    public boolean isOpen() {
        return camera != null;
    }

    public void openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(null, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            {
                manager.openCamera(cameraID, mCameraCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            Log.i(LOG_TAG, e.getMessage());
        }
    }

    public void closeCamera() {
        if (camera != null) {
            //camera.close();
            camera = null;
        }
    }

    public void stopStreamingVideo() {
        if (camera != null & mCodec != null) {
            try {
                mSession.stopRepeating();
                mSession.abortCaptures();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            mCodec.stop();
            mCodec.release();
            mEncoderSurface.release();
            closeCamera();
        }
    }

    private void setUpMediaCodec() {
        try {
            mCodec = MediaCodec.createEncoderByType("video/avc"); // H264 кодек
        } catch (Exception e) {
            Log.i(LOG_TAG, "а нету кодека");
        }
        int width = 320; // ширина видео
        int height = 240; // высота видео
        int colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface; // формат ввода цвета
        int videoBitrate = 500000; // битрейт видео в bps (бит в секунду)
        int videoFramePerSecond = 20; // FPS
        int iframeInterval = 3; // I-Frame интервал в секундах

        MediaFormat format = MediaFormat.createVideoFormat("video/avc", width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
        format.setInteger(MediaFormat.KEY_BIT_RATE, videoBitrate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, videoFramePerSecond);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iframeInterval);


        mCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE); // конфигурируем кодек как кодер
        mEncoderSurface = mCodec.createInputSurface(); // получаем Surface кодера

        //mCodec.setCallback(new EncoderCallback());
        mCodec.start(); // запускаем кодер
        Log.i(LOG_TAG, "запустили кодек");
    }

    public class EncoderCallback extends MediaCodec.Callback {
        private String address;

        private int port;

        @Override
        public void onInputBufferAvailable(MediaCodec codec, int index) {

        }

        @Override
        public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
            outPutByteBuffer = mCodec.getOutputBuffer(index);
            byte[] outDate = new byte[info.size];
            outPutByteBuffer.get(outDate);
            try {
                DatagramPacket packet = new DatagramPacket(outDate, outDate.length, InetAddress.getByName(address), port);
                //udpSocket.send(packet);
            } catch (IOException e) {
                Log.i(LOG_TAG, " не отправился UDP пакет");
            }
            mCodec.releaseOutputBuffer(index, false);
        }

        @Override
        public void onError(MediaCodec codec, MediaCodec.CodecException e) {
            Log.i(LOG_TAG, "Error: " + e);
        }

        @Override
        public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
            Log.i(LOG_TAG, "encoder output format changed: " + format);
        }

        /*@Override
        public void onPause() {
            if (myCameras[CAMERA1].isOpen()) {
                myCameras[CAMERA1].closeCamera();
            }
            stopBackgroundThread();
            super.onPause();
        }

        @Override
        public void onResume() {
            super.onResume();
            startBackgroundThread();
        }*/
    }
}



/**/