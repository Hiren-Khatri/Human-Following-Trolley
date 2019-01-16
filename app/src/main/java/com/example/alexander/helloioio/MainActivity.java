package com.example.alexander.helloioio;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
    private float turnedRight, degree;

    private float currDegree = 0f;
    private Boolean turnRight = false;
    private Boolean turnLeft = false;

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
                turnLeft=true;
                turnRight=false;
                RIGHT=false;
                LEFT=false;
                FORWARD=false;
                BACKWARD=false;
                IDLE=false;
                tvDirection.setText(strDirection());
                isStarted = false;
            }
        });

        rightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnRight=true;
                turnLeft=false;
                RIGHT=false;
                LEFT=false;
                FORWARD=false;
                BACKWARD=false;
                IDLE=false;
                tvDirection.setText(strDirection());
                isStarted = false;
            }
        });

//        Runnable checkTurnLeft = new Runnable() {
//            @Override
//            public void run() {
//                while (currDegree!=degree) {
//                    try {
//                        Thread.sleep(1000); // Waits for 1 second (1000 milliseconds)
//                        tvDegree.setText("Degree = " + String.valueOf(degree) + " [Turn Left]");
//                        tvCurrDegree.setText("CurrDegree = " + String.valueOf(currDegree) + " [Turn Left]");
//                        turningLeft();
//                        if(degree==currDegree){
//                            idle();
//                            turnLeft = false;
//                            break;
//                        }
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        };
//
//        Runnable checkTurnRight = new Runnable() {
//            @Override
//            public void run() {
//                while (currDegree!=degree) {
//                    try {
//                        Thread.sleep(1000); // Waits for 1 second (1000 milliseconds)
//                        tvDegree.setText("Degree = " + String.valueOf(degree) + " [Turn Right]");
//                        tvCurrDegree.setText("CurrDegree = " + String.valueOf(currDegree) + " [Turn Right]");
//                        turningRight();
//                        if(degree==currDegree){
//                            idle();
//                            turnRight = false;
//                            break;
//                        }
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        };
    }

    public void turningLeft(){
        IDLE = false;
        FORWARD = false;
        BACKWARD = false;
        RIGHT = false;
        LEFT = true;
    }

    public void turningRight(){
        IDLE = false;
        FORWARD = false;
        BACKWARD = false;
        RIGHT = true;
        LEFT = false;
    }
    public void idle(){
        IDLE = true;
        FORWARD = false;
        BACKWARD = false;
        RIGHT = false;
        LEFT = false;
        turnLeft=false;
        turnRight=false;
    }

    public String strDirection(){
        if(FORWARD) return "FORWARD";
        else if(RIGHT) return "RIGHT";
        else if(LEFT) return "LEFT";
        else if(BACKWARD) return "BACKWARD";
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
//                turnLeftThread.start();//cari cara pasang thread di sini

                while(currDegree!=degree) {
                    if(degree==currDegree){
                        idle();
                        break;
                    }
                }

                try {
                    Thread.sleep(1000);
                    FORWARD=true;
                    BACKWARD=false;
                    RIGHT=false;
                    LEFT=false;
                    IDLE=false;
                    turnLeft=false;
                    turnRight=false;
                    tvDirection.setText(strDirection());
                    isStarted = true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else if(turnRight){
                currDegree = degree;

                if(degree >=270){
                    currDegree = degree - 270;
                }
                else {
                    currDegree = degree + 90;

                }
                SPEED=78;
                SPEED2=0;
                FrontLeft(SPEED);//SPEED=78, bisa diganti lebih lambat atau lebih cepat
                FrontRight(SPEED2);//SPEED2=0
                do{
//                    tvDegree.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            tvDegree.setText("Degree = " + String.valueOf(degree) + " [Turn Right]");
//                        }
//                    });
//                    tvCurrDegree.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            tvCurrDegree.setText("CurrDegree = " + String.valueOf(currDegree) + " [Turn Right]");
//                        }
//                    });
//                    turningRight();
                    if(degree==currDegree){
                        idle();
                        break;
                    }
                }
                while(currDegree!=degree);

                try{
                    Thread.sleep(1000);
                    FORWARD=true;
                    BACKWARD=false;
                    RIGHT=false;
                    LEFT=false;
                    IDLE=false;
                    turnLeft=false;
                    turnRight=false;
                    tvDirection.setText(strDirection());
                    isStarted = true;
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
            }

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
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
