package com.example.easymedia.base

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.viewbinding.ViewBinding

abstract class BaseFragment<VB : ViewBinding, VM : ViewModel> : Fragment() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!
    protected abstract val viewModel: VM
    protected val savable = Bundle()
    private var loadingDialog: Dialog? = null
    abstract fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState
            ?.getBundle(SAVE_INSTANCE_STATE_KEY)
            ?.let { savable.putAll(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = inflateBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(savedInstanceState)
        initData()
    }

    open fun initView(savedInstanceState: Bundle?) {}
    open fun initData() {}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBundle(SAVE_INSTANCE_STATE_KEY, savable)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val SAVE_INSTANCE_STATE_KEY = "_save_state"
    }
}