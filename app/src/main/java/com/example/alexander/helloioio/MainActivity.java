package com.example.alexander.helloioio;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.Result;

import java.util.Timer;
import java.util.TimerTask;

import io.indoorlocation.core.IndoorLocationProvider;
import io.indoorlocation.manual.ManualIndoorLocationProvider;
import io.indoorlocation.navisens.NavisensIndoorLocationProvider;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.IOIO.VersionType;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

import static java.lang.Math.abs;

public class  MainActivity extends IOIOActivity implements SensorEventListener, ZXingScannerView.ResultHandler {

    private int SPEED, SPEED2;
    private int ANGLE;
    private String DIRECTION;

    private boolean IDLE;
    private boolean FORWARD;
    private boolean BACKWARD;
    private boolean RIGHT;
    private boolean LEFT;

    private TextView tvSpeed;
    private TextView tvAngle;
    private TextView tvDirection;


    private Button rightBtn, leftBtn;
    private Button btnStart, btnStop;

    private TextView tvDegree;
    private TextView tvCurrDegree;

    private boolean isStarted = false;

    private float currentDegree = 0f;
    private float prevDegree = 0f;

    private SensorManager mSensorManager;

    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private DatabaseReference refForward, refLeft, refRight, refIdle, trolleyLat1, trolleyLon1, trolleyLat2, trolleyLon2;
    private float degree;

    private float currDegree = 0f;

    private float tempDegree = 0f;
    private Boolean turnRight = false;
    private Boolean turnLeft = false;

    private String TAG = "DEBUG FORWARD";

    private ZXingScannerView qrView;

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 1;

    private String resultHolder;

    private float[] mGData = new float[3];
    private float[] mMData = new float[3];
    private float[] mR = new float[16];
    private float[] mI = new float[16];
    private float[] mOrientation = new float[3];
    private int mCount;

    private IndoorLocationProvider manualIndoorLocationProvider;
    private NavisensIndoorLocationProvider navisensIndoorLocationProvider;

    private String NAVISENS_API_KEY = "jAKCbUXq0tW1slgWfkFZwzCsrAPGe2Kyq1LZDz60RNclFGCLO4AKphJVkdk0lL3o";

    private boolean is1stTrolley = false, is2ndTrolley = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        idle();

        SPEED = 100;
        SPEED2 = 100;
        ANGLE = 0;
        DIRECTION = "Idle";

