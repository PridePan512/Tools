package com.example.tool.random.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.tool.R;

import java.util.Random;

public class GenerateRandomStringFragment extends Fragment {

    private SwitchCompat mNumberSwitch;
    private SwitchCompat mUppercaseSwitch;
    private SwitchCompat mLowercaseSwitch;
    private SwitchCompat mSpecialCharsSwitch;
    private SeekBar mSeekBarLength;
    private TextView mLengthTextView;
    private Button mGenerateButton;

    // 字符集
    private static final String NUMBERS = "0123456789";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;':,.<>?";

    public GenerateRandomStringFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_generate_random_string, container, false);

        // 初始化控件
        mNumberSwitch = view.findViewById(R.id.switch_number);
        mUppercaseSwitch = view.findViewById(R.id.switch_upper_case);
        mLowercaseSwitch = view.findViewById(R.id.switch_lower_case);
        mSpecialCharsSwitch = view.findViewById(R.id.switch_special_character);
//        mSeekBarLength = view.findViewById(R.id.seekBar_length);
        mLengthTextView = view.findViewById(R.id.tv_length);
//        mGenerateButton = view.findViewById(R.id.btn_generate);

        // 设置默认选中所有字符类型
        mNumberSwitch.setChecked(true);
        mUppercaseSwitch.setChecked(true);
        mLowercaseSwitch.setChecked(true);
        mSpecialCharsSwitch.setChecked(true);

        // 设置SeekBar监听
        mSeekBarLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mLengthTextView.setText("长度: " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // 设置默认长度为12
        mSeekBarLength.setProgress(12);
        mLengthTextView.setText("长度: 12");

        // 设置按钮点击事件
        mGenerateButton.setOnClickListener(v -> generateRandomString());

        return view;
    }

    private void generateRandomString() {
        // 获取选中的字符类型
        boolean includeNumbers = mNumberSwitch.isChecked();
        boolean includeUppercase = mUppercaseSwitch.isChecked();
        boolean includeLowercase = mLowercaseSwitch.isChecked();
        boolean includeSpecialChars = mSpecialCharsSwitch.isChecked();

        // 验证是否至少选择了一种字符类型
        if (!includeNumbers && !includeUppercase && !includeLowercase && !includeSpecialChars) {
            Toast.makeText(getActivity(), "请至少选择一种字符类型", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取字符串长度
        int length = mSeekBarLength.getProgress();

        // 构建字符集
        StringBuilder charSet = new StringBuilder();
        if (includeNumbers) charSet.append(NUMBERS);
        if (includeUppercase) charSet.append(UPPERCASE);
        if (includeLowercase) charSet.append(LOWERCASE);
        if (includeSpecialChars) charSet.append(SPECIAL_CHARS);

        // 生成随机字符串
        String randomString = generateRandomString(charSet.toString(), length);

        // 显示结果
        Toast.makeText(getActivity(), "随机字符串：\n" + randomString, Toast.LENGTH_LONG).show();
    }

    private String generateRandomString(String charSet, int length) {
        StringBuilder sb = new StringBuilder(length);
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(charSet.length());
            sb.append(charSet.charAt(randomIndex));
        }

        return sb.toString();
    }
}