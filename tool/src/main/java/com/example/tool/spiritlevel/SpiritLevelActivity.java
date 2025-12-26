package com.example.tool.spiritlevel;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lib.utils.AndroidUtils;
import com.example.tool.R;


public class SpiritLevelActivity extends AppCompatActivity implements SensorEventListener {

    private ImageView mDotImageView;
    private TextView mVerticalTextView;
    private TextView mHorizontalTextView;

    private SensorManager mSensorManager;
    private Sensor mRotationVectorSensor;

    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];

    private float mMaxRadius;

    private static final float ANGLE_EPSILON = 0.05f;
    private static final long UI_UPDATE_INTERVAL_MS = 16;

    private float mLastPitch = Float.NaN;
    private float mLastRoll = Float.NaN;
    private long mLastUiTime = 0;

    private final StringBuilder mTextBuilder = new StringBuilder(32);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spirit_level);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        mDotImageView = findViewById(R.id.iv_level_dot);
        mVerticalTextView = findViewById(R.id.tv_vertical);
        mHorizontalTextView = findViewById(R.id.tv_horizontal);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mRotationVectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        mMaxRadius = AndroidUtils.INSTANCE.dpToPx(124);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(
                this,
                mRotationVectorSensor,
                SensorManager.SENSOR_DELAY_UI
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ROTATION_VECTOR) return;

        // Rotation Vector → Matrix
        SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
        SensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

        float pitch = (float) Math.toDegrees(mOrientationAngles[1]);
        float roll = (float) Math.toDegrees(mOrientationAngles[2]);

        float verticalAngle = -pitch;
        float horizontalAngle = roll;

        // 角度变化太小直接忽略
        if (!Float.isNaN(mLastPitch)
                && Math.abs(verticalAngle - mLastPitch) < ANGLE_EPSILON
                && Math.abs(horizontalAngle - mLastRoll) < ANGLE_EPSILON) {
            return;
        }

        mLastPitch = verticalAngle;
        mLastRoll = horizontalAngle;

        // 限制更新频率
        long now = System.currentTimeMillis();
        if (now - mLastUiTime < UI_UPDATE_INTERVAL_MS) {
            return;
        }
        mLastUiTime = now;

        mTextBuilder.setLength(0);
        mTextBuilder.append("垂直: ")
                .append((int) (verticalAngle * 10) / 10f)
                .append("°");
        mVerticalTextView.setText(mTextBuilder);

        mTextBuilder.setLength(0);
        mTextBuilder.append("水平: ")
                .append((int) (horizontalAngle * 10) / 10f)
                .append("°");
        mHorizontalTextView.setText(mTextBuilder);

        // 位移
        float sinX = (float) Math.sin(Math.toRadians(horizontalAngle));
        float sinY = (float) Math.sin(Math.toRadians(verticalAngle));

        float translateX = sinX * mMaxRadius;
        float translateY = sinY * mMaxRadius;

        // 限制圆
        float d2 = translateX * translateX + translateY * translateY;
        float r2 = mMaxRadius * mMaxRadius;

        if (d2 > r2) {
            float scale = mMaxRadius / (float) Math.sqrt(d2);
            translateX *= scale;
            translateY *= scale;
        }

        mDotImageView.setTranslationX(translateX);
        mDotImageView.setTranslationY(translateY);
    }
}

