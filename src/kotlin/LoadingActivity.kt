package com.venside.x1n

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import java.io.File

class LoadingActivity : Activity() {

    private val LOADING_DELAY = 1500L // 1.5秒的加载动画

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        val workspacePath = intent.getStringExtra("workspace_path")

        if (workspacePath != null) {
            // 模拟加载过程
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, WorkspaceActivity::class.java)
                intent.putExtra("workspace_path", workspacePath)
                startActivity(intent)
                finish()
            }, LOADING_DELAY)
        } else {
            // 如果没有工作区路径，返回主界面
            finish()
        }
    }
}