package com.example.swipeclean.activity

import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.component1
import androidx.activity.result.component2
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.lib.mvvm.BaseActivity
import com.example.lib.photoview.PhotoViewFragment
import com.example.lib.utils.AndroidUtils
import com.example.lib.utils.PermissionUtils
import com.example.lib.utils.StringUtils.getHumanFriendlyByteCount
import com.example.swipeclean.adapter.RecyclerBinAdapter
import com.example.swipeclean.model.Image
import com.example.swipeclean.other.Constants.KEY_INTENT_ALBUM_ID
import com.example.swipeclean.other.Constants.MIN_SHOW_LOADING_TIME
import com.example.swipeclean.viewmodel.RecyclerBinViewModel
import com.example.tools.R
import com.example.tools.databinding.ActivityRecycleBinBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Collections

class RecycleBinActivity : BaseActivity<ActivityRecycleBinBinding>(), PhotoViewFragment.Listener {
    private lateinit var mAdapter: RecyclerBinAdapter
    private val mRecyclerBinViewModel: RecyclerBinViewModel by viewModels()
    private var mStartRestoreTime = 0L

    private val mNewDeleteLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                binding.vLoading.visibility = View.VISIBLE
                mRecyclerBinViewModel.cleanCompletedImages(mAdapter, this@RecycleBinActivity)
            }
        }

    private val mOldDeleteLauncher1: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                useOldDelete()
            }
        }

    private val mOldDeleteLauncher2: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { (resultCode, data) ->
            if (PermissionUtils.checkWritePermission(this)) {
                useOldDelete()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        init()

        mRecyclerBinViewModel.initAlbum(intent.getLongExtra(KEY_INTENT_ALBUM_ID, 0))

        showDeletedImages(mRecyclerBinViewModel.getAlbum()?.images?.filter { item -> item.isDelete() })
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.clTitleBar.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)

            val layoutParams1 = binding.btnRestoreAll.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams1.bottomMargin = systemBars.bottom + AndroidUtils.dpToPx(4)
            binding.btnRestoreAll.layoutParams = layoutParams1

            val layoutParams2 = binding.btnGotIt.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams2.bottomMargin = systemBars.bottom + AndroidUtils.dpToPx(4)
            binding.btnGotIt.layoutParams = layoutParams2

            WindowInsetsCompat.CONSUMED
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (binding.vLoading.isVisible) {
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun showPhoto(imageView: ImageView, index: Int) {
        Glide
            .with(this)
            .load(mAdapter.images[index].sourceUri)
            .into(imageView)
    }

    override fun getRecyclerView(): RecyclerView {
        return binding.rvPhotos
    }

    override fun getUri(position: Int): Uri? {
        return mAdapter.images[position].sourceUri
    }

    override fun getImageView(position: Int): ImageView? {
        val holder = binding.rvPhotos.findViewHolderForAdapterPosition(position)
        return if (holder is RecyclerBinAdapter.MyViewHolder) {
            holder.binding.ivCover
        } else {
            null
        }
    }

    private fun init() {
        binding.ivBack.setOnClickListener { finish() }

        mRecyclerBinViewModel.cleanCompletedLiveData.observe(this) {
            binding.vLoading.visibility = View.GONE
            showDeleteResult()
        }

        mRecyclerBinViewModel.restoreImageLiveData.observe(this) {
            mAdapter.images.forEach { it.doKeep() }

            val spendTime = SystemClock.elapsedRealtime() - mStartRestoreTime
            binding.vLoading.postDelayed(
                {
                    binding.vLoading.visibility = View.GONE
                    finish()
                },
                MIN_SHOW_LOADING_TIME - spendTime
            )
        }
    }

    private fun showDeletedImages(deletedImages: List<Image>?) {
        if (deletedImages.isNullOrEmpty()) {
            finish()
            return
        }
        Collections.reverse(deletedImages)
        mAdapter = RecyclerBinAdapter(
            deletedImages.toMutableList(),
            { image, position ->
                mAdapter.notifyItemRemoved(position)
                mAdapter.removeImage(image)
                showTotalSize(mAdapter.getTotalSize())
                image.doKeep()
                mRecyclerBinViewModel.converseDeleteToKeepImage(image)

                if (mAdapter.images.isEmpty()) {
                    finish()
                }
            }, { photoImageView, photo, position ->
                photo.sourceUri?.let {
                    PhotoViewFragment.show(
                        this,
                        position,
                        mAdapter.images.size,
                        it,
                        photoImageView
                    )
                }
            }
        )

        val spanCount =
            3.coerceAtLeast((Resources.getSystem().displayMetrics.widthPixels / (140 * Resources.getSystem().displayMetrics.density)).toInt())
        val layoutManager = GridLayoutManager(this, spanCount)

        binding.rvPhotos.adapter = mAdapter
        binding.rvPhotos.layoutManager = layoutManager

        showTotalSize(mAdapter.getTotalSize())

        binding.btnEmptyTrash.setOnClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.delete_picture)
                    .setMessage(R.string.dialog_delete_picture_message)
                    .setPositiveButton(
                        R.string.delete
                    ) { _, _ ->
                        if (PermissionUtils.checkWritePermission(this)) {
                            useOldDelete()

                        } else {
                            PermissionUtils.getWritePermission(
                                this,
                                mOldDeleteLauncher1,
                                mOldDeleteLauncher2
                            )
                        }
                    }
                    .setNegativeButton(R.string.cancel) { _, _ -> }
                    .show()

            } else {
                mNewDeleteLauncher.launch(
                    IntentSenderRequest.Builder(
                        MediaStore.createDeleteRequest(
                            contentResolver,
                            mAdapter.images.map { it.sourceUri }).intentSender
                    ).build()
                )
            }
        }

        binding.btnRestoreAll.setOnClickListener {
            binding.vLoading.visibility = View.VISIBLE
            mStartRestoreTime = SystemClock.elapsedRealtime()
            showTotalSize(0)
            binding.rvPhotos.visibility = View.GONE

            mRecyclerBinViewModel.converseDeleteToKeepImage(mAdapter.images)
        }
    }

    private fun showDeleteResult() {
        (findViewById<TextView>(R.id.tv_free_up_size)!!).text =
            getHumanFriendlyByteCount(mAdapter.getTotalSize(), 1)
        (findViewById<TextView>(R.id.tv_deleted_count)!!).text = resources.getQuantityString(
            R.plurals.picture_count,
            mAdapter.images.size,
            mAdapter.images.size
        )

        findViewById<View>(R.id.cl_trash_bin).visibility = View.GONE
        findViewById<View>(R.id.cl_complete).visibility = View.VISIBLE

        binding.tvTitle.text = mRecyclerBinViewModel.getAlbum()?.formatData
        findViewById<View>(R.id.btn_got_it).setOnClickListener { finish() }
    }

    private fun showTotalSize(totalSize: Long) {
        binding.tvTitle.text =
            getString(R.string.trash_bin_size, getHumanFriendlyByteCount(totalSize, 1))
    }

    private fun useOldDelete() {
        binding.vLoading.visibility = View.VISIBLE
        mRecyclerBinViewModel.cleanCompletedImagesUseOld(
            mAdapter,
            this@RecycleBinActivity,
            contentResolver
        )
    }
}