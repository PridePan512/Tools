package com.example.tool.random.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.tool.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GenerateRandomNumberFragment extends Fragment {

    private EditText mMinNumberEditText;
    private EditText mMaxNumberEditText;
    private EditText mGenerateAmountEditText;
    private Switch mRepeatSwitch;
    private Button mGenerateButton;

    public GenerateRandomNumberFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_generate_random_number, container, false);

        // 初始化控件
        mMinNumberEditText = view.findViewById(R.id.et_min);
        mMaxNumberEditText = view.findViewById(R.id.et_max);
        mGenerateAmountEditText = view.findViewById(R.id.et_generate_amount);
        mRepeatSwitch = view.findViewById(R.id.switch_repeat);
        mGenerateButton = view.findViewById(R.id.btn_generate);

        // 设置按钮点击事件
        mGenerateButton.setOnClickListener(v -> generateRandomNumbers());

        return view;
    }

    private void generateRandomNumbers() {
        // 获取输入值
        String minStr = mMinNumberEditText.getText().toString().trim();
        String maxStr = mMaxNumberEditText.getText().toString().trim();
        String countStr = mGenerateAmountEditText.getText().toString().trim();

        // 验证输入
        if (TextUtils.isEmpty(minStr) || TextUtils.isEmpty(maxStr) || TextUtils.isEmpty(countStr)) {
            Toast.makeText(getActivity(), "请输入完整信息", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int min = Integer.parseInt(minStr);
            int max = Integer.parseInt(maxStr);
            int count = Integer.parseInt(countStr);

            // 验证输入范围
            if (min > max) {
                Toast.makeText(getActivity(), "最小值不能大于最大值", Toast.LENGTH_SHORT).show();
                return;
            }

            if (count <= 0) {
                Toast.makeText(getActivity(), "生成数量必须大于0", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!mRepeatSwitch.isChecked() && (max - min + 1) < count) {
                Toast.makeText(getActivity(), "不允许重复时，生成数量不能超过范围", Toast.LENGTH_SHORT).show();
                return;
            }

            // 生成随机数
            List<Integer> randomNumbers;
            if (mRepeatSwitch.isChecked()) {
                // 允许重复
                randomNumbers = generateRandomNumbersWithDuplicates(min, max, count);
            } else {
                // 不允许重复
                randomNumbers = generateRandomNumbersWithoutDuplicates(min, max, count);
            }

            // 显示结果
            showResult(randomNumbers);

        } catch (NumberFormatException e) {
            Toast.makeText(getActivity(), "请输入有效的数字", Toast.LENGTH_SHORT).show();
        }
    }

    private List<Integer> generateRandomNumbersWithDuplicates(int min, int max, int count) {
        List<Integer> numbers = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < count; i++) {
            numbers.add(random.nextInt(max - min + 1) + min);
        }

        return numbers;
    }

    private List<Integer> generateRandomNumbersWithoutDuplicates(int min, int max, int count) {
        Set<Integer> numbersSet = new HashSet<>();
        Random random = new Random();

        while (numbersSet.size() < count) {
            numbersSet.add(random.nextInt(max - min + 1) + min);
        }

        return new ArrayList<>(numbersSet);
    }

    private void showResult(List<Integer> numbers) {
        StringBuilder result = new StringBuilder("随机数结果：\n");
        for (int i = 0; i < numbers.size(); i++) {
            result.append(i + 1).append(". ").append(numbers.get(i));
            if (i < numbers.size() - 1) {
                result.append("\n");
            }
        }

        Toast.makeText(getActivity(), result.toString(), Toast.LENGTH_LONG).show();
    }
}