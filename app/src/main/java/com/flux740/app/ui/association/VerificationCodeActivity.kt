package com.flux740.app.ui.association

import android.os.Bundle
import com.flux740.app.base.BaseActivity
import com.flux740.app.constant.SourceType
import com.flux740.app.databinding.ActivityTranslucenceBinding
import com.flux740.app.utils.showDialogFragment
import com.flux740.app.utils.viewbindingdelegate.viewBinding

/**
 * 验证码
 */
class VerificationCodeActivity :
    BaseActivity<ActivityTranslucenceBinding>() {

    override val binding by viewBinding(ActivityTranslucenceBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        intent.getStringExtra("imageUrl")?.let {
            val sourceOrigin = intent.getStringExtra("sourceOrigin")
            val sourceName = intent.getStringExtra("sourceName")
            val sourceType = intent.getIntExtra("sourceType", SourceType.book)
            showDialogFragment(
                VerificationCodeDialog(it, sourceOrigin, sourceName, sourceType)
            )
        } ?: finish()
    }

}