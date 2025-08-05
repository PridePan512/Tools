package com.example.lib.photoview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.RectEvaluator
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.lib.R
import com.example.lib.utils.AndroidUtils
import java.util.Locale
import kotlin.math.min

// TODO: 这个页面可以增加一个恢复按钮
class PhotoViewFragment() : Fragment() {
    companion object {
        const val TAG_SIZE = "tag_size"
        const val TAG_INDEX = "tag_index"
        const val TAG_ENABLE_TRANSITION = "tag_enable_transition"
        const val TAG_LOCATION_X = "tag_location_x"
        const val TAG_LOCATION_Y = "tag_location_y"
        const val TAG_LOCATION_WIDTH = "tag_location_width"
        const val TAG_LOCATION_HEIGHT = "tag_location_height"
        const val TAG_SOURCE_WIDTH = "tag_source_width"
        const val TAG_SOURCE_HEIGHT = "tag_source_height"
        const val TAG_URI = "tag_uri"
        fun show(
            activity: FragmentActivity,
            index: Int,
            size: Int
        ) {
            val fragment = PhotoViewFragment()
            val bundle = Bundle()
            bundle.putInt(TAG_INDEX, index)
            bundle.putInt(TAG_SIZE, size)
            fragment.arguments = bundle

            activity.supportFragmentManager.beginTransaction().add(android.R.id.content, fragment)
                .show(fragment).commitNowAllowingStateLoss()
        }

        fun show(
            activity: FragmentActivity,
            index: Int,
            size: Int,
            uri: Uri?,
            imageView: ImageView,
            pair: Pair<Int, Int>
        ) {
            val fragment = PhotoViewFragment()
            val location = IntArray(2)
            imageView.getLocationOnScreen(location)
            val bundle = Bundle()
            bundle.putInt(TAG_INDEX, index)
            bundle.putInt(TAG_SIZE, size)
            bundle.putBoolean(TAG_ENABLE_TRANSITION, true)
            bundle.putInt(TAG_LOCATION_X, location[0])
            bundle.putInt(TAG_LOCATION_Y, location[1])
            bundle.putInt(TAG_LOCATION_WIDTH, imageView.width)
            bundle.putInt(TAG_LOCATION_HEIGHT, imageView.height)
            bundle.putInt(TAG_SOURCE_WIDTH, pair.first)
            bundle.putInt(TAG_SOURCE_HEIGHT, pair.second)
            bundle.putParcelable(TAG_URI, uri)
            fragment.arguments = bundle

            activity.supportFragmentManager.beginTransaction().add(android.R.id.content, fragment)
                .show(fragment).commitNowAllowingStateLoss()
        }
    }

    interface Listener {
        fun showPhoto(imageView: ImageView, index: Int)
    }

