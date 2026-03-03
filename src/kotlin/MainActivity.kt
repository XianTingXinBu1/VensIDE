package com.venside.x1n

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.venside.x1n.utils.DialogHelper
import com.venside.x1n.utils.StorageHelper
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
        vensIDEBaseDir = StorageHelper.initVensIDEDirectory(this)
        if (vensIDEBaseDir != null) {
            Toast.makeText(this, "工作区目录初始化成功", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "工作区目录创建失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupButtonListeners() {
        try {
            // 开始按钮 - 选择工作区或进入默认工作区
            findViewById<Button>(R.id.btn_start)?.setOnClickListener {
                if (!hasStoragePermission) {
                    Toast.makeText(this, "请先授予存储权限", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                showWorkspaceSelectionDialog()
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

    private fun showWorkspaceSelectionDialog() {
        vensIDEBaseDir?.let { baseDir ->
            val workspaces = baseDir.listFiles()?.filter { it.isDirectory } ?: emptyList()

            if (workspaces.isEmpty()) {
                // 没有工作区，使用默认工作区
                val defaultWorkspace = File(baseDir, "Default")
                if (!defaultWorkspace.exists()) {
                    defaultWorkspace.mkdirs()
                }
                openWorkspaceWithLoading(defaultWorkspace)
            } else {
                // 有工作区，显示选择对话框
                showWorkspaceDialog(workspaces)
            }
        }
    }

    private fun showWorkspaceDialog(workspaces: List<File>) {
        val workspaceNames = workspaces.map { it.name }.toTypedArray()

        DialogHelper.showOptionsDialog(
            context = this,
            title = "选择工作区",
            options = workspaceNames
        ) { which ->
            openWorkspaceWithLoading(workspaces[which])
        }
    }

    private fun openWorkspaceWithLoading(workspace: File) {
        val intent = Intent(this, LoadingActivity::class.java)
        intent.putExtra("workspace_path", workspace.absolutePath)
        startActivity(intent)
    }
}