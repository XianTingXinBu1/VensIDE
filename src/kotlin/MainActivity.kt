package com.venside.x1n

import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import java.io.File

class MainActivity : Activity() {

    private val STORAGE_PERMISSION_REQUEST_CODE = 100
    private var vensIDEBaseDir: File? = null
    private var hasStoragePermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // 使用 XML 布局文件
            setContentView(R.layout.activity_main)

            // 延迟设置按钮点击事件，确保视图加载完成
            window.decorView.post {
                setupButtonListeners()
            }

            // 请求存储权限
            requestStoragePermission()
        } catch (e: Exception) {
            Toast.makeText(this, "初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun requestStoragePermission() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                        arrayOf(
                            android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ),
                        STORAGE_PERMISSION_REQUEST_CODE
                    )
                } else {
                    // 权限已授予，初始化目录
                    hasStoragePermission = true
                    initVensIDEDirectory()
                }
            } else {
                // Android 6.0 以下不需要运行时权限
                hasStoragePermission = true
                initVensIDEDirectory()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "权限请求失败: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限授予成功，初始化目录
                hasStoragePermission = true
                initVensIDEDirectory()
            } else {
                hasStoragePermission = false
                Toast.makeText(this, "需要存储权限才能创建工作区", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun initVensIDEDirectory() {
        try {
            // Android 10+ 使用应用专属目录，避免权限问题
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vensIDEBaseDir = File(getExternalFilesDir(null), "VensIDE")
            } else {
                // Android 10 以下使用外部存储
                vensIDEBaseDir = File(Environment.getExternalStorageDirectory(), "VensIDE")
            }

            vensIDEBaseDir?.let { dir ->
                if (!dir.exists()) {
                    if (dir.mkdirs()) {
                        Toast.makeText(this, "工作区目录初始化成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "工作区目录创建失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "目录初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun setupButtonListeners() {
        try {
            // 开始按钮 - 创建工作区
            findViewById<Button>(R.id.btn_start)?.setOnClickListener {
                showCreateWorkspaceDialog()
            }

            // 设置按钮（暂未实现）
            findViewById<Button>(R.id.btn_settings)?.setOnClickListener {
                Toast.makeText(this, "设置功能暂未实现", Toast.LENGTH_SHORT).show()
            }

            // 关于按钮（暂未实现）
            findViewById<Button>(R.id.btn_about)?.setOnClickListener {
                Toast.makeText(this, "关于功能暂未实现", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "按钮初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun showCreateWorkspaceDialog() {
        try {
            if (!hasStoragePermission) {
                Toast.makeText(this, "请先授予存储权限", Toast.LENGTH_SHORT).show()
                requestStoragePermission()
                return
            }

            val editText = EditText(this).apply {
                hint = "输入工作区名称"
                setPadding(50, 30, 50, 30)
            }

            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 40, 50, 10)
                addView(editText)
            }

            AlertDialog.Builder(this)
                .setTitle("创建工作区")
                .setView(layout)
                .setPositiveButton("创建") { _, _ ->
                    val workspaceName = editText.text.toString().trim()
                    if (workspaceName.isNotEmpty()) {
                        createWorkspace(workspaceName)
                    } else {
                        Toast.makeText(this, "请输入工作区名称", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "对话框显示失败: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun createWorkspace(name: String) {
        try {
            val baseDir = vensIDEBaseDir ?: run {
                Toast.makeText(this, "工作区目录未初始化", Toast.LENGTH_SHORT).show()
                return
            }

            val workspaceDir = File(baseDir, name)

            if (workspaceDir.exists()) {
                Toast.makeText(this, "工作区已存在", Toast.LENGTH_SHORT).show()
                return
            }

            if (workspaceDir.mkdirs()) {
                Toast.makeText(this, "工作区创建成功：$name", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "工作区创建失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "工作区创建失败: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}