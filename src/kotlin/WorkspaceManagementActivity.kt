package com.venside.x1n

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.venside.x1n.utils.DialogHelper
import com.venside.x1n.utils.FileUtils
import com.venside.x1n.utils.StorageHelper
import java.io.File

class WorkspaceManagementActivity : Activity() {

    private var vensIDEBaseDir: File? = null
    private var workspaceList: MutableList<File> = mutableListOf()
    private lateinit var workspaceAdapter: WorkspaceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workspace_management)

        initVensIDEDirectory()
        setupUI()
        loadWorkspaces()
    }

    private fun initVensIDEDirectory() {
        vensIDEBaseDir = StorageHelper.initVensIDEDirectory(this)
        if (vensIDEBaseDir == null) {
            Toast.makeText(this, "目录初始化失败", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupUI() {
        // 返回按钮
        findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            finish()
        }

        // 标题
        findViewById<TextView>(R.id.tv_title)?.text = "工作区管理"

        // 创建工作区按钮
        findViewById<Button>(R.id.btn_create_workspace)?.setOnClickListener {
            showCreateWorkspaceDialog()
        }
    }

    private fun loadWorkspaces() {
        workspaceList.clear()
        vensIDEBaseDir?.listFiles()?.let { files ->
            for (file in files) {
                if (file.isDirectory) {
                    workspaceList.add(file)
                }
            }
        }

        workspaceList.sortByDescending { it.lastModified() }

        val listView = findViewById<ListView>(R.id.workspace_list)
        workspaceAdapter = WorkspaceAdapter(this, workspaceList)
        listView.adapter = workspaceAdapter

        // 长按显示菜单
        listView.setOnItemLongClickListener { _, _, position, _ ->
            showWorkspaceOptions(workspaceList[position])
            true
        }

        // 单击打开工作区
        listView.setOnItemClickListener { _, _, position, _ ->
            openWorkspace(workspaceList[position])
        }

        updateEmptyView()
    }

    private fun updateEmptyView() {
        val emptyView = findViewById<LinearLayout>(R.id.empty_view)
        val listView = findViewById<ListView>(R.id.workspace_list)

        if (workspaceList.isEmpty()) {
            emptyView?.visibility = View.VISIBLE
            listView?.visibility = View.GONE
        } else {
            emptyView?.visibility = View.GONE
            listView?.visibility = View.VISIBLE
        }
    }

    private fun showCreateWorkspaceDialog() {
        DialogHelper.showInputDialog(
            context = this,
            title = "创建工作区",
            hint = "输入工作区名称"
        ) { workspaceName ->
            createWorkspace(workspaceName)
        }
    }

    private fun createWorkspace(name: String) {
        val baseDir = vensIDEBaseDir ?: run {
            Toast.makeText(this, "工作区目录未初始化", Toast.LENGTH_SHORT).show()
            return
        }

        FileUtils.createFolder(baseDir, name)
            .onSuccess {
                Toast.makeText(this, "工作区创建成功", Toast.LENGTH_SHORT).show()
                loadWorkspaces()
            }
            .onFailure { e ->
                val message = when (e) {
                    is java.nio.file.FileAlreadyExistsException -> "工作区已存在"
                    else -> "工作区创建失败: ${e.message}"
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun showWorkspaceOptions(workspace: File) {
        val options = arrayOf("重命名", "删除")

        DialogHelper.showOptionsDialog(
            context = this,
            title = "工作区选项",
            options = options
        ) { which ->
            when (which) {
                0 -> showRenameDialog(workspace)
                1 -> showDeleteDialog(workspace)
            }
        }
    }

    private fun showRenameDialog(workspace: File) {
        DialogHelper.showInputDialog(
            context = this,
            title = "重命名工作区",
            hint = workspace.name,
            defaultValue = workspace.name
        ) { newName ->
            renameWorkspace(workspace, newName)
        }
    }

    private fun renameWorkspace(workspace: File, newName: String) {
        if (newName == workspace.name) {
            Toast.makeText(this, "请输入新的工作区名称", Toast.LENGTH_SHORT).show()
            return
        }

        FileUtils.renameFile(workspace, newName)
            .onSuccess {
                Toast.makeText(this, "工作区重命名成功", Toast.LENGTH_SHORT).show()
                loadWorkspaces()
            }
            .onFailure { e ->
                val message = when (e) {
                    is java.nio.file.FileAlreadyExistsException -> "工作区名称已存在"
                    else -> "工作区重命名失败: ${e.message}"
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteDialog(workspace: File) {
        DialogHelper.showConfirmDialog(
            context = this,
            title = "删除工作区",
            message = "确定要删除工作区 \"${workspace.name}\" 吗？此操作不可恢复。",
            positiveText = "删除",
            onConfirm = {
                deleteWorkspace(workspace)
            }
        )
    }

    private fun deleteWorkspace(workspace: File) {
        FileUtils.deleteFile(workspace)
            .onSuccess {
                Toast.makeText(this, "工作区删除成功", Toast.LENGTH_SHORT).show()
                loadWorkspaces()
            }
            .onFailure { e ->
                Toast.makeText(this, "工作区删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openWorkspace(workspace: File) {
        Toast.makeText(this, "打开工作区: ${workspace.name}", Toast.LENGTH_SHORT).show()
        // TODO: 实现打开工作区的功能
    }

    // 工作区适配器
    private class WorkspaceAdapter(
        context: Activity,
        private val workspaces: List<File>
    ) : ArrayAdapter<File>(context, R.layout.item_workspace, workspaces) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_workspace, parent, false)

            val workspace = workspaces[position]

            view.findViewById<TextView>(R.id.tv_workspace_name)?.text = workspace.name

            // 显示创建时间
            val lastModified = java.util.Date(workspace.lastModified())
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            view.findViewById<TextView>(R.id.tv_workspace_date)?.text = formatter.format(lastModified)

            return view
        }
    }
}