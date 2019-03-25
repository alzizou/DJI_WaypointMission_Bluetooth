package com.example.android.bluetoothchatDJI;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.TextureView.SurfaceTextureListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.android.bluetoothchatDJI.R;

import java.util.ArrayList;
import java.util.List;

import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.product.Model;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.imu.IMUState;

import static android.os.SystemClock.sleep;

public class MainActivityDJI extends Activity implements SurfaceTextureListener,OnClickListener{

    private static final String TAG = MainActivityDJI.class.getName();
    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;

    // Codec for video live view
    protected DJICodecManager mCodecManager = null;

    protected TextureView mVideoSurface = null;
    private Button mCaptureBtn, mShootPhotoModeBtn, mRecordVideoModeBtn;
    private ToggleButton mRecordBtn;
    private TextView recordingTime;

    private Handler handler;
    private Handler handler1;

    private float Altitude = 6.0f;
    private float Speed = 3.0f;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;

    private double droneLocationLat = 0.0d;
    private double droneLocationLng = 0.0d;
    private double droneLocationAlt = 0.0d;
    private double droneVelocityX = 0.0d;
    private double droneVelocityY = 0.0d;
    private double droneVelocityZ = 0.0d;
    private double dronePitch = 0.0d;
    private double droneRoll = 0.0d;
    private double droneYaw = 0.0d;
    private double droneWx = 0.0d;
    private double droneWy = 0.0d;
    private double droneWz = 0.0d;

    private FlightController mFlightController;
    public static WaypointMission.Builder waypointMissionBuilder;
    private List<Waypoint> waypointList = new ArrayList<>();
    private WaypointMissionOperator instance;

    private TextView txt_long;
    private TextView txt_lat;
    private TextView txt_alt;
    private TextView txt_Vx;
    private TextView txt_Vy;
    private TextView txt_Vz;
    private TextView txt_pitch;
    private TextView txt_roll;
    private TextView txt_yaw;
    private TextView txt_Wx;
    private TextView txt_Wy;
    private TextView txt_Wz;

    private TextView mtxt_Alt_set;
    private TextView mtxt_Spd_set;
    private TextView mtxt_P1_lon;
    private TextView mtxt_P1_lat;
    private TextView mtxt_P2_lon;
    private TextView mtxt_P2_lat;
    private TextView mtxt_P3_lon;
    private TextView mtxt_P3_lat;
    private TextView mtxt_P4_lon;
    private TextView mtxt_P4_lat;

