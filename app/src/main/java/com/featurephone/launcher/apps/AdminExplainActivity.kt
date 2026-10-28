package com.featurephone.launcher.apps

import android.os.Bundle
import com.featurephone.launcher.databinding.ActivityAdminExplainBinding

class AdminExplainActivity : BaseActivity() {

    private lateinit var binding: ActivityAdminExplainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminExplainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCommonViews(
            root = binding.root,
            backButton = binding.btnBack,
            screenArea = binding.screenArea,
            keysArea = binding.keysArea,
            titleView = binding.tvTitle
        )
        applyColors()
    }

    override fun applyColors() {
        applyBaseColors(
            screenArea = binding.screenArea,
            keysArea = binding.keysArea,
            titleView = binding.tvTitle,
            backButton = binding.btnBack
        )
        // 遍历设置所有文字颜色
        val container = binding.screenArea.getChildAt(0) as android.view.ViewGroup
        applyTextColorToAllChildren(container, com.featurephone.launcher.ColorManager.getScreenText())
    }
}
