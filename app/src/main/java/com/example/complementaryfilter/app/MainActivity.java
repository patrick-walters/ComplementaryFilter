package com.example.complementaryfilter.app;

import android.app.Activity;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity implements SensorEventListener, LocationListener {

    private SensorManager _sensorManager;
    private LocationManager _locationManager;

    private float[] gyro = new float[3];
    private float[] gyroMatrix = new float[9];
    private float[] gyroOrientation = new float[3];
    private float[] mag = new float[3];
    private float[] accel = new float[3];
    private float[] accelMagOrientation = new float[3];
    private float[] fusedOrientation = new float[3];
    private float[] rotationMatrix = new float[9];

    private float timeStamp;
    private boolean initialized = false;

    public static final int TIME_CONSTANT = 30;
    public static final float FILTER_COEFFICIENT = 0.94f;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private static final float EPSILON = 0.000000001f;
    private Timer filterTimer = new Timer();
    public Handler mHandler;

    private CompassView compassView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        gyroOrientation[0] = 0.0f;
        gyroOrientation[1] = 0.0f;
        gyroOrientation[2] = 0.0f;

        gyroMatrix[0] = 1.0f; gyroMatrix[2] = 0.0f; gyroMatrix[2] = 0.0f;
        gyroMatrix[3] = 0.0f; gyroMatrix[4] = 1.0f; gyroMatrix[5] = 0.0f;
        gyroMatrix[6] = 0.0f; gyroMatrix[7] = 0.0f; gyroMatrix[8] = 1.0f;

        _sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        _locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        registerListeners();

        filterTimer.scheduleAtFixedRate(new FusedOrientationTask(), 1000, TIME_CONSTANT);

        compassView = new CompassView(this);
        setContentView(compassView);
        compassView.setBackgroundColor(Color.BLACK);

        mHandler = new Handler();
    }

    @Override
    public void onDestroy() {
        unregisterListeners();
        super.onDestroy();
    }

    @Override
    public void onPause() {
        unregisterListeners();
        super.onPause();
    }

    @Override
    public void onResume() {
        registerListeners();
        super.onResume();
    }


        private void unregisterListeners() {
            _sensorManager.unregisterListener(this);
            _locationManager.removeUpdates(this);
    }

    private void registerListeners() {
        _sensorManager.registerListener(this,
                _sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST);
        _sensorManager.registerListener(this,
                _sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);
        _sensorManager.registerListener(this,
                _sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(sensorEvent.values, 0, accel, 0, 3);
                calcAccelMagOrientation();
                break;
            case Sensor.TYPE_GYROSCOPE:
                System.arraycopy(sensorEvent.values, 0, gyro, 0, 3);
                calcGyroOrientation(sensorEvent, gyro);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(sensorEvent.values, 0, mag, 0, 3);
                break;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void calcGyroOrientation(SensorEvent sensorEvent, float[] gyro) {
        if(accelMagOrientation == null) return;

        if(!initialized){
            gyroMatrix = matrixMultiplication(gyroMatrix, rotationMatrix);
            initialized = true;
        }

        float[] deltaVector = new float[4];
        if(timeStamp != 0) {
            float dt = (sensorEvent.timestamp - timeStamp) * NS2S;
            getGyroRotationVector(gyro, deltaVector, dt/2.0f);
        }

        timeStamp = sensorEvent.timestamp;

        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);

        SensorManager.getOrientation(gyroMatrix, gyroOrientation);
    }

    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];

        float sinX = (float)Math.sin(o[1]);
        float cosX = (float)Math.cos(o[1]);
        float sinY = (float)Math.sin(o[2]);
        float cosY = (float)Math.cos(o[2]);
        float sinZ = (float)Math.sin(o[0]);
        float cosZ = (float)Math.cos(o[0]);

        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;

        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;

        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;

        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }

    private void getGyroRotationVector(float[] gyroValues, float[] deltaRotationVector,
                                       float timeFactor)
    {
        float[] normValues = new float[3];

        float omegaMagnitude =
                (float)Math.sqrt(gyroValues[0] * gyroValues[0] +
                        gyroValues[1] * gyroValues[1] +
                        gyroValues[2] * gyroValues[2]);

        if(omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude;
            normValues[1] = gyroValues[1] / omegaMagnitude;
            normValues[2] = gyroValues[2] / omegaMagnitude;
        }

        float thetaOverTwo = omegaMagnitude * timeFactor;
        float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
        deltaRotationVector[3] = cosThetaOverTwo;
    }

    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }

    private void calcAccelMagOrientation() {
        if(SensorManager.getRotationMatrix(rotationMatrix, null, accel, mag)) {
            SensorManager.getOrientation(rotationMatrix, accelMagOrientation);
        }
    }


    private class FusedOrientationTask extends TimerTask {
        public void run() {

            float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;

            if (gyroOrientation[0] < -0.5 * Math.PI && accelMagOrientation[0] > 0.0) {
                fusedOrientation[0] = (float) (FILTER_COEFFICIENT * (gyroOrientation[0] + 2.0 * Math.PI) + oneMinusCoeff * accelMagOrientation[0]);
                fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accelMagOrientation[0] < -0.5 * Math.PI && gyroOrientation[0] > 0.0) {
                fusedOrientation[0] = (float) (FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * (accelMagOrientation[0] + 2.0 * Math.PI));
                fusedOrientation[0] -= (fusedOrientation[0] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
                fusedOrientation[0] = FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * accelMagOrientation[0];
            }

            if (gyroOrientation[1] < -0.5 * Math.PI && accelMagOrientation[1] > 0.0) {
                fusedOrientation[1] = (float) (FILTER_COEFFICIENT * (gyroOrientation[1] + 2.0 * Math.PI) + oneMinusCoeff * accelMagOrientation[1]);
                fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accelMagOrientation[1] < -0.5 * Math.PI && gyroOrientation[1] > 0.0) {
                fusedOrientation[1] = (float) (FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * (accelMagOrientation[1] + 2.0 * Math.PI));
                fusedOrientation[1] -= (fusedOrientation[1] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
                fusedOrientation[1] = FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * accelMagOrientation[1];
            }

            if (gyroOrientation[2] < -0.5 * Math.PI && accelMagOrientation[2] > 0.0) {
                fusedOrientation[2] = (float) (FILTER_COEFFICIENT * (gyroOrientation[2] + 2.0 * Math.PI) + oneMinusCoeff * accelMagOrientation[2]);
                fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accelMagOrientation[2] < -0.5 * Math.PI && gyroOrientation[2] > 0.0) {
                fusedOrientation[2] = (float) (FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * (accelMagOrientation[2] + 2.0 * Math.PI));
                fusedOrientation[2] -= (fusedOrientation[2] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
                fusedOrientation[2] = FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * accelMagOrientation[2];
            }

            gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);

            mHandler.post(updateOrientationDisplayTask);
        }
    }

    public void updateOrientationDisplay() {

        float Rot[] = new float[9];
        float I[] = new float[9];
        float orientation[] = new float[3];
        /*
        float azimuth;
        float pitch;
        float roll;

        azimuth = (float) (fusedOrientation[0] * 180.0 / 3.143);
        pitch = (float) (fusedOrientation[1] * 180 / 3.143);
        roll = (float) (fusedOrientation[2] * 180 / 3.143);


        TextView fuseOriX = (TextView) findViewById(R.id.txtFuseOriX);
        TextView fuseOriY = (TextView) findViewById(R.id.txtFuseOriY);
        TextView fuseOriZ = (TextView) findViewById(R.id.txtFuseOriZ);
        fuseOriX.setText("Filtered Azimuth = " + azimuth);
        fuseOriY.setText("Filtered Pitch   = " + pitch);
        fuseOriZ.setText("Filtered Roll    = " + roll);
        */

        boolean success = SensorManager.getRotationMatrix(Rot, I, accel, mag);
        if (success) {
            SensorManager.getOrientation(Rot, orientation);
            /*
            azimuth = (float) (orientation[0] * 180.0 / 3.143);
            pitch = (float) (orientation[1] * 180 / 3.143);
            roll = (float) (orientation[2] * 180 / 3.143);

            TextView oriX = (TextView) findViewById(R.id.txtOriX);
            TextView oriY = (TextView) findViewById(R.id.txtOriY);
            TextView oriZ = (TextView) findViewById(R.id.txtOriZ);
            oriX.setText("Filtered Azimuth = " + azimuth);
            oriY.setText("Filtered Pitch   = " + pitch);
            oriZ.setText("Filtered Roll    = " + roll);
            */
        }



        compassView.setFusedAngle(fusedOrientation[0]);
        compassView.setRawAngle(orientation[0]);

    }

    private Runnable updateOrientationDisplayTask = new Runnable() {
        public void run() {
            updateOrientationDisplay();
        }
    };


}