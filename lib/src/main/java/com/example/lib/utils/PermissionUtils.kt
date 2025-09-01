package com.example.lib.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object PermissionUtils {

    /**
     * 获取读图片权限
     */
    fun getReadImagePermission(
        activity: Activity,
        launcher1: ActivityResultLauncher<String>,
        launcher2: ActivityResultLauncher<Intent>
    ) {
        if (checkReadImagePermission(activity)) {
            return
        }
        //在安卓14及以上机型，直接申请READ_MEDIA_IMAGES，系统会弹出部分授权的选项，不用同时申请 READ_MEDIA_VISUAL_USER_SELECTED
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            Manifest.permission.READ_MEDIA_IMAGES
        else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        requestPermission(
            permission,
            activity,
            launcher1,
            launcher2
        )
    }

    /**
     * 检查读图片权限
     */
    fun checkReadImagePermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    ) == PackageManager.PERMISSION_GRANTED

        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 检查是否是允许部分访问媒体文件
     */
    fun checkIsLimitReadMedia(context: Context): Boolean {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
            return false
        }

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 获取写文件权限
     */
    fun getWritePermission(
        activity: Activity,
        launcher1: ActivityResultLauncher<String>,
        launcher2: ActivityResultLauncher<Intent>
    ) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q || checkWritePermission(activity)) {
            return
        }

        requestPermission(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            activity,
            launcher1,
            launcher2
        )
    }

    /**
     * 检查写文件权限
     */
    fun checkWritePermission(context: Context): Boolean {
        //api29之后，默认赋予写权限
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查通知权限
     */
    fun checkNotificationPermission(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * 获取通知权限
     */
    fun getNotificationPermission(
        activity: Activity,
        launcher1: ActivityResultLauncher<String>,
        launcher2: ActivityResultLauncher<Intent>
    ) {
        if (checkNotificationPermission(activity)) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            // 当用户多次点击不再询问(一般是两次),授权界面就不再会出现
            if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                launcher1.launch(permission)

            } else {
                getNotificationPermissionByIntent(activity, launcher2)
            }

        } else {
            getNotificationPermissionByIntent(activity, launcher2)
        }
    }

    /**
     * 检查读取媒体位置权限
     */
    fun checkAccessMediaLocationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_MEDIA_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 获取读取媒体位置权限
     */
    fun getAccessMediaLocationPermission(
        activity: Activity,
        launcher1: ActivityResultLauncher<String>,
        launcher2: ActivityResultLauncher<Intent>
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || checkAccessMediaLocationPermission(
                activity
            )
        ) {
            return
        }

        requestPermission(Manifest.permission.ACCESS_MEDIA_LOCATION, activity, launcher1, launcher2)
    }

    private fun getNotificationPermissionByIntent(
        activity: Activity,
        launcher: ActivityResultLauncher<Intent>
    ) {
        //api 26之后可以直接跳转到app的通知权限界面，之前只能跳转到app所有权限界面
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
        }
        launcher.launch(intent)
    }

    private fun requestPermission(
        permission: String,
        activity: Activity,
        launcher1: ActivityResultLauncher<String>,
        launcher2: ActivityResultLauncher<Intent>
    ) {
        // 当用户多次点击不再询问(一般是两次),授权界面就不再会出现
        if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            launcher1.launch(permission)

        } else {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
            launcher2.launch(intent)
        }
    }
}