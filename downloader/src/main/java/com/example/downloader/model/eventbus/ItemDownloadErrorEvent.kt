package com.example.downloader.model.eventbus

import com.example.downloader.model.VideoTask

class ItemDownloadErrorEvent(val videoTask: VideoTask, val error:Exception)