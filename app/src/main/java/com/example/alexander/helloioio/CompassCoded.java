package com.example.alexander.helloioio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import static javax.microedition.khronos.opengles.GL10.*;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * This class provides a basic demonstration of how to use the
 * {@link android.hardware.SensorManager SensorManager} API to draw
 * a 3D compass.
 */
public class CompassCoded extends Activity implements Renderer, SensorEventListener {
    private GLSurfaceView mGLSurfaceView;
    private SensorManager mSensorManager;
    private float[] mGData = new float[3];
    private float[] mMData = new float[3];
    private float[] mR = new float[16];
    private float[] mI = new float[16];
    private FloatBuffer mVertexBuffer;
    private FloatBuffer mColorBuffer;
    private ByteBuffer mIndexBuffer;
    private float[] mOrientation = new float[3];
    private int mCount;

    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private DatabaseReference refForward, refLeft, refRight, refIdle;
    public CompassCoded() {
    }
    /** Called with the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mGLSurfaceView = new GLSurfaceView(this);
        mGLSurfaceView.setRenderer(this);
        setContentView(mGLSurfaceView);
        database = FirebaseDatabase.getInstance();
        Log.v ("DEEEEBUG", String.valueOf(database));
        myRef = database.getReference("heading");
        refForward = database.getReference("forward");
        refIdle = database.getReference("idle");
        refLeft = database.getReference("turnLeft");
        refRight = database.getReference("turnRight");

    }
    @Override
    protected void onResume() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onResume();
        mGLSurfaceView.onResume();
        Sensor gsensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor msensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(this, gsensor, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, msensor, SensorManager.SENSOR_DELAY_GAME);
    }
    @Override
    protected void onPause() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onPause();
        mGLSurfaceView.onPause();
        mSensorManager.unregisterListener(this);
    }
    public void onDrawFrame(GL10 gl) {

    }
    public void onSurfaceChanged(GL10 gl, int width, int height) {
    }
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

    }
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
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
// some test code which will be used/cleaned up before we ship this.
//        SensorManager.remapCoordinateSystem(mR,
//                SensorManager.AXIS_X, SensorManager.AXIS_Z, mR);
//        SensorManager.remapCoordinateSystem(mR,
//                SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, mR);
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
            myRef.setValue(mOrientation[0]*rad2deg);
            //tambahin code dari MainActivity.java yang dari onSensorChanged
            //jadi nanti sensornya bisa dibaca dari sini trus diupdate juga dari sini
        }
    }
}