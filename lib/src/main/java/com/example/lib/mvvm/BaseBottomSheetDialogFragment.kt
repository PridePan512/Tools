package com.example.lib.mvvm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.lang.reflect.ParameterizedType

abstract class BaseBottomSheetDialogFragment<VM : ViewModel, VB : ViewBinding> :
    BottomSheetDialogFragment() {

    protected lateinit var binding: VB
    protected lateinit var viewModel: VM

    abstract fun initView()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 初始化 ViewBinding
        binding = getViewBinding(inflater, container)
        // 初始化 ViewModel
        viewModel = ViewModelProvider(this)[getViewModelClass()]

        initView()
        return binding.root
    }

    /**
     * 通过反射获取 ViewBinding 实例
     */
    @Suppress("UNCHECKED_CAST")
    private fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB {
        // 获取泛型 VB (即 ViewBinding 类) 的类型
        val type =
            (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[1] as Class<VB>
        // 调用 inflate(inflater, container, false)，生成 ViewBinding 实例
        val method = type.getMethod(
            "inflate",
            LayoutInflater::class.java,
            ViewGroup::class.java,
            Boolean::class.java
        )
        return method.invoke(null, inflater, container, false) as VB
    }

    /**
     * 通过反射获取 ViewModel 的具体类型
     */
    @Suppress("UNCHECKED_CAST")
    private fun getViewModelClass(): Class<VM> {
        return (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<VM>
    }
}