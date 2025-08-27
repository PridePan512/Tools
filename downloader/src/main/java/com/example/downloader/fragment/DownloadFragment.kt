package com.example.downloader.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.downloader.R
import com.example.downloader.activity.MainActivity
import com.example.downloader.adapter.TaskDetectAdapter
import com.example.downloader.business.LibHelper
import com.example.downloader.business.model.YtDlpException
import com.example.downloader.business.model.YtDlpRequest
import com.example.downloader.dialog.ClipboardDialogFragment
import com.example.downloader.model.DownloadInfo
import com.example.downloader.model.DownloadState
import com.example.downloader.model.eventbus.FillUrlEvent
import com.example.downloader.model.eventbus.ItemDownloadErrorEvent
import com.example.downloader.model.eventbus.ItemDownloadedEvent
import com.example.downloader.model.eventbus.ItemDownloadingEvent
import com.example.downloader.model.eventbus.StartServiceDownloadEvent
import com.example.downloader.service.DownloadService
import com.example.downloader.utils.DownloadUtil
import com.example.lib.utils.AndroidUtils
import com.example.lib.utils.PermissionUtils
import com.example.lib.utils.StringUtils
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class DownloadFragment : Fragment() {

    private val TAG = "DownloadFragment"
    private val TAG_SHOW_CLIPBOARD_DIALOG: String = "show_clipboard_dialog"

    @Volatile
    private var detectingTaskCount: Int = 0

    private lateinit var mAdapter: TaskDetectAdapter
    private lateinit var mUrlEditText: TextInputEditText
    private lateinit var mProgressbar: ProgressBar
    private lateinit var mRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onDestroy() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        super.onDestroy()
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_download, container, false)
        initView(view)
        return view
    }

    override fun onStart() {
        super.onStart()
        checkClipBoard()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessage(event: FillUrlEvent) {
        searchUrl(event.url)
        mUrlEditText.setText(event.url)

        val activity = activity
        if (activity is MainActivity) {
            activity.selectDownloadIndex()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessage(event: ItemDownloadingEvent) {
        event.videoTask.state = DownloadState.DOWNLOADING
        mAdapter.notifyItemChanged(
            mAdapter.getPositionById(event.videoTask.id),
            DownloadInfo(event.downloadInfo.progress, event.downloadInfo.speed)
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessage(event: ItemDownloadedEvent) {
        event.videoTask.state = DownloadState.DOWNLOADED
        mAdapter.notifyItemChanged(
            mAdapter.getPositionById(event.videoTask.id),
            TaskDetectAdapter.FLAG_UPDATE_STATE
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessage(event: ItemDownloadErrorEvent) {
        event.videoTask.state = DownloadState.DOWNLOAD_FAILED
        event.videoTask.error = event.error
        mAdapter.notifyItemChanged(
            mAdapter.getPositionById(event.videoTask.id),
            TaskDetectAdapter.FLAG_UPDATE_STATE
        )
    }

    private fun initView(view: View) {
        val context = view.context ?: return
        mProgressbar = view.findViewById(R.id.v_progressbar)
        mUrlEditText = view.findViewById(R.id.v_input_edittext)
        mRecyclerView = view.findViewById<RecyclerView>(R.id.v_recyclerview)
        //防止局部刷新引起闪烁
        mRecyclerView.setItemAnimator(null)

        mAdapter = TaskDetectAdapter()
        mAdapter.onDownloadClick = { videoTask, position ->
            val videoInfo = videoTask.videoInfo

            if (videoInfo.getSize() > AndroidUtils.getAvailableInternalStorageSize()) {
                //存储空间不足
                Toast.makeText(context, R.string.insufficient_storage_space, Toast.LENGTH_SHORT)
                    .show()

            } else {
                if (!TextUtils.isEmpty(videoInfo.webpageUrl)) {
                    videoTask.state = DownloadState.DOWNLOADING
                    mAdapter.notifyItemChanged(
                        mAdapter.getPositionById(videoTask.id),
                        TaskDetectAdapter.FLAG_UPDATE_STATE
                    )
                    //在安卓13及以上，如果没有通知权限，无法启动前台服务
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !PermissionUtils.checkNotificationPermission(
                            context
                        )
                    ) {
                        DownloadUtil.downloadVideo(videoTask, lifecycleScope)

                    } else {
                        val service = Intent(context, DownloadService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(service)

                        } else {
                            context.startService(service)
                        }
                        EventBus.getDefault().postSticky(StartServiceDownloadEvent(videoTask))
                    }

                } else {
                    Toast.makeText(context, R.string.url_is_empty, Toast.LENGTH_SHORT).show()
                }
            }
        }
        mRecyclerView.adapter = mAdapter
        val layoutManager = LinearLayoutManager(context)
        layoutManager.reverseLayout = true
        layoutManager.stackFromEnd = true
        mRecyclerView.layoutManager = layoutManager

        mUrlEditText.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                AndroidUtils.hideKeyboard(context, mUrlEditText)
                mUrlEditText.clearFocus()
                searchUrl(mUrlEditText.text.toString())
                true
            } else {
                false
            }
        }
    }

    private fun searchUrl(url: String) {

        if (TextUtils.isEmpty(url)) {
            Toast.makeText(context, R.string.url_is_empty, Toast.LENGTH_SHORT).show()
            return
        }

        if (!StringUtils.isValidWebUrl(url)) {
            Toast.makeText(context, R.string.url_is_illegal, Toast.LENGTH_SHORT).show()
            return
        }

        if (!AndroidUtils.isNetworkAvailable(requireContext())) {
            Toast.makeText(context, R.string.network_not_available, Toast.LENGTH_SHORT).show()
            return
        }

        detectingTaskCount++
        mProgressbar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = YtDlpRequest(url)
                val videoInfo = LibHelper.getVideoInfo(request)
                withContext(Dispatchers.Main) {
                    mAdapter.insertTask(videoInfo)
                    mRecyclerView.scrollToPosition(mAdapter.itemCount - 1)
                }

            } catch (e: YtDlpException) {
                Log.e(TAG, "Get task info error: $e")
                withContext(Dispatchers.Main) {
                    // TODO: 处理失败的情况
                    Toast.makeText(context, R.string.failed, Toast.LENGTH_SHORT).show()
                }

            } finally {
                withContext(Dispatchers.Main) {
                    detectingTaskCount--
                    mProgressbar.visibility = if (detectingTaskCount == 0) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }
                }
            }
        }
    }

    private fun checkClipBoard() {
        val dialogFragment = childFragmentManager.findFragmentByTag(TAG_SHOW_CLIPBOARD_DIALOG)
        if (dialogFragment is ClipboardDialogFragment) {
            dialogFragment.dismissAllowingStateLoss()
        }

        //延时 因为从安卓10开始 当app没有获取到焦点时拿不到粘贴板的内容
        lifecycleScope.launch {
            delay(500)
            context?.let {
                val textFromClipboard = AndroidUtils.getTextFromClipboard(it)
                val standardUrl = StringUtils.processUrl(textFromClipboard)
                if (StringUtils.isValidWebUrl(standardUrl)) {
                    val dialogFragment = ClipboardDialogFragment.newInstance(standardUrl)
                    dialogFragment.isCancelable = false
                    dialogFragment.show(childFragmentManager, TAG_SHOW_CLIPBOARD_DIALOG)
                }
            }
        }
    }
}