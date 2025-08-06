package com.example.lib.utils

import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

object StringUtils {

    // 把bytes转化为kb/mb/gb
    fun getHumanFriendlyByteCount(bytes: Long, decimalPlaces: Int): String {
        val unit = 1024L
        val units = "KMGTPE"
        val locale = Locale.getDefault()

        if (bytes < unit) return "$bytes B"

        val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
        val prefix = units[exp - 1]
        val result = bytes / unit.toDouble().pow(exp.toDouble())

        return "%.${decimalPlaces}f %sB".format(locale, result, prefix)
    }
}