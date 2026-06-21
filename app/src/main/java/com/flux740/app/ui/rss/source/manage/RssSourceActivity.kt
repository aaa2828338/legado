package com.flux740.app.ui.rss.source.manage

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import com.flux740.app.R
import com.flux740.app.base.VMBaseActivity
import com.flux740.app.constant.AppLog
import com.flux740.app.data.appDb
import com.flux740.app.data.entities.RssSource
import com.flux740.app.databinding.ActivityRssSourceBinding
import com.flux740.app.databinding.DialogEditTextBinding
import com.flux740.app.help.DirectLinkUpload
import com.flux740.app.lib.dialogs.alert
import com.flux740.app.lib.theme.primaryColor
import com.flux740.app.lib.theme.primaryTextColor
import com.flux740.app.ui.association.ImportRssSourceDialog
import com.flux740.app.ui.file.HandleFileContract
import com.flux740.app.ui.qrcode.QrCodeResult
import com.flux740.app.ui.rss.source.edit.RssSourceEditActivity
import com.flux740.app.ui.widget.SelectActionBar
import com.flux740.app.ui.widget.recycler.DragSelectTouchHelper
import com.flux740.app.ui.widget.recycler.ItemTouchCallback
import com.flux740.app.ui.widget.recycler.VerticalDivider
import com.flux740.app.utils.ACache
import com.flux740.app.utils.applyTint
import com.flux740.app.utils.dpToPx
import com.flux740.app.utils.isAbsUrl
import com.flux740.app.utils.launch
import com.flux740.app.utils.sendToClip
import com.flux740.app.utils.setEdgeEffectColor
import com.flux740.app.utils.share
import com.flux740.app.utils.showDialogFragment
import com.flux740.app.utils.showHelp
import com.flux740.app.utils.splitNotBlank
import com.flux740.app.utils.startActivity
import com.flux740.app.utils.transaction
import com.flux740.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * 订阅源管理
 */
