package com.example.downloader.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import com.example.downloader.R
import com.example.downloader.activity.MainActivity
import com.example.downloader.model.eventbus.ClearCompleteNotificationEvent
import com.example.downloader.model.eventbus.ItemDownloadErrorEvent
import com.example.downloader.model.eventbus.ItemDownloadedEvent
import com.example.downloader.model.eventbus.ItemDownloadingEvent
import com.example.downloader.model.eventbus.StartServiceDownloadEvent
import com.example.downloader.utils.DownloadUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class DownloadService : Service() {
    companion object {
        const val CLEAR_COMPLETE_NOTIFICATION = "clear_complete_notification"
    }

    private val NOTIFICATION_CHANNEL_ID = "channel_id"
    private val NOTIFICATION_CHANNEL_NAME = "channel_name"
    private val FOREGROUND_ID = 1
    private val KEY_DOWNLOAD_GROUP = "notification_group"
    private var mNotificationManager: NotificationManager? = null
    private lateinit var mPendingIntent: PendingIntent
    private val mServiceScope = CoroutineScope(Dispatchers.Main + Job())
    private var mLastUpdateProgressTime = 0L
    private var mTotalCount = 0
    private val mCompleteIds = ArrayList<Int>()

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT //不会在视觉上打扰用户
            )
            mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            mNotificationManager?.createNotificationChannel(notificationChannel)
        }

        mPendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).putExtra(
                    CLEAR_COMPLETE_NOTIFICATION,
                    "clear"
                ),
                PendingIntent.FLAG_IMMUTABLE
            )

        val summaryNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setGroup(KEY_DOWNLOAD_GROUP)
                .setGroupSummary(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build()
        } else {
            NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setGroup(KEY_DOWNLOAD_GROUP)
                .setGroupSummary(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
        }

        startForeground(FOREGROUND_ID, summaryNotification)

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onDestroy() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        mServiceScope.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onMessage(event: StartServiceDownloadEvent) {
        mTotalCount++
        DownloadUtil.downloadVideo(event.videoTask, mServiceScope)
        EventBus.getDefault().removeStickyEvent(event)

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(event.videoTask.videoInfo.title)
                .setProgress(100, 0, true)
                .setGroup(KEY_DOWNLOAD_GROUP)
                .setContentIntent(mPendingIntent)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build()
        } else {
            NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(event.videoTask.videoInfo.title)
                .setProgress(100, 0, true)
                .setGroup(KEY_DOWNLOAD_GROUP)
                .setContentIntent(mPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
        }
        mNotificationManager?.notify(event.videoTask.id.toInt(), notification)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessage(event: ItemDownloadingEvent) {
        // 1s更新一次进度 防止过于频繁的更新 从而造成通知的丢失
        if (SystemClock.elapsedRealtime() - mLastUpdateProgressTime <= DateUtils.SECOND_IN_MILLIS) {
            return
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(event.videoTask.videoInfo.title)
                .setProgress(100, event.downloadInfo.progress, false)
                .setGroup(KEY_DOWNLOAD_GROUP)
                .setContentIntent(mPendingIntent)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build()
        } else {
            NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(event.videoTask.videoInfo.title)
                .setProgress(100, event.downloadInfo.progress, false)
                .setGroup(KEY_DOWNLOAD_GROUP)
                .setContentIntent(mPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
        }
        mNotificationManager?.notify(event.videoTask.id.toInt(), notification)
        mLastUpdateProgressTime = SystemClock.elapsedRealtime()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessage(event: ItemDownloadedEvent) {
        mCompleteIds.add(event.videoTask.id.toInt())
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(event.videoTask.videoInfo.title)
                .setContentText(getString(R.string.download_success))
                .setGroup(KEY_DOWNLOAD_GROUP)
                .setContentIntent(mPendingIntent)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build()
        } else {
            NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(event.videoTask.videoInfo.title)
                .setContentText(getString(R.string.download_success))
                .setGroup(KEY_DOWNLOAD_GROUP)
                .setContentIntent(mPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
        }
        mNotificationManager?.notify(event.videoTask.id.toInt(), notification)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessage(event: ItemDownloadErrorEvent) {
        mCompleteIds.add(event.videoTask.id.toInt())
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(event.videoTask.videoInfo.title)
                .setContentText(getString(R.string.download_failed))
                .setGroup(KEY_DOWNLOAD_GROUP)
                .setContentIntent(mPendingIntent)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build()
        } else {
            NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(event.videoTask.videoInfo.title)
                .setContentText(getString(R.string.download_failed))
                .setGroup(KEY_DOWNLOAD_GROUP)
                .setContentIntent(mPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
        }
        mNotificationManager?.notify(event.videoTask.id.toInt(), notification)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessage(event: ClearCompleteNotificationEvent) {
        if (mTotalCount == mCompleteIds.size) {
            //如果所有任务都完成 结束服务
            stopForeground(STOP_FOREGROUND_REMOVE) // 停止前台状态
            stopSelf()           // 停掉服务

        } else {
            //清理掉已完成的通知
            mTotalCount = mTotalCount - mCompleteIds.size
            mCompleteIds.forEach {
                mNotificationManager?.cancel(it)
            }
            mCompleteIds.clear()
        }
    }
}