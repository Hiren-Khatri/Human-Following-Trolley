package com.example.alexander.helloioio;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.IOIO.VersionType;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import static android.content.ContentValues.TAG;
import static java.lang.Math.abs;

public class  MainActivity extends IOIOActivity implements SensorEventListener {

    private int SPEED, SPEED2;
    private int ANGLE;
    private String DIRECTION;

    private boolean IDLE;
    private boolean FORWARD;
    private boolean BACKWARD;
    private boolean RIGHT;
    private boolean LEFT;
    private boolean FRONT_RIGHT;
    private boolean FRONT_LEFT;
    private boolean BACK_RIGHT;
    private boolean BACK_LEFT;

//    private JoystickView joystick;
    private TextView tvSpeed;
    private TextView tvAngle;
    private TextView tvDirection;

    private ToggleButton tbTest;

    private Button forwardBtn, backwardBtn, rightBtn, leftBtn, setSpd;
    private Button btnStart, btnStop;

    private EditText textSetSpdL, textSetSpdR;
    private boolean isStarted = false;

    private float currentDegree = 0f;
    private float prevDegree = 0f;

    private SensorManager mSensorManager;

    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private float turnedRight, degree;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        idle();

        SPEED = 100;
        SPEED2 = 100;
        ANGLE = 0;
        DIRECTION = "Idle";

//        forwardBtn = findViewById(R.id.btnForward);
//        backwardBtn = findViewById(R.id.btnBackward);
        leftBtn = findViewById(R.id.btnLeft);
        rightBtn = findViewById(R.id.btnRight);

        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);

//        setSpd = findViewById(R.id.btnSetSpeed);
//        textSetSpdL = findViewById(R.id.setSpd);
//        textSetSpdR = findViewById(R.id.setSpd2);

        tvDirection = (TextView) findViewById(R.id.tvDirection);
        tvDirection.setText(DIRECTION);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("heading");

//        setSpd.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                SPEED = Integer.parseInt(textSetSpdL.getText().toString());
//                SPEED2 = Integer.parseInt(textSetSpdR.getText().toString());
//            }
//        });

//        forwardBtn.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if(event.getAction() == MotionEvent.ACTION_DOWN) {
//                    FORWARD = true;
//                    BACKWARD = false;
//                    RIGHT = false;
//                    LEFT = false;
//                    IDLE = false;
//                    tvDirection.setText(strDirection());
//                }
//                else if(event.getAction() == MotionEvent.ACTION_UP){
//                    idle();
//                    tvDirection.setText("Idle");
//                }
//                return false;
//            }
//        });

