package com.example.lib.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.graphics.createBitmap

object BitmapUtil {

    /**
     * 获取ImageView内部图片的展示区域
     */
    private fun getVisibleImageRect(imageView: ImageView): RectF? {
        val drawable = imageView.drawable ?: return null
        val matrix = imageView.imageMatrix
        val drawableRect =
            RectF(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
        matrix.mapRect(drawableRect) // 映射 drawable 到 imageView 的坐标系
        return drawableRect
    }

    /**
     * 从drawable中获取bitmap
     */
    fun getBitmapFromDrawable(drawable: Drawable?): Bitmap? {
        // 检查 drawable 是否为 null
        if (drawable == null) {
            return null
        }

        // 如果 drawable 是 BitmapDrawable 类型，直接返回 bitmap
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        // 获取 drawable 的固有宽度和高度
        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight
        // 如果宽度或高度小于等于0，返回null
        if (width <= 0 || height <= 0) {
            return null
        }

        // 定义 bitmap 的配置：不透明使用 RGB_565，半透明使用 ARGB_8888
        val config =
            if (drawable.opacity != PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        // 创建一个空白的 bitmap
        val bitmap = createBitmap(width, height, config)

        // 创建 Canvas 对象并在其上绘制 drawable
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    /**
     * 高斯模糊
     */
    fun Bitmap.blur(radius: Int, canReuse: Boolean = true): Bitmap? {
        if (radius < 1) return null

        val bmp = if (canReuse) this else copy(config!!, true)
        val w = bmp.width
        val h = bmp.height
        val wh = w * h
        val wm = w - 1
        val hm = h - 1
        val div = radius * 2 + 1

        val pix = IntArray(wh).apply { bmp.getPixels(this, 0, w, 0, 0, w, h) }
        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        val vmin = IntArray(maxOf(w, h))

        val divsum = ((div + 1) / 2).let { it * it }
        val dv = IntArray(256 * divsum) { it / divsum }
        val stack = Array(div) { IntArray(3) }
        val r1 = radius + 1

        var yi = 0
        var yw = 0

        // 横向
        repeat(h) { y ->
            var rsum = 0;
            var gsum = 0;
            var bsum = 0
            var rinsum = 0;
            var ginsum = 0;
            var binsum = 0
            var routsum = 0;
            var goutsum = 0;
            var boutsum = 0

            (-radius..radius).forEach { i ->
                val p = pix[yi + minOf(wm, maxOf(i, 0))]
                val sir = stack[i + radius].apply {
                    this[0] = (p shr 16) and 0xff
                    this[1] = (p shr 8) and 0xff
                    this[2] = p and 0xff
                }
                val rbs = r1 - kotlin.math.abs(i)
                rsum += sir[0] * rbs; gsum += sir[1] * rbs; bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                } else {
                    routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                }
            }

            var stackPointer = radius
            repeat(w) { x ->
                r[yi] = dv[rsum]; g[yi] = dv[gsum]; b[yi] = dv[bsum]

                rsum -= routsum; gsum -= goutsum; bsum -= boutsum

                val sir = stack[(stackPointer - radius + div) % div].also {
                    routsum -= it[0]; goutsum -= it[1]; boutsum -= it[2]
                }

                if (y == 0) vmin[x] = minOf(x + radius + 1, wm)
                val p = pix[yw + vmin[x]]
                sir[0] = (p shr 16) and 0xff; sir[1] = (p shr 8) and 0xff; sir[2] = p and 0xff
                rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]

                rsum += rinsum; gsum += ginsum; bsum += binsum
                stackPointer = (stackPointer + 1) % div

                stack[stackPointer].also {
                    routsum += it[0]; goutsum += it[1]; boutsum += it[2]
                    rinsum -= it[0]; ginsum -= it[1]; binsum -= it[2]
                }

                yi++
            }
            yw += w
        }

        // 纵向
        repeat(w) { x ->
            var rsum = 0;
            var gsum = 0;
            var bsum = 0
            var rinsum = 0;
            var ginsum = 0;
            var binsum = 0
            var routsum = 0;
            var goutsum = 0;
            var boutsum = 0

            var yp = -radius * w
            (-radius..radius).forEach { i ->
                val yiTmp = maxOf(0, yp) + x
                val sir = stack[i + radius].apply {
                    this[0] = r[yiTmp]; this[1] = g[yiTmp]; this[2] = b[yiTmp]
                }
                val rbs = r1 - kotlin.math.abs(i)
                rsum += r[yiTmp] * rbs; gsum += g[yiTmp] * rbs; bsum += b[yiTmp] * rbs
                if (i > 0) {
                    rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                } else {
                    routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                }
                if (i < hm) yp += w
            }

            var yiTmp = x
            var stackPointer = radius
            repeat(h) { y ->
                pix[yiTmp] = (pix[yiTmp] and -0x1000000) or
                        (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= routsum; gsum -= goutsum; bsum -= boutsum

                val sir = stack[(stackPointer - radius + div) % div].also {
                    routsum -= it[0]; goutsum -= it[1]; boutsum -= it[2]
                }

                if (x == 0) vmin[y] = minOf(y + r1, hm) * w
                val p = x + vmin[y]
                sir[0] = r[p]; sir[1] = g[p]; sir[2] = b[p]
                rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]

                rsum += rinsum; gsum += ginsum; bsum += binsum
                stackPointer = (stackPointer + 1) % div

                stack[stackPointer].also {
                    routsum += it[0]; goutsum += it[1]; boutsum += it[2]
                    rinsum -= it[0]; ginsum -= it[1]; binsum -= it[2]
                }

                yiTmp += w
            }
        }

        bmp.setPixels(pix, 0, w, 0, 0, w, h)
        return bmp
    }

}