class RssSourceActivity : VMBaseActivity<ActivityRssSourceBinding, RssSourceViewModel>(),
    PopupMenu.OnMenuItemClickListener,
    SelectActionBar.CallBack,
    RssSourceAdapter.CallBack {

    override val binding by viewBinding(ActivityRssSourceBinding::inflate)
    override val viewModel by viewModels<RssSourceViewModel>()
    private val importRecordKey = "rssSourceRecordKey"
    private val adapter by lazy { RssSourceAdapter(this, this) }
    private val searchView: SearchView by lazy {
        binding.titleBar.findViewById(R.id.search_view)
    }
    private var sourceFlowJob: Job? = null
    private var groups = arrayListOf<String>()
    private var groupMenu: SubMenu? = null
    private val qrCodeResult = registerForActivityResult(QrCodeResult()) {
        it ?: return@registerForActivityResult
        showDialogFragment(ImportRssSourceDialog(it))
    }
    private val importDoc = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            showDialogFragment(ImportRssSourceDialog(uri.toString()))
        }
    }
    private val exportResult = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            alert(R.string.export_success) {
                if (uri.toString().isAbsUrl()) {
                    setMessage(DirectLinkUpload.getSummary())
                }
                val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                    editView.hint = getString(R.string.path)
                    editView.setText(uri.toString())
                }
                customView { alertBinding.root }
                okButton {
                    sendToClip(uri.toString())
                }
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
        initSearchView()
        initGroupFlow()
        upSourceFlow()
        initSelectActionBar()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.rss_source, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        groupMenu = menu.findItem(R.id.menu_group)?.subMenu
        upGroupMenu()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add -> startActivity<RssSourceEditActivity>()
            R.id.menu_import_local -> importDoc.launch {
                mode = HandleFileContract.FILE
                allowExtensions = arrayOf("txt", "json")
            }

            R.id.menu_import_onLine -> showImportDialog()
            R.id.menu_import_qr -> qrCodeResult.launch()
            R.id.menu_group_manage -> showDialogFragment<GroupManageDialog>()
            R.id.menu_import_default -> viewModel.importDefault()
            R.id.menu_enabled_group -> {
                searchView.setQuery(getString(R.string.enabled), true)
            }

            R.id.menu_disabled_group -> {
                searchView.setQuery(getString(R.string.disabled), true)
            }

            R.id.menu_group_login -> {
                searchView.setQuery(getString(R.string.need_login), true)
            }

            R.id.menu_group_null -> {
                searchView.setQuery(getString(R.string.no_group), true)
            }

            R.id.menu_help -> showHelp("SourceMRssHelp")
            else -> if (item.groupId == R.id.source_group) {
                searchView.setQuery("group:${item.title}", true)
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_enable_selection -> viewModel.enableSelection(adapter.selection)
            R.id.menu_disable_selection -> viewModel.disableSelection(adapter.selection)
            R.id.menu_add_group -> selectionAddToGroups()
            R.id.menu_remove_group -> selectionRemoveFromGroups()
            R.id.menu_top_sel -> viewModel.topSource(*adapter.selection.toTypedArray())
            R.id.menu_bottom_sel -> viewModel.bottomSource(*adapter.selection.toTypedArray())
            R.id.menu_export_selection -> viewModel.saveToFile(adapter.selection) { file, name ->
                exportResult.launch {
                    mode = HandleFileContract.EXPORT
                    fileData = HandleFileContract.FileData(
                        name, file, "application/json"
                    )
                }
            }

            R.id.menu_share_source -> viewModel.saveToFile(adapter.selection) { file, name ->
                share(file)
            }

            R.id.menu_check_selected_interval -> adapter.checkSelectedInterval()
        }
        return true
    }

    private fun initRecyclerView() {
        binding.recyclerView.setEdgeEffectColor(primaryColor)
        binding.recyclerView.addItemDecoration(VerticalDivider(this))
        binding.recyclerView.adapter = adapter
        // When this page is opened, it is in selection mode
        val dragSelectTouchHelper: DragSelectTouchHelper =
            DragSelectTouchHelper(adapter.dragSelectCallback).setSlideArea(16, 50)
        dragSelectTouchHelper.attachToRecyclerView(binding.recyclerView)
        dragSelectTouchHelper.activeSlideSelect()
        // Note: need judge selection first, so add ItemTouchHelper after it.
        val itemTouchCallback = ItemTouchCallback(adapter)
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.recyclerView)
    }

    private fun initSearchView() {
        binding.titleBar.findViewById<SearchView>(R.id.search_view).let {
            it.applyTint(primaryTextColor)
            it.onActionViewExpanded()
            it.queryHint = getString(R.string.search_rss_source)
            it.clearFocus()
            it.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    upSourceFlow(newText)
                    return false
                }
            })
        }
    }

    private fun initSelectActionBar() {
        binding.selectActionBar.setMainActionText(R.string.delete)
        binding.selectActionBar.inflateMenu(R.menu.rss_source_sel)
        binding.selectActionBar.setOnMenuItemClickListener(this)
        binding.selectActionBar.setCallBack(this)
    }

    private fun initGroupFlow() {
        lifecycleScope.launch {
            appDb.rssSourceDao.flowGroups().conflate().collect {
                groups.clear()
                groups.addAll(it)
                upGroupMenu()
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun selectionAddToGroups() {
        alert(titleResource = R.string.add_group) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.setHint(R.string.group_name)
                editView.setFilterValues(groups.toList())
                editView.dropDownHeight = 180.dpToPx()
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.let {
                    if (it.isNotEmpty()) {
                        viewModel.selectionAddToGroups(adapter.selection, it)
                    }
                }
            }
            cancelButton()
        }
    }

    @SuppressLint("InflateParams")
    private fun selectionRemoveFromGroups() {
        alert(titleResource = R.string.remove_group) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.setHint(R.string.group_name)
                editView.setFilterValues(groups.toList())
                editView.dropDownHeight = 180.dpToPx()
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.let {
                    if (it.isNotEmpty()) {
                        viewModel.selectionRemoveFromGroups(adapter.selection, it)
                    }
                }
            }
            cancelButton()
        }
    }

    override fun selectAll(selectAll: Boolean) {
        if (selectAll) {
            adapter.selectAll()
        } else {
            adapter.revertSelection()
        }
    }

    override fun revertSelection() {
        adapter.revertSelection()
    }

    override fun onClickSelectBarMainAction() {
        delSourceDialog()
    }

    private fun delSourceDialog() {
        alert(titleResource = R.string.draw, messageResource = R.string.sure_del) {
            yesButton { viewModel.del(*adapter.selection.toTypedArray()) }
            noButton()
        }
    }

    private fun upGroupMenu() = groupMenu?.transaction { menu ->
        menu.removeGroup(R.id.source_group)
        groups.forEach {
            menu.add(R.id.source_group, Menu.NONE, Menu.NONE, it)
        }
    }

    private fun upSourceFlow(searchKey: String? = null) {
        sourceFlowJob?.cancel()
        sourceFlowJob = lifecycleScope.launch {
            when {
                searchKey.isNullOrBlank() -> {
                    appDb.rssSourceDao.flowAll()
                }

                searchKey == getString(R.string.enabled) -> {
                    appDb.rssSourceDao.flowEnabled()
                }

                searchKey == getString(R.string.disabled) -> {
                    appDb.rssSourceDao.flowDisabled()
                }

                searchKey == getString(R.string.need_login) -> {
                    appDb.rssSourceDao.flowLogin()
                }

                searchKey == getString(R.string.no_group) -> {
                    appDb.rssSourceDao.flowNoGroup()
                }

                searchKey.startsWith("group:") -> {
                    val key = searchKey.substringAfter("group:")
                    appDb.rssSourceDao.flowGroupSearch(key)
                }

                else -> {
                    appDb.rssSourceDao.flowSearch(searchKey)
                }
            }.catch {
                AppLog.put("订阅源管理界面更新数据出错", it)
            }.flowOn(IO).conflate().collect {
                adapter.setItems(it, adapter.diffItemCallback)
                delay(100)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.upResumed(true)
    }

    override fun onPause() {
        adapter.upResumed(false)
        super.onPause()
    }

    override fun upCountView() {
        binding.selectActionBar.upCountView(
            adapter.selection.size,
            adapter.itemCount
        )
    }

    @SuppressLint("InflateParams")
    private fun showImportDialog() {
        val aCache = ACache.get(cacheDir = false)
        val cacheUrls: MutableList<String> = aCache
            .getAsString(importRecordKey)
            ?.splitNotBlank(",")
            ?.toMutableList() ?: mutableListOf()
        alert(titleResource = R.string.import_on_line) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "url"
                editView.setFilterValues(cacheUrls)
                editView.delCallBack = {
                    cacheUrls.remove(it)
                    aCache.put(importRecordKey, cacheUrls.joinToString(","))
                }
            }
            customView { alertBinding.root }
            okButton {
                val text = alertBinding.editView.text?.toString()
                text?.let {
                    if (it.isAbsUrl() && !cacheUrls.contains(it)) {
                        cacheUrls.add(0, it)
                        aCache.put(importRecordKey, cacheUrls.joinToString(","))
                    }
                    showDialogFragment(
                        ImportRssSourceDialog(it)
                    )
                }
            }
            cancelButton()
        }
    }

    override fun del(source: RssSource) {
        alert(R.string.draw) {
            setMessage(getString(R.string.sure_del) + "\n" + source.sourceName)
            noButton()
            yesButton {
                viewModel.del(source)
            }
        }
    }

    override fun edit(source: RssSource) {
        startActivity<RssSourceEditActivity> {
            putExtra("sourceUrl", source.sourceUrl)
        }
    }

    override fun update(vararg source: RssSource) {
        viewModel.update(*source)
    }

    override fun toTop(source: RssSource) {
        viewModel.topSource(source)
    }

    override fun toBottom(source: RssSource) {
        viewModel.bottomSource(source)
    }

    override fun upOrder() {
        viewModel.upOrder()
    }

}