        leftBtn = findViewById(R.id.btnLeft);
        rightBtn = findViewById(R.id.btnRight);

        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);

        tvDegree = findViewById(R.id.tvDegree);
        tvCurrDegree = findViewById(R.id.tvCurrDegree);

        tvDirection = (TextView) findViewById(R.id.tvDirection);
        tvDirection.setText(DIRECTION);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("heading");
        refForward = database.getReference("forward");
        refIdle = database.getReference("idle");
        refLeft = database.getReference("turnLeft");
        refRight = database.getReference("turnRight");
        trolleyLat1 = database.getReference().child("troli1").child("currentLat");
        trolleyLon1 = database.getReference().child("troli1").child("currentLon");
        trolleyLat2 = database.getReference().child("troli2").child("currentLon");
        trolleyLon2 = database.getReference().child("troli2").child("currentLon");
        is1stTrolley = true;

        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FORWARD=true;
                BACKWARD=false;
                RIGHT=false;
                LEFT=false;
                IDLE=false;
                turnLeft=false;
                turnRight=false;
                tvDirection.setText(strDirection());
                isStarted = true;
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FORWARD=false;
                BACKWARD=false;
                RIGHT=false;
                LEFT=false;
                IDLE=true;
                turnRight=false;
                turnLeft=false;
                tvDirection.setText(strDirection());
                isStarted = false;
            }
        });

        leftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turningLeft();
            }
        });

        rightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turningRight();
            }
        });

        refForward.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Boolean value = dataSnapshot.getValue(Boolean.class);
                if(value){
                    autoForward();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
        refLeft.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Boolean value = dataSnapshot.getValue(Boolean.class);
                if(value){
                    turningLeft();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        refRight.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Boolean value = dataSnapshot.getValue(Boolean.class);
                if(!turnRight&&value){
                    turningRight();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        refIdle.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Boolean value = dataSnapshot.getValue(Boolean.class);
                if(value){
                    idle();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        
        init();
        requestLocationPermission();
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendLocToDB();
            }
        }, 0, 1000);
    }

    private void sendLocToDB(){
        if(navisensIndoorLocationProvider.isStarted()&&navisensIndoorLocationProvider.latitude!=0.0){
            if(is1stTrolley==true&&is2ndTrolley==false){
                trolleyLat1.setValue(navisensIndoorLocationProvider.latitude);
                trolleyLon1.setValue(navisensIndoorLocationProvider.longitude);
            }
            else if(is2ndTrolley==true&&is1stTrolley==false){
                trolleyLat2.setValue(navisensIndoorLocationProvider.latitude);
                trolleyLon2.setValue(navisensIndoorLocationProvider.longitude);
            }
        }
    }
    private void init() {
        //Scanner
        qrView = new ZXingScannerView(this);
        RelativeLayout rl = (RelativeLayout) findViewById(R.id.relative_scan_take_single);
        rl.addView(qrView);
        qrView.setResultHandler(this);
        qrView.startCamera();
        qrView.setSoundEffectsEnabled(true);
        qrView.setAutoFocus(true);
    }

    public void turningLeft(){
        turnLeft=true;
        turnRight=false;
        RIGHT=false;
        LEFT=false;
        FORWARD=false;
        BACKWARD=false;
        IDLE=false;
        isStarted = false;
    }

    public void turningRight(){
        turnRight=true;
        turnLeft=false;
        RIGHT=false;
        LEFT=false;
        FORWARD=false;
        BACKWARD=false;
        IDLE=false;
        isStarted = false;
    }

    public void idle(){
        IDLE = true;
        FORWARD = false;
        BACKWARD = false;
        RIGHT = false;
        LEFT = false;
        turnLeft=false;
        turnRight=false;
        isStarted=false;
    }

    public void autoForward(){
        IDLE = false;
        FORWARD = true;
        BACKWARD = false;
        RIGHT = false;
        LEFT = false;
        turnLeft=false;
        turnRight=false;
        isStarted=true;
    }

    public String strDirection(){
        if(FORWARD) return "FORWARD";
        else if(RIGHT) return "RIGHT";
        else if(LEFT) return "LEFT";
        else if(BACKWARD) return "BACKWARD";
        else if(turnRight) return "TURNING RIGHT";
        else if(turnLeft) return "TURNING LEFT";
        else if(IDLE) return "IDLE";
        else return "";
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();
        float[] data;
        if (type == Sensor.TYPE_ACCELEROMETER) {
            data = mGData;
        } else if (type == Sensor.TYPE_MAGNETIC_FIELD) {
            data = mMData;
        } else {
            // we should not be here.
            return;
        }
        for (int i=0 ; i<3 ; i++)
            data[i] = event.values[i];
        SensorManager.getRotationMatrix(mR, mI, mGData, mMData);
        SensorManager.getOrientation(mR, mOrientation);
        float incl = SensorManager.getInclination(mI);
        if (mCount++ > 50) {
            final float rad2deg = (float)(180.0f/Math.PI);
            mCount = 0;
            Log.d("Compass", "yaw: " + (int)(mOrientation[0]*rad2deg) +
                    "  pitch: " + (int)(mOrientation[1]*rad2deg) +
                    "  roll: " + (int)(mOrientation[2]*rad2deg) +
                    "  incl: " + (int)(incl*rad2deg)
            );
            tvDegree.setText("Degree = " + String.valueOf(mOrientation[0]*rad2deg+180f));
            degree = mOrientation[0]*rad2deg+180f;
            if(degree!=prevDegree){
                myRef.setValue(mOrientation[0]*rad2deg+180f);
                myRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });
            }

            prevDegree = degree;

            currentDegree = -degree;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
        qrView.setResultHandler(this);
        qrView.startCamera();
        Sensor gsensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor msensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(this, gsensor, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, msensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        qrView.stopCamera();
    }

    boolean isDouble(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void handleResult(Result result) {
        resultHolder = result.getText();
        if(resultHolder!="FORWARD"&&resultHolder!="TURNLEFT"&&resultHolder!="TURNRIGHT") {
            String[] rawLatQR = resultHolder.split(",");
            Double latQR = Double.parseDouble(rawLatQR[0]);
            Double lonQR = Double.parseDouble(rawLatQR[1]);
            navisensIndoorLocationProvider.setLocFromQR(latQR, lonQR);
        }
        else if(resultHolder=="FORWARD"||resultHolder=="TURNLEFT"||resultHolder=="TURNRIGHT") {
                if (resultHolder.equals("FORWARD")) {
                    idle();
                    autoForward();
                } else if (resultHolder.equals("TURNLEFT")) {
                    idle();
                    turningLeft();
                } else if (resultHolder.equals("TURNRIGHT")) {
                    idle();
                    turningRight();
                } else {
                    idle();
                }
        }
        qrView.resumeCameraPreview(this);
    }

    class Looper extends BaseIOIOLooper {

        private DigitalOutput led_;
        private DigitalOutput FrontRight_1;
        private PwmOutput FrontRightPWM;

        private DigitalOutput FrontLeft_1;
        private PwmOutput FrontLeftPWM;

        private DigitalOutput ConnectedLED;

        private float realSpeed = 50;


        @Override
        protected void setup() throws ConnectionLostException, InterruptedException {
            showVersions(ioio_, "IOIO connected!");
            led_ = ioio_.openDigitalOutput(0, true);
            FrontLeft_1 = ioio_.openDigitalOutput(6);
            FrontLeftPWM = ioio_.openPwmOutput(5, 100);

            FrontRight_1 = ioio_.openDigitalOutput(3);
            FrontRightPWM = ioio_.openPwmOutput(4, 100);

            FrontRightPWM.setDutyCycle(0);
            FrontLeftPWM.setDutyCycle(0);

            ConnectedLED = ioio_.openDigitalOutput(8, true);
        }



        @Override
        public void loop() throws ConnectionLostException, InterruptedException {
            if(refForward.toString().equals("false")&&refLeft.toString().equals("false")&&refRight.toString().equals("false")){
                refIdle.setValue(true);
            }

            led_.write(false);
            if(FORWARD&&isStarted){
                SPEED=78;//jika mau diganti jadi dynamic, hapus assignment speed ini
                SPEED2=55;
                FrontLeft(SPEED);//SPEED=78
                FrontRight(SPEED2);//SPEED=55
            }

            else if(BACKWARD){
                SPEED=1;
                SPEED2=100;
                FrontLeft(SPEED);//SPEED=1
                FrontRight(-SPEED2);//SPEED2=-100

            }

            else if(RIGHT){
                SPEED=78;
                SPEED2=0;
                FrontLeft(SPEED);//SPEED=78, bisa diganti lebih lambat atau lebih cepat
                FrontRight(SPEED2);//SPEED2=0
            }

            else if(LEFT){
                SPEED=0;
                SPEED2=100;
                FrontLeft(SPEED);//SPEED=0
                FrontRight(SPEED2);//SPEED2=100
            }

            else if(turnLeft){
                SPEED=0;
                SPEED2=100;
                FrontLeft(SPEED);//SPEED=0
                FrontRight(SPEED2);//SPEED2=100
                currDegree = degree;
                Boolean x = false;
                Boolean y = false;
                tvDirection.post(new Runnable() {
                    @Override
                    public void run() {
                        tvDirection.setText(strDirection());
                    }
                });

                if(degree >=90){
                    currDegree = degree - 90f;
                    x=true;
                    y=false;
                }
                else if (degree<90){
                    currDegree = degree + 270f;
                    x=false;
                    y=true;
                }


                while(currDegree!=degree) {
                    if(x){
                        if(degree>=(currDegree-3f)&&degree<=currDegree+3f){
                            autoForward();
                            break;
                        }
                    }
                    if(y){
                        if(degree>=(currDegree-3f)&&degree<=currDegree+3f){
                            autoForward();
                            break;
                        }
                    }
                    else if(IDLE){
                        break;
                    }
                }

            }
            else if(turnRight){
                SPEED=78;
                SPEED2=0;
                FrontLeft(SPEED);//SPEED=78, bisa diganti lebih lambat atau lebih cepat
                FrontRight(SPEED2);//SPEED2=0
                currDegree = degree;
                Boolean x=false, y=false;
                tvDirection.post(new Runnable() {
                    @Override
                    public void run() {
                        tvDirection.setText(strDirection());
                    }
                });
                if(degree >=270){
                    currDegree = degree - 270f;
                    x=true;
                    y=false;
                }
                else if (degree<270){
                    currDegree = degree + 90f;
                    x=false;
                    y=true;
                }

                while(currDegree!=degree) {
                    if(x){
                        if(degree>=(currDegree-3f)&&degree<=currDegree+3f){
                            autoForward();
                            break;
                        }
                    }
                    if(y){
                        if(degree>=(currDegree-3f)&&degree<=currDegree+3f){
                            autoForward();
                            break;
                        }
                    }
                    else if(IDLE){
                        break;
                    }
                }
            }

            else if(IDLE){
                SPEED=0;
                SPEED2=0;
                FrontRight(SPEED2);
                FrontLeft(SPEED);
            }

            else{
                autoForward();
            }
            Thread.sleep(50);
        }

        public void FrontRight(float speed) throws ConnectionLostException, InterruptedException {
            if(speed > 0){
                FrontRightPWM.setDutyCycle(speed / 100);
                FrontRight_1.write(true);
            }
            else if(speed < 0){
                FrontRightPWM.setDutyCycle(abs(speed / 100));
                FrontRight_1.write(false);
            }
            else if(speed == 0){
                FrontRightPWM.setDutyCycle(0);
                FrontRight_1.write(false);
            }
        }

        public void FrontLeft(float speed) throws ConnectionLostException, InterruptedException {
            if(speed > 0){
                FrontLeftPWM.setDutyCycle(speed / 100);
                FrontLeft_1.write(true);
            }
            else if(speed < 0){
                FrontLeftPWM.setDutyCycle(abs(speed / 100));
                FrontLeft_1.write(false);
            }
            else if(speed == 0){
                FrontLeftPWM.setDutyCycle(0);
                FrontLeft_1.write(false);
            }
        }
        /**
         * Called when the IOIO is disconnected.
         *
         * @see ioio.lib.util.IOIOLooper#disconnected()
         */
        @Override
        public void disconnected() {
            toast("IOIO disconnected");
        }


        @Override
        public void incompatible() {
            showVersions(ioio_, "Incompatible firmware version!");
        }


    }


    @Override
    protected IOIOLooper createIOIOLooper() {
        return new Looper();
    }

    private void showVersions(IOIO ioio, String title) {
        toast(String.format("%s\n" +
                        "IOIOLib: %s\n" +
                        "Application firmware: %s\n" +
                        "Bootloader firmware: %s\n" +
                        "Hardware: %s",
                title,
                ioio.getImplVersion(VersionType.IOIOLIB_VER),
                ioio.getImplVersion(VersionType.APP_FIRMWARE_VER),
                ioio.getImplVersion(VersionType.BOOTLOADER_VER),
                ioio.getImplVersion(VersionType.HARDWARE_VER)));
    }

    private void toast(final String message) {
        final Context context = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupLocationProvider() {
        manualIndoorLocationProvider = new ManualIndoorLocationProvider();
        navisensIndoorLocationProvider = new NavisensIndoorLocationProvider(getApplicationContext(),
                NAVISENS_API_KEY, manualIndoorLocationProvider);
        navisensIndoorLocationProvider.start();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupLocationProvider();
                }
            }
        }
    }

    private void requestLocationPermission() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            setupLocationProvider();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data!=null) {
            if(!data.getStringExtra("url").equals("Troli2")&&!data.getStringExtra("url").equals("Troli1")) {
                String[] rawLatQR = data.getStringExtra("url").split(",");
                Double latQR = Double.parseDouble(rawLatQR[0]);
                Double lonQR = Double.parseDouble(rawLatQR[1]);
                navisensIndoorLocationProvider.setLocFromQR(latQR, lonQR);
            }
        }
    }
}
