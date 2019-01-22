package com.example.alexander.helloioio;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
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

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.IOIO.VersionType;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

import static android.content.ContentValues.TAG;
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
    private DatabaseReference refForward, refLeft, refRight, refIdle;
    private float degree;

    private float currDegree = 0f;
    private Boolean turnRight = false;
    private Boolean turnLeft = false;

    private String TAG = "DEBUG FORWARD";

    private ZXingScannerView qrView;

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
        Log.v ("DEEEEBUG", String.valueOf(database));
        myRef = database.getReference("heading");
        refForward = database.getReference("forward");
        refIdle = database.getReference("idle");
        refLeft = database.getReference("turnLeft");
        refRight = database.getReference("turnRight");

        //tambahin check untuk cek camera permission

//
//        private void init() {
//
//            //Scanner
//            qrView = new ZXingScannerView(this);
//            RelativeLayout rl = (RelativeLayout) findViewById(R.id.relative_scan_take_single);
//            rl.addView(qrView);
//            qrView.setResultHandler(this);
//            qrView.startCamera();
//            qrView.setSoundEffectsEnabled(true);
//            qrView.setAutoFocus(true);
//
//        }
//
//        @Override
//        public void onResume() {
//            super.onResume();
//            qrView.setResultHandler(this); // Register ourselves as a handler for scan results.
//            qrView.startCamera();          // Start camera on resume
//        }
//
//        @Override
//        public void onPause() {
//            super.onPause();
//            qrView.stopCamera();           // Stop camera on pause
//        }
//
//
//===================================================================================================================
//
//<RelativeLayout
//        android:id="@+id/relative_scan_take_single"
//        android:layout_width="200dp"
//        android:layout_height="200dp"
//        android:layout_marginBottom="120dp">
//
//
//        </RelativeLayout>
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
        });//kira kira ini dah bisa jalan dengan start (auto-forward) dan berhenti dengan stop (auto-idle)

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
                // Failed to read value
                Log.w("DEEEEBUG", "Failed to read value.", error.toException());
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
        degree = Math.round(event.values[0]);
        tvDegree.setText("Degree = " + String.valueOf(degree));

        if(degree != prevDegree) {

            myRef.setValue(degree);

            myRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.
                    Double value = dataSnapshot.getValue(Double.class);
                    Log.d(TAG, "Value is: " + value);
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.w(TAG, "Failed to read value.", error.toException());
                }
            });
        }
        prevDegree = degree;

        currentDegree = -degree;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void handleResult(Result result) {

    }

    class Looper extends BaseIOIOLooper {

        private DigitalOutput led_;
        private DigitalOutput FrontRight_1;
        private PwmOutput FrontRightPWM;

        private DigitalOutput FrontLeft_1;
        private PwmOutput FrontLeftPWM;

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
                tvDirection.post(new Runnable() {
                    @Override
                    public void run() {
                        tvDirection.setText(strDirection());
                    }
                });

                if(degree >=90){
                    currDegree = degree - 90;
                }
                else {
                    currDegree = degree + 270;
                }

                while(currDegree!=degree) {
                    if (degree == currDegree) {
//                        turnLeft=false;
//                        turnRight=false;//ada chance di sini ada false positive
                        //dimana setelah ini dijadiin false, pertama kali akan jalan maju
                        //tapi di percobaan kedua, langsung berhenti dan nggak mau terima input buat maju
                        autoForward();
                        break;
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
                tvDirection.post(new Runnable() {
                    @Override
                    public void run() {
                        tvDirection.setText(strDirection());
                    }
                });
                if(degree >=270){
                    currDegree = degree - 270;
                }
                else {
                    currDegree = degree + 90;

                }

                while(currDegree!=degree) {
                    if (degree == currDegree) {
//                        turnLeft=false;
//                        turnRight=false;
                        autoForward();
                        break;
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
//                FrontRight(0);
//                FrontLeft(0);
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
}
