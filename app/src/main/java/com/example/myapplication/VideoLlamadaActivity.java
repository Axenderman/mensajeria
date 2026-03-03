package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.Random;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;

public class VideoLlamadaActivity extends AppCompatActivity {

    private static final String TAG = "VideoLlamadaActivity";
    
    // App ID actualizado (Modo Testing sin Token)
    private final String appId = "4759378f3210475da9bcc0df992ec002";
    private final String channelName = "test-channel";
    private final String token = ""; 

    private RtcEngine agoraEngine;
    private SurfaceView localSurfaceView;
    private SurfaceView remoteSurfaceView;
    private int myUid = new Random().nextInt(10000) + 1;

    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };

    private boolean checkSelfPermission() {
        return ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[0]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[1]) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_llamada);

        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
        } else {
            initAgoraAndJoin();
        }
    }

    private void initAgoraAndJoin() {
        setupVideoSDKEngine();
        setupLocalVideo();
        joinChannel();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                initAgoraAndJoin();
            } else {
                Toast.makeText(this, "Permisos denegados", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        if (agoraEngine != null) {
            agoraEngine.stopPreview();
            agoraEngine.leaveChannel();
        }
        new Thread(() -> {
            RtcEngine.destroy();
            agoraEngine = null;
        }).start();
    }

    private void setupVideoSDKEngine() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getBaseContext();
            config.mAppId = appId;
            config.mEventHandler = mRtcEventHandler;
            agoraEngine = RtcEngine.create(config);
            agoraEngine.enableVideo();
            
            VideoEncoderConfiguration videoConfig = new VideoEncoderConfiguration();
            videoConfig.orientationMode = VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT;
            agoraEngine.setVideoEncoderConfiguration(videoConfig);
            
        } catch (Exception e) {
            Log.e(TAG, "Error al iniciar Agora", e);
            Toast.makeText(this, "Error al iniciar Agora: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onUserJoined(int uid, int elapsed) {
            Log.d(TAG, "Usuario remoto unido: " + uid);
            runOnUiThread(() -> setupRemoteVideo(uid));
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            Log.d(TAG, "Conectado: " + channel + " con UID: " + uid);
            runOnUiThread(() -> Toast.makeText(VideoLlamadaActivity.this, "Conectado con éxito", Toast.LENGTH_SHORT).show());
        }

        @Override
        public void onError(int err) {
            Log.e(TAG, "Error de Agora: " + err);
            runOnUiThread(() -> {
                if (err == 101) {
                    Toast.makeText(VideoLlamadaActivity.this, "Error 101: App ID inválido o requiere Token", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(VideoLlamadaActivity.this, "Error: " + err, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(() -> {
                if (remoteSurfaceView != null) {
                    remoteSurfaceView.setVisibility(View.GONE);
                }
            });
        }
    };

    private void setupLocalVideo() {
        FrameLayout container = findViewById(R.id.local_video_view_container);
        if (container == null) return;
        
        localSurfaceView = new SurfaceView(getBaseContext());
        localSurfaceView.setZOrderMediaOverlay(true);
        if (container.getChildCount() > 0) container.removeAllViews();
        container.addView(localSurfaceView);
        
        agoraEngine.setupLocalVideo(new VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_FIT, 0));
        agoraEngine.startPreview();
    }

    private void setupRemoteVideo(int uid) {
        FrameLayout container = findViewById(R.id.remote_video_view_container);
        if (container == null) return;

        remoteSurfaceView = new SurfaceView(getBaseContext());
        if (container.getChildCount() > 0) container.removeAllViews();
        container.addView(remoteSurfaceView);
        
        agoraEngine.setupRemoteVideo(new VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_FIT, uid));
    }

    private void joinChannel() {
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        options.autoSubscribeAudio = true;
        options.autoSubscribeVideo = true;
        options.publishCameraTrack = true;
        options.publishMicrophoneTrack = true;
        
        agoraEngine.joinChannel(token, channelName, myUid, options);
    }
}
