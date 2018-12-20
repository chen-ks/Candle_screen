package com.example.candle_screen;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.util.TimeUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.util.Log;

import android.view.View;

import android.widget.Button;

import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String mFileName = null;

    private MediaRecorder mRecorder = null;

    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};
    private int BASE = 1;
    private int SPACE = 100;
    private double recorderDB;
    private ImageView imageView;
    private DevicePolicyManager devicePolicyManager;


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }

    private void onRecord() {
            startRecording();
    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mFileName = getExternalCacheDir().getAbsolutePath();
        mFileName += DateFormat.format("yyyyMMdd_HHmmss", Calendar.getInstance(Locale.CHINA))+".3gp";
        //     Toast.makeText(MainActivity.this,mFileName+"",Toast.LENGTH_LONG).show();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();

    }
    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Record to the external cache directory for visibility
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        getAdmin();
        devicePolicyManager=(DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        imageView=(ImageView)findViewById(R.id.id_image);
        onRecord();
        updateMicStatus();
    }

    private void getAdmin() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        // 激活MyAdmin广播接收着
        ComponentName who = new ComponentName(this, Admin.class);

        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, who);
        // 说明用户开启管理员权限的好处
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "开启可以一键锁屏，防止勿碰");
        startActivity(intent);

        Toast.makeText(MainActivity.this, "管理员权限已开启!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
    }

    private final Handler mHandler = new Handler();
    private Runnable mUpdateMicStatusTimer = new Runnable() {
        public void run() {
            updateMicStatus();
        }
    };
    private void updateMicStatus() {
        if (mRecorder != null) {
            double ratio = (double)mRecorder.getMaxAmplitude() /BASE;
            recorderDB = 0;
            if (ratio > 1)
                recorderDB = 20 * Math.log10(ratio);
            Log.d("uuu",recorderDB+"");
            if(recorderDB>=50){
               imageView.setBackgroundResource(R.drawable.candle_flow);
                Message msg=new Message();
                msg.what=1;
                myHandler.sendMessage(msg);
            }
            mHandler.postDelayed(mUpdateMicStatusTimer, SPACE);
        }
    }

    Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                        lockScreen();
                        }
                    },1500);
                    break;

            }
        }
    };

    private void lockScreen(){
        ComponentName who = new ComponentName(this, Admin.class);
        // 判断是否已经开启管理员权限
        if (devicePolicyManager.isAdminActive(who)) {
            // 锁屏
            devicePolicyManager.lockNow();
            // 设置屏幕密码 第一个是密码 第二个是附加参数
            devicePolicyManager.resetPassword("123", 0);
        } else {
            // 如果为未开启 提示
            Toast.makeText(MainActivity.this, "请先开启管理员权限!", Toast.LENGTH_SHORT)
                    .show();
        }
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (0 == requestCode) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}