    private var mListener: Listener? = null
    private lateinit var mViewpager: ViewPager2
    private lateinit var mTitleBar: View
    private lateinit var mMainView: View
    private var mIsAnimating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        val enterAnimator = Fade()
//        enterAnimator.duration = 2000
//        enterTransition = enterAnimator
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_photo_view, container, false)
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val titleBar = view.findViewById<View>(R.id.v_title_bar)
            titleBar.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                0
            )
            insets
        }
        initView(view)
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Listener) {
            mListener = context
        }
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                close()
            }
        })
    }

    private fun initView(view: View) {
        val arguments = arguments ?: return

        mTitleBar = view.findViewById(R.id.v_title_bar)
        mMainView = view.findViewById(R.id.main)
        mViewpager = view.findViewById(R.id.v_viewpager)
        val countTextView: TextView = view.findViewById(R.id.tv_count)
        val translationImageView: ImageView = view.findViewById(R.id.iv_transition)
        val size = arguments.getInt(TAG_SIZE)
        val index = arguments.getInt(TAG_INDEX)
        val enableTransition = arguments.getBoolean(TAG_ENABLE_TRANSITION)
        if (enableTransition) {
            translationImageView.visibility = View.VISIBLE
            mViewpager.visibility = View.INVISIBLE
            doEnterAnimator(arguments, translationImageView)

        } else {
            translationImageView.visibility = View.GONE
            mViewpager.visibility = View.VISIBLE
        }

        mViewpager.offscreenPageLimit = 1
        mViewpager.adapter = object : RecyclerView.Adapter<MyViewHolder>() {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): MyViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_photo_view, parent, false)
                return MyViewHolder(view)
            }

            override fun onBindViewHolder(
                holder: MyViewHolder,
                position: Int
            ) {
                mListener?.showPhoto(holder.photoView, position)
                holder.photoView.setOnClickListener {
                    doBackgroundChangeAnimator()
                }
            }

            override fun getItemCount(): Int {
                return size
            }

        }
        mViewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                countTextView.text = String.format(Locale.getDefault(), "%d/%d", position + 1, size)
            }
        })

        if (index == 0) {
            countTextView.text = String.format(Locale.getDefault(), "%d/%d", 1, size)

        } else {
            mViewpager.setCurrentItem(index, false)
        }

        mViewpager.setPageTransformer { page, position ->
            val clampedPosition = position.coerceIn(-1f, 1f)
            val scale = 1f - kotlin.math.abs(clampedPosition) * 0.2f
            val alpha = 1f - kotlin.math.abs(clampedPosition) * 0.4f

            page.scaleX = scale
            page.scaleY = scale
            page.alpha = alpha
        }

        view.findViewById<Button>(R.id.btn_back).setOnClickListener {
            close()
        }
    }

    private fun close() {
        getParentFragmentManager()
            .beginTransaction()
            .remove(this@PhotoViewFragment)
            .commit()
    }

    private fun doBackgroundChangeAnimator() {
        if (mIsAnimating) {
            return
        }
        val animatorSet = AnimatorSet()
        animatorSet.duration = 200
        if (mTitleBar.isVisible) {
            val colorAnimator = ObjectAnimator.ofArgb(
                mMainView,
                "backgroundColor",
                Color.WHITE,
                Color.BLACK
            )
            val alphaAnimator = ObjectAnimator.ofFloat(
                mTitleBar,
                View.ALPHA,
                1f,
                0f
            )
            val translateAnimator = ObjectAnimator.ofFloat(
                mTitleBar,
                View.TRANSLATION_Y,
                0f,
                -mTitleBar.height.toFloat()
            )
            animatorSet.playTogether(
                colorAnimator,
                alphaAnimator,
                translateAnimator
            )
            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    mIsAnimating = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mIsAnimating = false
                    mTitleBar.visibility = View.INVISIBLE
                }
            })

        } else {
            val colorAnimator = ObjectAnimator.ofArgb(
                mMainView,
                "backgroundColor",
                Color.BLACK,
                Color.WHITE
            )
            val alphaAnimator = ObjectAnimator.ofFloat(
                mTitleBar,
                View.ALPHA,
                0f,
                1f
            )
            val translateAnimator = ObjectAnimator.ofFloat(
                mTitleBar,
                View.TRANSLATION_Y,
                -mTitleBar.height.toFloat(),
                0f
            )
            animatorSet.playTogether(
                colorAnimator,
                alphaAnimator,
                translateAnimator
            )
            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    mIsAnimating = true
                    mTitleBar.visibility = View.VISIBLE
                }

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mIsAnimating = false
                }
            })
        }
        animatorSet.start()
    }

    private fun doEnterAnimator(arguments: Bundle, translationImageView: ImageView) {
        val locationX = arguments.getInt(TAG_LOCATION_X)
        val locationY = arguments.getInt(TAG_LOCATION_Y)
        val originWidth = arguments.getInt(TAG_LOCATION_WIDTH)
        val originHeight = arguments.getInt(TAG_LOCATION_HEIGHT)
        val sourceWidth = arguments.getInt(TAG_SOURCE_WIDTH)
        val sourceHeight = arguments.getInt(TAG_SOURCE_HEIGHT)
        val uri = arguments.getParcelable<Uri>(TAG_URI)

        //获取centerCrop的放缩倍数
        val scale = maxOf(
            originWidth / sourceWidth.toFloat(),
            originHeight / sourceHeight.toFloat()
        )

        //获取transitionImageView的宽高
        val transitionWidth = (sourceWidth * scale).toInt()
        val translationHeight = (sourceHeight * scale).toInt()
        Glide
            .with(translationImageView.context)
            .load(uri)
            .override(transitionWidth, translationHeight)
            .into(translationImageView)

        //获取中心点
        val centerX = locationX + originWidth / 2f
        val centerY = locationY + originHeight / 2f

        //使transitionImageView对齐原始imageView中心点
        translationImageView.x = centerX - transitionWidth / 2f
        translationImageView.y = centerY - translationHeight / 2f

        //获取transitionImageView全屏显示时需要放大的倍数
        val animatorScale = min(
            AndroidUtils.getScreenWidth() / transitionWidth.toFloat(),
            AndroidUtils.getScreenHeight() / translationHeight.toFloat()
        )

        val animatorSet = AnimatorSet()
        animatorSet.duration = 300
        val scaleXAnimator = ObjectAnimator.ofFloat(
            translationImageView,
            View.SCALE_X,
            animatorScale
        )
        val scaleYAnimator = ObjectAnimator.ofFloat(
            translationImageView,
            View.SCALE_Y,
            animatorScale
        )
        val backgroundAnimator = ObjectAnimator.ofArgb(
            mMainView,
            "backgroundColor",
            Color.TRANSPARENT,
            Color.BLACK
        )
        val clipBoundAnimator = ObjectAnimator.ofObject(
            translationImageView,
            "clipBounds",
            RectEvaluator(),
            Rect(
                (transitionWidth - originWidth) / 2,
                (translationHeight - originHeight) / 2,
                (transitionWidth + originWidth) / 2,
                (translationHeight + originHeight) / 2
            ),
            Rect(0, 0, transitionWidth, translationHeight)
        )
        val translateXAnimator = ObjectAnimator.ofFloat(
            translationImageView,
            View.TRANSLATION_X,
            translationImageView.translationX,
            translationImageView.translationX + AndroidUtils.getScreenWidth() / 2f - centerX
        )
        val translateYAnimator = ObjectAnimator.ofFloat(
            translationImageView,
            View.TRANSLATION_Y,
            translationImageView.translationY,
            translationImageView.translationY + AndroidUtils.getScreenHeight() / 2f - centerY
        )
        animatorSet.playTogether(
            scaleXAnimator,
            scaleYAnimator,
            translateXAnimator,
            translateYAnimator,
            backgroundAnimator,
            clipBoundAnimator
        )
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
            }

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                mViewpager.visibility = View.VISIBLE
                translationImageView.visibility = View.GONE
            }
        })
        animatorSet.start()
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoView: PhotoView = itemView.findViewById(R.id.v_photo)
    }
}