package com.flux740.app.ui.book.manga.config

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.flux740.app.R
import com.flux740.app.base.BaseDialogFragment
import com.flux740.app.databinding.DialogMangaEpaperBinding
import com.flux740.app.help.config.AppConfig
import com.flux740.app.utils.setLayout
import com.flux740.app.utils.viewbindingdelegate.viewBinding

class MangaEpaperDialog : BaseDialogFragment(R.layout.dialog_manga_epaper) {
    private val binding by viewBinding(DialogMangaEpaperBinding::bind)
    private val callback get() = activity as? Callback
    private var mMangaEInkThreshold = 150
    override fun onStart() {
        super.onStart()
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initData()
        initView()
    }

    private fun initData() {
        binding.dsbEpaper.progress = AppConfig.mangaEInkThreshold
    }

    private fun initView() {
        binding.dsbEpaper.onChanged = {
            mMangaEInkThreshold = it
            callback?.updateEepaper(it)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        AppConfig.mangaEInkThreshold = mMangaEInkThreshold
    }

    interface Callback {
        fun updateEepaper(value: Int)
    }

}