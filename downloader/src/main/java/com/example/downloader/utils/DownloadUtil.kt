package com.example.downloader.utils

import android.util.Log
import com.example.downloader.MyApplication
import com.example.downloader.business.LibHelper
import com.example.downloader.model.DownloadInfo
import com.example.downloader.model.TaskHistory
import com.example.downloader.model.VideoTask
import com.example.downloader.model.eventbus.AddHistoryEvent
import com.example.downloader.model.eventbus.ItemDownloadErrorEvent
import com.example.downloader.model.eventbus.ItemDownloadedEvent
import com.example.downloader.model.eventbus.ItemDownloadingEvent
import com.example.lib.utils.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.io.File

object DownloadUtil {
    private val TAG = "DownloadUtil"

    // TODO: 处理安卓10以下的权限适配 处理文件已存在的情况
    // TODO: 处理多个任务同时下载出现的异常
    // TODO: temp文件夹的清理时机？
    fun downloadVideo(
        videoTask: VideoTask,
        coroutineScope: CoroutineScope
    ) {
        val videoInfo = videoTask.videoInfo
        try {
            if (videoInfo.requestedFormats != null && videoInfo.requestedFormats.size == 2) {
                //音视频分离的情况
                val taskHistory = TaskHistory()
                var isFinish = false

                coroutineScope.launch(Dispatchers.IO) {
                    var sourcePath: String? = null

                    LibHelper.downloadVideo(
                        videoInfo.webpageUrl!!,
                        processId = null
                    ) { progress, speed, line ->
                        val progress = progress.toInt()

                        if (progress != 0) {
                            EventBus.getDefault().post(
                                ItemDownloadingEvent(videoTask, DownloadInfo(progress, speed))
                            )
                        }

                        when {
                            //下载已完成 开始合并视频和音频
                            line.startsWith("[Merger]") -> {
                                taskHistory.title = videoInfo.title
                                taskHistory.thumbnail = videoInfo.thumbnail
                                taskHistory.uploader = videoInfo.uploader
                                taskHistory.url = videoInfo.webpageUrl
                                taskHistory.duration = videoInfo.duration
                                taskHistory.downloadTime = System.currentTimeMillis()
                                taskHistory.uploadDate = videoInfo.uploadDate
                                taskHistory.source =
                                    videoInfo.extractorKey ?: videoInfo.extractor

                                //用正则表达式 找到文件的下载路径
                                sourcePath = "\"([^\"]+)\"".toRegex()
                                    .find(line)?.groups?.get(1)?.value
                            }

                            // 在输出Deleting之后 文件已经merge完成
                            line.startsWith("Deleting") -> {
                                if (!isFinish) {
                                    isFinish = true
                                    //把最终文件移出temp 记录存入数据库
                                    val sourceFile = File(
                                        sourcePath!!
                                    )
                                    val targetFile =
                                        File(
                                            sourceFile.parentFile!!.parent!!,
                                            FileUtils.appendTimestampToFilename(sourceFile.name)
                                        )
                                    sourceFile.renameTo(targetFile)

                                    taskHistory.path = targetFile.absolutePath
                                    taskHistory.size = targetFile.length()
                                    MyApplication.database.historyDao()
                                        .insertHistory(taskHistory)
                                    EventBus.getDefault().post(AddHistoryEvent(taskHistory.source))
                                    EventBus.getDefault().post(
                                        ItemDownloadedEvent(videoTask)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                //音视频没有分离的情况
                coroutineScope.launch(Dispatchers.IO) {
                    LibHelper.downloadVideo(
                        videoInfo.webpageUrl!!,
                        processId = null
                    ) { progress, speed, line ->
                        val progress = progress.toInt()
                        if (progress != 0) {
                            EventBus.getDefault().post(
                                ItemDownloadingEvent(videoTask, DownloadInfo(progress, speed))
                            )
                        }

                        if (line.startsWith("[FixupM3u8]")) {
                            coroutineScope.launch(Dispatchers.IO) {
                                //延时 因为输出[FixupM3u8]时 下载可能并没有完全结束 为了保险起见 先延时三秒
                                delay(3000)
                                val taskHistory = TaskHistory()
                                taskHistory.title = videoInfo.title
                                taskHistory.thumbnail = videoInfo.thumbnail
                                taskHistory.uploader = videoInfo.uploader
                                taskHistory.url = videoInfo.webpageUrl
                                taskHistory.duration = videoInfo.duration
                                taskHistory.downloadTime = System.currentTimeMillis()
                                taskHistory.uploadDate = videoInfo.uploadDate
                                taskHistory.source =
                                    videoInfo.extractorKey ?: videoInfo.extractor

                                //把最终文件移出temp 记录存入数据库
                                val sourceFile = File(
                                    //用正则表达式 找到文件的下载路径
                                    "\"([^\"]+)\"".toRegex()
                                        .find(line)?.groups?.get(1)?.value!!
                                )
                                val targetFile =
                                    File(
                                        sourceFile.parentFile!!.parent!!,
                                        FileUtils.appendTimestampToFilename(sourceFile.name)
                                    )
                                sourceFile.renameTo(targetFile)

                                taskHistory.path = targetFile.absolutePath
                                taskHistory.size = targetFile.length()
                                MyApplication.database.historyDao().insertHistory(taskHistory)
                                EventBus.getDefault().post(AddHistoryEvent(taskHistory.source))
                                EventBus.getDefault()
                                    .post(ItemDownloadedEvent(videoTask))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download error : , ${e.message}")
            EventBus.getDefault().post(ItemDownloadErrorEvent(videoTask, e))
        }
    }
}