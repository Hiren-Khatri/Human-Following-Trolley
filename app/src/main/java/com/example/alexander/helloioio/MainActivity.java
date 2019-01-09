package com.example.alexander.helloioio;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.w3c.dom.Text;

import io.github.controlwear.virtual.joystick.android.JoystickView;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.IOIO.VersionType;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import static java.lang.Math.abs;

public class  MainActivity extends IOIOActivity {

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

    private SeekBar sbTopServo;
    private SeekBar sbBotomServo;

    private ToggleButton tbTest;

    private Button forwardBtn, backwardBtn, rightBtn, leftBtn, setSpd;

    private EditText textSetSpdL, textSetSpdR;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        idle();

        SPEED = 100;
        SPEED2 = 100;
        ANGLE = 0;
        DIRECTION = "Idle";

        forwardBtn = findViewById(R.id.btnForward);
        backwardBtn = findViewById(R.id.btnBackward);
        leftBtn = findViewById(R.id.btnLeft);
        rightBtn = findViewById(R.id.btnRight);

        setSpd = findViewById(R.id.btnSetSpeed);
        textSetSpdL = findViewById(R.id.setSpd);
        textSetSpdR = findViewById(R.id.setSpd2);

        tvDirection = (TextView) findViewById(R.id.tvDirection);
        tvDirection.setText(DIRECTION);

        setSpd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SPEED = Integer.parseInt(textSetSpdL.getText().toString());
                SPEED2 = Integer.parseInt(textSetSpdR.getText().toString());
            }
        });

        forwardBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    FORWARD = true;
                    BACKWARD = false;
                    RIGHT = false;
                    LEFT = false;
                    IDLE = false;
                    tvDirection.setText(strDirection());
                }
                else if(event.getAction() == MotionEvent.ACTION_UP){
                    idle();
                    tvDirection.setText("Idle");
                }
                return false;
            }
        });

        backwardBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    FORWARD = false;
                    BACKWARD = true;
                    RIGHT = false;
                    LEFT = false;
                    IDLE = false;
                    tvDirection.setText(strDirection());
                }
                else if(event.getAction() == MotionEvent.ACTION_UP){
                    idle();
                    tvDirection.setText("Idle");
                }
                return false;
            }
        });

        leftBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    FORWARD = false;
                    BACKWARD = false;
                    RIGHT = false;
                    LEFT = true;
                    IDLE = false;
                    tvDirection.setText(strDirection());
                }
                else if(event.getAction() == MotionEvent.ACTION_UP){
                    idle();
                    tvDirection.setText("Idle");
                }
                return false;
            }
        });

        rightBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    FORWARD = false;
                    BACKWARD = false;
                    RIGHT = true;
                    LEFT = false;
                    IDLE = false;
                    tvDirection.setText(strDirection());
                }
                else if(event.getAction() == MotionEvent.ACTION_UP){
                    idle();
                    tvDirection.setText("Idle");
                }
                return false;
            }
        });

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

            if(FORWARD){
                FrontLeft(SPEED);//SPEED=78
                FrontRight(SPEED2);//SPEED=55
            }

            else if(BACKWARD){
                FrontLeft(SPEED);//SPEED=1
                FrontRight(-SPEED2);//SPEED2=-100
            }

            else if(RIGHT){
                SPEED2=0;
                FrontRight(SPEED2);//SPEED2=0
                FrontLeft(SPEED);//SPEED=78, bisa diganti lebih lambat atau lebih cepat
            }

            else if(LEFT){
                SPEED=0;
                FrontRight(SPEED2);//SPEED2=100
                FrontLeft(SPEED);//SPEED=0
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

    private int numConnected_ = 0;

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