//        backwardBtn.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if(event.getAction() == MotionEvent.ACTION_DOWN) {
//                    FORWARD = false;
//                    BACKWARD = true;
//                    RIGHT = false;
//                    LEFT = false;
//                    IDLE = false;
//                    tvDirection.setText(strDirection());
//                }
//                else if(event.getAction() == MotionEvent.ACTION_UP){
//                    idle();
//                    tvDirection.setText("Idle");
//                }
//                return false;
//            }
//        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FORWARD=true;
                BACKWARD=false;
                RIGHT=false;
                LEFT=false;
                IDLE=false;
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
                tvDirection.setText(strDirection());
                isStarted = false;
            }
        });//kira kira ini dah bisa jalan dengan start (auto-forward) dan berhenti dengan stop (auto-idle)

        leftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FORWARD = false;
                BACKWARD = false;
                RIGHT = false;
                LEFT = true;
                IDLE = false;
                tvDirection.setText(strDirection());
                try {
                    Thread.sleep(2000);
                    FORWARD=true;
                    BACKWARD=false;
                    RIGHT=false;
                    LEFT=false;
                    IDLE=false;
                    tvDirection.setText(strDirection());
                    isStarted = true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        rightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FORWARD = false;
                BACKWARD = false;
                RIGHT = true;
                LEFT = false;
                IDLE = false;
                tvDirection.setText(strDirection());
                turnedRight = (degree>=90) ? (degree - 90) : 270 + x;//update ini dengan ternary operator
                try {
                    Thread.sleep(2000);
                    FORWARD=true;
                    BACKWARD=false;
                    RIGHT=false;
                    LEFT=false;
                    IDLE=false;
                    tvDirection.setText(strDirection());
                    isStarted = true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

//        leftBtn.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if(event.getAction() == MotionEvent.ACTION_DOWN) {
//                    FORWARD = false;
//                    BACKWARD = false;
//                    RIGHT = false;
//                    LEFT = true;
//                    IDLE = false;
//                    tvDirection.setText(strDirection());
//                }
//                else if(event.getAction() == MotionEvent.ACTION_UP){
//                    idle();
//                    tvDirection.setText("Idle");
//                }
//                return false;
//            }
//        });
//
//        rightBtn.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if(event.getAction() == MotionEvent.ACTION_DOWN) {
//                    FORWARD = false;
//                    BACKWARD = false;
//                    RIGHT = true;
//                    LEFT = false;
//                    IDLE = false;
//                    tvDirection.setText(strDirection());
//                }
//                else if(event.getAction() == MotionEvent.ACTION_UP){
//                    idle();
//                    tvDirection.setText("Idle");
//                }
//                return false;
//            }
//        });

//        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
//            @Override
//            public void onMove(int angle, int strength) {
//                SPEED = strength;
//
//                tvSpeed.setText("Speed = " + SPEED);
//
//                setDirection(angle);
//
//                tvDirection.setText(strDirection());
//
//                Log.i("debug_dong", String.valueOf(RIGHT) + "\n" +
//                        String.valueOf(FRONT_RIGHT) + "\n" +
//                        String.valueOf(FORWARD) + "\n" +
//                        String.valueOf(FRONT_LEFT) + "\n" +
//                        String.valueOf(LEFT) + "\n" +
//                        String.valueOf(BACK_LEFT) + "\n" +
//                        String.valueOf(BACKWARD) + "\n" +
//                        String.valueOf(BACK_RIGHT) + "\n");
//            }
//        });


    }
    public void idle(){
        IDLE = true;
        FORWARD = false;
        BACKWARD = false;
        RIGHT = false;
        LEFT = false;
//        FRONT_RIGHT = false;//uncomment if using 8 directional movement
//        FRONT_LEFT = false;
//        BACK_RIGHT = false;
//        BACK_LEFT = false;
    }

    public String strDirection(){
        if(FORWARD) return "FORWARD";
        else if(RIGHT) return "RIGHT";
        else if(LEFT) return "LEFT";
        else if(BACKWARD) return "BACKWARD";
//        else if(FRONT_RIGHT) return "FRONT RIGHT"; //uncomment if using 8 directional movement
//        else if(FRONT_LEFT) return "FRONT LEFT";
//        else if(BACK_RIGHT) return "BACK RIGHT";
//        else if(BACK_LEFT) return "BACK LEFT";
        else if(IDLE) return "IDLE";
        else return "";
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        degree = Math.round(event.values[0]);

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
            led_.write(false);

//            FrontRightPWM.setDutyCycle((float)0.5);
//            FrontRight_1.write(false);
//            FrontLeftPWM.setDutyCycle((float)0.5);
//            FrontLeft_1.write(false);

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
//            else if(FRONT_RIGHT){
//                FrontRight(SPEED/2);
//                FrontLeft(SPEED);
//            }
//            else if(FRONT_LEFT){
//                FrontRight(SPEED);
//                FrontLeft(SPEED/2);
//
//            }
//            else if(BACK_RIGHT){
//                FrontRight(-SPEED/2);
//                FrontLeft(-SPEED);
//
//            }
//            else if(BACK_LEFT){
//                FrontRight(-SPEED);
//                FrontLeft(-SPEED/2);
//
//            } //uncomment this to use 4 more direction

            else{
                FrontRight(0);
                FrontLeft(0);
            }
            Thread.sleep(100);
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
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }

//    private int numConnected_ = 0;

//    private void enableUi(final boolean enable) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                if (enable) {
//                    if (numConnected_++ == 0) {
//                        joystick.setEnabled(true);
//
//                    }
//                } else {
//                    if (--numConnected_ == 0) {
//                        joystick.setEnabled(false);
//                    }
//                }
//            }
//        });
//    }


//        public void FrontRight(float speed) throws ConnectionLostException, InterruptedException {
//            FrontRightPWM.setDutyCycle(speed / 100);
//            if(speed == 50){
//                FrontRight_1.write(false);
//            }
//            else{
//                FrontRight_1.write(true);
//            }
//        }
//
//        public void FrontLeft(float speed) throws ConnectionLostException, InterruptedException {
//            FrontLeftPWM.setDutyCycle(speed / 100);
//            if(speed == 50){
//                FrontLeft_1.write(false);
//            }
//            else{
//                FrontLeft_1.write(true);
//            }
//        }


//            realSpeed = SPEED / 2;


//            if(FORWARD){
//                FrontRight(50 + realSpeed);
//                FrontLeft(50 + realSpeed);
//            }
//
//            else if(BACKWARD){
//                FrontLeft(50);
//                FrontRight(50);
//
//            }
//
//            else if(RIGHT){
//                FrontRight(50 - realSpeed);
//                FrontLeft(50 + realSpeed);
//            }
//
//            else if(LEFT){
//                FrontRight(50 + realSpeed);
//                FrontLeft(50 - realSpeed);
//            }
//            else if(FRONT_RIGHT){
//                FrontRight((50 + realSpeed)/2);
//                FrontLeft(50 + realSpeed);
//            }
//            else if(FRONT_LEFT){
//                FrontRight(50 + realSpeed);
//                FrontLeft((50 + realSpeed)/2);
//
//            }
//            else if(BACK_RIGHT){
//                FrontRight((50 - realSpeed)/2);
//                FrontLeft(50 - realSpeed);
//
//            }
//            else if(BACK_LEFT){
//                FrontRight(50 - realSpeed);
//                FrontLeft((50 - realSpeed)/2);
//
//            }
//
//            else{
//                FrontRight(50);
//                FrontLeft(50);
//            }

//    public void setDirection(int angle){ //uncomment if using joystick
//        if(SPEED > 0 && ((angle >= 338 && angle <= 359) || (angle >= 0 && angle <= 22)) ){
//            IDLE = false;
//            FORWARD = false;
//            BACKWARD = false;
//            RIGHT = true;
//            LEFT = false;
//            FRONT_RIGHT = false;
//            FRONT_LEFT = false;
//            BACK_RIGHT = false;
//            BACK_LEFT = false;
//        }
//        else if(angle >= 23 && angle <= 67){
//            IDLE = false;
//            FORWARD = false;
//            BACKWARD = false;
//            RIGHT = false;
//            LEFT = false;
//            FRONT_RIGHT = true;
//            FRONT_LEFT = false;
//            BACK_RIGHT = false;
//            BACK_LEFT = false;
//        }
//        else if(angle >= 68 && angle <= 112){
//            IDLE = false;
//            FORWARD = true;
//            BACKWARD = false;
//            RIGHT = false;
//            LEFT = false;
//            FRONT_RIGHT = false;
//            FRONT_LEFT = false;
//            BACK_RIGHT = false;
//            BACK_LEFT = false;
//        }
//        else if(angle >= 113 && angle <= 157){
//            IDLE = false;
//            FORWARD = false;
//            BACKWARD = false;
//            RIGHT = false;
//            LEFT = false;
//            FRONT_RIGHT = false;
//            FRONT_LEFT = true;
//            BACK_RIGHT = false;
//            BACK_LEFT = false;
//        }
//        else if(angle >= 158 && angle <= 202){
//            IDLE = false;
//            FORWARD = false;
//            BACKWARD = false;
//            RIGHT = false;
//            LEFT = true;
//            FRONT_RIGHT = false;
//            FRONT_LEFT = false;
//            BACK_RIGHT = false;
//            BACK_LEFT = false;
//        }
//        else if(angle >= 203 && angle <= 247){
//            IDLE = false;
//            FORWARD = false;
//            BACKWARD = false;
//            RIGHT = false;
//            LEFT = false;
//            FRONT_RIGHT = false;
//            FRONT_LEFT = false;
//            BACK_RIGHT = false;
//            BACK_LEFT = true;
//        }
//        else if(angle >= 248 && angle <= 292){
//            IDLE = false;
//            FORWARD = false;
//            BACKWARD = true;
//            RIGHT = false;
//            LEFT = false;
//            FRONT_RIGHT = false;
//            FRONT_LEFT = false;
//            BACK_RIGHT = false;
//            BACK_LEFT = false;
//        }
//        else if(angle >= 293 && angle <= 337){
//            IDLE = false;
//            FORWARD = false;
//            BACKWARD = false;
//            RIGHT = false;
//            LEFT = false;
//            FRONT_RIGHT = false;
//            FRONT_LEFT = false;
//            BACK_RIGHT = true;
//            BACK_LEFT = false;
//        }
//        else if(angle == 0 && SPEED == 0){
//            IDLE = true;
//            FORWARD = false;
//            BACKWARD = false;
//            RIGHT = false;
//            LEFT = false;
//            FRONT_RIGHT = false;
//            FRONT_LEFT = false;
//            BACK_RIGHT = false;
//            BACK_LEFT = false;
//        }
//    }
}
