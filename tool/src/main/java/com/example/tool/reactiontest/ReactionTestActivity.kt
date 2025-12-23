package com.example.tool.reactiontest

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.lib.mvvm.BaseActivity
import com.example.tool.R
import com.example.tool.business.ConfigHost
import com.example.tool.databinding.ActivityReactionTestBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.concurrent.ThreadLocalRandom

class ReactionTestActivity : BaseActivity<ActivityReactionTestBinding>() {
    private val mHandler: Handler = Handler(Looper.getMainLooper())
    private var mColorChangeTime = 0L

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ConfigHost.getReactionTestRecord(this).takeIf { it != 0L }?.let {
            binding.tvRecord.text = getString(R.string.reaction_test_record, it)
        }

        binding.ivArea.setOnTouchListener { view: View, motionEvent: MotionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    binding.tvResult.text = ""
                    mHandler.postDelayed({
                        mColorChangeTime = SystemClock.elapsedRealtime()
                        view.setBackgroundColor(getColor(R.color.reaction_test_area_change))
                    }, ThreadLocalRandom.current().nextLong(2000, 4000))
                }

                MotionEvent.ACTION_UP -> {
                    binding.tvResult.text =
                        if (mColorChangeTime == 0L) getString(R.string.reaction_test_warning) else {
                            val spendTime = SystemClock.elapsedRealtime() - mColorChangeTime
                            val record = ConfigHost.getReactionTestRecord(this)
                            if (record == 0L || spendTime < record) {
                                ConfigHost.setReactionTestRecord(this, spendTime)
                                binding.tvRecord.text =
                                    getString(R.string.reaction_test_record, spendTime)
                            }
                            getString(R.string.reaction_test_result, spendTime)
                        }
                    mColorChangeTime = 0L
                    view.setBackgroundColor(getColor(R.color.reaction_test_area_normal))
                    mHandler.removeCallbacksAndMessages(null)
                }
            }
            true
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.ivClear.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.text_clear_confirm)
                .setPositiveButton(R.string.clear) { _, _ ->
                    binding.tvRecord.text = null
                    binding.tvResult.text = null
                    ConfigHost.setReactionTestRecord(this, 0L)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val param = binding.btnBack.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            binding.btnBack.layoutParams = param
            WindowInsetsCompat.CONSUMED
        }
    }
}