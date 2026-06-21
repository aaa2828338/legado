package com.flux740.app.ui.book.group

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flux740.app.R
import com.flux740.app.base.BaseDialogFragment
import com.flux740.app.base.adapter.ItemViewHolder
import com.flux740.app.base.adapter.RecyclerAdapter
import com.flux740.app.constant.AppLog
import com.flux740.app.data.appDb
import com.flux740.app.data.entities.BookGroup
import com.flux740.app.databinding.DialogRecyclerViewBinding
import com.flux740.app.databinding.ItemBookGroupManageBinding
import com.flux740.app.lib.theme.accentColor
import com.flux740.app.lib.theme.backgroundColor
import com.flux740.app.lib.theme.primaryColor
import com.flux740.app.ui.widget.recycler.ItemTouchCallback
import com.flux740.app.ui.widget.recycler.VerticalDivider
import com.flux740.app.utils.applyTint
import com.flux740.app.utils.setLayout
import com.flux740.app.utils.showDialogFragment
import com.flux740.app.utils.toastOnUi
import com.flux740.app.utils.viewbindingdelegate.viewBinding
import com.flux740.app.utils.visible
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * 书籍分组管理
 */
class GroupManageDialog : BaseDialogFragment(R.layout.dialog_recycler_view),
    Toolbar.OnMenuItemClickListener {

    private val viewModel: GroupViewModel by viewModels()
    private val binding by viewBinding(DialogRecyclerViewBinding::bind)
    private val adapter by lazy { GroupAdapter(requireContext()) }

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, 0.9f)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.setTitle(R.string.group_manage)
        initView()
        initData()
        initMenu()
    }

    private fun initView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.addItemDecoration(VerticalDivider(requireContext()))
        binding.recyclerView.adapter = adapter
        val itemTouchCallback = ItemTouchCallback(adapter)
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.recyclerView)
        binding.tvOk.setTextColor(requireContext().accentColor)
        binding.tvOk.visible()
        binding.tvOk.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    private fun initData() {
        lifecycleScope.launch {
            appDb.bookGroupDao.flowAll().catch {
                AppLog.put("书籍分组管理界面获取分组数据失败\n${it.localizedMessage}", it)
            }.flowOn(IO).conflate().collect {
                adapter.setItems(it)
            }
        }
    }

    private fun initMenu() {
        binding.toolBar.setOnMenuItemClickListener(this)
        binding.toolBar.inflateMenu(R.menu.book_group_manage)
        binding.toolBar.menu.applyTint(requireContext())
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_add -> {
                if (appDb.bookGroupDao.canAddGroup) {
                    showDialogFragment(GroupEditDialog())
                } else {
                    toastOnUi("分组已达上限(64个)")
                }
            }
        }
        return true
    }

    private inner class GroupAdapter(context: Context) :
        RecyclerAdapter<BookGroup, ItemBookGroupManageBinding>(context),
        ItemTouchCallback.Callback {

        private var isMoved = false

        override fun getViewBinding(parent: ViewGroup): ItemBookGroupManageBinding {
            return ItemBookGroupManageBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemBookGroupManageBinding,
            item: BookGroup,
            payloads: MutableList<Any>
        ) {
            binding.run {
                root.setBackgroundColor(context.backgroundColor)
                tvGroup.text = item.getManageName(context)
                swShow.isChecked = item.show
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemBookGroupManageBinding) {
            binding.run {
                tvEdit.setOnClickListener {
                    getItem(holder.layoutPosition)?.let { bookGroup ->
                        showDialogFragment(
                            GroupEditDialog(bookGroup)
                        )
                    }
                }
                swShow.setOnUserCheckedChangeListener { isChecked ->
                    getItem(holder.layoutPosition)?.let {
                        viewModel.upGroup(it.copy(show = isChecked))
                    }
                }
            }
        }

        override fun swap(srcPosition: Int, targetPosition: Int): Boolean {
            swapItem(srcPosition, targetPosition)
            isMoved = true
            return true
        }

        override fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            if (isMoved) {
                for ((index, item) in getItems().withIndex()) {
                    item.order = index + 1
                }
                viewModel.upGroup(*getItems().toTypedArray())
            }
            isMoved = false
        }
    }

}