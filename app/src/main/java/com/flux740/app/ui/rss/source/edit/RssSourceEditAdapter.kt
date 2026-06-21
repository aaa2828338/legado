package com.flux740.app.ui.rss.source.edit

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.flux740.app.R
import com.flux740.app.databinding.ItemSourceEditBinding
import com.flux740.app.databinding.ItemSourceEditCheckBoxBinding
import com.flux740.app.help.config.AppConfig
import com.flux740.app.ui.widget.code.addJsPattern
import com.flux740.app.ui.widget.code.addJsonPattern
import com.flux740.app.ui.widget.code.addLegadoPattern
import com.flux740.app.ui.widget.text.EditEntity
import com.flux740.app.utils.isTrue

class RssSourceEditAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val editEntityMaxLine = AppConfig.sourceEditMaxLine

    var editEntities: ArrayList<EditEntity> = ArrayList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemViewType(position: Int): Int {
        val item = editEntities[position]
        return item.viewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            EditEntity.ViewType.checkBox -> {
                val binding = ItemSourceEditCheckBoxBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
                CheckBoxViewHolder(binding)
            }

            else -> {
                val binding = ItemSourceEditBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
                binding.editText.addLegadoPattern()
                binding.editText.addJsonPattern()
                binding.editText.addJsPattern()
                EditTextViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CheckBoxViewHolder -> holder.bind(editEntities[position])
            is EditTextViewHolder -> holder.bind(editEntities[position])
        }
    }

    override fun getItemCount(): Int {
        return editEntities.size
    }

    inner class EditTextViewHolder(val binding: ItemSourceEditBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(editEntity: EditEntity) = binding.run {
            editText.maxLines = editEntityMaxLine
            if (editText.getTag(R.id.tag1) == null) {
                val listener = object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        editText.isCursorVisible = false
                        editText.isCursorVisible = true
                        editText.isFocusable = true
                        editText.isFocusableInTouchMode = true
                    }

                    override fun onViewDetachedFromWindow(v: View) {

                    }
                }
                editText.addOnAttachStateChangeListener(listener)
                editText.setTag(R.id.tag1, listener)
            }
            editText.getTag(R.id.tag2)?.let {
                if (it is TextWatcher) {
                    editText.removeTextChangedListener(it)
                }
            }
            editText.setText(editEntity.value)
            textInputLayout.hint = editEntity.hint
            val textWatcher = object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

                }

                override fun afterTextChanged(s: Editable?) {
                    editEntity.value = (s?.toString())
                }
            }
            editText.addTextChangedListener(textWatcher)
            editText.setTag(R.id.tag2, textWatcher)
            editText.clearFocus()
        }
    }

    class CheckBoxViewHolder(val binding: ItemSourceEditCheckBoxBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(editEntity: EditEntity) = binding.run {
            checkBox.text = editEntity.hint
            checkBox.isChecked = editEntity.value.isTrue()
            checkBox.setOnUserCheckedChangeListener { isChecked ->
                editEntity.value = isChecked.toString()
            }
        }

    }


}