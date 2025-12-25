package com.example.tool

import android.content.Intent
import android.os.Bundle
import com.example.lib.mvvm.BaseActivity
import com.example.tool.compass.CompassActivity
import com.example.tool.databinding.ActivityMainBinding
import com.example.tool.random.GenerateRandomActivity
import com.example.tool.reactiontest.ReactionTestActivity
import com.example.tool.wallpaper.activity.WallpaperActivity

class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.btnReactionTest.setOnClickListener {
            startActivity(Intent(this, ReactionTestActivity::class.java))
        }
        binding.btnRandom.setOnClickListener {
            startActivity(Intent(this, GenerateRandomActivity::class.java))
        }
        binding.btnCompass.setOnClickListener {
            startActivity(Intent(this, CompassActivity::class.java))
        }
        binding.btnWallpaper.setOnClickListener {
            startActivity(Intent(this, WallpaperActivity::class.java))
        }
    }
}