    private Button mBtn_Creat_Misn;
    private Button mBtn_Upload_Misn;
    private Button mBtn_Strt_Misn;
    private Button mBtn_Stop_Misn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_dji);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        txt_long = findViewById(R.id.textView4);
        txt_lat = findViewById(R.id.textView5);
        txt_alt = findViewById(R.id.textView6);
        txt_Vx = findViewById(R.id.textView13);
        txt_Vy = findViewById(R.id.textView14);
        txt_Vz = findViewById(R.id.textView15);
        txt_pitch = findViewById(R.id.textView19);
        txt_roll = findViewById(R.id.textView20);
        txt_yaw = findViewById(R.id.textView21);
        txt_Wx = findViewById(R.id.textView25);
        txt_Wy = findViewById(R.id.textView26);
        txt_Wz = findViewById(R.id.textView27);

        mtxt_Alt_set = findViewById(R.id.txt_Alt_set);
        mtxt_Spd_set = findViewById(R.id.txt_Spd_set);
        mtxt_P1_lon = findViewById(R.id.txt_P1_lon);
        mtxt_P1_lat = findViewById(R.id.txt_P1_lat);
        mtxt_P2_lon = findViewById(R.id.txt_P2_lon);
        mtxt_P2_lat = findViewById(R.id.txt_P2_lat);
        mtxt_P3_lon = findViewById(R.id.txt_P3_lon);
        mtxt_P3_lat = findViewById(R.id.txt_P3_lat);
        mtxt_P4_lon = findViewById(R.id.txt_P4_lon);
        mtxt_P4_lat = findViewById(R.id.txt_P4_lat);

        mBtn_Creat_Misn = findViewById(R.id.button_Creat_Misn);
        mBtn_Creat_Misn.setEnabled(false);

        mBtn_Upload_Misn = findViewById(R.id.button_Upload_Misn);
        mBtn_Upload_Misn.setEnabled(false);

        mBtn_Strt_Misn = findViewById(R.id.button_Strt_Misn);
        mBtn_Strt_Misn.setEnabled(false);

        mBtn_Stop_Misn = findViewById(R.id.button_Stop_Misn);
        mBtn_Stop_Misn.setEnabled(false);

        txt_long.setText(String.valueOf(0.0));
        txt_lat.setText(String.valueOf(0.0));
        txt_alt.setText(String.valueOf(0.0));
        txt_Vx.setText(String.valueOf(0.0));
        txt_Vy.setText(String.valueOf(0.0));
        txt_Vz.setText(String.valueOf(0.0));
        txt_pitch.setText(String.valueOf(0.0d));
        txt_roll.setText(String.valueOf(0.0d));
        txt_yaw.setText(String.valueOf(0.0d));
        txt_Wx.setText(String.valueOf(0.0d));
        txt_Wy.setText(String.valueOf(0.0d));
        txt_Wz.setText(String.valueOf(0.0d));

        mtxt_Alt_set.setText(String.valueOf(0.0d));
        mtxt_Spd_set.setText(String.valueOf(0.0d));
        mtxt_P1_lon.setText(String.valueOf(0.0d));
        mtxt_P1_lat.setText(String.valueOf(0.0d));
        mtxt_P2_lon.setText(String.valueOf(0.0d));
        mtxt_P2_lat.setText(String.valueOf(0.0d));
        mtxt_P3_lon.setText(String.valueOf(0.0d));
        mtxt_P3_lat.setText(String.valueOf(0.0d));
        mtxt_P4_lon.setText(String.valueOf(0.0d));
        mtxt_P4_lat.setText(String.valueOf(0.0d));

        Intent intent_Strt = getIntent();
        Bundle Received_Data = intent_Strt.getExtras();
        if (Received_Data != null){
            final String[] Data_AltSpd = Received_Data.getStringArray("AltSpd");
            final String[] Data_Point1 = Received_Data.getStringArray("Point-1");
            final String[] Data_Point2 = Received_Data.getStringArray("Point-2");
            final String[] Data_Point3 = Received_Data.getStringArray("Point-3");
            final String[] Data_Point4 = Received_Data.getStringArray("Point-4");

            mtxt_Alt_set.setText(Data_AltSpd[0]);
            mtxt_Spd_set.setText(Data_AltSpd[1]);

            mtxt_P1_lat.setText(Data_Point1[0]);
            mtxt_P1_lon.setText(Data_Point1[1]);
            mtxt_P2_lat.setText(Data_Point2[0]);
            mtxt_P2_lon.setText(Data_Point2[1]);
            mtxt_P3_lat.setText(Data_Point3[0]);
            mtxt_P3_lon.setText(Data_Point3[1]);
            mtxt_P4_lat.setText(Data_Point4[0]);
            mtxt_P4_lon.setText(Data_Point4[1]);

            mBtn_Creat_Misn.setEnabled(true);

        }

        handler = new Handler();
        handler1 = new Handler();

        IntentFilter filter = new IntentFilter();
        filter.addAction(FPVDemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver1, filter);

        initUI();

        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };

        Camera camera = FPVDemoApplication.getCameraInstance();

        if (camera != null) {

            camera.setSystemStateCallback(new SystemState.Callback() {
                @Override
                public void onUpdate(SystemState cameraSystemState) {
                    if (null != cameraSystemState) {

                        int recordTime = cameraSystemState.getCurrentVideoRecordingTimeInSeconds();
                        int minutes = (recordTime % 3600) / 60;
                        int seconds = recordTime % 60;

                        final String timeString = String.format("%02d:%02d", minutes, seconds);
                        final boolean isVideoRecording = cameraSystemState.isRecording();

                        MainActivityDJI.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                recordingTime.setText(timeString);

                                /*
                                 * Update recordingTime TextView visibility and mRecordBtn's check state
                                 */
                                if (isVideoRecording){
                                    recordingTime.setVisibility(View.VISIBLE);
                                }else
                                {
                                    recordingTime.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    }
                }
            });

        }

        handler1.post(runnable1);
        addListener();
    }

    Runnable runnable1= new Runnable() {
        @Override
        public void run() {
            if (mFlightController != null) {
                mFlightController.setStateCallback(new FlightControllerState.Callback() {

                    @Override
                    public void onUpdate(FlightControllerState djiFlightControllerCurrentState) {
                        droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                        droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                        droneLocationAlt = djiFlightControllerCurrentState.getAircraftLocation().getAltitude();
                        droneVelocityX = djiFlightControllerCurrentState.getVelocityX();
                        droneVelocityY = djiFlightControllerCurrentState.getVelocityY();
                        droneVelocityZ = djiFlightControllerCurrentState.getVelocityZ();
                        dronePitch = djiFlightControllerCurrentState.getAttitude().pitch;
                        droneRoll = djiFlightControllerCurrentState.getAttitude().roll;
                        droneYaw = djiFlightControllerCurrentState.getAttitude().yaw;
                    }
                });
            }
            if (checkGpsCoordination(droneLocationLat, droneLocationLng)) {
                txt_long.setText(String.valueOf(droneLocationLng));
                txt_lat.setText(String.valueOf(droneLocationLat));
                txt_alt.setText(String.valueOf(droneLocationAlt));
                txt_Vx.setText(String.valueOf(droneVelocityX));
                txt_Vy.setText(String.valueOf(droneVelocityY));
                txt_Vz.setText(String.valueOf(droneVelocityZ));
                txt_pitch.setText(String.valueOf(dronePitch));
                txt_roll.setText(String.valueOf(droneRoll));
                txt_yaw.setText(String.valueOf(droneYaw));
            }
            handler1.postDelayed(this, 100);
        }
    };

    protected BroadcastReceiver mReceiver1 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onProductChange();
        }
    };

    protected void onProductChange() {
        initPreviewer();
        initFlightController();
        loginAccount();
    }

    private void loginAccount(){

        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Log.e(TAG, "Login Success");
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        showToast("Login Error:"
                                + error.getDescription());
                    }
                });
    }

    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    private void initFlightController() {

        BaseProduct product1 = FPVDemoApplication.getProductInstance();
        if (product1 != null && product1.isConnected()) {
            if (product1 instanceof Aircraft) {
                mFlightController = ((Aircraft) product1).getFlightController();
            }
        }

        if (mFlightController != null) {
            mFlightController.setStateCallback(new FlightControllerState.Callback() {

                @Override
                public void onUpdate(FlightControllerState djiFlightControllerCurrentState) {
                    droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                    droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                    droneLocationAlt = djiFlightControllerCurrentState.getAircraftLocation().getAltitude();
                    droneVelocityX = djiFlightControllerCurrentState.getVelocityX();
                    droneVelocityY = djiFlightControllerCurrentState.getVelocityY();
                    droneVelocityZ = djiFlightControllerCurrentState.getVelocityZ();
                    dronePitch = djiFlightControllerCurrentState.getAttitude().pitch;
                    droneRoll = djiFlightControllerCurrentState.getAttitude().roll;
                    droneYaw = djiFlightControllerCurrentState.getAttitude().yaw;
                }
            });
        }
        if (checkGpsCoordination(droneLocationLat, droneLocationLng)) {
            txt_long.setText(String.valueOf(droneLocationLng));
            txt_lat.setText(String.valueOf(droneLocationLat));
            txt_alt.setText(String.valueOf(droneLocationAlt));
            txt_Vx.setText(String.valueOf(droneVelocityX));
            txt_Vy.setText(String.valueOf(droneVelocityY));
            txt_Vz.setText(String.valueOf(droneVelocityZ));
            txt_pitch.setText(String.valueOf(dronePitch));
            txt_roll.setText(String.valueOf(droneRoll));
            txt_yaw.setText(String.valueOf(droneYaw));
        }
    }

    //Add Listener for WaypointMissionOperator
    private void addListener() {
        if (getWaypointMissionOperator() != null){
            getWaypointMissionOperator().addListener(eventNotificationListener);
        }
    }

    private void removeListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().removeListener(eventNotificationListener);
        }
    }

    private WaypointMissionOperatorListener eventNotificationListener = new WaypointMissionOperatorListener() {
        @Override
        public void onDownloadUpdate(WaypointMissionDownloadEvent downloadEvent) {

        }

        @Override
        public void onUploadUpdate(WaypointMissionUploadEvent uploadEvent) {

        }

        @Override
        public void onExecutionUpdate(WaypointMissionExecutionEvent executionEvent) {

        }

        @Override
        public void onExecutionStart() {

        }

        @Override
        public void onExecutionFinish(@Nullable final DJIError error) {
            setResultToToast("Execution finished: " + (error == null ? "Success!" : error.getDescription()));
        }
    };


    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        initPreviewer();
        initFlightController();
        onProductChange();

        if(mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null");
        }
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        uninitPreviewer();
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    public void onReturn(View view){
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        uninitPreviewer();
        super.onDestroy();

        removeListener();
    }

    private void initUI() {
        // init mVideoSurface
        mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface);

        recordingTime = (TextView) findViewById(R.id.timer);
        mCaptureBtn = (Button) findViewById(R.id.btn_capture);
        mRecordBtn = (ToggleButton) findViewById(R.id.btn_record);
        mShootPhotoModeBtn = (Button) findViewById(R.id.btn_shoot_photo_mode);
        mRecordVideoModeBtn = (Button) findViewById(R.id.btn_record_video_mode);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }

        mCaptureBtn.setOnClickListener(this);
        mRecordBtn.setOnClickListener(this);
        mShootPhotoModeBtn.setOnClickListener(this);
        mRecordVideoModeBtn.setOnClickListener(this);

        mBtn_Creat_Misn.setOnClickListener(this);
        mBtn_Upload_Misn.setOnClickListener(this);
        mBtn_Strt_Misn.setOnClickListener(this);
        mBtn_Stop_Misn.setOnClickListener(this);

        recordingTime.setVisibility(View.INVISIBLE);

        mRecordBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startRecord();
                } else {
                    stopRecord();
                }
            }
        });
    }

    private void initPreviewer() {

        BaseProduct product = FPVDemoApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            showToast(getString(R.string.disconnected));
            mBtn_Upload_Misn.setEnabled(false);
            mBtn_Strt_Misn.setEnabled(false);
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
            }
        }
    }

    private void uninitPreviewer() {
        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null){
            // Reset the callback
            VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(null);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG,"onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivityDJI.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_capture:{
                captureAction();
                break;
            }
            case R.id.btn_shoot_photo_mode:{
                switchCameraMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO);
                break;
            }
            case R.id.btn_record_video_mode:{
                switchCameraMode(SettingsDefinitions.CameraMode.RECORD_VIDEO);
                break;
            }
            case R.id.button_Creat_Misn: {
                final String Read_Alt_set = String.valueOf(mtxt_Alt_set.getText());
                final String Read_Spd_set = String.valueOf(mtxt_Spd_set.getText());
                final String Read_P1_lon = String.valueOf(mtxt_P1_lon.getText());
                final String Read_P1_lat = String.valueOf(mtxt_P1_lat.getText());
                final String Read_P2_lon = String.valueOf(mtxt_P2_lon.getText());
                final String Read_P2_lat = String.valueOf(mtxt_P2_lat.getText());
                final String Read_P3_lon = String.valueOf(mtxt_P3_lon.getText());
                final String Read_P3_lat = String.valueOf(mtxt_P3_lat.getText());
                final String Read_P4_lon = String.valueOf(mtxt_P4_lon.getText());
                final String Read_P4_lat = String.valueOf(mtxt_P4_lat.getText());

                float Alt_set = Float.parseFloat(Read_Alt_set);
                float Spd_set = Float.parseFloat(Read_Spd_set);
                double P1_lon = Double.parseDouble(Read_P1_lon);
                double P1_lat = Double.parseDouble(Read_P1_lat);
                double P2_lon = Double.parseDouble(Read_P2_lon);
                double P2_lat = Double.parseDouble(Read_P2_lat);
                double P3_lon = Double.parseDouble(Read_P3_lon);
                double P3_lat = Double.parseDouble(Read_P3_lat);
                double P4_lon = Double.parseDouble(Read_P4_lon);
                double P4_lat = Double.parseDouble(Read_P4_lat);

                Waypoint mWaypoint1 = new Waypoint(P1_lat, P1_lon, Alt_set);
                Waypoint mWaypoint2 = new Waypoint(P2_lat, P2_lon, Alt_set);
                Waypoint mWaypoint3 = new Waypoint(P3_lat, P3_lon, Alt_set);
                Waypoint mWaypoint4 = new Waypoint(P4_lat, P4_lon, Alt_set);

                waypointMissionBuilder = new WaypointMission.Builder().finishedAction(mFinishedAction)
                        .headingMode(mHeadingMode)
                        .autoFlightSpeed(Spd_set)
                        .maxFlightSpeed(Speed)
                        .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

                waypointList.add(mWaypoint1);
                waypointList.add(mWaypoint2);
                waypointList.add(mWaypoint3);
                waypointList.add(mWaypoint4);
                waypointMissionBuilder.waypointList(waypointList);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());

                DJIError error = getWaypointMissionOperator().loadMission(waypointMissionBuilder.build());
                if (error == null) {
                    setResultToToast("loadWaypoint succeeded");
                } else {
                    setResultToToast("loadWaypoint failed " + error.getDescription());
                }

                BaseProduct product = FPVDemoApplication.getProductInstance();
                if (product == null || !product.isConnected()) {
                    mBtn_Upload_Misn.setEnabled(false);
                }else {
                    mBtn_Upload_Misn.setEnabled(true);
                }
                break;

            }
            case R.id.button_Upload_Misn: {
                uploadWayPointMission();
                mBtn_Strt_Misn.setEnabled(true);
                break;
            }
            case R.id.button_Strt_Misn: {
                startWaypointMission();
                mBtn_Stop_Misn.setEnabled(true);
                break;
            }
            case R.id.button_Stop_Misn: {
                stopWaypointMission();
                break;
            }
            default:
                break;
        }
    }

    private void switchCameraMode(SettingsDefinitions.CameraMode cameraMode){

        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.setMode(cameraMode, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {

                    if (error == null) {
                        showToast("Switch Camera Mode Succeeded");
                    } else {
                        showToast(error.getDescription());
                    }
                }
            });
        }
    }

    // Method for taking photo
    private void captureAction(){

        final Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {

            SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE; // Set the camera capture mode as Single mode
            camera.setShootPhotoMode(photoMode, new CommonCallbacks.CompletionCallback(){
                @Override
                public void onResult(DJIError djiError) {
                    if (null == djiError) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        if (djiError == null) {
                                            showToast("take photo: success");
                                        } else {
                                            showToast(djiError.getDescription());
                                        }
                                    }
                                });
                            }
                        }, 2000);
                    }
                }
            });
        }
    }

    // Method for starting recording
    private void startRecord(){

        final Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.startRecordVideo(new CommonCallbacks.CompletionCallback(){
                @Override
                public void onResult(DJIError djiError)
                {
                    if (djiError == null) {
                        showToast("Record video: success");
                    }else {
                        showToast(djiError.getDescription());
                    }
                }
            }); // Execute the startRecordVideo API
        }
    }

    // Method for stopping recording
    private void stopRecord(){

        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.stopRecordVideo(new CommonCallbacks.CompletionCallback(){

                @Override
                public void onResult(DJIError djiError)
                {
                    if(djiError == null) {
                        showToast("Stop recording: success");
                    }else {
                        showToast(djiError.getDescription());
                    }
                }
            }); // Execute the stopRecordVideo API
        }

    }


    public WaypointMissionOperator getWaypointMissionOperator() {
        if (instance == null) {
            if (DJISDKManager.getInstance().getMissionControl() != null){
                instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
            }
        }
        return instance;
    }


    private void uploadWayPointMission(){

        getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    setResultToToast("Mission upload successfully!");
                } else {
                    setResultToToast("Mission upload failed, error: " + error.getDescription() + " retrying...");
                    getWaypointMissionOperator().retryUploadMission(null);
                }
            }
        });

    }

    private void startWaypointMission(){

        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Start: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });
    }


    private void stopWaypointMission(){

        getWaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }


    private void setResultToToast(final String string){
        MainActivityDJI.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivityDJI.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
