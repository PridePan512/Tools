package com.example.lib.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import android.widget.ImageView
import androidx.exifinterface.media.ExifInterface

object AndroidUtils {

    fun getScreenWidth(): Int {
        return Resources.getSystem().displayMetrics.widthPixels
    }

    fun getScreenHeight(): Int {
        return Resources.getSystem().displayMetrics.heightPixels
    }

    fun dpToPx(dp: Int): Int {
        return (Resources.getSystem().displayMetrics.density * dp).toInt()
    }

    //获取imageview的真实显示区域
    fun getVisibleImageRect(imageView: ImageView): RectF? {
        val drawable = imageView.drawable ?: return null
        val drawableRect =
            RectF(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
        imageView.imageMatrix.mapRect(drawableRect) // 映射 drawable 到 imageView 的坐标系
        return drawableRect
    }

    //根据uri获取图片的宽高
    fun getImageSizeFromUri(context: Context, uri: Uri): Pair<Int, Int>? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // 先读取尺寸
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(inputStream, null, options)

                // 再读取一次流用于读取 EXIF（因为 decodeStream 会关闭它）
                context.contentResolver.openInputStream(uri)?.use { exifStream ->
                    val exif = ExifInterface(exifStream)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED
                    )

                    val isRotated = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                            orientation == ExifInterface.ORIENTATION_ROTATE_270

                    if (isRotated) {
                        Pair(options.outHeight, options.outWidth)

                    } else {
                        Pair(options.outWidth, options.outHeight)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(0, 0)
        }
    }

}