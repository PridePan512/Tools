package com.example.tool.compass;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tool.R;

public class CompassActivity extends AppCompatActivity implements SensorEventListener {

    private ImageView mCompassImageView;
    private TextView mDegreeTextView;

    // 当前指针角度（角度制）
    private float mCurrentDegree = 0f;

    private SensorManager mSensorManager;

    // 传感器数据
    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];

    // 计算所需矩阵
    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];

    // 数据是否准备完成
    private boolean mHasAccelerometer = false;
    private boolean mHasMagnetometer = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        initView();
    }

    private void initView() {
        mCompassImageView = findViewById(R.id.iv_compass);
        mDegreeTextView = findViewById(R.id.tv_degree);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        if (accelerometer != null) {
            mSensorManager.registerListener(
                    this,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_UI
            );
        }

        if (magneticField != null) {
            mSensorManager.registerListener(
                    this,
                    magneticField,
                    SensorManager.SENSOR_DELAY_UI
            );
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 可根据需要处理精度变化
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccelerometerReading, 0, 3);
            mHasAccelerometer = true;

        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagnetometerReading, 0, 3);
            mHasMagnetometer = true;
        }

        if (!mHasAccelerometer || !mHasMagnetometer) {
            return;
        }

        updateOrientation();
    }

    /**
     * 计算方向并更新 UI
     */
    private void updateOrientation() {
        boolean success = SensorManager.getRotationMatrix(
                mRotationMatrix,
                null,
                mAccelerometerReading,
                mMagnetometerReading
        );

        if (!success) {
            return;
        }

        SensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

        // 方位角（弧度 -> 角度）
        float azimuthRad = mOrientationAngles[0];
        float azimuthDeg = (float) Math.toDegrees(azimuthRad);

        // 转为 0~360°
        azimuthDeg = (azimuthDeg + 360) % 360;

        // 避免过于频繁的小幅抖动
        if (Math.abs(Math.abs(azimuthDeg) - Math.abs(mCurrentDegree)) < 1) {
            return;
        }

        RotateAnimation rotateAnimation = new RotateAnimation(
                mCurrentDegree,
                -azimuthDeg,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        rotateAnimation.setDuration(200);
        rotateAnimation.setFillAfter(true);

        mCompassImageView.startAnimation(rotateAnimation);

        mCurrentDegree = -azimuthDeg;
        mDegreeTextView.setText(String.format("方向：%.0f°", azimuthDeg));
    }
}
