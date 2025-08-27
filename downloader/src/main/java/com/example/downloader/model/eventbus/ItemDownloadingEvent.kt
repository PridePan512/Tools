package com.example.downloader.model.eventbus

import com.example.downloader.model.DownloadInfo
import com.example.downloader.model.VideoTask

class ItemDownloadingEvent(val videoTask: VideoTask, val downloadInfo: DownloadInfo)