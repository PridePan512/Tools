package com.example.tool.random.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.lib.utils.AndroidUtils;
import com.example.tool.R;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class GenerateRandomNumberFragment extends Fragment {

    private TextInputLayout mMinNumberLayout;
    private TextInputLayout mMaxNumberLayout;
    private TextInputLayout mGenerateAmountLayout;

    private EditText mMinNumberEditText;
    private EditText mMaxNumberEditText;
    private EditText mGenerateAmountEditText;

    private SwitchCompat mRepeatSwitch;
    private Button mGenerateButton;

    private ConstraintLayout mResultLayout;
    private TextView mResultTextView;
    private ImageView mCopyImageView;

    private final Random mRandom = new Random();

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_generate_random_number,
                container,
                false
        );

        initView(view);
        initListeners();

        validateInput(); // 初始化按钮状态
        return view;
    }

    private void initView(View view) {
        mMinNumberLayout = view.findViewById(R.id.view_min_number);
        mMaxNumberLayout = view.findViewById(R.id.view_max_number);
        mGenerateAmountLayout = view.findViewById(R.id.view_generate_amount);

        mMinNumberEditText = mMinNumberLayout.getEditText();
        mMaxNumberEditText = mMaxNumberLayout.getEditText();
        mGenerateAmountEditText = mGenerateAmountLayout.getEditText();

        mRepeatSwitch = view.findViewById(R.id.switch_repeat);
        mGenerateButton = view.findViewById(R.id.btn_generate);

        mResultTextView = view.findViewById(R.id.tv_result);
        mCopyImageView = view.findViewById(R.id.iv_copy);
        mResultLayout = view.findViewById(R.id.view_generate);
        mResultLayout.setVisibility(View.GONE);
    }

    private void initListeners() {
        TextWatcher watcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                validateInput();
            }
        };

        mMinNumberEditText.addTextChangedListener(watcher);
        mMaxNumberEditText.addTextChangedListener(watcher);
        mGenerateAmountEditText.addTextChangedListener(watcher);

        mRepeatSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> validateInput()
        );

        mGenerateButton.setOnClickListener(v -> {
            AndroidUtils.INSTANCE.hideKeyboard(requireContext(), v);
            generateRandomNumbers();
        });

        mCopyImageView.setOnClickListener(v -> {
            String text = mResultTextView.getText().toString();
            if (!TextUtils.isEmpty(text)) {
                AndroidUtils.INSTANCE.copyToClipboard(requireContext(), text, null);
                Toast.makeText(getContext(), R.string.toast_copied, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 所有输入校验的唯一入口
     */
    private void validateInput() {
        clearErrors();

        String minStr = getText(mMinNumberEditText);
        String maxStr = getText(mMaxNumberEditText);
        String countStr = getText(mGenerateAmountEditText);

        Integer min = parseInt(minStr);
        Integer max = parseInt(maxStr);
        Integer count = parseInt(countStr);

        boolean valid = true;

        // min / max 校验
        if (min != null && max != null && min > max) {
            mMinNumberLayout.setError(getString(R.string.helper_text_min_max));
            mMaxNumberLayout.setError(getString(R.string.helper_text_min_max));
            valid = false;
        }

        // 生成数量校验
        if (count != null) {
            if (count <= 0) {
                mGenerateAmountLayout.setError(
                        getString(R.string.helper_text_generate_amount)
                );
                valid = false;
            } else if (!mRepeatSwitch.isChecked()
                    && min != null && max != null
                    && count > (max - min + 1)) {

                mGenerateAmountLayout.setError(
                        getString(R.string.toast_no_repeat_exceed_range)
                );
                valid = false;
            }
        }

        mGenerateButton.setEnabled(valid);
    }

    private void clearErrors() {
        mMinNumberLayout.setError(null);
        mMaxNumberLayout.setError(null);
        mGenerateAmountLayout.setError(null);
    }

    private void generateRandomNumbers() {
        Integer min = parseInt(getText(mMinNumberEditText));
        Integer max = parseInt(getText(mMaxNumberEditText));
        Integer count = parseInt(getText(mGenerateAmountEditText));

        if (min == null || max == null || count == null) {
            Toast.makeText(getContext(), R.string.toast_input_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        List<Integer> result;
        if (mRepeatSwitch.isChecked()) {
            result = generateWithDuplicates(min, max, count);

        } else {
            result = generateWithoutDuplicates(min, max, count);
        }

        showResult(result);
    }

    private List<Integer> generateWithDuplicates(int min, int max, int count) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(mRandom.nextInt(max - min + 1) + min);
        }
        return list;
    }

    private List<Integer> generateWithoutDuplicates(int min, int max, int count) {
        List<Integer> pool = new ArrayList<>();
        for (int i = min; i <= max; i++) {
            pool.add(i);
        }
        Collections.shuffle(pool, mRandom);
        return pool.subList(0, count);
    }

    private void showResult(List<Integer> numbers) {
        if (numbers == null || numbers.isEmpty()) {
            return;
        }

        String result = numbers.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));

        mResultLayout.setVisibility(View.VISIBLE);
        mResultTextView.setText(result);
    }

    private String getText(EditText editText) {
        return editText == null
                ? ""
                : editText.getText().toString().trim();
    }

    private Integer parseInt(String value) {
        if (TextUtils.isEmpty(value)) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}