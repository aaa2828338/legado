package com.flux740.app.ui.login

import android.os.Bundle
import androidx.activity.viewModels
import com.flux740.app.R
import com.flux740.app.base.VMBaseActivity
import com.flux740.app.data.entities.BaseSource
import com.flux740.app.databinding.ActivitySourceLoginBinding
import com.flux740.app.utils.showDialogFragment
import com.flux740.app.utils.viewbindingdelegate.viewBinding


class SourceLoginActivity : VMBaseActivity<ActivitySourceLoginBinding, SourceLoginViewModel>() {

    override val binding by viewBinding(ActivitySourceLoginBinding::inflate)
    override val viewModel by viewModels<SourceLoginViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.initData(intent, success = { source ->
            initView(source)
        }, error = {
            finish()
        })
    }

    private fun initView(source: BaseSource) {
        if (source.loginUi.isNullOrEmpty()) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fl_fragment, WebViewLoginFragment(), "webViewLogin")
                .commit()
        } else {
            showDialogFragment<SourceLoginDialog>()
        }
    }

}